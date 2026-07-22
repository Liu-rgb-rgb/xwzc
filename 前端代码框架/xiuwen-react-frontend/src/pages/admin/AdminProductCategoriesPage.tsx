import { useEffect, useState } from 'react';
import { Button } from 'antd';
import PageHeader from '@/components/PageHeader';
import PlaceholderTable from '@/components/PlaceholderTable';
import { getAdminProductCategoriesApi } from '@/api/adminProducts';

export default function AdminProductCategoriesPage() {
  const [list, setList] = useState<any[]>([]);
  useEffect(() => { getAdminProductCategoriesApi().then((res: any) => setList(res?.list || res || [])).catch(() => {}); }, []);
  return <><PageHeader title="商品分类" desc="接口对应：/api/admin/product-categories。" /><Button type="primary" style={{ marginBottom: 16 }}>新增分类</Button><PlaceholderTable data={list} /></>;
}
