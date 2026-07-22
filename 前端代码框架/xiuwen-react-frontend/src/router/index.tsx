import { createBrowserRouter, Navigate } from 'react-router-dom';
import RequireAuth from '@/components/RequireAuth';
import RequireAdmin from '@/components/RequireAdmin';
import UserLayout from '@/layouts/UserLayout';
import AdminLayout from '@/layouts/AdminLayout';
import LoginPage from '@/pages/LoginPage';
import RegisterPage from '@/pages/RegisterPage';
import HomePage from '@/pages/user/HomePage';
import PatternGeneratePage from '@/pages/user/PatternGeneratePage';
import MyPatternsPage from '@/pages/user/MyPatternsPage';
import ProductsPage from '@/pages/user/ProductsPage';
import ProductDetailPage from '@/pages/user/ProductDetailPage';
import CustomizePage from '@/pages/user/CustomizePage';
import CartPage from '@/pages/user/CartPage';
import OrdersPage from '@/pages/user/OrdersPage';
import OrderDetailPage from '@/pages/user/OrderDetailPage';
import CoursesPage from '@/pages/user/CoursesPage';
import CourseDetailPage from '@/pages/user/CourseDetailPage';
import ResourcesPage from '@/pages/user/ResourcesPage';
import ProfilePage from '@/pages/user/ProfilePage';
import AdminDashboardPage from '@/pages/admin/AdminDashboardPage';
import AdminOrdersPage from '@/pages/admin/AdminOrdersPage';
import AdminProductsPage from '@/pages/admin/AdminProductsPage';
import AdminProductCategoriesPage from '@/pages/admin/AdminProductCategoriesPage';
import AdminCustomDesignsPage from '@/pages/admin/AdminCustomDesignsPage';
import AdminPatternsPage from '@/pages/admin/AdminPatternsPage';
import AdminPromptTemplatesPage from '@/pages/admin/AdminPromptTemplatesPage';
import AdminCoursesPage from '@/pages/admin/AdminCoursesPage';
import AdminResourcesPage from '@/pages/admin/AdminResourcesPage';
import AdminUsersPage from '@/pages/admin/AdminUsersPage';
import AdminHomePage from '@/pages/admin/AdminHomePage';
import AdminShopPage from '@/pages/admin/AdminShopPage';

export const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/home" replace /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  {
    path: '/',
    element: <UserLayout />,
    children: [
      { path: 'home', element: <HomePage /> },
      { path: 'patterns/generate', element: <PatternGeneratePage /> },
      { path: 'pattern-generate', element: <Navigate to="/patterns/generate" replace /> },
      { path: 'my/patterns', element: <RequireAuth><MyPatternsPage /></RequireAuth> },
      { path: 'my-patterns', element: <Navigate to="/my/patterns" replace /> },
      { path: 'products', element: <ProductsPage /> },
      { path: 'products/:productId', element: <ProductDetailPage /> },
      { path: 'customize', element: <RequireAuth><CustomizePage /></RequireAuth> },
      { path: 'cart', element: <RequireAuth><CartPage /></RequireAuth> },
      { path: 'orders', element: <RequireAuth><OrdersPage /></RequireAuth> },
      { path: 'orders/:orderId', element: <RequireAuth><OrderDetailPage /></RequireAuth> },
      { path: 'courses', element: <CoursesPage /> },
      { path: 'courses/:courseId', element: <CourseDetailPage /> },
      { path: 'resources', element: <ResourcesPage /> },
      { path: 'profile', element: <RequireAuth><ProfilePage /></RequireAuth> }
    ]
  },
  {
    path: '/admin',
    element: <RequireAdmin><AdminLayout /></RequireAdmin>,
    children: [
      { path: '', element: <Navigate to="dashboard" replace /> },
      { path: 'dashboard', element: <AdminDashboardPage /> },
      { path: 'orders', element: <AdminOrdersPage /> },
      { path: 'products', element: <AdminProductsPage /> },
      { path: 'product-categories', element: <AdminProductCategoriesPage /> },
      { path: 'custom-designs', element: <AdminCustomDesignsPage /> },
      { path: 'patterns', element: <AdminPatternsPage /> },
      { path: 'prompt-templates', element: <AdminPromptTemplatesPage /> },
      { path: 'courses', element: <AdminCoursesPage /> },
      { path: 'resources', element: <AdminResourcesPage /> },
      { path: 'users', element: <AdminUsersPage /> },
      { path: 'home', element: <AdminHomePage /> },
      { path: 'shop', element: <AdminShopPage /> }
    ]
  },
  { path: '*', element: <Navigate to="/home" replace /> }
]);
