import { useEffect, useState } from 'react';
import { Button } from 'antd';
import PageHeader from '@/components/PageHeader';
import PlaceholderTable from '@/components/PlaceholderTable';
import { getAdminCoursesApi } from '@/api/adminCourses';

export default function AdminCoursesPage() {
  const [list, setList] = useState<any[]>([]);
  useEffect(() => { getAdminCoursesApi({ page: 1, pageSize: 10 }).then((res: any) => setList(res?.list || [])).catch(() => {}); }, []);
  return <><PageHeader title="非遗课堂管理" desc="接口对应：/api/admin/courses。" /><Button type="primary" style={{ marginBottom: 16 }}>新增课程</Button><PlaceholderTable data={list} /></>;
}
