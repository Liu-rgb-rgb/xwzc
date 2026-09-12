<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { api, listFrom, type ApiQuery, type Payload } from '../../api';

type FieldType = 'text' | 'textarea' | 'number' | 'select' | 'image';
type Choice = { label: string; value: string | number };
type Field = { key: string; label: string; type?: FieldType; required?: boolean; options?: Choice[]; defaultValue?: string | number; sourceKey?: string; bizType?: string };
type Spec = { fields?: Field[]; list?: (params?: ApiQuery) => Promise<any>; detail?: (id: number | string) => Promise<any>; create?: (data: Payload) => Promise<any>; update?: (id: number | string, data: Payload) => Promise<any>; remove?: (id: number | string) => Promise<any> };

const route = useRoute();
const keyword = ref('');
const rows = ref<any[]>([]);
const loading = ref(false);
const error = ref('');
const notice = ref('');
const dialog = ref<'form' | 'detail' | 'order' | 'status' | null>(null);
const formMode = ref<'create' | 'edit'>('create');
const form = ref<Record<string, any>>({});
const detailRecord = ref<Record<string, any> | null>(null);
const selectedRow = ref<any>(null);
const saving = ref(false);
const uploadingField = ref('');
const productCategoryOptions = ref<Choice[]>([]);

const moduleName = computed(() => String(route.path.split('/').pop() || 'dashboard'));
const title = computed(() => String(route.meta.title || '管理模块'));
const yesNo: Choice[] = [{ label: '否', value: 0 }, { label: '是', value: 1 }];
const publishStatuses: Choice[] = [{ label: '草稿', value: 'DRAFT' }, { label: '已发布', value: 'PUBLISHED' }, { label: '已隐藏', value: 'HIDDEN' }];
const productStatuses: Choice[] = [{ label: '已上架', value: 'ON_SALE' }, { label: '缓存（草稿）', value: 'DRAFT' }, { label: '已下架', value: 'OFF_SALE' }, { label: '已售罄', value: 'SOLD_OUT' }];
const productQuickStatuses: Choice[] = [{ label: '上架', value: 'ON_SALE' }, { label: '下架', value: 'OFF_SALE' }, { label: '缓存（草稿）', value: 'DRAFT' }];
const productStatusLabels: Record<string, string> = { ON_SALE: '已上架', OFF_SALE: '已下架', DRAFT: '缓存（草稿）', SOLD_OUT: '已售罄' };

const specs: Record<string, Spec> = {
  dashboard: { list: () => api.admin.dashboard() },
  orders: { list: (params) => api.admin.orders.list(params), detail: (id) => api.admin.orders.detail(id) },
  'product-categories': {
    list: (params) => api.admin.productCategories.list(params), create: (data) => api.admin.productCategories.create(data), update: (id, data) => api.admin.productCategories.update(id, data), remove: (id) => api.admin.productCategories.remove(id),
    fields: [{ key: 'name', label: '分类名称', required: true }, { key: 'icon', label: '分类图标', type: 'image', bizType: 'PRODUCT_CATEGORY' }, { key: 'parentId', label: '上级分类 ID', type: 'number' }, { key: 'sort', label: '排序', type: 'number', defaultValue: 0 }, { key: 'status', label: '状态', type: 'select', defaultValue: 'NORMAL', options: [{ label: '启用', value: 'NORMAL' }, { label: '禁用', value: 'DISABLED' }] }]
  },
  products: {
    list: (params) => api.admin.products.list(params), detail: (id) => api.admin.products.detail(id), create: (data) => api.admin.products.create(data), update: (id, data) => api.admin.products.update(id, data), remove: (id) => api.admin.products.remove(id),
    fields: [{ key: 'categoryId', label: '商品分类', type: 'select', required: true }, { key: 'name', label: '商品名称', required: true }, { key: 'subtitle', label: '副标题' }, { key: 'price', label: '价格', type: 'number', required: true }, { key: 'stock', label: '库存', type: 'number', required: true }, { key: 'status', label: '商品状态', type: 'select', options: productStatuses, defaultValue: 'ON_SALE', required: true }, { key: 'coverImage', label: '封面图片', type: 'image', bizType: 'PRODUCT' }, { key: 'mockupImage', label: '定制底图', type: 'image', bizType: 'PRODUCT' }, { key: 'description', label: '商品描述', type: 'textarea' }, { key: 'isCustomizable', label: '支持定制', type: 'select', options: yesNo, defaultValue: 1 }, { key: 'isRecommend', label: '首页推荐', type: 'select', options: yesNo, defaultValue: 0 }, { key: 'sort', label: '排序', type: 'number', defaultValue: 0 }]
  },
  'custom-designs': { list: (params) => api.admin.customDesigns.list(params), detail: (id) => api.admin.customDesigns.detail(id) },
  patterns: { list: (params) => api.admin.patterns.list(params), detail: (id) => api.admin.patterns.detail(id), remove: (id) => api.admin.patterns.remove(id) },
  'pattern-generations': { list: (params) => api.admin.patternGenerations.list(params) },
  'prompt-templates': {
    list: (params) => api.admin.promptTemplates.list(params), create: (data) => api.admin.promptTemplates.create(data), update: (id, data) => api.admin.promptTemplates.update(id, data), remove: (id) => api.admin.promptTemplates.remove(id),
    fields: [{ key: 'name', label: '模板名称', required: true }, { key: 'style', label: '适用风格', required: true }, { key: 'usageScene', label: '使用场景' }, { key: 'colorTheme', label: '颜色主题' }, { key: 'templateText', label: '提示词内容', type: 'textarea', required: true }, { key: 'status', label: '状态', type: 'select', options: [{ label: '启用', value: 1 }, { label: '停用', value: 0 }], defaultValue: 1 }, { key: 'sort', label: '排序', type: 'number', defaultValue: 0 }]
  },
  'course-categories': {
    list: (params) => api.admin.courseCategories.list(params), create: (data) => api.admin.courseCategories.create(data), update: (id, data) => api.admin.courseCategories.update(id, data), remove: (id) => api.admin.courseCategories.remove(id),
    fields: [{ key: 'name', label: '分类名称', required: true }, { key: 'description', label: '分类说明', type: 'textarea' }, { key: 'parentId', label: '上级分类 ID', type: 'number' }, { key: 'sort', label: '排序', type: 'number', defaultValue: 0 }, { key: 'status', label: '状态', type: 'select', options: [{ label: '启用', value: 1 }, { label: '禁用', value: 0 }], defaultValue: 1 }]
  },
  courses: {
    list: (params) => api.admin.courses.list(params), detail: (id) => api.admin.courses.detail(id), create: (data) => api.admin.courses.create(data), update: (id, data) => api.admin.courses.update(id, data), remove: (id) => api.admin.courses.remove(id),
    fields: [{ key: 'id', sourceKey: 'categoryId', label: '课程分类 ID', type: 'number' }, { key: 'title', label: '课程标题', required: true }, { key: 'subtitle', label: '副标题' }, { key: 'coverImage', label: '课程封面', type: 'image', bizType: 'COURSE' }, { key: 'price', label: '价格', type: 'number' }, { key: 'teacherName', label: '讲师' }, { key: 'duration', label: '课时' }, { key: 'difficulty', label: '难度' }, { key: 'description', label: '课程简介', type: 'textarea' }, { key: 'content', label: '课程正文', type: 'textarea' }, { key: 'videoUrl', label: '视频地址' }, { key: 'isRecommend', label: '首页推荐', type: 'select', options: yesNo, defaultValue: 0 }, { key: 'sort', label: '排序', type: 'number', defaultValue: 0 }, { key: 'status', label: '状态', type: 'select', options: publishStatuses, defaultValue: 'DRAFT' }]
  },
  resources: {
    list: (params) => api.admin.resources.list(params), detail: (id) => api.admin.resources.detail(id), create: (data) => api.admin.resources.create(data), update: (id, data) => api.admin.resources.update(id, data), remove: (id) => api.admin.resources.remove(id),
    fields: [{ key: 'id', sourceKey: 'courseId', label: '关联课程 ID', type: 'number' }, { key: 'title', label: '资源标题', required: true }, { key: 'subtitle', label: '副标题' }, { key: 'resourceType', label: '资源类型', required: true }, { key: 'coverImage', label: '资源封面', type: 'image', bizType: 'RESOURCE' }, { key: 'resourceUrl', label: '资源链接' }, { key: 'price', label: '价格', type: 'number' }, { key: 'content', label: '资源说明', type: 'textarea' }, { key: 'isRecommend', label: '首页推荐', type: 'select', options: yesNo, defaultValue: 0 }, { key: 'sort', label: '排序', type: 'number', defaultValue: 0 }, { key: 'status', label: '状态', type: 'select', options: publishStatuses, defaultValue: 'DRAFT' }]
  },
  users: { list: (params) => api.admin.users.list(params), detail: (id) => api.admin.users.detail(id) },
  home: { list: async (params) => {
    const [banners, recommends] = await Promise.all([api.admin.homeBanners.list(params), api.admin.homeRecommends.list(params)]);
    return [...listFrom(banners).map((row) => ({ ...row, entryKind: 'banner' })), ...listFrom(recommends).map((row) => ({ ...row, entryKind: 'recommend' }))];
  } },
  shop: { list: () => api.admin.shop.detail() },
  messages: { list: async () => [] }
};

const homeFields = computed<Field[]>(() => form.value.entryKind === 'recommend' ? [
  { key: 'entryKind', label: '配置类型', type: 'select', options: [{ label: '推荐位', value: 'recommend' }, { label: 'Banner', value: 'banner' }], defaultValue: 'recommend' },
  { key: 'recommendType', label: '推荐类型', required: true, type: 'select', options: [{ label: '纹样', value: 'PATTERN' }, { label: '商品', value: 'PRODUCT' }, { label: '课程', value: 'COURSE' }, { label: '资源', value: 'RESOURCE' }] }, { key: 'relatedId', label: '关联内容 ID', type: 'number', required: true }, { key: 'title', label: '标题' }, { key: 'coverImage', label: '封面图片', type: 'image', bizType: 'HOME_RECOMMEND' }, { key: 'description', label: '描述', type: 'textarea' }, { key: 'sort', label: '排序', type: 'number', defaultValue: 0 }, { key: 'status', label: '状态', type: 'select', options: [{ label: '启用', value: 1 }, { label: '停用', value: 0 }], defaultValue: 1 }
] : [
  { key: 'entryKind', label: '配置类型', type: 'select', options: [{ label: 'Banner', value: 'banner' }, { label: '推荐位', value: 'recommend' }], defaultValue: 'banner' }, { key: 'title', label: 'Banner 标题', required: true }, { key: 'imageUrl', label: 'Banner 图片', type: 'image', required: true, bizType: 'HOME_BANNER' }, { key: 'subtitle', label: '副标题' }, { key: 'buttonText', label: '按钮文字' }, { key: 'linkType', label: '链接类型' }, { key: 'linkId', label: '关联内容 ID', type: 'number' }, { key: 'linkUrl', label: '外部链接' }, { key: 'sort', label: '排序', type: 'number', defaultValue: 0 }, { key: 'status', label: '状态', type: 'select', options: [{ label: '启用', value: 1 }, { label: '停用', value: 0 }], defaultValue: 1 }
]);
const shopFields: Field[] = [{ key: 'shopName', label: '店铺名称', required: true }, { key: 'logo', label: '店铺 Logo', type: 'image', bizType: 'SHOP' }, { key: 'slogan', label: '店铺标语' }, { key: 'contactName', label: '联系人' }, { key: 'contactPhone', label: '联系电话' }, { key: 'email', label: '邮箱' }, { key: 'address', label: '地址' }, { key: 'description', label: '店铺介绍', type: 'textarea' }, { key: 'status', label: '状态', type: 'select', options: [{ label: '营业', value: 1 }, { label: '停业', value: 0 }], defaultValue: 1 }];
const messageFields: Field[] = [{ key: 'title', label: '消息标题', required: true }, { key: 'content', label: '消息内容', type: 'textarea', required: true }, { key: 'noticeType', label: '消息类型', required: true, type: 'select', options: [{ label: '系统通知', value: 'SYSTEM' }, { label: '订单通知', value: 'ORDER' }, { label: '活动通知', value: 'ACTIVITY' }] }, { key: 'userId', label: '接收用户 ID（留空则全站）', type: 'number' }, { key: 'relatedType', label: '关联类型' }, { key: 'relatedId', label: '关联内容 ID', type: 'number' }];
const orderStatusLabels: Record<string, string> = { WAIT_PAY: '待支付', WAIT_CONFIRM: '待确认', PRODUCING: '制作中', WAIT_DELIVERY: '待发货', DELIVERED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消' };
const orderNextStatus: Record<string, string> = { WAIT_CONFIRM: 'PRODUCING', PRODUCING: 'WAIT_DELIVERY', WAIT_DELIVERY: 'DELIVERED' };
const orderFields = computed<Field[]>(() => {
  const current = String(selectedRow.value?.status || '');
  const next = orderNextStatus[current];
  const options: Choice[] = current ? [{ label: `${orderStatusLabels[current] || current}（当前）`, value: current }] : [];
  if (next) options.push({ label: orderStatusLabels[next] || next, value: next });
  return [{ key: 'status', label: '订单状态', type: 'select', options }, { key: 'merchantRemark', label: '商家备注', type: 'textarea' }];
});

const spec = computed(() => specs[moduleName.value] || {});
const visibleRows = computed(() => rows.value.filter((row) => JSON.stringify(row).toLowerCase().includes(keyword.value.trim().toLowerCase())));
const canCreate = computed(() => Boolean(spec.value.create) || ['home', 'messages'].includes(moduleName.value));
const formFields = computed(() => dialog.value === 'status' ? [{ key: 'status', label: '商品状态', type: 'select', options: productQuickStatuses, required: true } as Field] : moduleName.value === 'home' ? homeFields.value : moduleName.value === 'shop' ? shopFields : moduleName.value === 'messages' ? messageFields : dialog.value === 'order' ? orderFields.value : spec.value.fields || []);
function fieldOptions(field: Field) { return moduleName.value === 'products' && field.key === 'categoryId' ? productCategoryOptions.value : field.options || []; }
function messageFrom(reason: any) { return reason?.response?.data?.message || reason?.message || '操作失败，请检查填写内容和后端服务。'; }
function rowId(row: any) { return row.id || row.orderId || row.productId || row.courseId || row.resourceId || row.userId || row.customDesignId; }
function rowName(row: any) { return row.name || row.title || row.orderNo || row.shopName || row.username || row.nickname || row.id || '-'; }
function rowStatus(row: any) { const status = row.statusName || row.status; if (moduleName.value === 'products' && status) return productStatusLabels[String(status)] || status; return status || (row.isRead === 1 ? '已读' : row.isRead === 0 ? '未读' : '正常'); }
function rowTime(row: any) { return row.updatedAt || row.updateTime || row.createdAt || row.createTime || '-'; }
function showNotice(text: string) { notice.value = text; window.setTimeout(() => { if (notice.value === text) notice.value = ''; }, 3200); }
function isListResult(result: any) { return Boolean(result && ['records', 'list', 'items', 'content'].some((key) => Array.isArray(result[key]))); }
async function loadRows() { const loader = spec.value.list; if (!loader) return; loading.value = true; error.value = ''; try { const result = await loader({ page: 1, pageSize: 50, keyword: keyword.value }); const list = listFrom(result); rows.value = list.length || isListResult(result) ? list : result && typeof result === 'object' ? [result] : []; } catch (reason: any) { rows.value = []; error.value = messageFrom(reason); } finally { loading.value = false; } }
async function loadProductCategories() {
  if (moduleName.value !== 'products') return;
  try {
    const result = await api.admin.productCategories.list({ status: 'NORMAL' });
    productCategoryOptions.value = listFrom(result).map((item: any) => ({ label: item.name, value: item.id }));
    const current = Number(form.value.categoryId);
    if (dialog.value === 'form' && formMode.value === 'create' && !productCategoryOptions.value.some((item) => Number(item.value) === current)) {
      form.value.categoryId = productCategoryOptions.value[0]?.value ?? '';
    }
  } catch (reason: any) {
    error.value = messageFrom(reason);
  }
}
function initialForm(row?: Record<string, any>) { const values: Record<string, any> = {}; const fields = moduleName.value === 'home' ? homeFields.value : moduleName.value === 'shop' ? shopFields : moduleName.value === 'messages' ? messageFields : spec.value.fields || []; for (const field of fields) values[field.key] = row?.[field.sourceKey || field.key] ?? field.defaultValue ?? ''; return values; }
function openCreate() { formMode.value = 'create'; selectedRow.value = null; form.value = moduleName.value === 'home' ? { entryKind: 'banner' } : initialForm(); dialog.value = 'form'; error.value = ''; if (moduleName.value === 'products') void loadProductCategories(); }
function openEdit(row: any) { if (moduleName.value === 'orders') { openOrder(row); return; } if (moduleName.value === 'shop') { formMode.value = 'edit'; selectedRow.value = row; form.value = initialForm(row); dialog.value = 'form'; return; } if (!spec.value.update && moduleName.value !== 'home') return; formMode.value = 'edit'; selectedRow.value = row; form.value = moduleName.value === 'home' ? { ...row, entryKind: row.entryKind || 'banner' } : initialForm(row); dialog.value = 'form'; error.value = ''; }
async function openDetail(row: any) { error.value = ''; selectedRow.value = row; try { const id = rowId(row); if (spec.value.detail && id == null) throw new Error('当前数据缺少有效 ID，请刷新列表后重试'); detailRecord.value = spec.value.detail ? await spec.value.detail(id) : row; dialog.value = 'detail'; } catch (reason: any) { error.value = messageFrom(reason); } }
function openOrder(row: any) { selectedRow.value = row; form.value = { status: row.status || '', merchantRemark: row.merchantRemark || '' }; dialog.value = 'order'; error.value = ''; }
function closeDialog() { dialog.value = null; selectedRow.value = null; saving.value = false; }
async function selectImage(field: Field, event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  if (!file.type.startsWith('image/')) { error.value = '请选择 JPG、PNG、WebP 等图片文件。'; input.value = ''; return; }
  if (file.size > 50 * 1024 * 1024) { error.value = '图片不能超过 50MB。'; input.value = ''; return; }
  uploadingField.value = field.key;
  error.value = '';
  try {
    const data = new FormData();
    data.append('file', file);
    data.append('bizType', field.bizType || moduleName.value.toUpperCase().replace(/-/g, '_'));
    const result: any = await api.admin.uploadFile(data);
    const fileUrl = result?.fileUrl || result?.url;
    if (!fileUrl) throw new Error('上传接口未返回图片地址');
    form.value[field.key] = fileUrl;
    showNotice(`${field.label}上传成功`);
  } catch (reason: any) {
    error.value = messageFrom(reason);
  } finally {
    uploadingField.value = '';
    input.value = '';
  }
}
function payloadFromForm() { const data: Record<string, any> = {}; for (const field of formFields.value) { if (field.key === 'entryKind') continue; const value = form.value[field.key]; if (value === '' || value === undefined || value === null) continue; data[field.key] = field.type === 'number' ? Number(value) : value; } return data; }
async function saveForm() { saving.value = true; error.value = ''; try { const data = payloadFromForm(); if (dialog.value === 'status') { const id = rowId(selectedRow.value); if (id == null) throw new Error('商品 ID 无效，请刷新列表后重试'); await api.admin.products.updateStatus(id, { status: data.status }); } else if (dialog.value === 'order') { const id = rowId(selectedRow.value); if (id == null) throw new Error('订单 ID 无效，请刷新列表后重试'); if (data.status && data.status !== selectedRow.value?.status) await api.admin.orders.updateStatus(id, { status: data.status }); if (data.merchantRemark !== undefined) await api.admin.orders.updateRemark(id, { remark: data.merchantRemark }); } else if (moduleName.value === 'home') { const target = form.value.entryKind === 'recommend' ? api.admin.homeRecommends : api.admin.homeBanners; const id = rowId(selectedRow.value); if (formMode.value === 'edit' && id == null) throw new Error('配置 ID 无效，请刷新列表后重试'); formMode.value === 'create' ? await target.create(data) : await target.update(id, data); } else if (moduleName.value === 'shop') await api.admin.shop.update(data); else if (moduleName.value === 'messages') await api.admin.sendMessage(data); else if (formMode.value === 'create') await spec.value.create?.(data); else { const id = rowId(selectedRow.value); if (id == null) throw new Error('数据 ID 无效，请刷新列表后重试'); await spec.value.update?.(id, data); } showNotice(dialog.value === 'status' ? '商品状态已更新' : formMode.value === 'create' ? '新增成功' : '保存成功'); closeDialog(); await loadRows(); } catch (reason: any) { error.value = messageFrom(reason); } finally { saving.value = false; } }
async function removeRow(row: any) { if (!window.confirm(`确认删除“${rowName(row)}”吗？`)) return; try { if (moduleName.value === 'home') { const target = row.entryKind === 'recommend' ? api.admin.homeRecommends : api.admin.homeBanners; await target.remove(rowId(row)); } else await spec.value.remove?.(rowId(row)); showNotice('删除成功'); await loadRows(); } catch (reason: any) { error.value = messageFrom(reason); } }
async function changeStatus(row: any, kind: 'product' | 'course' | 'resource' | 'pattern' | 'user') { if (kind === 'product') { selectedRow.value = row; form.value = { status: row.status || 'DRAFT' }; dialog.value = 'status'; error.value = ''; return; } const current = String(row.status ?? row.statusName ?? ''); const next = window.prompt('请输入新状态', current); if (!next || next === current) return; try { const id = rowId(row); if (kind === 'course') await api.admin.courses.updateStatus(id, { status: next }); if (kind === 'resource') await api.admin.resources.updateStatus(id, { status: next }); if (kind === 'pattern') await api.admin.patterns.updateStatus(id, { status: next }); if (kind === 'user') await api.admin.users.updateStatus(id, { status: Number(next) }); showNotice('状态已更新'); await loadRows(); } catch (reason: any) { error.value = messageFrom(reason); } }
async function toggleRecommend(row: any) { try { await api.admin.patterns.recommend(rowId(row), { isRecommend: row.isRecommend === 1 ? 0 : 1 }); showNotice('推荐状态已更新'); await loadRows(); } catch (reason: any) { error.value = messageFrom(reason); } }
async function downloadDesign(row: any) { try { const urls: any = await api.admin.customDesigns.download(rowId(row)); const url = urls?.previewUrl || urls?.patternUrl || Object.values(urls || {}).find((value) => typeof value === 'string'); if (url) window.open(String(url), '_blank', 'noopener'); else showNotice('该记录暂无可下载文件'); } catch (reason: any) { error.value = messageFrom(reason); } }
watch(() => route.path, () => { keyword.value = ''; closeDialog(); loadRows(); if (moduleName.value === 'products') void loadProductCategories(); }, { immediate: true });
</script>

<template>
  <section class="admin-page">
    <div class="admin-title"><div><span>MANAGEMENT</span><h1>{{ title }}</h1></div><button v-if="canCreate" class="primary" @click="openCreate">＋ {{ moduleName === 'messages' ? '发布消息' : '新增' }}</button></div>
    <div class="admin-toolbar"><input v-model="keyword" placeholder="搜索名称、编号或关键词" @keyup.enter="loadRows" /><button @click="loadRows">搜索</button></div>
    <p v-if="error" class="form-error">{{ error }}</p><p v-if="notice" class="form-success">{{ notice }}</p>
    <div class="metric-row"><article><span>全部数据</span><b>{{ rows.length }}</b></article><article><span>当前模块</span><b>{{ title }}</b></article><article><span>接口状态</span><b>{{ loading ? '加载中' : error ? '异常' : '正常' }}</b></article></div>
    <table><thead><tr><th>ID</th><th>名称</th><th>状态</th><th>更新时间</th><th>操作</th></tr></thead><tbody><tr v-if="loading"><td colspan="5">正在加载...</td></tr><tr v-else-if="!visibleRows.length"><td colspan="5">暂无数据</td></tr><tr v-for="row in visibleRows" :key="`${moduleName}-${rowId(row)}`"><td>{{ rowId(row) || '-' }}</td><td>{{ rowName(row) }}</td><td><i>{{ rowStatus(row) }}</i></td><td>{{ rowTime(row) }}</td><td class="row-actions"><button @click="openDetail(row)">查看</button><button v-if="spec.update || moduleName === 'orders' || moduleName === 'shop' || moduleName === 'home'" @click="openEdit(row)">编辑</button><button v-if="['products', 'courses', 'resources', 'patterns', 'users'].includes(moduleName)" @click="changeStatus(row, moduleName.slice(0, -1) as any)">状态</button><button v-if="moduleName === 'patterns'" @click="toggleRecommend(row)">{{ row.isRecommend === 1 ? '取消推荐' : '设为推荐' }}</button><button v-if="moduleName === 'custom-designs'" @click="downloadDesign(row)">下载</button><button v-if="spec.remove || moduleName === 'home'" class="danger" @click="removeRow(row)">删除</button></td></tr></tbody></table>
    <div v-if="dialog" class="admin-dialog-mask" @click.self="closeDialog"><div class="admin-dialog"><button class="dialog-close" aria-label="关闭" @click="closeDialog">×</button><template v-if="dialog === 'detail'"><h2>{{ title }}详情</h2><dl class="detail-list"><template v-for="(value, key) in detailRecord" :key="String(key)"><dt>{{ key }}</dt><dd>{{ typeof value === 'object' ? JSON.stringify(value) : value ?? '-' }}</dd></template></dl></template><template v-else><h2>{{ dialog === 'order' ? '处理订单' : dialog === 'status' ? '设置商品状态' : `${formMode === 'create' ? '新增' : '编辑'}${title}` }}</h2><p v-if="error" class="form-error">{{ error }}</p><form class="admin-form" @submit.prevent="saveForm"><label v-for="field in formFields" :key="field.key" :class="{ wide: field.type === 'textarea' || field.type === 'image' }"><span>{{ field.label }}<em v-if="field.required"> *</em></span><textarea v-if="field.type === 'textarea'" v-model="form[field.key]" :required="field.required" rows="4" /><select v-else-if="field.type === 'select'" v-model="form[field.key]" :required="field.required"><option value="">{{ moduleName === 'products' && field.key === 'categoryId' && !fieldOptions(field).length ? '暂无分类，请先新增商品分类' : '请选择' }}</option><option v-for="option in fieldOptions(field)" :key="String(option.value)" :value="option.value">{{ option.label }}</option></select><div v-else-if="field.type === 'image'" class="admin-image-upload"><input :id="`image-${field.key}`" class="admin-image-input" type="file" accept="image/*" @change="selectImage(field, $event)" /><label class="admin-image-picker" :class="{ disabled: Boolean(uploadingField) }" :for="`image-${field.key}`">{{ uploadingField === field.key ? '正在上传…' : form[field.key] ? '重新选择图片' : '从本地选择图片' }}</label><div v-if="form[field.key]" class="admin-image-preview"><img :src="form[field.key]" :alt="field.label" /><span>已选择并上传</span><button type="button" @click.stop.prevent="form[field.key] = ''">移除</button></div></div><input v-else v-model="form[field.key]" :type="field.type === 'number' ? 'number' : 'text'" :required="field.required" :step="field.type === 'number' ? 'any' : undefined" /></label><div class="dialog-actions"><span v-if="error" class="dialog-inline-error">{{ error }}</span><button type="button" @click="closeDialog">取消</button><button class="primary" type="submit" :disabled="saving || Boolean(uploadingField)">{{ saving ? '保存中...' : uploadingField ? '图片上传中...' : dialog === 'status' ? '确认设置' : '保存' }}</button></div></form></template></div></div>
  </section>
</template>
