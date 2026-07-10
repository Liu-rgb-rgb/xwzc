import { useEffect, useState } from 'react';
import { Button, Table } from 'antd';
import PageHeader from '@/components/PageHeader';
import { getAdminPatternsApi, recommendAdminPatternApi, updateAdminPatternStatusApi } from '@/api/adminPatterns';

export default function AdminPatternsPage() {
  const [list, setList] = useState<any[]>([]);
  const load = () => getAdminPatternsApi({ page: 1, pageSize: 10 }).then((res: any) => setList(res?.list || []));
  useEffect(() => { load().catch(() => {}); }, []);
  return <><PageHeader title="纹样管理" desc="接口对应：/api/admin/patterns。" /><Table rowKey="id" dataSource={list} columns={[{ title: '标题', dataIndex: 'title' }, { title: '状态', dataIndex: 'status' }, { title: '操作', render: (_, row: any) => <><Button type="link" onClick={() => recommendAdminPatternApi(row.id).then(load)}>推荐</Button><Button type="link" onClick={() => updateAdminPatternStatusApi(row.id, { status: 'HIDDEN' }).then(load)}>隐藏</Button></> }]} /></>;
}
