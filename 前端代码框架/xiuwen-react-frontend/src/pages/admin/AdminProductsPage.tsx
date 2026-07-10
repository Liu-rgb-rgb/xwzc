import { useEffect, useState } from 'react';
import { Button, Table } from 'antd';
import PageHeader from '@/components/PageHeader';
import { getAdminProductsApi, updateAdminProductStatusApi } from '@/api/adminProducts';
import { PRODUCT_STATUS_TEXT } from '@/constants/status';

export default function AdminProductsPage() {
  const [list, setList] = useState<any[]>([]);
  const load = () => getAdminProductsApi({ page: 1, pageSize: 10 }).then((res: any) => setList(res?.list || []));
  useEffect(() => { load().catch(() => {}); }, []);
  return <><PageHeader title="商品管理" desc="接口对应：/api/admin/products。" /><Button type="primary" style={{ marginBottom: 16 }}>新增商品</Button><Table rowKey="id" dataSource={list} columns={[{ title: '名称', dataIndex: 'name' }, { title: '价格', dataIndex: 'price' }, { title: '状态', dataIndex: 'status', render: (v) => PRODUCT_STATUS_TEXT[v] || v }, { title: '操作', render: (_, row: any) => <Button onClick={() => updateAdminProductStatusApi(row.id, { status: row.status === 'ON_SALE' ? 'OFF_SALE' : 'ON_SALE' }).then(load)}>上下架</Button> }]} /></>;
}
