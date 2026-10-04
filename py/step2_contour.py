import cv2

binary = cv2.imread('binary.png', cv2.IMREAD_GRAYSCALE)

if binary is None:
    print("错误：没有找到 binary.png，请先运行上一步！")
else:
    contours, _ = cv2.findContours(binary, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
    print(f"过滤前找到轮廓数量：{len(contours)}")

    # 过滤掉面积小于 100 像素的细小轮廓（去除噪点）
    valid_contours = [c for c in contours if cv2.contourArea(c) > 100]
    print(f"过滤后保留轮廓数量：{len(valid_contours)}")

    canvas = cv2.cvtColor(binary, cv2.COLOR_GRAY2BGR)

    # 只画保留下来的轮廓
    cv2.drawContours(canvas, valid_contours, -1, (0, 0, 255), 1)

    cv2.imwrite('contours.png', canvas)
    print("成功！已生成过滤后的 contours.png，请去文件夹里查看。")