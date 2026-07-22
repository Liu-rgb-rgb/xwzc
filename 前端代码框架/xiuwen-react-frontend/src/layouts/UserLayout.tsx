import { Layout, Menu, Button, Badge, Space } from 'antd';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';
import { isAdminRole } from '@/constants/roles';

const { Header, Content, Footer } = Layout;

const menuItems = [
  { key: '/home', label: '首页' },
  { key: '/patterns/generate', label: 'AI纹样生成' },
  { key: '/my/patterns', label: '我的纹样' },
  { key: '/products', label: '文创商品' },
  { key: '/courses', label: '非遗课堂' },
  { key: '/resources', label: '创作资源' }
];

export default function UserLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { token, userInfo, logout } = useAuthStore();

  return (
    <Layout style={{ minHeight: '100vh', background: '#f7f2ea' }}>
      <Header style={{ display: 'flex', alignItems: 'center', gap: 24, background: '#fffaf3', borderBottom: '1px solid #ead9c5' }}>
        <div style={{ fontWeight: 800, fontSize: 22, color: '#6d3f1f', cursor: 'pointer' }} onClick={() => navigate('/home')}>绣纹智创</div>
        <Menu
          mode="horizontal"
          selectedKeys={[menuItems.find((i) => location.pathname.startsWith(i.key))?.key || '/home']}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          style={{ flex: 1, background: 'transparent', borderBottom: 'none' }}
        />
        <Space>
          <Button onClick={() => navigate('/cart')}><Badge count={0} size="small">购物车</Badge></Button>
          <Button onClick={() => navigate('/profile')}>个人中心</Button>
          {isAdminRole(userInfo?.role) && <Button type="primary" onClick={() => navigate('/admin/dashboard')}>商家后台</Button>}
          {token ? <Button onClick={() => { logout(); navigate('/login'); }}>退出</Button> : <Button type="primary" onClick={() => navigate('/login')}>登录</Button>}
        </Space>
      </Header>
      <Content style={{ maxWidth: 1200, width: '100%', margin: '24px auto', padding: '0 16px' }}>
        <Outlet />
      </Content>
      <Footer style={{ textAlign: 'center', background: 'transparent', color: '#8c7a66' }}>广绣 AI 纹样设计与文创定制平台</Footer>
    </Layout>
  );
}
