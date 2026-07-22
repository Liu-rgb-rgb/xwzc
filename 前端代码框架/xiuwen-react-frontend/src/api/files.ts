import { post } from '@/utils/request';

export const uploadFileApi = (file: File, bizType = 'common') => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('bizType', bizType);
  return post('/api/files/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
};
