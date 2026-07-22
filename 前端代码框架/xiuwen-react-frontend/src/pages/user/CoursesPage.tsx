import { useEffect, useState } from 'react';
import { Card } from 'antd';
import { useNavigate } from 'react-router-dom';
import PageHeader from '@/components/PageHeader';
import { getCoursesApi } from '@/api/courses';

export default function CoursesPage() {
  const navigate = useNavigate();
  const [list, setList] = useState<any[]>([]);
  useEffect(() => { getCoursesApi({ page: 1, pageSize: 12 }).then((res: any) => setList(res?.list || [])).catch(() => {}); }, []);
  return <div className="page-card"><PageHeader title="非遗课堂" desc="接口对应：GET /api/courses。" /><div className="grid">{list.map((item) => <Card hoverable key={item.id} onClick={() => navigate(`/courses/${item.id}`)} cover={<img className="cover" src={item.coverImage} />}><Card.Meta title={item.title} description={item.summary} /></Card>)}</div></div>;
}
