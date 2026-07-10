import { useState } from 'react';
import { Button, Card, Form, Input, Select, InputNumber, message } from 'antd';
import PageHeader from '@/components/PageHeader';
import { generatePatternsApi, savePatternApi } from '@/api/patterns';

export default function PatternGeneratePage() {
  const [patterns, setPatterns] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  const onFinish = async (values: any) => {
    setLoading(true);
    try {
      const res: any = await generatePatternsApi({ ...values, count: values.count || 4 });
      setPatterns(res?.patterns || []);
      message.success('纹样生成成功');
    } finally { setLoading(false); }
  };

  return (
    <div className="page-card">
      <PageHeader title="AI 纹样生成" desc="接口对应：POST /api/patterns/generate；保存对应 POST /api/patterns/{id}/save。" />
      <Form layout="vertical" onFinish={onFinish} initialValues={{ style: 'classic', colorTheme: 'chinese_elegant', usageScene: 'product', count: 4 }}>
        <Form.Item label="风格" name="style"><Select options={[{ value: 'classic', label: '传统广绣' }, { value: 'new_chinese', label: '新中式' }]} /></Form.Item>
        <Form.Item label="元素" name="elements"><Select mode="tags" placeholder="牡丹、凤凰、祥云" /></Form.Item>
        <Form.Item label="配色" name="colorTheme"><Select options={[{ value: 'chinese_elegant', label: '中式典雅' }, { value: 'red_gold', label: '红金色' }]} /></Form.Item>
        <Form.Item label="应用场景" name="usageScene"><Select options={[{ value: 'product', label: '文创商品' }, { value: 'poster', label: '海报' }]} /></Form.Item>
        <Form.Item label="灵感描述" name="description"><Input.TextArea rows={4} placeholder="以牡丹和凤凰为主题，适合帆布袋使用..." /></Form.Item>
        <Form.Item label="生成数量" name="count"><InputNumber min={1} max={4} /></Form.Item>
        <Button type="primary" htmlType="submit" loading={loading}>生成纹样</Button>
      </Form>
      <div className="grid" style={{ marginTop: 24 }}>
        {patterns.map((item) => (
          <Card key={item.id} cover={<img className="cover" src={item.thumbnailUrl || item.imageUrl} />} actions={[<a onClick={() => savePatternApi(item.id).then(() => message.success('已保存'))}>保存</a>]}> 
            <Card.Meta title={item.title || `纹样 ${item.id}`} description={item.style} />
          </Card>
        ))}
      </div>
    </div>
  );
}
