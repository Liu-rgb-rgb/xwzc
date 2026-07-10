# 接口文件映射

## 用户端

- `auth.ts`：`/api/auth/register`、`/api/auth/login`、`/api/auth/logout`、`/api/auth/me`
- `user.ts`：`/api/user/profile`、`/api/user/avatar`、`/api/user/password`、`/api/user/addresses`
- `messages.ts`：`/api/messages`、`/api/messages/unread-count`、`/api/messages/{id}/read`
- `home.ts`：`/api/home`、`/api/home/banners`、`/api/home/recommends`、`/api/search`、`/api/shop/info`
- `patterns.ts`：`/api/patterns/options`、`/api/patterns/generate`、`/api/patterns/my` 等
- `products.ts`：`/api/products/categories`、`/api/products`、`/api/products/{id}`、`/api/products/recommends`
- `customDesigns.ts`：`/api/custom-designs`、`/api/custom-designs/my`、`/api/custom-designs/{id}`
- `cart.ts`：`/api/cart/items`
- `orders.ts`：`/api/orders`、`/api/orders/{id}/mock-pay`、`/api/orders/my`、`/api/orders/status-count`
- `courses.ts`：`/api/courses/categories`、`/api/courses`、`/api/courses/{id}`、`/api/courses/{id}/study`
- `resources.ts`：`/api/resources`、`/api/resources/{id}`、`/api/resources/{id}/download`
- `files.ts`：`/api/files/upload`

## 商家端

- `adminDashboard.ts`：`/api/admin/dashboard`
- `adminOrders.ts`：`/api/admin/orders`、`/api/admin/orders/{id}`、`/api/admin/orders/{id}/status`
- `adminProducts.ts`：`/api/admin/product-categories`、`/api/admin/products`、`/api/admin/custom-designs`
- `adminPatterns.ts`：`/api/admin/patterns`、`/api/admin/pattern-generations`、`/api/admin/prompt-templates`
- `adminCourses.ts`：`/api/admin/course-categories`、`/api/admin/courses`
- `adminResources.ts`：`/api/admin/resources`
- `adminUsers.ts`：`/api/admin/users`
- `adminHome.ts`：`/api/admin/home-banners`、`/api/admin/home-recommends`
- `adminShop.ts`：`/api/admin/shop/info`、`/api/admin/messages`、`/api/admin/files/upload`
