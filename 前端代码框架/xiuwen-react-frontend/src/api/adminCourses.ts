import { get, post, put, del } from '@/utils/request';
import type { PageParams } from '@/types/api';

export const getAdminCourseCategoriesApi = (params?: PageParams) => get('/api/admin/course-categories', params);
export const createAdminCourseCategoryApi = (data: any) => post('/api/admin/course-categories', data);
export const updateAdminCourseCategoryApi = (categoryId: number | string, data: any) => put(`/api/admin/course-categories/${categoryId}`, data);
export const deleteAdminCourseCategoryApi = (categoryId: number | string) => del(`/api/admin/course-categories/${categoryId}`);

export const getAdminCoursesApi = (params?: PageParams & { status?: string; categoryId?: number | string }) => get('/api/admin/courses', params);
export const createAdminCourseApi = (data: any) => post('/api/admin/courses', data);
export const getAdminCourseDetailApi = (courseId: number | string) => get(`/api/admin/courses/${courseId}`);
export const updateAdminCourseApi = (courseId: number | string, data: any) => put(`/api/admin/courses/${courseId}`, data);
export const updateAdminCourseStatusApi = (courseId: number | string, data: { status: string }) => put(`/api/admin/courses/${courseId}/status`, data);
export const deleteAdminCourseApi = (courseId: number | string) => del(`/api/admin/courses/${courseId}`);
