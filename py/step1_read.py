import cv2

img = cv2.imread('pattern.png', cv2.IMREAD_GRAYSCALE)

if img is None:
    print("错误：没有找到 pattern.png，请检查图片名字和路径！")
else:
    # 1. 高斯模糊：模糊掉细碎的纹理和噪点
    blurred = cv2.GaussianBlur(img, (5, 5), 0)

    # 2. 二值化
    _, binary = cv2.threshold(blurred, 127, 255, cv2.THRESH_BINARY)

    # 3. 形态学开运算：去除细小的白点（噪点），让轮廓更平滑
    kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (3, 3))
    cleaned = cv2.morphologyEx(binary, cv2.MORPH_OPEN, kernel)

    # 4. 保存处理后的图
    cv2.imwrite('binary.png', cleaned)
    print("成功！已生成清理后的 binary.png，请去文件夹里查看。")