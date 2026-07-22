import { useEffect, useState } from 'react';
import { Card } from 'antd';
import PageHeader from '@/components/PageHeader';
import { getResourcesApi, downloadResourceApi } from '@/api/resources';

export default function ResourcesPage() {
  const [list, setList] = useState<any[]>([]);
  useEffect(() => { getResourcesApi({ page: 1, pageSize: 12 }).then((res: any) => setList(res?.list || [])).catch(() => {}); }, []);
  return <div className="page-card"><PageHeader title="创作资源" desc="接口对应：GET /api/resources。" /><div className="grid">{list.map((item) => <Card key={item.id} cover={<img className="cover" src={item.coverImage} />} actions={[<a onClick={() => downloadResourceApi(item.id)}>下载</a>]}><Card.Meta title={item.title} description={item.resourceType} /></Card>)}</div></div>;
}
