export interface PatternItem {
  id: number;
  title: string;
  imageUrl: string;
  thumbnailUrl?: string;
  style?: string;
  isFavorite?: boolean;
  isApplied?: boolean;
  createdAt?: string;
}

export interface ProductItem {
  id: number;
  name: string;
  price: number;
  coverImage?: string;
  isCustomizable?: boolean;
  salesCount?: number;
  status?: string;
}

export interface OrderItem {
  id: number;
  orderNo?: string;
  totalAmount?: number;
  status: string;
  createdAt?: string;
}

export interface CourseItem {
  id: number;
  title: string;
  coverImage?: string;
  summary?: string;
  status?: string;
}

export interface ResourceItem {
  id: number;
  title: string;
  coverImage?: string;
  resourceType?: string;
  downloadUrl?: string;
}
