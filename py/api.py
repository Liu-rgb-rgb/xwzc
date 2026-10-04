import os
import shutil
import cv2
import pyembroidery
import matplotlib.pyplot as plt
import sqlite3
from datetime import datetime
from fastapi import FastAPI, UploadFile, File, Request
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware

# ================= 1. 初始化 FastAPI 和跨域配置 =================
app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

os.makedirs("static", exist_ok=True)
app.mount("/static", StaticFiles(directory="static"), name="static")


# ================= 2. 数据库初始化 =================
def init_db():
    conn = sqlite3.connect('embroidery.db')
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
def generate_dst_logic(image_path, mode="outline"):
    img = cv2.imread(image_path, cv2.IMREAD_GRAYSCALE)
    if img is None:
        raise ValueError("无法读取图片，请检查路径")

    blurred = cv2.GaussianBlur(img, (5, 5), 0)
    _, binary = cv2.threshold(blurred, 127, 255, cv2.THRESH_BINARY)

    pattern = pyembroidery.EmbPattern()
    pattern.add_thread(0x000000)
    x_coords, y_coords = [], []

    if mode == "outline":
        # ========== 轮廓模式（保持不变） ==========
        contours, _ = cv2.findContours(binary, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
        valid_contours = [c for c in contours if cv2.contourArea(c) > 100]

        for contour in valid_contours:
            start_pt = contour[0][0]
            pattern.add_stitch_absolute(pyembroidery.JUMP, int(start_pt[0]), int(start_pt[1]))
            for point in contour:
                pattern.add_stitch_absolute(pyembroidery.STITCH, int(point[0][0]), int(point[0][1]))
                x_coords.append(int(point[0][0]))
                y_coords.append(int(point[0][1]))
            x_coords.append(None)
            y_coords.append(None)

    elif mode == "fill":
        # ========== 填针模式（终极修正版） ==========
        # 1. 阈值反转让花朵变成黑色(0)，背景变成白色(255)，阈值设为200过滤阴影
        _, binary_fill = cv2.threshold(blurred, 200, 255, cv2.THRESH_BINARY_INV)

        # 2. 形态学开运算：抹掉细小的噪点，让花瓣变成实心色块
        kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (3, 3))
        binary_fill = cv2.morphologyEx(binary_fill, cv2.MORPH_OPEN, kernel)

        # 3. 提取边界框，限制扫描范围
        contours, _ = cv2.findContours(binary_fill, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        if contours:
            x_min = min(cv2.boundingRect(c)[0] for c in contours)
            y_min = min(cv2.boundingRect(c)[1] for c in contours)
            x_max = max(cv2.boundingRect(c)[0] + cv2.boundingRect(c)[2] for c in contours)
            y_max = max(cv2.boundingRect(c)[1] + cv2.boundingRect(c)[3] for c in contours)
        else:
            x_min, y_min, x_max, y_max = 0, 0, binary_fill.shape[1], binary_fill.shape[0]

        # 4. 加大步长（15或20），避免针数爆炸
        step = 15

        for y in range(y_min, y_max, step):
            is_stitching = False
            for x in range(x_min, x_max):
                pixel = binary_fill[y, x]
                # 遇到黑色图案（即原图的花瓣亮部），开始落针
                if pixel == 0 and not is_stitching:
                    pattern.add_stitch_absolute(pyembroidery.JUMP, x, y)
                    pattern.add_stitch_absolute(pyembroidery.STITCH, x, y)
                    x_coords.append(x)
                    y_coords.append(y)
                    is_stitching = True
                # 遇到白色背景，停止落针
                elif pixel == 255 and is_stitching:
                    pattern.add_stitch_absolute(pyembroidery.STITCH, x, y)
                    x_coords.append(x)
                    y_coords.append(y)
                    is_stitching = False
                    x_coords.append(None)
                    y_coords.append(None)

    pattern.end()
    dst_path = "static/output.dst"
    pyembroidery.write_dst(pattern, dst_path)

    # 画图
    plt.figure(figsize=(10, 10))
    plt.plot(x_coords, y_coords, color='red', linewidth=0.5)
    plt.gca().invert_yaxis()
    plt.axis('equal')
    plt.axis('off')
    img_path = "static/stitch_path.png"
    plt.savefig(img_path, dpi=200, bbox_inches='tight')
    plt.close()

    file_size = os.path.getsize(dst_path) / 1024
    return dst_path, img_path, len(pattern.stitches), round(file_size, 2)


# ================= 4. 接口 =================
@app.post("/generate-dst")
async def generate_dst(request: Request, file: UploadFile = File(...)):
    timestamp = int(datetime.now().timestamp())
    preview_filename = f"preview_{timestamp}.png"
    preview_path = f"static/{preview_filename}"

    with open(preview_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    mode = request.query_params.get("mode", "outline")
    print(f"⚙️ 收到请求，当前模式：{mode}")

    dst_path, img_path, count, size = generate_dst_logic(preview_path, mode)

    base_url = "http://localhost:8000/static"
    preview_image_url = f"{base_url}/{preview_filename}"
    stitch_image_url = f"{base_url}/stitch_path.png"
    dst_file_url = f"{base_url}/output.dst"

    try:
        conn = sqlite3.connect('embroidery.db')
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
            "file_size": f"{size} KB"
        }
    }