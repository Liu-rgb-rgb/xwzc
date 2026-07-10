import { Layout, Menu, Button, Space } from 'antd';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';

const { Header, Sider, Content } = Layout;

const menuItems = [
  { key: '/admin/dashboard', label: '工作台' },
  { key: '/admin/orders', label: '订单管理' },
  { key: '/admin/products', label: '商品管理' },
  { key: '/admin/product-categories', label: '商品分类' },
  { key: '/admin/custom-designs', label: '定制管理' },
  { key: '/admin/patterns', label: '纹样管理' },
  { key: '/admin/prompt-templates', label: '提示词模板' },
  { key: '/admin/courses', label: '非遗课堂' },
  { key: '/admin/resources', label: '创作资源' },
  { key: '/admin/users', label: '用户管理' },
  { key: '/admin/home', label: '首页运营' },
  { key: '/admin/shop', label: '店铺设置' }
];

export default function AdminLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { userInfo, logout } = useAuthStore();
  const selectedKey = menuItems.find((item) => location.pathname.startsWith(item.key))?.key || '/admin/dashboard';

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider width={220} theme="light" style={{ borderRight: '1px solid #eee' }}>
        <div style={{ height: 64, display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800, fontSize: 20, color: '#6d3f1f' }}>绣纹智创后台</div>
        <Menu mode="inline" selectedKeys={[selectedKey]} items={menuItems} onClick={({ key }) => navigate(key)} />
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #eee' }}>
          <strong>商家管理端</strong>
          <Space>
            <span>{userInfo?.nickname || userInfo?.username}</span>
            <Button onClick={() => navigate('/home')}>返回前台</Button>
            <Button onClick={() => { logout(); navigate('/login'); }}>退出登录</Button>
          </Space>
        </Header>
        <Content style={{ padding: 24, background: '#f5f5f5' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
