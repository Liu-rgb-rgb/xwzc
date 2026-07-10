import { useEffect, useState } from 'react';
import { Tabs } from 'antd';
import PageHeader from '@/components/PageHeader';
import PlaceholderTable from '@/components/PlaceholderTable';
import { getAdminHomeBannersApi, getAdminHomeRecommendsApi } from '@/api/adminHome';

export default function AdminHomePage() {
  const [banners, setBanners] = useState<any[]>([]);
  const [recommends, setRecommends] = useState<any[]>([]);
  useEffect(() => { getAdminHomeBannersApi().then((res: any) => setBanners(res?.list || res || [])).catch(() => {}); getAdminHomeRecommendsApi().then((res: any) => setRecommends(res?.list || res || [])).catch(() => {}); }, []);
  return <><PageHeader title="首页运营" desc="接口对应：/api/admin/home-banners 和 /api/admin/home-recommends。" /><Tabs items={[{ key: 'banners', label: 'Banner 管理', children: <PlaceholderTable data={banners} /> }, { key: 'recommends', label: '推荐位管理', children: <PlaceholderTable data={recommends} /> }]} /></>;
}
