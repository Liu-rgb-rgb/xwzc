import { useEffect, useState } from 'react';
import { Button } from 'antd';
import PageHeader from '@/components/PageHeader';
import PlaceholderTable from '@/components/PlaceholderTable';
import { getAdminResourcesApi } from '@/api/adminResources';

export default function AdminResourcesPage() {
  const [list, setList] = useState<any[]>([]);
  useEffect(() => { getAdminResourcesApi({ page: 1, pageSize: 10 }).then((res: any) => setList(res?.list || [])).catch(() => {}); }, []);
  return <><PageHeader title="创作资源管理" desc="接口对应：/api/admin/resources。" /><Button type="primary" style={{ marginBottom: 16 }}>新增资源</Button><PlaceholderTable data={list} /></>;
}
