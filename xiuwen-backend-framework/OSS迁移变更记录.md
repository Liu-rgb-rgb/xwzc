# 阿里云 OSS 迁移变更记录

## 一、概述

将项目图片存储从本地文件系统迁移到阿里云 OSS，图片地址字段仍为字符串 URL，只是从本地路径 `/api/uploads/xxx` 换成 OSS 的完整 URL `https://bucket.oss-cn-xxx.aliyuncs.com/xxx`。

---

## 二、新增文件

| 文件路径 | 说明 |
|---------|------|
| `xiuwen-framework/src/main/java/com/xiuwen/framework/config/OssProperties.java` | OSS 配置属性类，读取 `xiuwen.oss.*` 配置 |
| `xiuwen-framework/src/main/java/com/xiuwen/framework/service/OssFileService.java` | OSS 上传服务，提供 `upload()` 和 `getOssDomain()` 方法 |

---

## 三、修改文件

### 1. `xiuwen-framework/pom.xml`
- 新增依赖：`com.aliyun.oss:aliyun-sdk-oss:3.17.4`

### 2. `xiuwen-product/pom.xml`
- 新增依赖：`com.xiuwen:xiuwen-framework`（使 product 模块能访问 `OssFileService`）

### 3. `xiuwen-pattern/pom.xml`
- 新增依赖：`com.xiuwen:xiuwen-framework`（使 pattern 模块能访问 `OssFileService`）

### 4. `xiuwen-web/src/main/resources/application.yml`
- 新增 OSS 配置节点：
  ```yaml
  xiuwen:
    oss:
      endpoint: oss-cn-guangzhou.aliyuncs.com
      access-key-id: YOUR_ACCESS_KEY_ID
      access-key-secret: YOUR_ACCESS_KEY_SECRET
      bucket-name: YOUR_BUCKET_NAME
      domain: https://YOUR_BUCKET_NAME.oss-cn-guangzhou.aliyuncs.com
  ```
  **部署前需将 `YOUR_*` 替换为真实的阿里云 OSS 配置。**

### 5. `xiuwen-web/.../controller/user/FileController.java`
- 移除 `FileUploadProperties` 依赖，改注入 `OssFileService`
- 上传逻辑：不再写入本地磁盘，改为调用 `ossFileService.upload(file, bizType)` 直接上传到 OSS
- 返回的 `FileResource.fileUrl` 从相对路径 `/api/uploads/xxx` 变为 OSS 完整 URL

### 6. `xiuwen-web/.../controller/user/UserProfileController.java`
- 移除 `FileUploadProperties` 依赖，改注入 `OssFileService`
- 头像上传逻辑：不再写入本地磁盘，改为调用 `ossFileService.upload(file, "AVATAR")` 上传到 OSS
- 返回的 `avatar` URL 从相对路径变为 OSS 完整 URL

### 7. `xiuwen-product/.../service/impl/CustomDesignServiceImpl.java`
- 注入 `OssFileService`
- 预览图 URL 从硬编码 `https://cdn.example.com/custom/preview-xxx.png` 改为 `ossFileService.getOssDomain() + "custom/preview-xxx.png"`

### 8. `xiuwen-pattern/.../service/impl/PatternGenerateServiceImpl.java`
- 注入 `OssFileService`
- `MOCK_IMAGE_URLS` 常量改名为 `MOCK_IMAGE_PATHS`，值从完整相对路径改为 OSS object key（如 `demo/pattern/xxx.jpg`）
- `selectMockImages()` 方法改为拼接 `ossFileService.getOssDomain()` + path，返回完整 OSS URL

---

## 四、未修改但相关文件

| 文件 | 说明 |
|------|------|
| `xiuwen-framework/.../config/FileUploadProperties.java` | 保留，本地文件上传配置仍可用（如需要回退） |
| `xiuwen-framework/.../config/WebMvcConfig.java` | 保留，静态资源映射仍可服务本地文件（如 demo 素材） |
| `xiuwen-web/.../config/DemoStaticResourceConfig.java` | 保留，demo 素材仍可从 classpath 访问 |

---

## 五、数据库已有数据迁移

已有数据的图片字段存的是本地相对路径（如 `/api/uploads/xxx`），需要手动更新为 OSS 地址。示例 SQL：

```sql
-- file_resource 表
UPDATE file_resource SET file_url = REPLACE(file_url, '/api/uploads/', 'https://YOUR_BUCKET_NAME.oss-cn-guangzhou.aliyuncs.com/') WHERE file_url LIKE '/api/uploads/%';

-- user 表头像
UPDATE user SET avatar = REPLACE(avatar, '/api/uploads/', 'https://YOUR_BUCKET_NAME.oss-cn-guangzhou.aliyuncs.com/') WHERE avatar LIKE '/api/uploads/%';

-- home_banner 表
UPDATE home_banner SET image_url = REPLACE(image_url, '/api/uploads/', 'https://YOUR_BUCKET_NAME.oss-cn-guangzhou.aliyuncs.com/') WHERE image_url LIKE '/api/uploads/%';

-- home_recommend 表
UPDATE home_recommend SET cover_image = REPLACE(cover_image, '/api/uploads/', 'https://YOUR_BUCKET_NAME.oss-cn-guangzhou.aliyuncs.com/') WHERE cover_image LIKE '/api/uploads/%';

-- pattern 表
UPDATE pattern SET image_url = REPLACE(image_url, '/api/uploads/', 'https://YOUR_BUCKET_NAME.oss-cn-guangzhou.aliyuncs.com/') WHERE image_url LIKE '/api/uploads/%';
UPDATE pattern SET thumbnail_url = REPLACE(thumbnail_url, '/api/uploads/', 'https://YOUR_BUCKET_NAME.oss-cn-guangzhou.aliyuncs.com/') WHERE thumbnail_url LIKE '/api/uploads/%';

-- product 表
UPDATE product SET cover_image = REPLACE(cover_image, '/api/uploads/', 'https://YOUR_BUCKET_NAME.oss-cn-guangzhou.aliyuncs.com/') WHERE cover_image LIKE '/api/uploads/%';
UPDATE product SET mockup_image = REPLACE(mockup_image, '/api/uploads/', 'https://YOUR_BUCKET_NAME.oss-cn-guangzhou.aliyuncs.com/') WHERE mockup_image LIKE '/api/uploads/%';

-- course 表
UPDATE course SET cover_image = REPLACE(cover_image, '/api/uploads/', 'https://YOUR_BUCKET_NAME.oss-cn-guangzhou.aliyuncs.com/') WHERE cover_image LIKE '/api/uploads/%';

-- learning_resource 表
UPDATE learning_resource SET cover_image = REPLACE(cover_image, '/api/uploads/', 'https://YOUR_BUCKET_NAME.oss-cn-guangzhou.aliyuncs.com/') WHERE cover_image LIKE '/api/uploads/%';

-- shop_info 表
UPDATE shop_info SET logo = REPLACE(logo, '/api/uploads/', 'https://YOUR_BUCKET_NAME.oss-cn-guangzhou.aliyuncs.com/') WHERE logo LIKE '/api/uploads/%';
```

> **注意**：执行 SQL 前，需先将本地 `./uploads/` 目录下的文件上传到 OSS 对应的路径中，确保 URL 可访问。

---

## 六、部署前配置清单

1. 在阿里云 OSS 控制台创建 Bucket，获取 Endpoint、AccessKey、Bucket 名称
2. 将 `application.yml` 中的 `YOUR_*` 占位符替换为真实值
3. 将本地 `./uploads/` 目录下已有文件上传到 OSS 对应路径
4. 执行上述 SQL 更新数据库中已有记录的图片 URL
5. 确认 OSS Bucket 的访问权限（公开读 或 使用签名 URL）

---

## 七、验证方法

### 1. 编译验证
```bash
mvn compile
```
编译通过说明所有依赖和引用正确。

### 2. 启动验证
```bash
mvn spring-boot:run -pl xiuwen-web
```
启动成功后观察日志中是否有 `OssProperties` 和 `OssFileService` Bean 注册。

### 3. 上传接口验证
```bash
curl -X POST http://localhost:8080/api/admin/files/upload \
  -H "Authorization: Bearer <your-jwt-token>" \
  -F "file=@test.jpg" \
  -F "bizType=TEST"
```
返回的 `fileUrl` 应为 `https://xwzc.oss-cn-beijing.aliyuncs.com/TEST/xxx.jpg` 格式。

### 4. 头像上传验证
```bash
curl -X POST http://localhost:8080/api/user/avatar \
  -H "Authorization: Bearer <your-jwt-token>" \
  -F "file=@avatar.jpg"
```
返回的 `avatar` 应为 OSS 完整 URL。

### 5. 浏览器验证
在浏览器中直接访问返回的 OSS URL，确认图片能正常显示。

### 6. 阿里云控制台验证
登录阿里云 OSS 控制台，进入 `xwzc` Bucket，确认文件列表中出现了上传的文件。
