import { createApp } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';
import App from './App.vue';
import Home from './pages/Home.vue';
import Generate from './pages/Generate.vue';
import Products from './pages/Products.vue';
import Patterns from './pages/Patterns.vue';
import Courses from './pages/Courses.vue';
import Login from './pages/Login.vue';
import Register from './pages/Register.vue';
import DetailPage from './pages/DetailPage.vue';
import WorkspacePage from './pages/WorkspacePage.vue';
import AdminLayout from './pages/admin/AdminLayout.vue';
import AdminModule from './pages/admin/AdminModule.vue';
import { isAdmin, isLoggedIn } from './auth';
import './styles.css';

const protectedRoute = (path: string, title: string, mode: string) => ({
  path,
  component: WorkspacePage,
  meta: { requiresAuth: true, title, mode }
});
const adminModules = [
  ['dashboard', '数据总览'],
  ['orders', '订单管理'],
  ['products', '商品管理'],
  ['product-categories', '商品分类'],
  ['custom-designs', '定制设计'],
  ['patterns', '纹样管理'],
  ['prompt-templates', '提示词模板'],
  ['courses', '课程管理'],
  ['resources', '资源管理'],
  ['users', '用户管理'],
  ['home', '首页配置'],
  ['shop', '店铺配置']
];

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: Home },
    { path: '/home', redirect: '/' },
    { path: '/generate', component: Generate },
    { path: '/patterns/generate', redirect: '/generate' },
    { path: '/pattern-generate', redirect: '/generate' },
    { path: '/products', component: Products },
    { path: '/products/:productId', component: DetailPage, meta: { kind: 'product' } },
    { path: '/patterns', component: Patterns },
    { path: '/my/patterns', redirect: '/patterns' },
    { path: '/my-patterns', redirect: '/patterns' },
    { path: '/courses', component: Courses },
    { path: '/courses/:courseId', component: DetailPage, meta: { kind: 'course' } },
    { path: '/login', component: Login, meta: { layout: 'auth' } },
    { path: '/register', component: Register, meta: { layout: 'auth' } },
    protectedRoute('/customize', '文创商品定制', 'customize'),
    protectedRoute('/cart', '购物车', 'cart'),
    protectedRoute('/orders', '我的订单', 'orders'),
    {
      path: '/orders/:orderId',
      component: DetailPage,
      meta: { requiresAuth: true, kind: 'order' }
    },
    {
      path: '/resources',
      component: WorkspacePage,
      meta: { title: '创作资源', mode: 'resources' }
    },
    protectedRoute('/profile', '个人中心', 'profile'),
    {
      path: '/admin',
      component: AdminLayout,
      meta: { layout: 'admin', requiresAdmin: true },
      children: [
        { path: '', redirect: '/admin/dashboard' },
        ...adminModules.map(([path, title]) => ({
          path,
          component: AdminModule,
          meta: { layout: 'admin', requiresAdmin: true, title }
        }))
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ],
  scrollBehavior: () => ({ top: 0 })
});

router.beforeEach((to) => {
  if (to.meta.requiresAdmin && !isAdmin.value)
    return { path: '/login', query: { redirect: to.fullPath } };
  if (to.meta.requiresAuth && !isLoggedIn.value)
    return { path: '/login', query: { redirect: to.fullPath } };
});

createApp(App).use(router).mount('#app');
