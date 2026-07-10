import { useEffect, useState } from 'react';
import { Button, message } from 'antd';
import PageHeader from '@/components/PageHeader';
import PlaceholderTable from '@/components/PlaceholderTable';
import { getCartItemsApi, clearCartItemsApi } from '@/api/cart';

export default function CartPage() {
  const [list, setList] = useState<any[]>([]);
  const load = () => getCartItemsApi().then((res: any) => setList(res?.list || res || []));
  useEffect(() => { load().catch(() => {}); }, []);
  return (
    <div className="page-card">
      <PageHeader title="购物车" desc="接口对应：GET /api/cart/items。" />
      <Button danger onClick={() => clearCartItemsApi().then(() => { message.success('已清空'); load(); })}>清空购物车</Button>
      <div style={{ marginTop: 16 }}><PlaceholderTable data={list} /></div>
    </div>
  );
}
