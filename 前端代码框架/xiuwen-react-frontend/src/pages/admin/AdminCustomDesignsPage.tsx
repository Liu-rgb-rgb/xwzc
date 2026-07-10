import { useEffect, useState } from 'react';
import PageHeader from '@/components/PageHeader';
import PlaceholderTable from '@/components/PlaceholderTable';
import { getAdminCustomDesignsApi } from '@/api/adminProducts';

export default function AdminCustomDesignsPage() {
  const [list, setList] = useState<any[]>([]);
  useEffect(() => { getAdminCustomDesignsApi({ page: 1, pageSize: 10 }).then((res: any) => setList(res?.list || [])).catch(() => {}); }, []);
  return <><PageHeader title="定制管理" desc="接口对应：GET /api/admin/custom-designs。" /><PlaceholderTable data={list} /></>;
}
