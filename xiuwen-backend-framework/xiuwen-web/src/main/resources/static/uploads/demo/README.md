# 绣纹智创共享测试图片

把固定测试素材放在本目录后提交 Git，另一位开发者拉取代码即可直接使用，不需要重新上传。

访问前缀统一为：

```text
/api/uploads/demo/
```

目录用途：

```text
banner/    首页轮播图
category/  商品分类图标
product/   商品封面图、商品定制底图
pattern/   AI Mock 纹样图
course/    课程封面、演示视频（视频可后补）
resource/  资源封面、PDF、ZIP、图片素材（文件可后补）
```

数据库示例：

```text
/api/uploads/demo/product/peony-canvas-bag-cover.jpg
```

对应文件：

```text
xiuwen-web/src/main/resources/static/uploads/demo/product/peony-canvas-bag-cover.jpg
```

注意：文件名和扩展名必须与数据库完全一致，推荐使用小写英文、数字和连字符。
