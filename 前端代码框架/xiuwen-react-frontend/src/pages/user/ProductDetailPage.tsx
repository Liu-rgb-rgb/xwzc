import { useEffect, useState } from 'react';
import { Button, Card, Descriptions } from 'antd';
import { useNavigate, useParams } from 'react-router-dom';
import PageHeader from '@/components/PageHeader';
import { getProductDetailApi } from '@/api/products';

export default function ProductDetailPage() {
  const { productId } = useParams();
  const navigate = useNavigate();
  const [detail, setDetail] = useState<any>({});
  useEffect(() => { if (productId) getProductDetailApi(productId).then(setDetail).catch(() => {}); }, [productId]);
  return (
    <div className="page-card">
      <PageHeader title="商品详情" desc="接口对应：GET /api/products/{productId}。" />
      <Card>
        <img className="cover" style={{ maxWidth: 360 }} src={detail.coverImage} />
        <Descriptions column={1} style={{ marginTop: 16 }}>
          <Descriptions.Item label="商品名称">{detail.name}</Descriptions.Item>
          <Descriptions.Item label="价格">¥{detail.price}</Descriptions.Item>
          <Descriptions.Item label="是否支持定制">{detail.isCustomizable ? '支持' : '不支持'}</Descriptions.Item>
        </Descriptions>
        <Button type="primary" onClick={() => navigate(`/customize?productId=${productId}`)}>立即定制</Button>
      </Card>
    </div>
  );
}
