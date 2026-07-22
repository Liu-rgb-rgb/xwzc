import { get, post, del } from '@/utils/request';
import type { PageParams } from '@/types/api';

export interface GeneratePatternParams {
  style?: string;
  elements?: string[];
  colorTheme?: string;
  usageScene?: string;
  description?: string;
  referenceImageUrl?: string;
  count?: number;
}

export const getPatternOptionsApi = () => get('/api/patterns/options');
export const generatePatternsApi = (data: GeneratePatternParams) => post('/api/patterns/generate', data);
export const regeneratePatternsApi = (data: any) => post('/api/patterns/regenerate', data);
export const getMyPatternGenerationsApi = (params?: PageParams) => get('/api/pattern-generations/my', params);
export const getMyPatternsApi = (params?: PageParams & { tab?: string; style?: string }) => get('/api/patterns/my', params);
export const getPatternDetailApi = (patternId: number | string) => get(`/api/patterns/${patternId}`);
export const savePatternApi = (patternId: number | string) => post(`/api/patterns/${patternId}/save`);
export const favoritePatternApi = (patternId: number | string) => post(`/api/patterns/${patternId}/favorite`);
export const unfavoritePatternApi = (patternId: number | string) => del(`/api/patterns/${patternId}/favorite`);
export const deletePatternApi = (patternId: number | string) => del(`/api/patterns/${patternId}`);
export const downloadPatternApi = (patternId: number | string) => get(`/api/patterns/${patternId}/download`);
