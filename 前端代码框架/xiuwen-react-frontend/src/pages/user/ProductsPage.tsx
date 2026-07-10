import { useEffect, useState } from 'react';
import { Card, Input } from 'antd';
import { useNavigate } from 'react-router-dom';
import PageHeader from '@/components/PageHeader';
import { getProductsApi } from '@/api/products';
import type { ProductItem } from '@/types/business';

export default function ProductsPage() {
  const navigate = useNavigate();
  const [list, setList] = useState<ProductItem[]>([]);
  const load = (keyword?: string) => getProductsApi({ page: 1, pageSize: 12, keyword }).then((res: any) => setList(res?.list || []));
  useEffect(() => { load().catch(() => {}); }, []);
  return (
    <div className="page-card">
      <PageHeader title="文创商品" desc="接口对应：GET /api/products。" />
      <div className="toolbar"><Input.Search placeholder="搜索商品" onSearch={load} /></div>
      <div className="grid">
        {list.map((item) => (
          <Card hoverable key={item.id} onClick={() => navigate(`/products/${item.id}`)} cover={<img className="cover" src={item.coverImage} />}>
            <Card.Meta title={item.name} description={`¥${item.price || 0}`} />
          </Card>
        ))}
      </div>
    </div>
  );
}
