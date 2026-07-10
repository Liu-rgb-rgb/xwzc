import { useEffect, useState } from 'react';
import PageHeader from '@/components/PageHeader';
import PlaceholderTable from '@/components/PlaceholderTable';
import { getAdminUsersApi } from '@/api/adminUsers';

export default function AdminUsersPage() {
  const [list, setList] = useState<any[]>([]);
  useEffect(() => { getAdminUsersApi({ page: 1, pageSize: 10 }).then((res: any) => setList(res?.list || [])).catch(() => {}); }, []);
  return <><PageHeader title="用户管理" desc="接口对应：GET /api/admin/users。" /><PlaceholderTable data={list} /></>;
}
