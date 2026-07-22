import { useEffect, useState } from 'react';
import { Card, Col, Row, Statistic } from 'antd';
import PageHeader from '@/components/PageHeader';
import { getAdminDashboardApi } from '@/api/adminDashboard';

export default function AdminDashboardPage() {
  const [data, setData] = useState<any>({});
  useEffect(() => { getAdminDashboardApi().then(setData).catch(() => {}); }, []);
  return (
    <>
      <PageHeader title="商家工作台" desc="接口对应：GET /api/admin/dashboard。" />
      <Row gutter={[16, 16]}>
        <Col span={6}><Card><Statistic title="今日订单" value={data.todayOrderCount || 0} /></Card></Col>
        <Col span={6}><Card><Statistic title="待处理订单" value={data.pendingOrderCount || 0} /></Card></Col>
        <Col span={6}><Card><Statistic title="本月销售额" value={data.monthSalesAmount || 0} prefix="¥" /></Card></Col>
        <Col span={6}><Card><Statistic title="纹样数量" value={data.patternCount || 0} /></Card></Col>
      </Row>
    </>
  );
}
