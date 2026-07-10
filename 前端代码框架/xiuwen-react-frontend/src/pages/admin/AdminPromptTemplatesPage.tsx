import { useEffect, useState } from 'react';
import { Button } from 'antd';
import PageHeader from '@/components/PageHeader';
import PlaceholderTable from '@/components/PlaceholderTable';
import { getAdminPromptTemplatesApi } from '@/api/adminPatterns';

export default function AdminPromptTemplatesPage() {
  const [list, setList] = useState<any[]>([]);
  useEffect(() => { getAdminPromptTemplatesApi({ page: 1, pageSize: 10 }).then((res: any) => setList(res?.list || [])).catch(() => {}); }, []);
  return <><PageHeader title="提示词模板" desc="接口对应：/api/admin/prompt-templates。" /><Button type="primary" style={{ marginBottom: 16 }}>新增模板</Button><PlaceholderTable data={list} /></>;
}
