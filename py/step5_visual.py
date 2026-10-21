import pyembroidery
import matplotlib.pyplot as plt

# 1. 读取刚才生成的 DST 文件
pattern = pyembroidery.read_dst('output.dst')

if not pattern:
    print("错误：没有找到或无法读取 output.dst")
else:
    # 2. 提取所有的针脚坐标
    # stitches 列表里的每个元素格式是 [x坐标, y坐标, 指令类型]
    x_coords = []
    y_coords = []

    for stitch in pattern.stitches:
        # 只有当指令是 STITCH（真正落针）时，才记录坐标画线
        if stitch[2] == pyembroidery.STITCH:
            x_coords.append(stitch[0])
            y_coords.append(stitch[1])
        else:
            # 遇到 JUMP（跳针）时，断开线条，避免出现跨越全图的长直线
            x_coords.append(None)
            y_coords.append(None)

    print(f"读取到 {len(x_coords)} 个有效针脚，正在生成优化后的路径图...")

    plt.figure(figsize=(10, 10))
    # 这里 linewidth 改成 0.5，线条细一点更好看
    plt.plot(x_coords, y_coords, color='red', linewidth=0.5)

    # 4. 调整显示方式，让它和真实图片方向一致
    plt.gca().invert_yaxis()  # 翻转Y轴（因为图片的Y轴是向下的）
    plt.axis('equal')  # 保持XY比例，防止图形拉伸变形
    plt.axis('off')  # 隐藏坐标轴边框

    # 5. 保存并显示
    plt.savefig('stitch_path.png', dpi=300, bbox_inches='tight')
    print("🎉 成功！已生成 stitch_path.png，请去文件夹里查看！")
    plt.show()