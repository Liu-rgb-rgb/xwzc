import { useEffect, useState } from 'react';
import { Button, Card } from 'antd';
import { useParams } from 'react-router-dom';
import PageHeader from '@/components/PageHeader';
import { getCourseDetailApi, studyCourseApi } from '@/api/courses';

export default function CourseDetailPage() {
  const { courseId } = useParams();
  const [detail, setDetail] = useState<any>({});
  useEffect(() => { if (courseId) getCourseDetailApi(courseId).then(setDetail).catch(() => {}); }, [courseId]);
  return <div className="page-card"><PageHeader title={detail.title || '课程详情'} desc="接口对应：GET /api/courses/{courseId}。" /><Card><div dangerouslySetInnerHTML={{ __html: detail.content || detail.description || '课程内容区域' }} /><Button type="primary" onClick={() => courseId && studyCourseApi(courseId)}>开始学习</Button></Card></div>;
}
