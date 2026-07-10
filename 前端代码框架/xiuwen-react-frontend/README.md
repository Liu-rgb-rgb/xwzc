# 绣纹智创 React 前端框架

技术栈：React 18 + Vite + TypeScript + Ant Design + Axios + React Router + Zustand。

本框架已经按照《绣纹智创第一版前后端接口设计文档》预置用户端与商家端页面、路由、API 调用文件、鉴权守卫和布局。业务页面目前是骨架/占位实现，方便你直接接后端接口继续写功能。

## 启动方式

```bash
npm install
npm run dev
```

默认前端地址：

```text
http://localhost:5173
```

默认通过 Vite 代理访问后端：

```text
/api -> http://localhost:8080
```

需要修改后端地址时，改 `.env.development`：

```env
VITE_PROXY_TARGET=http://localhost:8080
```

## 目录说明

```text
src/api                 所有接口请求，路径已对应接口文档
src/layouts             用户端布局、商家端布局
src/pages/user          用户端页面
src/pages/admin         商家端页面
src/router              路由与权限守卫
src/store               登录状态、token、userInfo
src/types               通用类型
src/constants           角色、状态枚举
src/components          通用组件
src/styles              全局样式
```

## 登录跳转规则

登录成功后根据 `userInfo.role` 判断跳转：

```text
USER -> /home
MERCHANT_ADMIN / ADMIN -> /admin/dashboard
```

## 当前已预留页面

用户端：登录、注册、首页、AI 纹样生成、我的纹样、商品列表、商品详情、定制预览、购物车、订单、课程、资源、个人中心。

商家端：工作台、订单管理、商品管理、商品分类、定制管理、纹样管理、提示词模板、课程管理、资源管理、用户管理、首页运营、店铺设置。

## 接口说明

所有 API 文件都在 `src/api` 下，接口路径完整保留 `/api/...` 前缀，例如：

```ts
loginApi(data) -> POST /api/auth/login
getProductListApi(params) -> GET /api/products
getAdminOrdersApi(params) -> GET /api/admin/orders
```

后端返回格式按文档约定：

```ts
{
  code: 200,
  message: 'success',
  data: {}
}
```

## 下一步开发建议

1. 先确认登录接口 `/api/auth/login` 能调通。
2. 再接首页 `/api/home`、商品 `/api/products`、纹样 `/api/patterns/generate`。
3. 最后接订单和商家端后台。
