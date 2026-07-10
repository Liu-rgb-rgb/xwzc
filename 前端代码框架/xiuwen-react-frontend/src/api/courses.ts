import { get, post } from '@/utils/request';
import type { PageParams } from '@/types/api';

export const getCourseCategoriesApi = () => get('/api/courses/categories');
export const getCoursesApi = (params?: PageParams & { categoryId?: number | string }) => get('/api/courses', params);
export const getCourseDetailApi = (courseId: number | string) => get(`/api/courses/${courseId}`);
export const studyCourseApi = (courseId: number | string) => post(`/api/courses/${courseId}/study`);
