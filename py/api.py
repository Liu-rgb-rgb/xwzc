import os
import shutil
import sys
import hashlib
import cv2
import numpy as np
import pyembroidery
import matplotlib.pyplot as plt
import sqlite3
from datetime import datetime
from fastapi import FastAPI, UploadFile, File, Request
from fastapi.responses import RedirectResponse
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware

# Windows 控制台默认 GBK 编码，打印 emoji/中文会 UnicodeEncodeError 导致请求 500
for _stream in (sys.stdout, sys.stderr):
    if hasattr(_stream, "reconfigure"):
        try:
            _stream.reconfigure(encoding="utf-8", errors="replace")
        except Exception:
            pass

# ================= 1. 初始化 FastAPI 和跨域配置 =================
app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# 输出目录 / 数据库路径可用环境变量覆盖：
#   本地开发：默认落在 py/ 下（static/、embroidery.db），行为不变
#   Docker 部署：Dockerfile 里指到 /mnt/workspace，重启后生成的文件不丢
_APP_DIR = os.path.dirname(os.path.abspath(__file__))
STATIC_DIR = os.environ.get("DST_STATIC_DIR") or os.path.join(_APP_DIR, "static")
DB_PATH = os.environ.get("DST_DB_PATH") or os.path.join(_APP_DIR, "embroidery.db")

os.makedirs(STATIC_DIR, exist_ok=True)
app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")


@app.get("/")
async def root():
    # 浏览器直接打开 Studio 落到 Swagger UI，方便演示 / 调试
    return RedirectResponse(url="/docs")


# ================= 2. 数据库初始化 =================
def init_db():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    cursor.execute(
        '''CREATE TABLE IF NOT EXISTS users (user_id INTEGER PRIMARY KEY AUTOINCREMENT, username VARCHAR(50), phone VARCHAR(20), created_at DATETIME)''')
    cursor.execute(
        '''CREATE TABLE IF NOT EXISTS products (product_id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR(100), price DECIMAL(10, 2), image_url VARCHAR(255))''')
    cursor.execute(
        '''CREATE TABLE IF NOT EXISTS embroidery_designs (design_id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, product_id INTEGER, prompt VARCHAR(255), preview_image_url VARCHAR(255), stitch_image_url VARCHAR(255), dst_file_url VARCHAR(255), stitch_count INTEGER, file_size VARCHAR(20), status INTEGER DEFAULT 1, created_at DATETIME)''')
    cursor.execute(
        '''CREATE TABLE IF NOT EXISTS orders (order_id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, design_id INTEGER, total_price DECIMAL(10, 2), order_status INTEGER DEFAULT 0, created_at DATETIME)''')

    conn.commit()
    conn.close()


init_db()


# ================= 3. 核心算法逻辑 =================
def get_file_hash(file_path):
    """计算文件的 SHA-256 数字指纹（分块读取，大文件不占内存）。"""
    sha256 = hashlib.sha256()
    with open(file_path, "rb") as f:
        for block in iter(lambda: f.read(65536), b""):
            sha256.update(block)
    return sha256.hexdigest()


def build_foreground_mask(image_path):
    """自适应提取花纹前景（255=需要绣花的花纹/叶片，0=背景）。

    AI 生成图是有渐变的彩色图，写死阈值会糊成整块或扫满全画布，
    这里用 Otsu 自适应阈值 + 开运算去噪 + 小连通域过滤，
    让花瓣/叶片之间浅色纹理缝隙被正确保留为背景。
    """
    gray = cv2.imread(image_path, cv2.IMREAD_GRAYSCALE)
    if gray is None:
        raise ValueError("无法读取图片，请检查路径")

    blurred = cv2.GaussianBlur(gray, (5, 5), 0)
    _, mask = cv2.threshold(blurred, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)

    kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (3, 3))
    mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, kernel)

    # 去掉面积过小的连通域（噪点/纹理碎片），避免机器空跑跳针
    # LUT 重映射代替逐连通域布尔掩码遍历：670 个连通域时 274ms → 10ms
    # 背景 labels==0 必须保持 0，否则整幅图被反色
    num, labels, stats, _ = cv2.connectedComponentsWithStats(mask, connectivity=8)
    lut = np.zeros(num, dtype=np.uint8)
    keep = stats[:, cv2.CC_STAT_AREA] >= 60
    keep[0] = False  # 背景域
    lut[keep] = 255
    return lut[labels]


def row_runs(row, min_len=0):
    """返回一行中所有前景连续段 [(start, end), ...]，向量化实现。"""
    padded = np.concatenate(([0], row, [0]))
    diff = np.diff(padded.astype(np.int16))
    starts = np.where(diff > 0)[0]
    ends = np.where(diff < 0)[0] - 1
    return [(int(s), int(e)) for s, e in zip(starts, ends) if e - s + 1 >= min_len]


def build_fill_regions(image_path, mask):
    """距离变换 + 分水岭：把粘连的花瓣/叶片拆成独立绣花区域。

    灰度阈值无法分开同色粘连的花瓣（整朵花会糊成一块），
    这里用经典 watershed 分割：区域中心做种子，沿颜色梯度向外扩展，
    在花瓣交界处自动停止。返回 markers 数组：
    1=背景，>=2=绣花区域，-1=区域边界。
    """
    dist = cv2.distanceTransform(mask, cv2.DIST_L2, 5)
    _, sure_fg = cv2.threshold(dist, 0.35 * dist.max(), 255, 0)
    sure_fg = np.uint8(sure_fg)
    sure_bg = cv2.dilate(mask, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (7, 7)))
    unknown = cv2.subtract(sure_bg, sure_fg)
    _, markers = cv2.connectedComponents(sure_fg)
    markers = markers + 1  # 背景从 1 开始，给分水岭留出 0 作为未知区标记
    markers[unknown == 255] = 0
    color_img = cv2.imread(image_path)
    markers = cv2.watershed(color_img, markers)
    return markers


# 针法 → 绣线颜色 / 可视化颜色 / 中文名
STITCH_THREAD = {"outline": 0xB73722, "satin": 0x1A9C46, "tatami": 0x2B6CB0}
STITCH_PLOT = {"outline": "red", "satin": "green", "tatami": "blue"}
STITCH_LABEL = {"outline": "轮廓走针", "satin": "平包针", "tatami": "他他米针"}
LAYER_ORDER = ["tatami", "satin", "outline"]  # 工业顺序：先填充，最后勾边

# 面料 → (张力补偿系数, 中文名)。绣完后布料收缩，实物会比设计图小一圈，
# 打版时按面料缩水率把绣花区域整体等比放大（以设计中心为原点），
# 补偿发生在"生成针迹之前"，DST 文件与预览图是同一套坐标，保证一致。
FABRIC_COMP = {
    "canvas": (1.005, "帆布"),
    "cotton": (1.01, "棉布"),
    "silk": (1.02, "丝绸"),
}


def scale_mask(mask, factor):
    """以图像中心为原点做等比缩放（张力补偿）。"""
    if factor <= 1.0:
        return mask
    h, w = mask.shape
    M = cv2.getRotationMatrix2D((w / 2, h / 2), 0, factor)
    return cv2.warpAffine(mask, M, (w, h), flags=cv2.INTER_NEAREST, borderValue=0)


def iter_regions(image_path, mask, min_area=60):
    """逐一带面积过滤的分水岭绣花区域，产出 (x偏移, y偏移, bbox内掩码)。

    性能：先取有效前景像素坐标按 marker 排序分组，再仅在各自的
    bounding box 内精确提取区域，避免对上百个区域各做 3 次
    2048×2048 整图布尔比较（实测 2430ms → 30ms，区域集合一致）。
    """
    markers = build_fill_regions(image_path, mask)
    valid = (mask == 255) & (markers != -1)
    ys, xs = np.nonzero(valid & (markers > 1))
    lab_vals = markers[ys, xs].astype(np.int64)
    order = np.argsort(lab_vals, kind="stable")
    ys, xs, lab_vals = ys[order], xs[order], lab_vals[order]
    uniq = np.unique(lab_vals)
    starts = np.searchsorted(lab_vals, uniq)
    ends = np.searchsorted(lab_vals, uniq, side="right")
    for lab, s, e in zip(uniq, starts, ends):
        if e - s < min_area:
            continue
        by, bx = ys[s:e], xs[s:e]
        y0, y1, x0, x1 = by.min(), by.max(), bx.min(), bx.max()
        region_mask = np.uint8(
            ((markers[y0 : y1 + 1, x0 : x1 + 1] == lab) & valid[y0 : y1 + 1, x0 : x1 + 1]) * 255
        )
        yield x0, y0, region_mask


def satin_fill(pattern, region_mask, pts, x_off=0, y_off=0, row_step=2, stitch_len=4, min_run=6):
    """平包针：区域内蛇形密排短针，行间跳针（不跨空白落针）。坐标加 x_off/y_off 回到全图系。

    步进锚点必须落在全局坐标系（range 起点加 x_off），否则 bbox 内重新锚定
    会让针迹相位相对原实现偏移，同样的行针数分布不同。
    """
    present = np.where((region_mask > 0).any(axis=1))[0]
    was_stitching = False
    for y in range(int(present[0]), int(present[-1]) + 1, row_step):
        for (s, e) in row_runs(region_mask[y], min_len=min_run):
            gs, ge = s + x_off, e + x_off
            xs = [v - x_off for v in range(gs, ge + 1, stitch_len)]
            if xs[-1] != e:
                xs.append(e)
            # 蛇形：奇偶行方向交替。奇偶必须按全局行号算，
            # 否则 bbox 局部行号会让相邻区域的行方向与原实现相反
            if ((y + y_off) // row_step) % 2 == 1:
                xs.reverse()
            for i, x in enumerate(xs):
                gx, gy = x + x_off, y + y_off
                if i == 0 and not was_stitching:
                    pattern.add_stitch_absolute(pyembroidery.JUMP, gx, gy)
                    pts["x"].append(None)  # 跳针断线，可视化不画移动轨迹
                    pts["y"].append(None)
                else:
                    pattern.add_stitch_absolute(pyembroidery.STITCH, gx, gy)
                pts["x"].append(gx)
                pts["y"].append(gy)
            was_stitching = True


def tatami_fill(pattern, region_mask, pts, x_off=0, y_off=0, row_step=5, stitch_len=10, min_run=10):
    """他他米针：区域内平行长针（按 stitch_len 分段），段间跳针。坐标加 x_off/y_off 回到全图系。

    步进锚点必须落在全局坐标系，理由同 satin_fill。
    """
    present = np.where((region_mask > 0).any(axis=1))[0]
    for y in range(int(present[0]), int(present[-1]) + 1, row_step):
        for (s, e) in row_runs(region_mask[y], min_len=min_run):
            gs, ge = s + x_off, e + x_off
            xs = [v - x_off for v in range(gs, ge + 1, stitch_len)]
            if xs[-1] != e:
                xs.append(e)
            pattern.add_stitch_absolute(pyembroidery.JUMP, s + x_off, y + y_off)
            pts["x"].append(None)  # 跳针断线，可视化不画移动轨迹
            pts["y"].append(None)
            pts["x"].append(s + x_off)
            pts["y"].append(y + y_off)
            for x in xs[1:]:
                pattern.add_stitch_absolute(pyembroidery.STITCH, x + x_off, y + y_off)
                pts["x"].append(x + x_off)
                pts["y"].append(y + y_off)


def outline_stitches(pattern, binary, pts, min_area=100):
    """轮廓走针：沿二值图全部轮廓（含内部纹理边）逐点落针。"""
    contours, _ = cv2.findContours(binary, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
    for contour in [c for c in contours if cv2.contourArea(c) > min_area]:
        start_pt = contour[0][0]
        pattern.add_stitch_absolute(pyembroidery.JUMP, int(start_pt[0]), int(start_pt[1]))
        for point in contour:
            pattern.add_stitch_absolute(pyembroidery.STITCH, int(point[0][0]), int(point[0][1]))
            pts["x"].append(int(point[0][0]))
            pts["y"].append(int(point[0][1]))
        pts["x"].append(None)
        pts["y"].append(None)


def generate_dst_logic(image_path, stitch_type="outline", tag="", fabric="canvas"):
    """生成工业级 DST。

    outline  轮廓走针：沿全部轮廓勾边
    satin    平包针：分水岭区域蛇形密排短针
    tatami   他他米针：分水岭区域平行长针
    auto     智能混合：按区域形态自动匹配针法
             细长区域→平包针(绿)  大面积区域→他他米针(蓝)  边缘→轮廓走针(红)
    fabric   面料（canvas/cotton/silk）：按缩水率做张力补偿
    返回 (dst_path, img_path, 总针数, 文件大小KB, 针法分布, 底布留白%, 补偿提示)
    """
    factor, fabric_name = FABRIC_COMP.get(fabric, FABRIC_COMP["canvas"])
    comp_msg = f"已根据{fabric_name}面料加入 {round((factor - 1) * 100, 1)}% 张力补偿"

    mask = build_foreground_mask(image_path)
    if cv2.countNonZero(mask) == 0:
        raise ValueError("未识别到可绣花的花纹区域，请更换纹样图")
    # 张力补偿：在生成针迹之前缩放整个绣花区域，DST 与预览图坐标天然一致
    mask = scale_mask(mask, factor)

    pattern = pyembroidery.EmbPattern()
    layers = {}  # 针法 -> {"x": [...], "y": [...]}，跳针处插 None 断线

    def layer(name):
        if name not in layers:
            layers[name] = {"x": [], "y": []}
        return layers[name]

    def begin_layer(name):
        pattern.add_thread(STITCH_THREAD[name])  # 换线 = 绣线颜色切换
        return layer(name)

    breakdown = {}

    if stitch_type == "outline":
        # 二值化后沿全部轮廓（含花瓣内部纹理）勾边
        gray = cv2.imread(image_path, cv2.IMREAD_GRAYSCALE)
        blurred = cv2.GaussianBlur(gray, (5, 5), 0)
        _, binary = cv2.threshold(blurred, 127, 255, cv2.THRESH_BINARY)
        binary = scale_mask(binary, factor)  # 与 mask 同步做张力补偿
        pts = begin_layer("outline")
        outline_stitches(pattern, binary, pts)

    elif stitch_type == "satin":
        pts = begin_layer("satin")
        for x_off, y_off, region_mask in iter_regions(image_path, mask, min_area=80):
            satin_fill(pattern, region_mask, pts, x_off, y_off)

    elif stitch_type == "tatami":
        pts = begin_layer("tatami")
        for x_off, y_off, region_mask in iter_regions(image_path, mask, min_area=120):
            tatami_fill(pattern, region_mask, pts, x_off, y_off)

    else:
        # ========== 🪄 智能混合模式：区域形态 → 针法自动匹配 ==========
        # 用距离变换求每个区域的"最大厚度"：
        # 厚度小 = 细长形（花梗/尾羽/叶脉）→ 平包针
        # 面积大 = 大面积（花瓣/叶片）    → 他他米针
        # 所有区域边缘                        → 轮廓走针
        thin_regions = []
        large_regions = []
        for x_off, y_off, region_mask in iter_regions(image_path, mask, min_area=80):
            thickness = float(cv2.distanceTransform(region_mask, cv2.DIST_L2, 3).max())
            if thickness < 12:
                thin_regions.append((x_off, y_off, region_mask))
            else:
                large_regions.append((x_off, y_off, region_mask))

        # 工业顺序：先他他米填充，再平包填充，最后轮廓勾边（边在顶层）
        if large_regions:
            pts = begin_layer("tatami")
            for x_off, y_off, region_mask in large_regions:
                tatami_fill(pattern, region_mask, pts, x_off, y_off)
        if thin_regions:
            pts = begin_layer("satin")
            for x_off, y_off, region_mask in thin_regions:
                satin_fill(pattern, region_mask, pts, x_off, y_off)
        # 边缘层：前景整体轮廓（Otsu 前景的内外边界都勾）
        pts = begin_layer("outline")
        outline_stitches(pattern, mask, pts, min_area=50)

    # 统计各针法针数（按实际落针点数，跳针 JUMP 不计入）
    pattern.end()
    for name in LAYER_ORDER:
        pts = layers.get(name)
        if not pts:
            continue
        breakdown[name] = sum(1 for v in pts["x"] if v is not None)
    total = sum(breakdown.values())

    # 每次请求生成独立文件（带时间戳+针法），工厂端按工单永久下载，互不覆盖
    dst_path = os.path.join(STATIC_DIR, f"output_{tag}.dst") if tag else os.path.join(STATIC_DIR, "output.dst")
    pyembroidery.write_dst(pattern, dst_path)

    # 多色针法分布图：红=轮廓走针 绿=平包针 蓝=他他米针（图例由前端弹窗卡片展示）
    plt.figure(figsize=(10, 10))
    for name in LAYER_ORDER:
        pts = layers.get(name)
        if pts and any(v is not None for v in pts["x"]):
            plt.plot(pts["x"], pts["y"], color=STITCH_PLOT[name], linewidth=0.5)
    plt.gca().invert_yaxis()
    plt.axis('equal')
    plt.axis('off')
    img_path = os.path.join(STATIC_DIR, f"stitch_path_{tag}.png") if tag else os.path.join(STATIC_DIR, "stitch_path.png")
    plt.savefig(img_path, dpi=200, bbox_inches='tight')
    plt.close()

    # 底布留白占比：前景之外不绣不收费，前端用于"节省成本"说明（真实计算值）
    bg_percent = round((1 - cv2.countNonZero(mask) / mask.size) * 100)

    file_size = os.path.getsize(dst_path) / 1024
    return dst_path, img_path, total, round(file_size, 2), breakdown, bg_percent, comp_msg


# ================= 4. 接口 =================
@app.post("/generate-dst")
async def generate_dst(request: Request, file: UploadFile = File(...)):
    timestamp = int(datetime.now().timestamp())
    preview_filename = f"preview_{timestamp}.png"
    preview_path = os.path.join(STATIC_DIR, preview_filename)

    with open(preview_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    # 针法：stitch_type（outline/satin/tatami/auto），兼容旧版 mode（outline/fill）
    stitch_type = (request.query_params.get("stitch_type")
                   or request.query_params.get("mode")
                   or "outline").lower()
    if stitch_type == "fill":
        stitch_type = "tatami"
    if stitch_type not in ("outline", "satin", "tatami", "auto"):
        stitch_type = "outline"
    # 面料（canvas/cotton/silk）：决定张力补偿系数
    fabric = (request.query_params.get("fabric") or "canvas").lower()
    if fabric not in FABRIC_COMP:
        fabric = "canvas"
    tag = f"{timestamp}_{stitch_type}"
    print(f"⚙️ 收到请求，当前针法：{stitch_type}，面料：{fabric}")

    dst_path, img_path, count, size, breakdown, bg_percent, comp_msg = generate_dst_logic(
        preview_path, stitch_type, tag, fabric)

    # 用本次请求的 host 拼回链，避免写死 localhost:8000（线上域名 / 反代才能正确打开）
    proto = request.headers.get("x-forwarded-proto") or request.url.scheme
    host = request.headers.get("x-forwarded-host") or request.headers.get("host", "")
    base_url = f"{proto}://{host}/static"
    preview_image_url = f"{base_url}/{preview_filename}"
    stitch_image_url = f"{base_url}/stitch_path_{tag}.png"
    dst_file_url = f"{base_url}/output_{tag}.dst"

    # 数字指纹存证：设计指纹（纹样图）+ 打版指纹（DST 文件），SHA-256 防篡改
    design_hash = get_file_hash(preview_path)
    dst_hash = get_file_hash(dst_path)

    try:
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        cursor.execute('''
            INSERT INTO embroidery_designs 
            (user_id, product_id, prompt, preview_image_url, stitch_image_url, dst_file_url, stitch_count, file_size, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (1, 1, "牡丹主题演示", preview_image_url, stitch_image_url, dst_file_url, count, f"{size} KB", 1,
              datetime.now().strftime("%Y-%m-%d %H:%M:%S")))
        conn.commit()
        conn.close()
        print(f"✅ 数据库写入成功！本次针数：{count}")
    except Exception as e:
        print(f"⚠️ 数据库写入失败：{e}")

    return {
        "code": 200,
        "data": {
            "stitch_image_url": stitch_image_url,
            "dst_file_url": dst_file_url,
            "stitch_count": count,
            "file_size": f"{size} KB",
            "stitch_type": stitch_type,
            "stitch_breakdown": breakdown,
            "bg_percent": bg_percent,
            "fabric": fabric,
            "compensation_msg": comp_msg,
            "design_hash": design_hash,
            "dst_hash": dst_hash
        }
    }