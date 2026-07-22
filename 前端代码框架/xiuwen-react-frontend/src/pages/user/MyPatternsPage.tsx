import { useEffect, useState } from 'react';
import { Card, Button, message } from 'antd';
import PageHeader from '@/components/PageHeader';
import { getMyPatternsApi, favoritePatternApi, unfavoritePatternApi, deletePatternApi } from '@/api/patterns';
import type { PatternItem } from '@/types/business';

export default function MyPatternsPage() {
  const [list, setList] = useState<PatternItem[]>([]);
  const load = () => getMyPatternsApi({ page: 1, pageSize: 12, tab: 'all' }).then((res: any) => setList(res?.list || []));
  useEffect(() => { load().catch(() => {}); }, []);
  return (
    <div className="page-card">
      <PageHeader title="我的纹样" desc="接口对应：GET /api/patterns/my。" />
      <div className="grid">
        {list.map((item) => (
          <Card key={item.id} cover={<img className="cover" src={item.thumbnailUrl || item.imageUrl} />} actions={[
            <a onClick={() => (item.isFavorite ? unfavoritePatternApi(item.id) : favoritePatternApi(item.id)).then(load)}>收藏</a>,
            <a onClick={() => deletePatternApi(item.id).then(() => { message.success('已删除'); load(); })}>删除</a>
          ]}>
            <Card.Meta title={item.title} description={item.createdAt} />
          </Card>
        ))}
      </div>
    </div>
  );
}
