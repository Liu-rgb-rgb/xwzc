import { useEffect, useState } from 'react';
import { Button, Select, Table } from 'antd';
import PageHeader from '@/components/PageHeader';
import { getAdminOrdersApi, updateAdminOrderStatusApi } from '@/api/adminOrders';
import { ORDER_STATUS_TEXT } from '@/constants/status';

export default function AdminOrdersPage() {
  const [list, setList] = useState<any[]>([]);
  const load = () => getAdminOrdersApi({ page: 1, pageSize: 10 }).then((res: any) => setList(res?.list || []));
  useEffect(() => { load().catch(() => {}); }, []);
  return <><PageHeader title="订单管理" desc="接口对应：GET /api/admin/orders，PUT /api/admin/orders/{orderId}/status。" /><Table rowKey="id" dataSource={list} columns={[
    { title: '订单号', dataIndex: 'orderNo' },
    { title: '金额', dataIndex: 'totalAmount' },
    { title: '状态', dataIndex: 'status', render: (v) => ORDER_STATUS_TEXT[v] || v },
    { title: '修改状态', render: (_, row: any) => <Select style={{ width: 160 }} value={row.status} onChange={(status) => updateAdminOrderStatusApi(row.id, { status }).then(load)} options={Object.entries(ORDER_STATUS_TEXT).map(([value, label]) => ({ value, label }))} /> }
  ]} /></>;
}
