import { useEffect, useState } from 'react';
import { Button, Table } from 'antd';
import { useNavigate } from 'react-router-dom';
import PageHeader from '@/components/PageHeader';
import { getMyOrdersApi, mockPayOrderApi } from '@/api/orders';
import { ORDER_STATUS_TEXT } from '@/constants/status';

export default function OrdersPage() {
  const navigate = useNavigate();
  const [list, setList] = useState<any[]>([]);
  const load = () => getMyOrdersApi({ page: 1, pageSize: 10 }).then((res: any) => setList(res?.list || []));
  useEffect(() => { load().catch(() => {}); }, []);
  return (
    <div className="page-card">
      <PageHeader title="我的订单" desc="接口对应：GET /api/orders/my。" />
      <Table rowKey="id" dataSource={list} columns={[
        { title: '订单号', dataIndex: 'orderNo' },
        { title: '金额', dataIndex: 'totalAmount' },
        { title: '状态', dataIndex: 'status', render: (v) => ORDER_STATUS_TEXT[v] || v },
        { title: '操作', render: (_, row: any) => <><Button type="link" onClick={() => navigate(`/orders/${row.id}`)}>详情</Button><Button type="link" onClick={() => mockPayOrderApi(row.id).then(load)}>模拟支付</Button></> }
      ]} />
    </div>
  );
}
