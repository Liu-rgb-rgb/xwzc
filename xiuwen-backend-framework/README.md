# 绣纹智创第一版后端代码框架

本工程是 JDK8 + Maven + Spring Boot 2.7.x + MyBatis-Plus 的后端聚合工程骨架，已按用户端与商家端预留 Controller、Service、Mapper、Entity 和配置类。

## 模块说明

- xiuwen-common：通用返回、分页、常量、异常、工具类。
- xiuwen-framework：MyBatis-Plus、JWT、拦截器、跨域、全局异常配置。
- xiuwen-system：用户、店铺、地址、消息、文件、首页配置等系统基础模块。
- xiuwen-pattern：AI纹样生成、纹样库、提示词模板模块。
- xiuwen-product：文创商品、分类、定制设计、购物车模块。
- xiuwen-order：订单主表、订单明细模块。
- xiuwen-course：非遗课堂、课程分类、创作资源模块。
- xiuwen-web：启动类和用户端/商家端 Controller。

## 启动步骤

1. 先执行数据库 SQL：`xiuwen_zhichuang_v1_schema.sql`。
2. 修改 `xiuwen-web/src/main/resources/application.yml` 中的数据库账号密码。
3. 在项目根目录执行：

```bash
mvn clean package
```

4. 启动：

```bash
cd xiuwen-web
mvn spring-boot:run
```

## 默认登录

数据库脚本初始化了超级管理员账号：

- 账号：shopadmin
- 密码：123456
- 角色：ADMIN

第一版使用 MD5，后续真实上线建议改为 BCrypt。
