import { createApp } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';
import App from './App.vue';
import Home from './pages/client/Home.vue';
import Generate from './pages/client/Generate.vue';
import Stitch3D from './pages/client/Stitch3D.vue';
import Products from './pages/client/Products.vue';
import Patterns from './pages/patterns/index.vue';
import Courses from './pages/client/Courses.vue';
import Login from './pages/client/Login.vue';
import Register from './pages/client/Register.vue';
import DetailPage from './pages/client/DetailPage.vue';
import WorkspacePage from './pages/client/WorkspacePage.vue';
import CustomizeUpgrade from './pages/client/CustomizeUpgrade.vue';
import ProfilePage from './pages/profile/index.vue';
import AdminLayout from './pages/merchant/AdminLayout.vue';
import AdminModule from './pages/merchant/AdminModule.vue';
import WorkshopOrders from './pages/merchant/WorkshopOrders.vue';
import { isClient, isLoggedIn, isMerchant, isRealClient } from './auth';
import './styles.css';

const protectedRoute = (path: string, title: string, mode: string) => ({
  path,
  component: WorkspacePage,
  meta: { requiresAuth: true, requiresClient: true, title, mode }
});
const adminModules = [
  ['dashboard', '数据总览'],
  ['orders', '订单管理'],
  ['products', '商品管理'],
  ['product-categories', '商品分类'],
  ['custom-designs', '定制设计'],
  ['patterns', '纹样管理'],
  ['pattern-generations', '生成记录'],
  ['prompt-templates', '提示词模板'],
  ['course-categories', '课程分类'],
  ['courses', '课程管理'],
  ['resources', '资源管理'],
  ['users', '用户管理'],
  ['home', '首页配置'],
  ['shop', '店铺配置'],
  ['messages', '消息管理']
];

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: Home },
    { path: '/home', redirect: '/' },
    { path: '/generate', component: Generate },
    { path: '/stitch-3d', component: Stitch3D },
    { path: '/patterns/generate', redirect: '/generate' },
    { path: '/pattern-generate', redirect: '/generate' },
    { path: '/products', component: Products },
    { path: '/products/:productId', component: DetailPage, meta: { kind: 'product' } },
    { path: '/patterns', component: Patterns },
    { path: '/patterns/:patternId', component: DetailPage, meta: { kind: 'pattern' } },
    { path: '/my/patterns', redirect: '/patterns' },
    { path: '/my-patterns', redirect: '/patterns' },
    { path: '/courses', component: Courses },
    { path: '/courses/:courseId', component: DetailPage, meta: { kind: 'course' } },
    { path: '/login', component: Login, meta: { layout: 'auth' } },
    {
      path: '/merchant/login',
      component: Login,
      meta: { layout: 'auth', portal: 'merchant' }
    },
    { path: '/register', component: Register, meta: { layout: 'auth' } },
    // 旧画布贴图式定制暂时下线，入口统一落到升级占位页。
    {
      path: '/customize',
      component: CustomizeUpgrade,
      meta: { title: 'AI 定制' }
    },
    {
      path: '/cart',
      component: WorkspacePage,
      meta: {
        requiresAuth: true,
        requiresClient: true,
        requiresRealClient: true,
        title: '打样需求',
        mode: 'cart'
      }
    },
    protectedRoute('/orders', '我的订单', 'orders'),
    {
      path: '/orders/:orderId',
      component: DetailPage,
      meta: { requiresAuth: true, requiresClient: true, kind: 'order' }
    },
    {
      path: '/resources',
      component: WorkspacePage,
      meta: { title: '创作资源', mode: 'resources' }
    },
    {
      path: '/profile',
      component: ProfilePage,
      meta: {
        requiresAuth: true,
        requiresClient: true,
        requiresRealClient: true,
        title: '个人中心'
      }
    },
    {
      path: '/merchant',
      component: AdminLayout,
      meta: { layout: 'merchant', requiresMerchant: true },
      children: [
        { path: '', redirect: '/merchant/dashboard' },
        // 乡村工坊接单大厅：独立页面（非通用管理模块）
        {
          path: 'factory-orders',
          component: WorkshopOrders,
          meta: { layout: 'merchant', requiresMerchant: true, title: '工坊接单大厅' }
        },
        ...adminModules.map(([path, title]) => ({
          path,
          component: AdminModule,
          meta: { layout: 'merchant', requiresMerchant: true, title }
        }))
      ]
    },
    {
      // 旧的独立工厂端门户并入乡村工坊端（商家端），旧链接重定向
      path: '/factory',
      redirect: '/merchant/factory-orders'
    },
    {
      path: '/admin/:pathMatch(.*)*',
      redirect: (to) => `/merchant/${String(to.params.pathMatch || '')}`
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ],
  scrollBehavior: () => ({ top: 0 })
});

router.beforeEach((to) => {
  if (to.meta.requiresMerchant && !isMerchant.value)
    return { path: '/merchant/login', query: { redirect: to.fullPath } };
  if (to.meta.requiresAuth && !isLoggedIn.value)
    return { path: '/login', query: { redirect: to.fullPath } };
  if (to.meta.requiresRealClient && !isRealClient.value)
    return { path: '/login', query: { redirect: to.fullPath } };
  if (to.meta.requiresClient && !isClient.value) return { path: '/merchant', replace: true };
});

createApp(App).use(router).mount('#app');
