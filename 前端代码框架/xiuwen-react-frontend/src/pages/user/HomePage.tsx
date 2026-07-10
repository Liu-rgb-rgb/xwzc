import { useEffect, useState } from 'react';
import { Button, Card, Col, Row, Typography } from 'antd';
import { useNavigate } from 'react-router-dom';
import PageHeader from '@/components/PageHeader';
import { getHomeApi } from '@/api/home';

export default function HomePage() {
  const navigate = useNavigate();
  const [home, setHome] = useState<any>({});
  useEffect(() => { getHomeApi().then(setHome).catch(() => {}); }, []);

  const cards = [
    { title: 'AI纹样生成', desc: '输入灵感，生成广绣风格纹样', path: '/patterns/generate' },
    { title: '文创商品', desc: '帆布袋、明信片、丝巾、杯垫等', path: '/products' },
    { title: '我的纹样', desc: '管理保存、收藏、应用记录', path: '/my/patterns' },
    { title: '非遗课堂', desc: '学习广绣文化与纹样知识', path: '/courses' }
  ];

  return (
    <>
      <div className="page-card" style={{ background: 'linear-gradient(135deg, #fffaf3, #e9d4b8)' }}>
        <Typography.Title style={{ color: '#5a351c' }}>绣纹智创 · 广绣 AI 纹样设计平台</Typography.Title>
        <Typography.Paragraph>从 AI 生成纹样，到文创商品定制，再到商家后台订单处理。</Typography.Paragraph>
        <Button type="primary" size="large" onClick={() => navigate('/patterns/generate')}>开始生成纹样</Button>
      </div>
      <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
        {cards.map((item) => (
          <Col xs={24} md={12} lg={6} key={item.path}>
            <Card hoverable onClick={() => navigate(item.path)} title={item.title}>{item.desc}</Card>
          </Col>
        ))}
      </Row>
      <div style={{ marginTop: 24 }} className="page-card">
        <PageHeader title="首页推荐数据" desc="这里会展示 /api/home 返回的 Banner、推荐纹样、商品、课程、资源。" />
        <pre style={{ whiteSpace: 'pre-wrap' }}>{JSON.stringify(home, null, 2)}</pre>
      </div>
    </>
  );
}
