import { Button, Card, Form, Input, Typography, message } from 'antd';
import { useNavigate, Link } from 'react-router-dom';
import { loginApi } from '@/api/auth';
import { useAuthStore } from '@/store/authStore';
import { isAdminRole } from '@/constants/roles';

export default function LoginPage() {
  const navigate = useNavigate();
  const setLogin = useAuthStore((s) => s.setLogin);

  const onFinish = async (values: any) => {
    const res = await loginApi(values);
    setLogin(res.token, res.userInfo);
    message.success('登录成功');
    navigate(isAdminRole(res.userInfo.role) ? '/admin/dashboard' : '/home');
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'linear-gradient(135deg, #f7f2ea, #ead9c5)' }}>
      <Card style={{ width: 420, borderRadius: 18 }}>
        <Typography.Title level={2} style={{ textAlign: 'center', color: '#6d3f1f' }}>绣纹智创</Typography.Title>
        <Typography.Paragraph style={{ textAlign: 'center' }} type="secondary">广绣 AI 纹样设计与文创定制平台</Typography.Paragraph>
        <Form layout="vertical" initialValues={{ username: 'admin', password: '123456' }} onFinish={onFinish}>
          <Form.Item label="账号" name="username" rules={[{ required: true, message: '请输入账号' }]}><Input placeholder="admin" /></Form.Item>
          <Form.Item label="密码" name="password" rules={[{ required: true, message: '请输入密码' }]}><Input.Password placeholder="123456" /></Form.Item>
          <Button block type="primary" htmlType="submit">登录</Button>
        </Form>
        <div style={{ marginTop: 16, display: 'flex', justifyContent: 'space-between' }}>
          <Link to="/register">注册账号</Link>
          <Link to="/home">体验版进入</Link>
        </div>
      </Card>
    </div>
  );
}
