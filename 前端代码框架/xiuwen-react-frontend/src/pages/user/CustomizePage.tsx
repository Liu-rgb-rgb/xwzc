import { Button, Form, InputNumber, message } from 'antd';
import { useSearchParams, useNavigate } from 'react-router-dom';
import PageHeader from '@/components/PageHeader';
import { createCustomDesignApi } from '@/api/customDesigns';

export default function CustomizePage() {
  const [search] = useSearchParams();
  const navigate = useNavigate();
  const onFinish = async (values: any) => {
    const res: any = await createCustomDesignApi({
      productId: search.get('productId') || values.productId,
      patternId: values.patternId,
      previewImageUrl: values.previewImageUrl || '',
      designConfig: { x: values.x, y: values.y, width: values.width, height: values.height, rotate: values.rotate }
    });
    message.success('定制方案已保存');
    navigate(`/customize/detail/${res?.customDesignId || ''}`);
  };
  return (
    <div className="page-card">
      <PageHeader title="商品定制预览" desc="接口对应：POST /api/custom-designs。第一版可先由前端 Canvas 生成预览图 URL。" />
      <Form layout="vertical" onFinish={onFinish} initialValues={{ productId: search.get('productId'), x: 120, y: 180, width: 260, height: 260, rotate: 0 }}>
        <Form.Item label="商品ID" name="productId" rules={[{ required: true }]}><InputNumber style={{ width: '100%' }} /></Form.Item>
        <Form.Item label="纹样ID" name="patternId" rules={[{ required: true }]}><InputNumber style={{ width: '100%' }} /></Form.Item>
        <Form.Item label="X" name="x"><InputNumber /></Form.Item>
        <Form.Item label="Y" name="y"><InputNumber /></Form.Item>
        <Form.Item label="宽度" name="width"><InputNumber /></Form.Item>
        <Form.Item label="高度" name="height"><InputNumber /></Form.Item>
        <Form.Item label="旋转" name="rotate"><InputNumber /></Form.Item>
        <Button type="primary" htmlType="submit">保存定制方案</Button>
      </Form>
    </div>
  );
}
