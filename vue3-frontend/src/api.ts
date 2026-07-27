import axios from 'axios';

export type Id = number | string;
export type Query = Record<string, unknown>;
export type ApiQuery = Query;
export type Payload = Record<string, unknown>;

const http = axios.create({ baseURL: '/', timeout: 12000 });
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('xiuwen_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
http.interceptors.response.use((response) => response.data?.data ?? response.data);

const get = (url: string, params?: Query) => http.get(url, { params });
const post = (url: string, data?: unknown) => http.post(url, data);
const put = (url: string, data?: unknown) => http.put(url, data);
const del = (url: string, params?: Query) => http.delete(url, { params });

/** API V1.0：与《绣纹智创_完整API接口文档_V1.0》保持一致。 */
export const api = {
  auth: {
    register: (data: Payload) => post('/api/auth/register', data),
    login: (data: { username: string; password: string }) => post('/api/auth/login', data),
    logout: () => post('/api/auth/logout'),
    me: () => get('/api/auth/me')
  },
  user: {
    profile: () => get('/api/user/profile'),
    updateProfile: (data: Payload) => put('/api/user/profile', data),
    uploadAvatar: (data: FormData) => post('/api/user/avatar', data),
    updatePassword: (data: Payload) => put('/api/user/password', data),
    addresses: () => get('/api/user/addresses'),
    createAddress: (data: Payload) => post('/api/user/addresses', data),
    updateAddress: (addressId: Id, data: Payload) => put(`/api/user/addresses/${addressId}`, data),
    deleteAddress: (addressId: Id) => del(`/api/user/addresses/${addressId}`),
    setDefaultAddress: (addressId: Id) => put(`/api/user/addresses/${addressId}/default`)
  },
  messages: {
    list: (params: Query = {}) => get('/api/messages', params),
    unreadCount: () => get('/api/messages/unread-count'),
    markRead: (messageId: Id) => put(`/api/messages/${messageId}/read`),
    markAllRead: () => put('/api/messages/read-all'),
    remove: (messageId: Id) => del(`/api/messages/${messageId}`)
  },
  home: {
    detail: () => get('/api/home'),
    banners: () => get('/api/home/banners'),
    recommends: (params: Query = {}) => get('/api/home/recommends', params),
    search: (params: Query) => get('/api/search', params),
    shopInfo: () => get('/api/shop/info')
  },
  patterns: {
    options: () => get('/api/patterns/options'),
    generate: (data: Payload) => post('/api/patterns/generate', data),
    regenerate: (data: Payload) => post('/api/patterns/regenerate', data),
    generations: (params: Query = {}) => get('/api/pattern-generations/my', params),
    mine: (params: Query = {}) => get('/api/patterns/my', params),
    detail: (patternId: Id) => get(`/api/patterns/${patternId}`),
    save: (patternId: Id) => post(`/api/patterns/${patternId}/save`),
    favorite: (patternId: Id) => post(`/api/patterns/${patternId}/favorite`),
    unfavorite: (patternId: Id) => del(`/api/patterns/${patternId}/favorite`),
    remove: (patternId: Id) => del(`/api/patterns/${patternId}`),
    download: (patternId: Id) => get(`/api/patterns/${patternId}/download`)
  },
  products: {
    categories: () => get('/api/products/categories'),
    list: (params: Query = {}) => get('/api/products', params),
    detail: (productId: Id) => get(`/api/products/${productId}`),
    recommends: (params: Query = {}) => get('/api/products/recommends', params)
  },
  customDesigns: {
    create: (data: Payload) => post('/api/custom-designs', data),
    mine: (params: Query = {}) => get('/api/custom-designs/my', params),
    detail: (customDesignId: Id) => get(`/api/custom-designs/${customDesignId}`),
    remove: (customDesignId: Id) => del(`/api/custom-designs/${customDesignId}`)
  },
  cart: {
    items: () => get('/api/cart/items'),
    add: (data: Payload) => post('/api/cart/items', data),
    update: (cartItemId: Id, data: Payload) => put(`/api/cart/items/${cartItemId}`, data),
    remove: (cartItemId: Id) => del(`/api/cart/items/${cartItemId}`),
    clear: () => del('/api/cart/items')
  },
  orders: {
    create: (data: Payload) => post('/api/orders', data),
    mockPay: (orderId: Id) => post(`/api/orders/${orderId}/mock-pay`),
    mine: (params: Query = {}) => get('/api/orders/my', params),
    statusCount: () => get('/api/orders/status-count'),
    detail: (orderId: Id) => get(`/api/orders/${orderId}`),
    cancel: (orderId: Id, data: Payload = {}) => put(`/api/orders/${orderId}/cancel`, data),
    confirm: (orderId: Id) => put(`/api/orders/${orderId}/confirm`)
  },
  courses: {
    categories: () => get('/api/courses/categories'),
    list: (params: Query = {}) => get('/api/courses', params),
    detail: (courseId: Id) => get(`/api/courses/${courseId}`),
    study: (courseId: Id) => post(`/api/courses/${courseId}/study`)
  },
  resources: {
    list: (params: Query = {}) => get('/api/resources', params),
    detail: (resourceId: Id) => get(`/api/resources/${resourceId}`),
    download: (resourceId: Id) => get(`/api/resources/${resourceId}/download`)
  },
  files: { upload: (data: FormData) => post('/api/files/upload', data) },
  admin: {
    dashboard: () => get('/api/admin/dashboard'),
    orders: {
      list: (params: Query = {}) => get('/api/admin/orders', params),
      detail: (id: Id) => get(`/api/admin/orders/${id}`),
      updateStatus: (id: Id, data: Payload) => put(`/api/admin/orders/${id}/status`, data),
      updateRemark: (id: Id, data: Payload) => put(`/api/admin/orders/${id}/remark`, data)
    },
    productCategories: listCreateUpdateDelete('/api/admin/product-categories'),
    products: {
      ...crud('/api/admin/products'),
      updateStatus: (id: Id, data: Payload) => put(`/api/admin/products/${id}/status`, data)
    },
    customDesigns: {
      list: (params: Query = {}) => get('/api/admin/custom-designs', params),
      detail: (id: Id) => get(`/api/admin/custom-designs/${id}`),
      download: (id: Id) => get(`/api/admin/custom-designs/${id}/download`)
    },
    patterns: {
      list: (params: Query = {}) => get('/api/admin/patterns', params),
      detail: (id: Id) => get(`/api/admin/patterns/${id}`),
      recommend: (id: Id, data: Payload) => put(`/api/admin/patterns/${id}/recommend`, data),
      updateStatus: (id: Id, data: Payload) => put(`/api/admin/patterns/${id}/status`, data),
      remove: (id: Id) => del(`/api/admin/patterns/${id}`)
    },
    patternGenerations: {
      list: (params: Query = {}) => get('/api/admin/pattern-generations', params)
    },
    promptTemplates: listCreateUpdateDelete('/api/admin/prompt-templates'),
    courseCategories: listCreateUpdateDelete('/api/admin/course-categories'),
    courses: {
      ...crud('/api/admin/courses'),
      updateStatus: (id: Id, data: Payload) => put(`/api/admin/courses/${id}/status`, data)
    },
    resources: {
      ...crud('/api/admin/resources'),
      updateStatus: (id: Id, data: Payload) => put(`/api/admin/resources/${id}/status`, data)
    },
    users: {
      list: (params: Query = {}) => get('/api/admin/users', params),
      detail: (id: Id) => get(`/api/admin/users/${id}`),
      updateStatus: (id: Id, data: Payload) => put(`/api/admin/users/${id}/status`, data)
    },
    homeBanners: listCreateUpdateDelete('/api/admin/home-banners'),
    homeRecommends: listCreateUpdateDelete('/api/admin/home-recommends'),
    shop: {
      detail: () => get('/api/admin/shop/info'),
      update: (data: Payload) => put('/api/admin/shop/info', data)
    },
    sendMessage: (data: Payload) => post('/api/admin/messages', data),
    uploadFile: (data: FormData) => post('/api/admin/files/upload', data)
  }
};

function crud(base: string) {
  return {
    list: (params: Query = {}) => get(base, params),
    detail: (id: Id) => get(`${base}/${id}`),
    create: (data: Payload) => post(base, data),
    update: (id: Id, data: Payload) => put(`${base}/${id}`, data),
    remove: (id: Id) => del(`${base}/${id}`)
  };
}

function listCreateUpdateDelete(base: string) {
  const { list, create, update, remove } = crud(base);
  return { list, create, update, remove };
}

export function listFrom(result: any): any[] {
  if (Array.isArray(result)) return result;
  return result?.records ?? result?.list ?? result?.items ?? result?.content ?? [];
}
