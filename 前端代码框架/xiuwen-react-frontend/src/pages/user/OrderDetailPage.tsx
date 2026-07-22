import { useEffect, useState } from 'react';
import { Button, Descriptions } from 'antd';
import { useParams } from 'react-router-dom';
import PageHeader from '@/components/PageHeader';
import { getOrderDetailApi, cancelOrderApi, confirmOrderApi } from '@/api/orders';
import { ORDER_STATUS_TEXT } from '@/constants/status';

export default function OrderDetailPage() {
  const { orderId } = useParams();
  const [detail, setDetail] = useState<any>({});
  const load = () => orderId && getOrderDetailApi(orderId).then(setDetail);
  useEffect(() => { load()?.catch(() => {}); }, [orderId]);
  return (
    <div className="page-card">
      <PageHeader title="订单详情" desc="接口对应：GET /api/orders/{orderId}。" />
      <Descriptions column={1} bordered>
        <Descriptions.Item label="订单号">{detail.orderNo}</Descriptions.Item>
        <Descriptions.Item label="金额">{detail.totalAmount}</Descriptions.Item>
        <Descriptions.Item label="状态">{ORDER_STATUS_TEXT[detail.status] || detail.status}</Descriptions.Item>
      </Descriptions>
      <div style={{ marginTop: 16 }}>
        <Button onClick={() => orderId && cancelOrderApi(orderId).then(load)}>取消订单</Button>
        <Button type="primary" style={{ marginLeft: 8 }} onClick={() => orderId && confirmOrderApi(orderId).then(load)}>确认收货</Button>
      </div>
    </div>
  );
}
