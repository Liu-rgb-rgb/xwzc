import { Button, Card, Form, Input, Typography, message } from 'antd';
import { Link, useNavigate } from 'react-router-dom';
import { registerApi } from '@/api/auth';

export default function RegisterPage() {
  const navigate = useNavigate();
  const onFinish = async (values: any) => {
    await registerApi(values);
    message.success('注册成功，请登录');
    navigate('/login');
  };
  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'linear-gradient(135deg, #f7f2ea, #ead9c5)' }}>
      <Card style={{ width: 420, borderRadius: 18 }}>
        <Typography.Title level={3} style={{ textAlign: 'center', color: '#6d3f1f' }}>注册账号</Typography.Title>
        <Form layout="vertical" onFinish={onFinish}>
          <Form.Item label="账号" name="username" rules={[{ required: true, message: '请输入账号' }]}><Input /></Form.Item>
          <Form.Item label="昵称" name="nickname"><Input /></Form.Item>
          <Form.Item label="密码" name="password" rules={[{ required: true, message: '请输入密码' }]}><Input.Password /></Form.Item>
          <Button block type="primary" htmlType="submit">注册</Button>
        </Form>
        <div style={{ marginTop: 16, textAlign: 'center' }}><Link to="/login">已有账号，去登录</Link></div>
      </Card>
    </div>
  );
}
