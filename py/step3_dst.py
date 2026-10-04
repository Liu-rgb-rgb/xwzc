import cv2
import pyembroidery

# 读取二值化图
binary = cv2.imread('binary.png', cv2.IMREAD_GRAYSCALE)

if binary is None:
    print("错误：没有找到 binary.png")
else:
    # 提取所有轮廓
    contours, _ = cv2.findContours(binary, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)

    # 过滤小噪点（保留面积 > 100 的轮廓）
    valid_contours = [c for c in contours if cv2.contourArea(c) > 100]
    # ... 前面获取 valid_contours 的代码不变 ...

    # 🚨 核心优化：按轮廓的X坐标从左到右排序，大幅减少跳线
    valid_contours = sorted(valid_contours, key=lambda c: cv2.boundingRect(c)[0])

    pattern = pyembroidery.EmbPattern()
    pattern.add_thread(0x000000)

    for contour in valid_contours:
        # 移动到轮廓起点（用 JUMP 指令，机器不落针）
        start_pt = contour[0][0]
        pattern.add_stitch_absolute(pyembroidery.JUMP, int(start_pt[0]), int(start_pt[1]))

        # 开始刺绣（用 STITCH 指令，机器落针）
        for point in contour:
            x, y = int(point[0][0]), int(point[0][1])
            pattern.add_stitch_absolute(pyembroidery.STITCH, x, y)

    pattern.end()
    pyembroidery.write_dst(pattern, 'output.dst')
    print("🎉 优化后的 DST 生成成功！")