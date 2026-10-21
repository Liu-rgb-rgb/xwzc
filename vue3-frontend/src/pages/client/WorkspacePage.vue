<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api, listFrom } from '../../api';
import { patterns, products } from '../../data';
import { isRealClient } from '../../auth';
import { readUserData, userDataEvent, writeUserData } from '../../userData';
import PatternComposer from '../../components/PatternComposer.vue';
import { downloadResource, resourceCover } from '../../resourceFiles';

type DesignConfig = { x: number; y: number; scale: number; rotation: number };

const route = useRoute();
const router = useRouter();
const mode = computed(() => String(route.meta.mode || 'resources'));
const title = computed(() => String(route.meta.title || '功能中心'));
const items = ref<any[]>([]);
const selectedCartKeys = ref<Set<string>>(new Set());
const knownCartKeys = ref<Set<string>>(new Set());
const profile = ref<any>({ nickname: '', phone: '', email: '', intro: '' });
const customProductId = ref<number>(0);
const customPatternId = ref<number>(0);
const customProducts = ref<any[]>([]);
const customPatterns = ref<any[]>([]);
const customizeLoading = ref(false);
const customizeError = ref('');
const designConfig = ref<DesignConfig>({ x: 0.5, y: 0.48, scale: 0.75, rotation: 0 });
const composer = ref<{ exportBlob: () => Promise<Blob> } | null>(null);
const generatingPreview = ref(false);
const customNote = ref('');
const notice = ref('');
const savedDesigns = ref<any[]>([]);
const addedDesignIds = ref<Set<string>>(new Set());
const addedDesignTimers = new Map<string, ReturnType<typeof setTimeout>>();
const selectedItem = ref<any>(null);
const detailLoading = ref(false);
const downloadingResourceId = ref<string | number | null>(null);
const submittingOrder = ref(false);
const clearingCart = ref(false);
const clearConfirmOpen = ref(false);
const updatingCartKeys = ref<Set<string>>(new Set());
const removingCartKeys = ref<Set<string>>(new Set());
const cartClearing = ref(false);
const editingNoteKey = ref('');
const noteDraft = ref('');
const removeTimers = new Map<string, ReturnType<typeof setTimeout>>();
const addresses = ref<any[]>([]);
const addressDrawerOpen = ref(false);
const addressLoading = ref(false);
const selectedAddressId = ref<string>('');
const messages = ref<any[]>([]);
const addressForm = ref<any>({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: 0
});
const editingAddress = ref<any>(null);

function imageUrl(...values: unknown[]) {
  return String(
    values.find((value) => typeof value === 'string' && /^(https?:|data:|blob:|\/)/.test(value)) ||
      ''
  );
}

// 列表缩略图用 OSS 图片处理生成小图（原图 1~3MB → 约 100KB），避免一次性拉取几十张大图
// 挤占 OSS 连接、拖慢画布对当前纹样原图的加载。画布本身仍用原图保证清晰度。
function thumbUrl(url: string, width = 200) {
  if (!/^https?:/i.test(url) || url.includes('x-oss-process=')) return url;
  return `${url}${url.includes('?') ? '&' : '?'}x-oss-process=image/resize,w_${width}`;
}

function normalizeCustomProduct(product: any) {
  return {
    ...product,
    id: product.id,
    title: product.title || product.name || `商品 #${product.id}`,
    image: imageUrl(product.mockupImage, product.coverImage, product.imageUrl, product.image)
  };
}

function normalizeCustomPattern(pattern: any, index: number) {
  const id = pattern.id ?? pattern.patternId;
  return {
    ...pattern,
    id,
    title: pattern.title || pattern.name || `AI 纹样 #${id ?? index + 1}`,
    image: imageUrl(pattern.imageUrl, pattern.thumbnailUrl, pattern.previewImageUrl, pattern.image)
  };
}

function uniqueById(values: any[]) {
  return values.filter(
    (value, index, all) =>
      value?.id != null &&
      all.findIndex((other) => String(other?.id) === String(value.id)) === index
  );
}

function productStock(product: any) {
  return Math.max(0, Number(product?.stock ?? 0) || 0);
}

const availableCustomProducts = computed(() =>
  customProducts.value.filter((product) => productStock(product) > 0)
);
const selectedCustomProduct = computed(() =>
  availableCustomProducts.value.find(
    (product) => String(product.id) === String(customProductId.value)
  )
);
const selectedCustomPattern = computed(
  () =>
    customPatterns.value.find((pattern) => String(pattern.id) === String(customPatternId.value)) ||
    customPatterns.value[0]
);

// 缩略图本身加载失败说明该 OSS 图片不可用，直接从候选列表移除，避免画布选中后无法绘制。
// 这里不阻塞页面加载（相比逐一探测全部大图），只在实际发现坏图时按需处理。
function onPatternThumbError(pattern: any) {
  const removed = customPatterns.value.some((p) => String(p.id) === String(pattern.id));
  if (!removed) return;
  customPatterns.value = customPatterns.value.filter((p) => String(p.id) !== String(pattern.id));
  notice.value = '已移除一个图片暂不可用的纹样，请重新选择';
}

async function loadCustomizeOptions() {
  customizeLoading.value = true;
  customizeError.value = '';
  try {
    const [productResult, patternResult, designResult] = await Promise.all([
      api.products.list({ page: 1, pageSize: 100 }),
      api.patterns.list({ page: 1, pageSize: 100 }),
      api.customDesigns.mine({ page: 1, pageSize: 50 })
    ]);
    customProducts.value = uniqueById(listFrom(productResult).map(normalizeCustomProduct)).filter(
      (product) =>
        product.image && product.status === 'ON_SALE' && Number(product.isCustomizable) === 1
    );
    const candidatePatterns = uniqueById(
      listFrom(patternResult).map((pattern, index) => normalizeCustomPattern(pattern, index))
    ).filter(
      (pattern) => pattern.image && pattern.generationId != null && pattern.status === 'NORMAL'
    );
    // 直接展示候选纹样，避免逐一探测几十张 1~3MB 的 OSS 大图导致整页长时间停在"加载中"。
    // 个别图片若真的加载失败，由下方画布加载与列表缩略图 @error 兜底提示。
    customPatterns.value = candidatePatterns;
    savedDesigns.value = listFrom(designResult);
    writeUserData('applied_designs', savedDesigns.value);
  } catch (reason: any) {
    customProducts.value = [];
    customPatterns.value = [];
    savedDesigns.value = [];
    customizeError.value =
      reason?.response?.data?.message || '定制数据加载失败，请确认已经登录并稍后重试';
  } finally {
    customizeLoading.value = false;
  }
  const requestedProductId = Number(route.query.productId);
  const requestedProduct = customProducts.value.find(
    (product) => String(product.id) === String(requestedProductId)
  );
  const currentProduct = availableCustomProducts.value.find(
    (product) => String(product.id) === String(customProductId.value)
  );
  const nextProduct =
    (requestedProduct && productStock(requestedProduct) > 0 ? requestedProduct : null) ||
    currentProduct ||
    availableCustomProducts.value[0];
  customProductId.value = Number(nextProduct?.id || 0);
  if (requestedProduct && productStock(requestedProduct) <= 0) {
    notice.value = '该商品已售罄，暂不可定制，已为您选择其他有库存商品';
  }
  if (
    !customPatterns.value.some((pattern) => String(pattern.id) === String(customPatternId.value))
  ) {
    const requestedPatternId = Number(route.query.patternId);
    const requestedPattern = customPatterns.value.find(
      (pattern) => String(pattern.id) === String(requestedPatternId)
    );
    customPatternId.value = Number(requestedPattern?.id || customPatterns.value[0]?.id || 0);
  }
}

async function loadItems() {
  items.value = [];
  if (mode.value === 'cart') {
    const localCart = readUserData<any[]>('cart_items', []);
    items.value = localCart;
    try {
      const serverCart = listFrom(await api.cart.items());
      if (!localCart.length && serverCart.length) {
        items.value = serverCart.map((item) => ({
          ...item,
          cartItemId: item.id,
          serverCartItem: true
        }));
      }
    } catch {
      /* 后端未启动时直接使用本地购物车。 */
    }
    syncCartSelection();
    return;
  }
  if (mode.value === 'orders') {
    const localOrders = readUserData<any[]>('user_orders', []);
    items.value = localOrders;
    try {
      const serverOrders = listFrom(await api.orders.mine());
      items.value = [...serverOrders, ...localOrders].filter(
        (order, index, values) =>
          values.findIndex(
            (other) => String(other.id || other.orderNo) === String(order.id || order.orderNo)
          ) === index
      );
    } catch {
      /* 后端未启动时显示本地提交的订单。 */
    }
    return;
  }
  try {
    if (mode.value === 'resources') items.value = listFrom(await api.resources.list());
    else if (mode.value === 'profile') {
      profile.value = await api.user.profile();
      addresses.value = listFrom(await api.user.addresses());
      messages.value = listFrom(await api.messages.list());
    }
  } catch {
    if (mode.value === 'profile') profile.value = readUserData<any>('profile', profile.value);
  }
  if (!items.value.length && mode.value !== 'profile') {
    items.value = patterns.slice(0, 6);
  }
}
async function saveAddress() {
  const data = { ...addressForm.value };
  try {
    const r = editingAddress.value
      ? await api.user.updateAddress(editingAddress.value.id, data)
      : await api.user.createAddress(data);
    if (editingAddress.value)
      addresses.value = addresses.value.map((a) =>
        a.id === editingAddress.value.id ? r || { ...a, ...data } : a
      );
    else addresses.value.push(r || { ...data, id: Date.now() });
  } catch {
    addresses.value.push({ ...data, id: Date.now() });
  }
  editingAddress.value = null;
  addressForm.value = {
    receiverName: '',
    receiverPhone: '',
    province: '',
    city: '',
    district: '',
    detailAddress: '',
    isDefault: 0
  };
}
async function deleteAddress(a: any) {
  try {
    await api.user.deleteAddress(a.id);
  } catch {}
  addresses.value = addresses.value.filter((x) => x.id !== a.id);
}
async function setDefault(a: any) {
  try {
    await api.user.setDefaultAddress(a.id);
  } catch {}
  addresses.value = addresses.value.map((x) => ({ ...x, isDefault: x.id === a.id ? 1 : 0 }));
}
async function markAllMessages() {
  try {
    await api.messages.markAllRead();
  } catch {}
  messages.value = messages.value.map((m) => ({ ...m, isRead: 1 }));
}

function handleUserData(event: Event) {
  if ((event as CustomEvent).detail?.name === 'cart_items' && mode.value === 'cart') loadItems();
  if ((event as CustomEvent).detail?.name === 'applied_designs') {
    savedDesigns.value = readUserData<any[]>('applied_designs', []);
  }
}

onMounted(() => {
  // 从纹样库“应用到商品”或商品页“立即定制”进入时，带入用户刚选择的对象。
  const queryProductId = Number(route.query.productId);
  const queryPatternId = Number(route.query.patternId);
  if (queryProductId) customProductId.value = queryProductId;
  if (queryPatternId) customPatternId.value = queryPatternId;
  loadItems();
  if (mode.value === 'customize') void loadCustomizeOptions();
  if (mode.value !== 'customize') savedDesigns.value = [];
  window.addEventListener(userDataEvent, handleUserData);
});
onBeforeUnmount(() => {
  window.removeEventListener(userDataEvent, handleUserData);
  addedDesignTimers.forEach((timer) => clearTimeout(timer));
  addedDesignTimers.clear();
  removeTimers.forEach((timer) => clearTimeout(timer));
  removeTimers.clear();
});
watch(mode, (value) => {
  void loadItems();
  if (value === 'customize') void loadCustomizeOptions();
});
watch(
  () => route.query.productId,
  (value) => {
    if (mode.value !== 'customize') return;
    const requested = customProducts.value.find((product) => String(product.id) === String(value));
    if (requested && productStock(requested) > 0) {
      customProductId.value = Number(requested.id);
      notice.value = '';
    } else if (requested) {
      notice.value = '该商品已售罄，暂不可定制';
    }
  }
);

function cartProduct(item: any) {
  const source = products.find(
    (product) => String(product.id) === String(item.productId || item.id)
  );
  return { ...source, ...item };
}

function cartItemKey(item: any) {
  if (item.cartItemId != null) return String(item.cartItemId);
  if (item.customDesignId != null) return `custom-${item.customDesignId}`;
  return String(item.productId ?? item.id);
}

function cartIdentity(item: any) {
  return [item.productId ?? item.id, item.patternId || '', item.customDesignId || ''].join('-');
}

function isCustomCartItem(item: any) {
  return item.customDesignId != null || item.isCustomDesign === true;
}

function cartSpecs(item: any) {
  const product = cartProduct(item);
  const specs: Array<{ label: string; value: string }> = [];
  const patternTitle = String(item.patternTitle || item.patternName || '').trim();
  if (patternTitle) specs.push({ label: '纹样', value: patternTitle });
  const config = item.designConfig;
  if (config) {
    const scale = Number(config.scale);
    if (Number.isFinite(scale))
      specs.push({ label: '图案缩放', value: `${Math.round(scale * 100)}%` });
    const rotation = Number(config.rotation);
    if (Number.isFinite(rotation)) specs.push({ label: '旋转', value: `${Math.round(rotation)}°` });
  }
  const category = String(product.category || product.type || '').trim();
  if (category) specs.push({ label: '类别', value: category });
  if (!specs.length) specs.push({ label: '规格', value: '待补充' });
  return specs;
}

function cartItemNote(item: any) {
  return String(item.remark || '').trim();
}

function startEditNote(item: any) {
  editingNoteKey.value = cartItemKey(item);
  noteDraft.value = cartItemNote(item);
}

function cancelEditNote() {
  editingNoteKey.value = '';
}

function saveCartNote(item: any) {
  const key = cartItemKey(item);
  if (editingNoteKey.value !== key) return;
  const remark = noteDraft.value.trim();
  const localCart = readUserData<any[]>('cart_items', []);
  const exists = localCart.some((entry) => cartItemKey(entry) === key);
  writeUserData(
    'cart_items',
    exists
      ? localCart.map((entry) => (cartItemKey(entry) === key ? { ...entry, remark } : entry))
      : [...localCart, { ...item, remark }]
  );
  items.value = items.value.map((entry) =>
    cartItemKey(entry) === key ? { ...entry, remark } : entry
  );
  editingNoteKey.value = '';
}

function onCartItemClick(item: any, event: MouseEvent) {
  const target = event.target as HTMLElement | null;
  if (target?.closest('button, input, label, textarea, .cart-specs')) return;
  toggleCartSelection(item);
}

function syncCartSelection() {
  const currentKeys = new Set(items.value.map(cartItemKey));
  const nextKeys = new Set([...selectedCartKeys.value].filter((key) => currentKeys.has(key)));
  currentKeys.forEach((key) => {
    if (!knownCartKeys.value.has(key)) nextKeys.add(key);
  });
  selectedCartKeys.value = nextKeys;
  knownCartKeys.value = currentKeys;
}

function isCartSelected(item: any) {
  return selectedCartKeys.value.has(cartItemKey(item));
}

function toggleCartSelection(item: any) {
  const key = cartItemKey(item);
  const nextKeys = new Set(selectedCartKeys.value);
  if (nextKeys.has(key)) nextKeys.delete(key);
  else nextKeys.add(key);
  selectedCartKeys.value = nextKeys;
  if (item.serverCartItem && item.cartItemId) {
    void api.cart.update(item.cartItemId, { selected: nextKeys.has(key) ? 1 : 0 }).catch(() => {});
  }
}

async function updateCartQuantity(item: any, change: number) {
  const cart = readUserData<any[]>('cart_items', []);
  const key = cartItemKey(item);
  if (updatingCartKeys.value.has(key)) return;
  if (change < 0 && Number(item.quantity || 1) <= 1) return;
  const quantity = Math.max(0, Number(item.quantity || 1) + change);

  if (item.cartItemId != null) {
    const pending = new Set(updatingCartKeys.value);
    pending.add(key);
    updatingCartKeys.value = pending;
    try {
      if (quantity === 0) await api.cart.remove(item.cartItemId);
      else await api.cart.update(item.cartItemId, { quantity });
    } catch {
      notice.value = '数量更新失败，请稍后重试';
      return;
    } finally {
      const completed = new Set(updatingCartKeys.value);
      completed.delete(key);
      updatingCartKeys.value = completed;
    }
  }

  const next = cart
    .map((entry) => (cartItemKey(entry) === key ? { ...entry, quantity } : entry))
    .filter((entry) => Number(entry.quantity || 0) > 0);
  writeUserData('cart_items', next);
  items.value = items.value
    .map((entry) => (cartItemKey(entry) === key ? { ...entry, quantity } : entry))
    .filter((entry) => Number(entry.quantity || 0) > 0);
  if (quantity === 0) {
    const nextSelected = new Set(selectedCartKeys.value);
    nextSelected.delete(key);
    selectedCartKeys.value = nextSelected;
    knownCartKeys.value = new Set(items.value.map(cartItemKey));
  }
}

function animateRemove(item: any) {
  const key = cartItemKey(item);
  if (removingCartKeys.value.has(key)) return;
  removingCartKeys.value = new Set([...removingCartKeys.value, key]);
  removeTimers.set(
    key,
    setTimeout(async () => {
      removeTimers.delete(key);
      await removeFromCart(item);
      const next = new Set(removingCartKeys.value);
      next.delete(key);
      removingCartKeys.value = next;
    }, 300)
  );
}

async function removeFromCart(item: any) {
  const key = cartItemKey(item);
  if (item.cartItemId != null) {
    try {
      await api.cart.remove(item.cartItemId);
    } catch {
      notice.value = '移除失败，请稍后重试';
      return;
    }
  }
  writeUserData(
    'cart_items',
    readUserData<any[]>('cart_items', []).filter((entry) => cartItemKey(entry) !== key)
  );
  items.value = items.value.filter((entry) => cartItemKey(entry) !== key);
  const nextSelected = new Set(selectedCartKeys.value);
  nextSelected.delete(key);
  selectedCartKeys.value = nextSelected;
  knownCartKeys.value = new Set(items.value.map(cartItemKey));
}

function clearCart() {
  if (!items.value.length || clearingCart.value) return;
  clearConfirmOpen.value = true;
}

async function confirmClearCart() {
  clearConfirmOpen.value = false;
  if (clearingCart.value) return;
  clearingCart.value = true;
  cartClearing.value = true;
  const staggerDelay = 300 + Math.max(0, items.value.length - 1) * 70;
  await new Promise((resolve) => setTimeout(resolve, staggerDelay));
  try {
    await api.cart.clear();
    writeUserData('cart_items', []);
    items.value = [];
    selectedCartKeys.value = new Set();
    knownCartKeys.value = new Set();
  } catch {
    notice.value = '清空失败，请稍后重试';
  } finally {
    clearingCart.value = false;
    cartClearing.value = false;
  }
}

const selectedCartItems = computed(() => items.value.filter(isCartSelected));
const selectedCartQuantity = computed(() =>
  selectedCartItems.value.reduce((sum, item) => sum + Number(item.quantity || 1), 0)
);
const cartTotal = computed(() =>
  selectedCartItems.value.reduce((sum, item) => {
    const product = cartProduct(item);
    return sum + Number(product.price ?? product.unitPrice ?? 0) * Number(item.quantity || 1);
  }, 0)
);

const selectedCheckoutAddress = computed(() =>
  addresses.value.find((address) => String(address.id) === selectedAddressId.value)
);

function fullAddress(address: any) {
  return [address?.province, address?.city, address?.district, address?.detailAddress]
    .filter(Boolean)
    .join('');
}

async function openAddressDrawer() {
  if (!selectedCartItems.value.length) {
    notice.value = '请先选择要提交的商品';
    return;
  }
  addressDrawerOpen.value = true;
  addressLoading.value = true;
  notice.value = '';
  try {
    addresses.value = listFrom(await api.user.addresses());
    const preferred =
      addresses.value.find((address) => Number(address.isDefault) === 1) || addresses.value[0];
    selectedAddressId.value = preferred?.id == null ? '' : String(preferred.id);
  } catch (reason: any) {
    addresses.value = [];
    selectedAddressId.value = '';
    notice.value = reason?.response?.data?.message || '收货地址加载失败，请稍后重试';
  } finally {
    addressLoading.value = false;
  }
}

function goAddAddress() {
  addressDrawerOpen.value = false;
  router.push({ path: '/profile', query: { section: 'addresses', notice: 'address' } });
}

async function submitOrder() {
  if (!selectedCartItems.value.length) {
    notice.value = '请先选择要提交的商品';
    return;
  }
  const checkoutAddress = selectedCheckoutAddress.value;
  if (!checkoutAddress) {
    notice.value = '请选择收货地址';
    return;
  }
  if (submittingOrder.value) return;
  notice.value = '';
  submittingOrder.value = true;
  const selectedKeys = new Set(selectedCartKeys.value);
  const itemsToSubmit = [...selectedCartItems.value];
  const orderItems = itemsToSubmit.map((item) => {
    const product = cartProduct(item);
    return {
      productId: item.productId || product.id,
      patternId: item.patternId,
      customDesignId: item.customDesignId,
      remark: item.remark || '',
      productName: product.title || product.name,
      productImage:
        product.previewImageUrl || product.image || product.coverImage || product.mockupImage,
      previewImageUrl: product.previewImageUrl,
      unitPrice: Number(product.price ?? product.unitPrice ?? 0),
      price: Number(product.price ?? product.unitPrice ?? 0),
      quantity: Number(item.quantity || 1)
    };
  });
  let savedOrder: any = {
    id: `local-${Date.now()}`,
    orderNo: `XW${Date.now()}`,
    items: orderItems,
    totalAmount: cartTotal.value,
    status: '待付款',
    createdAt: new Date().toLocaleString('zh-CN'),
    addressId: checkoutAddress.id,
    receiverName: checkoutAddress.receiverName,
    receiverPhone: checkoutAddress.receiverPhone,
    receiverAddress: fullAddress(checkoutAddress)
  };
  try {
    const serverCartResult = await api.cart.items();
    const serverCart = listFrom(serverCartResult);
    const cartItemIds = await Promise.all(
      itemsToSubmit.map(async (item) => {
        const existing = serverCart.find(
          (entry: any) => cartIdentity(entry) === cartIdentity(item)
        );
        if (existing?.id != null) {
          await api.cart.update(existing.id, { quantity: Number(item.quantity || 1) });
          return existing.id;
        }
        const created: any = await api.cart.add({
          productId: item.productId,
          patternId: item.patternId,
          customDesignId: item.customDesignId,
          quantity: Number(item.quantity || 1)
        });
        return created?.id;
      })
    );
    const created: any = await api.orders.create({
      addressId: checkoutAddress.id,
      cartItemIds: cartItemIds.filter((id) => id != null)
    });
    savedOrder = {
      ...savedOrder,
      ...(created || {}),
      items: orderItems,
      receiverName: created?.receiverName || checkoutAddress.receiverName,
      receiverPhone: created?.receiverPhone || checkoutAddress.receiverPhone,
      receiverAddress: created?.receiverAddress || fullAddress(checkoutAddress)
    };
  } catch (reason: any) {
    notice.value =
      reason?.response?.data?.message || reason?.message || '生产工单提交失败，请稍后重试';
    submittingOrder.value = false;
    return;
  }
  const orders = readUserData<any[]>('user_orders', []);
  writeUserData('user_orders', [savedOrder, ...orders]);
  const remainingCart = readUserData<any[]>('cart_items', []).filter(
    (item) => !selectedKeys.has(cartItemKey(item))
  );
  selectedCartKeys.value = new Set();
  knownCartKeys.value = new Set(remainingCart.map(cartItemKey));
  writeUserData('cart_items', remainingCart);
  items.value = items.value.filter((item) => !selectedKeys.has(cartItemKey(item)));
  addressDrawerOpen.value = false;
  submittingOrder.value = false;
  router.push({ path: '/profile', query: { section: 'orders' } });
}

async function saveProfile() {
  notice.value = '';
  try {
    await api.user.updateProfile(profile.value);
    notice.value = '资料保存成功';
  } catch {
    notice.value = '资料保存失败，请检查后端服务';
  }
}

async function createPreview() {
  if (generatingPreview.value) return;
  notice.value = '';
  if (!selectedCustomProduct.value || !selectedCustomPattern.value || !composer.value) {
    notice.value = '请选择商品和纹样';
    return;
  }
  if (productStock(selectedCustomProduct.value) <= 0) {
    notice.value = '该商品已售罄，暂不可定制';
    return;
  }
  generatingPreview.value = true;
  try {
    const blob = await composer.value.exportBlob();
    const formData = new FormData();
    formData.append('file', blob, 'preview.png');
    const uploaded: any = await api.files.upload(formData);
    const previewImageUrl = String(uploaded?.fileUrl || '');
    if (!previewImageUrl) throw new Error('上传接口未返回 fileUrl');

    const created: any = await api.customDesigns.create({
      productId: customProductId.value,
      patternId: customPatternId.value,
      designConfig: { ...designConfig.value },
      previewImageUrl,
      remark: customNote.value
    });
    recordAppliedDesign(previewImageUrl, created);
    notice.value = '定制预览已生成并保存';
  } catch (reason: any) {
    notice.value = reason?.response?.data?.message || reason?.message || '定制预览生成失败';
  } finally {
    generatingPreview.value = false;
  }
}

function recordAppliedDesign(previewImageUrl: string, created: any) {
  const records = readUserData<any[]>('applied_designs', []);
  const selectedPattern = selectedCustomPattern.value;
  const selectedProduct = selectedCustomProduct.value;
  const saved = {
    ...created,
    id: created?.id ?? `design-${Date.now()}`,
    productId: customProductId.value,
    patternId: customPatternId.value,
    title: created?.productName || selectedProduct?.title,
    productName: created?.productName || selectedProduct?.title,
    productImage: created?.productCoverImage || selectedProduct?.image,
    price: Number(created?.productPrice ?? selectedProduct?.price ?? 0),
    patternTitle: created?.patternTitle || selectedPattern?.title,
    patternImage: created?.patternImageUrl || selectedPattern?.image,
    pattern: selectedPattern,
    previewImageUrl,
    designConfig: { ...designConfig.value },
    remark: customNote.value,
    createdAt: created?.createdAt || new Date().toLocaleString('zh-CN')
  };
  const next = [saved, ...records].filter(
    (design, index, all) =>
      all.findIndex((other) => String(other.id) === String(design.id)) === index
  );
  writeUserData('applied_designs', next);
  savedDesigns.value = next;
}

function designProduct(design: any) {
  return (
    customProducts.value.find((product) => String(product.id) === String(design.productId)) || {
      id: design.productId,
      title: design.productName || `商品 #${design.productId}`,
      image: design.productCoverImage || '',
      price: design.productPrice || 0,
      stock: design.productStock || 0
    }
  );
}

function designPrice(design: any) {
  return Number(design.price ?? designProduct(design)?.price ?? 0);
}

function showAddedDesignTip(designId: unknown) {
  const key = String(designId);
  const currentTimer = addedDesignTimers.get(key);
  if (currentTimer) clearTimeout(currentTimer);
  addedDesignIds.value = new Set([...addedDesignIds.value, key]);
  addedDesignTimers.set(
    key,
    setTimeout(() => {
      const next = new Set(addedDesignIds.value);
      next.delete(key);
      addedDesignIds.value = next;
      addedDesignTimers.delete(key);
    }, 3000)
  );
}

async function addDesignToCart(design: any) {
  if (!isRealClient.value) {
    await router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }
  const product = designProduct(design);
  const price = designPrice(design);
  const stock = Math.max(0, Number(product.stock ?? 0) || 0);
  if (stock <= 0) {
    notice.value = '该商品已售罄，无法提交打样需求';
    return;
  }
  const cart = readUserData<any[]>('cart_items', []);
  const existing = cart.find((item) => String(item.customDesignId) === String(design.id));
  if (Number(existing?.quantity || 0) >= stock) {
    notice.value = `商品库存不足，最多可购买${stock}件`;
    return;
  }
  const cartItem = {
    id: `custom-${design.id}`,
    productId: design.productId || product.id,
    customDesignId: design.id,
    patternId: design.patternId,
    title: design.productName || product.title || product.name,
    productName: design.productName || product.title || product.name,
    image: design.previewImageUrl || design.productImage || product.image,
    previewImageUrl: design.previewImageUrl,
    patternTitle: design.patternTitle || designPattern(design).title,
    designConfig: design.designConfig,
    remark: design.remark,
    price,
    unitPrice: price,
    quantity: existing ? Number(existing.quantity || 1) + 1 : 1,
    isCustomDesign: true
  };
  let serverItem: any;
  try {
    serverItem = await api.cart.add({
      productId: cartItem.productId,
      patternId: cartItem.patternId,
      customDesignId: cartItem.customDesignId,
      quantity: 1
    });
  } catch (reason: any) {
    notice.value = reason?.response?.data?.message || '提交打样需求失败';
    return;
  }
  const next = existing
    ? cart.map((item) =>
        String(item.customDesignId) === String(design.id) ? { ...item, ...cartItem } : item
      )
    : [...cart, cartItem];
  writeUserData('cart_items', next);
  showAddedDesignTip(design.id);
  notice.value = `已提交打样需求，预估报价 ¥${price.toFixed(2)}`;
  if (serverItem?.id != null) {
    const synced = readUserData<any[]>('cart_items', []).map((item) =>
      String(item.customDesignId) === String(design.id)
        ? { ...item, cartItemId: serverItem.id, serverCartItem: true }
        : item
    );
    writeUserData('cart_items', synced);
  }
}

function designPattern(design: any) {
  return (
    design.pattern ||
    customPatterns.value.find((pattern) => String(pattern.id) === String(design.patternId)) || {
      id: design.patternId,
      title: design.patternTitle || `AI 纹样 #${design.patternId}`,
      image: design.patternImageUrl || ''
    }
  );
}

async function removeDesign(design: any) {
  try {
    await api.customDesigns.remove(design.id);
    const next = savedDesigns.value.filter((item) => item.id !== design.id);
    writeUserData('applied_designs', next);
    savedDesigns.value = next;
    notice.value = '定制作品已删除';
  } catch (reason: any) {
    notice.value = reason?.response?.data?.message || '删除定制作品失败';
  }
}

async function openDetail(item: any) {
  if (mode.value === 'orders') return router.push(`/orders/${item.id}`);
  if (mode.value === 'cart') return router.push(`/products/${item.productId || item.id}`);
  selectedItem.value = item;
  if (mode.value !== 'resources') return;
  detailLoading.value = true;
  try {
    selectedItem.value = { ...item, ...(await api.resources.detail(item.id)) };
  } catch {
    /* 接口不可用时保留当前卡片资料 */
  } finally {
    detailLoading.value = false;
  }
}

function resourceImage(item: any, index = 0) {
  return mode.value === 'resources'
    ? resourceCover(item, index)
    : item.image ||
        item.coverImage ||
        item.imageUrl ||
        item.productCoverImage ||
        item.previewImageUrl;
}

function useResourceFallback(event: Event, item: any, index = 0) {
  const image = event.currentTarget as HTMLImageElement;
  const fallback = resourceCover({ ...item, coverImage: '', image: '', imageUrl: '' }, index);
  if (!image.src.endsWith(fallback)) image.src = fallback;
}

async function startResourceDownload(item: any, index = 0) {
  downloadingResourceId.value = item.id;
  notice.value = '';
  try {
    await downloadResource(item, index);
    notice.value = '资源已开始下载';
  } catch {
    notice.value = '资源暂时无法下载，请稍后重试';
  } finally {
    downloadingResourceId.value = null;
  }
}
</script>
<template>
  <section
    class="content workspace-page"
    :class="{ 'cart-page': mode === 'cart' }"
  >
    <div class="page-heading">
      <span class="eyebrow">XIUWEN WORKSPACE</span>
      <h1>{{ title }}</h1>
      <p>
        {{
          mode === 'cart'
            ? '在这里查看已提交的打样需求，可调整数量、移除样品，或整单提交给工厂。'
            : '统一管理您的广绣创作、学习与订单信息。'
        }}
      </p>
      <p
        v-if="mode === 'resources' && notice"
        class="resource-download-notice"
      >
        {{ notice }}
      </p>
    </div>
    <div
      v-if="mode === 'profile'"
      class="profile-layout"
    >
      <aside>
        <div class="avatar">绣</div>
        <b>绣纹爱好者</b><span>个人资料</span><span>收货地址</span><span>消息中心</span
        ><span>账号安全</span>
      </aside>
      <form
        class="panel"
        @submit.prevent="saveProfile"
      >
        <h2>完善个人资料</h2>
        <label>昵称<input v-model="profile.nickname" /></label
        ><label>手机号<input v-model="profile.phone" /></label
        ><label>邮箱<input v-model="profile.email" /></label
        ><label>个人简介<textarea v-model="profile.intro"></textarea></label
        ><button
          class="primary"
          type="submit"
        >
          保存资料
        </button>
        <p v-if="notice">{{ notice }}</p>
        <h3>收货地址</h3>
        <div
          v-for="a in addresses"
          :key="a.id"
        >
          <b>{{ a.receiverName }}</b> {{ a.receiverPhone }} {{ a.province }}{{ a.city
          }}{{ a.district }}{{ a.detailAddress }}
          <button @click="setDefault(a)">{{ a.isDefault ? '默认' : '设为默认' }}</button
          ><button
            @click="
              editingAddress = a;
              addressForm = { ...a };
            "
          >
            修改</button
          ><button @click="deleteAddress(a)">删除</button>
        </div>
        <input
          v-model="addressForm.receiverName"
          placeholder="收货人"
        /><input
          v-model="addressForm.receiverPhone"
          placeholder="电话"
        /><input
          v-model="addressForm.detailAddress"
          placeholder="详细地址"
        /><button @click="saveAddress">{{ editingAddress ? '保存修改' : '新增地址' }}</button>
        <h3>消息中心 <button @click="markAllMessages">全部已读</button></h3>
        <div
          v-for="m in messages"
          :key="m.id"
        >
          {{ m.title }} <span>{{ m.isRead ? '已读' : '未读' }}</span>
        </div>
      </form>
    </div>
    <div
      v-else-if="mode === 'customize'"
      class="panel customize-panel"
    >
      <h2>智造工坊定制</h2>
      <p>选择后台在售商品和您真实生成的 AI 纹样，在画布中调整位置、大小与角度。</p>
      <p
        v-if="customizeLoading"
        class="customize-state"
      >
        正在加载商品与 AI 纹样…
      </p>
      <p
        v-else-if="customizeError"
        class="customize-state error"
      >
        {{ customizeError }}
      </p>
      <div class="customize-layout">
        <div class="customize-options">
          <section>
            <h3>选择商品</h3>
            <div
              v-if="customProducts.length"
              class="custom-choice-grid"
            >
              <button
                v-for="product in customProducts"
                :key="product.id"
                type="button"
                :class="{
                  on: customProductId === Number(product.id),
                  soldout: productStock(product) <= 0
                }"
                :disabled="productStock(product) <= 0"
                @click="customProductId = Number(product.id)"
              >
                <img
                  :src="thumbUrl(product.image)"
                  :alt="product.title"
                  crossorigin="anonymous"
                />
                <span>
                  <b>{{ product.title }}</b>
                  <small>{{
                    productStock(product) > 0
                      ? `库存 ${productStock(product)}`
                      : '已售罄 · 不可定制'
                  }}</small>
                </span>
              </button>
            </div>
            <p
              v-else
              class="custom-option-empty"
            >
              暂无可定制的在售商品，请联系商家上架商品。
            </p>
          </section>
          <section>
            <h3>选择全部 AI 纹样</h3>
            <button
              class="generate-ai-pattern"
              type="button"
              @click="router.push('/generate')"
            >
              生成新的 AI 纹样
            </button>
            <div
              v-if="customPatterns.length"
              class="custom-choice-grid pattern-choices"
            >
              <button
                v-for="pattern in customPatterns"
                :key="pattern.id"
                type="button"
                :class="{ on: customPatternId === Number(pattern.id) }"
                @click="customPatternId = Number(pattern.id)"
              >
                <img
                  :src="thumbUrl(pattern.image)"
                  :alt="pattern.title"
                  crossorigin="anonymous"
                  @error="onPatternThumbError(pattern)"
                />
                <span>{{ pattern.title }}</span>
              </button>
            </div>
            <div
              v-else
              class="custom-pattern-empty"
            >
              <p>暂无可用的 AI 纹样，请先生成一款纹样后再回来定制。</p>
              <button
                type="button"
                @click="router.push('/generate')"
              >
                去生成纹样
              </button>
            </div>
          </section>
        </div>
        <PatternComposer
          ref="composer"
          v-model="designConfig"
          :product-image="selectedCustomProduct?.image || ''"
          :pattern-image="selectedCustomPattern?.image || ''"
        />
      </div>
      <textarea
        v-model="customNote"
        placeholder="填写您的定制要求"
      ></textarea>
      <button
        class="primary"
        type="button"
        :disabled="
          generatingPreview ||
          !selectedCustomProduct ||
          !selectedCustomPattern ||
          productStock(selectedCustomProduct) <= 0
        "
        @click="createPreview"
      >
        {{ generatingPreview ? '正在生成…' : '生成定制预览' }}
      </button>
      <p v-if="notice">{{ notice }}</p>
      <div class="saved-designs">
        <div class="saved-designs-heading">
          <h3>我的定制作品</h3>
          <span>{{ savedDesigns.length }} 件</span>
        </div>
        <div
          v-if="savedDesigns.length"
          class="saved-design-grid"
        >
          <article
            v-for="design in savedDesigns"
            :key="design.id"
            class="saved-design-card"
          >
            <img
              :src="String(design.previewImageUrl || designProduct(design).image || '')"
              :alt="String(design.productName || designProduct(design).title || '')"
            />
            <div>
              <b>{{ design.productName || designProduct(design).title }}</b>
              <span>纹样：{{ designPattern(design).title }}</span>
              <strong class="saved-design-price">¥ {{ designPrice(design).toFixed(2) }}</strong>
              <small v-if="design.remark">{{ design.remark }}</small>
              <div class="saved-design-actions">
                <div class="saved-design-cart-action">
                  <button
                    class="saved-design-cart"
                    type="button"
                    title="提交打样需求"
                    aria-label="提交打样需求"
                    @click="addDesignToCart(design)"
                  >
                    +
                  </button>
                  <small v-if="addedDesignIds.has(String(design.id))">已提交打样需求</small>
                </div>
                <button
                  class="saved-design-remove"
                  @click="removeDesign(design)"
                >
                  删除记录
                </button>
              </div>
            </div>
          </article>
        </div>
        <p
          v-else
          class="saved-empty"
        >
          生成预览后，定制作品会保存在这里。
        </p>
      </div>
    </div>
    <div
      v-else-if="mode === 'cart'"
      class="cart-panel"
    >
      <template v-if="items.length">
        <p
          v-if="notice"
          class="cart-notice"
        >
          {{ notice }}
        </p>
        <div
          class="cart-list"
          :class="{ clearing: cartClearing }"
        >
          <article
            v-for="(item, index) in items"
            :key="cartItemKey(item)"
            class="cart-item"
            :class="{
              selected: isCartSelected(item),
              removing: removingCartKeys.has(cartItemKey(item))
            }"
            :style="{ '--i': index }"
            @click="onCartItemClick(item, $event)"
          >
            <label
              class="cart-select"
              :title="isCartSelected(item) ? '取消选择' : '选择样品'"
            >
              <input
                class="cart-selector"
                type="checkbox"
                :checked="isCartSelected(item)"
                :aria-label="`选择${cartProduct(item).title || cartProduct(item).name || '样品'}`"
                @change="toggleCartSelection(item)"
              />
            </label>
            <div class="cart-basic">
              <div class="cart-thumb">
                <img
                  :src="
                    cartProduct(item).image ||
                    cartProduct(item).coverImage ||
                    cartProduct(item).mockupImage
                  "
                  :alt="cartProduct(item).title || cartProduct(item).name"
                  loading="lazy"
                />
              </div>
              <div class="cart-info">
                <b>{{ cartProduct(item).title || cartProduct(item).name }}</b>
                <span class="cart-price">¥ {{ cartProduct(item).price }}</span>
              </div>
            </div>
            <div
              class="cart-specs"
              @click.stop
            >
              <div class="spec-tags">
                <span
                  v-for="spec in cartSpecs(item)"
                  :key="spec.label"
                >
                  <em>{{ spec.label }}</em
                  >{{ spec.value }}
                </span>
              </div>
              <div class="spec-foot">
                <input
                  v-if="editingNoteKey === cartItemKey(item)"
                  v-model="noteDraft"
                  class="spec-note-input"
                  type="text"
                  maxlength="40"
                  placeholder="例如：包装用礼盒，5 月 20 日前交付"
                  autofocus
                  @keydown.enter.prevent="saveCartNote(item)"
                  @keydown.esc.prevent="cancelEditNote"
                  @blur="saveCartNote(item)"
                />
                <button
                  v-else
                  class="spec-note"
                  :class="{ filled: cartItemNote(item) }"
                  type="button"
                  @click="startEditNote(item)"
                >
                  <svg
                    width="14"
                    height="14"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path d="M12 20h9" />
                    <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
                  </svg>
                  <span>{{ cartItemNote(item) || '点击添加打样备注…' }}</span>
                </button>
                <span
                  v-if="isCustomCartItem(item)"
                  class="status-badge"
                >
                  ✔ AI 定制样品
                </span>
              </div>
            </div>
            <div class="cart-actions">
              <div
                class="quantity-control"
                aria-label="打样数量"
              >
                <button
                  aria-label="减少数量"
                  :disabled="updatingCartKeys.has(cartItemKey(item)) || (item.quantity || 1) <= 1"
                  @click.stop="updateCartQuantity(item, -1)"
                >
                  −
                </button>
                <span
                  :key="item.quantity"
                  class="qty-num"
                >
                  {{ item.quantity || 1 }}
                </span>
                <button
                  aria-label="增加数量"
                  :disabled="updatingCartKeys.has(cartItemKey(item))"
                  @click.stop="updateCartQuantity(item, 1)"
                >
                  ＋
                </button>
              </div>
              <button
                class="remove-cart"
                @click.stop="animateRemove(item)"
              >
                移除
              </button>
            </div>
          </article>
        </div>
        <div class="cart-checkout">
          <div class="cart-summary">
            <span class="cart-count">
              已选
              <b :key="selectedCartQuantity">{{ selectedCartQuantity }}</b>
              件 · 打样费用合计
            </span>
            <strong
              :key="cartTotal.toFixed(2)"
              class="cart-total"
            >
              ¥ {{ cartTotal.toFixed(2) }}
            </strong>
            <small v-if="!selectedCartItems.length">请先勾选要提交打样的样品</small>
          </div>
          <button
            class="clear-cart"
            :disabled="clearingCart"
            @click="clearCart"
          >
            {{ clearingCart ? '正在清空…' : '清空打样单' }}
          </button>
          <button
            class="primary checkout-submit"
            :disabled="submittingOrder || !selectedCartItems.length"
            @click="openAddressDrawer"
          >
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"
              />
              <polyline points="3.27 6.96 12 12.01 20.73 6.96" />
              <line
                x1="12"
                y1="22.08"
                x2="12"
                y2="12"
              />
            </svg>
            {{ submittingOrder ? '正在提交…' : '提交给工厂' }}
          </button>
        </div>
      </template>
      <div
        v-else
        class="cart-empty"
      >
        <span class="cart-empty-mark">🧵</span>
        <b>还没有打样需求</b>
        <p>去智造工坊挑选喜欢的广绣好物，定制后可在这里提交打样。</p>
        <button
          class="primary"
          @click="router.push('/products')"
        >
          去智造工坊看看
        </button>
      </div>
    </div>
    <div
      v-else
      class="workspace-grid"
    >
      <article
        v-for="(item, itemIndex) in items"
        :key="item.id"
      >
        <img
          :src="resourceImage(item, itemIndex)"
          :alt="item.title || item.name || '内容封面'"
          @error="useResourceFallback($event, item, itemIndex)"
        />
        <div>
          <b>{{
            item.title || item.name || item.productName || item.orderNo || `订单 ${item.id}`
          }}</b>
          <p>{{ item.desc || item.meta || item.status || '广绣文化创意内容' }}</p>
          <button @click="openDetail(item)">查看详情</button>
        </div>
      </article>
    </div>
    <div
      v-if="addressDrawerOpen"
      class="address-drawer-mask"
      @click.self="addressDrawerOpen = false"
    >
      <aside
        class="address-drawer"
        aria-label="选择收货地址"
      >
        <header>
          <div>
            <span class="eyebrow">DELIVERY ADDRESS</span>
            <h2>选择收货地址</h2>
          </div>
          <button
            type="button"
            aria-label="关闭"
            @click="addressDrawerOpen = false"
          >
            ×
          </button>
        </header>
        <p
          v-if="addressLoading"
          class="address-loading"
        >
          正在读取收货地址…
        </p>
        <div
          v-else-if="addresses.length"
          class="checkout-address-list"
        >
          <label
            v-for="address in addresses"
            :key="address.id"
            :class="{ selected: String(address.id) === selectedAddressId }"
          >
            <input
              v-model="selectedAddressId"
              type="radio"
              :value="String(address.id)"
            />
            <span>
              <b>{{ address.receiverName }}</b
              ><em>{{ address.receiverPhone }}</em>
              <i v-if="Number(address.isDefault) === 1">默认</i>
              <small>{{ fullAddress(address) }}</small>
            </span>
          </label>
        </div>
        <div
          v-else
          class="checkout-address-empty"
        >
          <span>⌖</span>
          <h3>还没有收货地址</h3>
          <p>新增地址后才能提交给工厂。</p>
          <button
            class="primary"
            type="button"
            @click="goAddAddress"
          >
            ＋ 新增收货地址
          </button>
        </div>
        <footer v-if="addresses.length">
          <button
            type="button"
            @click="goAddAddress"
          >
            新增地址
          </button>
          <button
            class="primary"
            type="button"
            :disabled="submittingOrder || !selectedCheckoutAddress"
            @click="submitOrder"
          >
            {{ submittingOrder ? '正在提交…' : '确认地址并提交' }}
          </button>
        </footer>
      </aside>
    </div>
    <div
      v-if="selectedItem"
      class="resource-detail-backdrop"
      @click.self="selectedItem = null"
    >
      <article class="resource-detail-card">
        <button
          class="detail-close"
          aria-label="关闭"
          @click="selectedItem = null"
        >
          ×
        </button>
        <img
          :src="resourceCover(selectedItem)"
          :alt="selectedItem.title || selectedItem.name"
          @error="useResourceFallback($event, selectedItem)"
        />
        <div>
          <span class="eyebrow">CREATIVE RESOURCE</span>
          <h2>{{ selectedItem.title || selectedItem.name || '资源详情' }}</h2>
          <p>
            {{
              selectedItem.description ||
              selectedItem.content ||
              selectedItem.desc ||
              selectedItem.subtitle ||
              selectedItem.meta ||
              '广绣文化创作与学习资源。'
            }}
          </p>
          <small v-if="detailLoading">正在读取详细资料…</small>
          <div class="resource-detail-actions">
            <button
              class="primary"
              type="button"
              :disabled="detailLoading || downloadingResourceId === selectedItem.id"
              @click="startResourceDownload(selectedItem)"
            >
              {{ downloadingResourceId === selectedItem.id ? '下载中…' : '⇩ 下载资源' }}
            </button>
            <button
              type="button"
              @click="selectedItem = null"
            >
              关闭详情
            </button>
          </div>
        </div>
      </article>
    </div>
    <div
      v-if="clearConfirmOpen"
      class="confirm-backdrop"
      @click.self="clearConfirmOpen = false"
    >
      <div
        class="confirm-card"
        role="dialog"
        aria-modal="true"
        aria-label="清空打样单确认"
      >
        <h3>清空打样单</h3>
        <p>确定要清空全部打样需求吗？移除后需重新提交。</p>
        <div class="confirm-actions">
          <button
            type="button"
            @click="clearConfirmOpen = false"
          >
            取消
          </button>
          <button
            class="primary"
            type="button"
            :disabled="clearingCart"
            @click="confirmClearCart"
          >
            {{ clearingCart ? '正在清空…' : '确定清空' }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.resource-download-notice {
  color: var(--jade);
}
.resource-detail-actions {
  display: flex;
  gap: 10px;
  margin-top: 22px;
}
.customize-layout {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(360px, 1.1fr);
  gap: 28px;
  margin: 24px 0;
  align-items: start;
}
.customize-options {
  display: grid;
  gap: 24px;
  min-width: 0;
}
.customize-options h3 {
  margin: 0 0 12px;
}
.custom-choice-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  max-height: 290px;
  overflow-y: auto;
  padding: 2px;
}
.custom-choice-grid button {
  display: grid;
  grid-template-columns: 62px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 8px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fffaf3;
  color: var(--ink);
  text-align: left;
  cursor: pointer;
}
.custom-choice-grid button.on {
  border-color: var(--red);
  box-shadow: 0 0 0 1px var(--red);
}
.custom-choice-grid button:disabled,
.custom-choice-grid button.soldout {
  cursor: not-allowed;
  opacity: 0.58;
  border-color: #d8d0c6;
  box-shadow: none;
}
.custom-choice-grid img {
  width: 62px;
  height: 54px;
  border-radius: 7px;
  object-fit: cover;
  background: #f3eadf;
}
.custom-choice-grid span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.custom-choice-grid span b,
.custom-choice-grid span small {
  display: block;
}
.custom-choice-grid span small {
  margin-top: 5px;
  color: var(--muted);
  font-size: 12px;
}
.custom-choice-grid button.soldout span small {
  color: var(--red);
}
.customize-state {
  margin: 10px 0 0;
  padding: 10px 12px;
  border-radius: 8px;
  color: var(--muted);
  background: #fff8ef;
}
.customize-state.error {
  color: var(--red);
  background: #fff0ed;
}
.custom-option-empty {
  margin: 0;
  padding: 18px 12px;
  border: 1px dashed var(--line);
  border-radius: 10px;
  color: var(--muted);
  text-align: center;
}
.generate-ai-pattern {
  margin: 0 0 10px;
  padding: 7px 12px;
  border: 1px solid var(--red);
  border-radius: 7px;
  color: var(--red);
  background: transparent;
  cursor: pointer;
}
.custom-pattern-empty {
  padding: 22px;
  border: 1px dashed var(--line);
  border-radius: 10px;
  color: var(--muted);
  text-align: center;
}
.custom-pattern-empty p {
  margin: 0 0 12px;
}
.custom-pattern-empty button {
  border: 0;
  background: transparent;
  color: var(--red);
  cursor: pointer;
}
.customize-panel > textarea {
  width: 100%;
  min-height: 92px;
  margin-bottom: 14px;
}
.customize-panel > .primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.cart-page {
  --cart-ink: #1f3b2c;
  --cart-red: #c13e33;
  --cart-muted: #888;
  --cart-line: #eae6df;
  --cart-ease: cubic-bezier(0.2, 0.8, 0.2, 1);
  padding-top: 44px;
  padding-bottom: 170px;
}
.cart-page .page-heading {
  animation: cartHeadIn 0.7s var(--cart-ease) both;
}
.cart-page .page-heading .eyebrow {
  color: var(--cart-red);
  font-size: 12px;
  letter-spacing: 3px;
  text-transform: uppercase;
}
.cart-page .page-heading h1 {
  color: var(--cart-ink);
  font-size: 42px;
  font-weight: 900;
  letter-spacing: 2px;
}
.cart-page .page-heading p {
  color: var(--cart-muted);
  font-size: 14px;
}
@keyframes cartHeadIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}
.cart-notice {
  margin: 0 0 16px;
  padding: 11px 16px;
  border: 1px solid rgba(193, 62, 51, 0.18);
  border-radius: 10px;
  background: #fff4f1;
  color: var(--cart-red);
  font-size: 13px;
}
.cart-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.cart-item {
  position: relative;
  display: grid;
  grid-template-columns: 24px minmax(230px, 0.9fr) minmax(0, 1.25fr) auto;
  grid-template-areas: 'select basic specs actions';
  gap: 24px;
  align-items: center;
  padding: 22px 28px;
  border: 1px solid rgba(0, 0, 0, 0.035);
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.02);
  cursor: pointer;
  opacity: 1;
  transform: translateY(0);
  animation: cartItemIn 0.62s var(--cart-ease) backwards;
  animation-delay: calc(var(--i, 0) * 70ms);
  transition:
    transform 0.45s var(--cart-ease),
    box-shadow 0.45s var(--cart-ease),
    border-color 0.45s var(--cart-ease),
    background 0.45s var(--cart-ease);
}
.cart-item.selected {
  border-color: rgba(193, 62, 51, 0.22);
  background: linear-gradient(180deg, #fff, #fdfaf8);
  box-shadow: 0 8px 26px rgba(193, 62, 51, 0.07);
}
@media (hover: hover) {
  .cart-item:hover {
    transform: translateY(-4px);
    border-color: rgba(31, 59, 44, 0.1);
    box-shadow: 0 14px 34px rgba(31, 59, 44, 0.08);
  }
}
@keyframes cartItemIn {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
.cart-item.removing {
  pointer-events: none;
  animation: cartItemLeave 0.3s var(--cart-ease) forwards;
}
@keyframes cartItemLeave {
  to {
    opacity: 0;
    transform: translateX(-26px) scale(0.96);
  }
}
.cart-list.clearing .cart-item {
  pointer-events: none;
  animation: cartItemLeave 0.34s var(--cart-ease) forwards;
  animation-delay: calc(var(--i, 0) * 70ms);
}
.cart-select {
  grid-area: select;
  display: grid;
  place-items: center;
  cursor: pointer;
}
.cart-selector {
  appearance: none;
  width: 22px;
  height: 22px;
  margin: 0;
  border: 2px solid #ccc;
  border-radius: 50%;
  background: #fff;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    background 0.2s ease,
    transform 0.2s ease;
}
.cart-selector:hover {
  border-color: var(--cart-red);
}
.cart-selector:checked {
  border-color: var(--cart-red);
  background: var(--cart-red);
  box-shadow: inset 0 0 0 5px #fff;
  animation: radioPop 0.38s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.cart-selector:focus-visible {
  outline: 2px solid color-mix(in srgb, var(--cart-red) 45%, transparent);
  outline-offset: 3px;
}
@keyframes radioPop {
  0% {
    transform: scale(0.8);
  }
  55% {
    transform: scale(1.12);
  }
  100% {
    transform: scale(1);
  }
}
.cart-basic {
  grid-area: basic;
  display: flex;
  align-items: center;
  gap: 18px;
  min-width: 0;
}
.cart-thumb {
  width: 80px;
  height: 80px;
  flex: 0 0 80px;
  overflow: hidden;
  border-radius: 10px;
  background: #f5f2ee;
}
.cart-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s var(--cart-ease);
}
@media (hover: hover) {
  .cart-item:hover .cart-thumb img {
    transform: scale(1.09);
  }
}
.cart-info {
  display: grid;
  gap: 8px;
  min-width: 0;
}
.cart-info b {
  overflow: hidden;
  color: var(--cart-ink);
  font-family: 'Noto Serif SC', serif;
  font-size: 18px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cart-price {
  color: var(--cart-red);
  font-size: 16px;
  font-weight: 600;
}
.cart-specs {
  grid-area: specs;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
  min-width: 0;
  padding: 0 26px;
  border-left: 1px solid var(--cart-line);
  border-right: 1px solid var(--cart-line);
}
.spec-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  color: #666;
  font-size: 13px;
}
.spec-tags span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.spec-tags span::before {
  content: '';
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--cart-red);
}
.spec-tags em {
  color: #9a938a;
  font-style: normal;
}
.spec-foot {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  min-height: 28px;
}
.spec-note {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  padding: 6px 12px;
  border: 1px dashed #ddd;
  border-radius: 6px;
  background: #f9f9f9;
  color: #999;
  font-size: 13px;
  transition:
    border-color 0.3s,
    color 0.3s,
    background 0.3s,
    transform 0.3s;
}
.spec-note span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.spec-note:hover {
  border-color: var(--cart-ink);
  background: #fff;
  color: var(--cart-ink);
  transform: translateY(-1px);
}
.spec-note.filled {
  border-style: solid;
  border-color: rgba(31, 59, 44, 0.18);
  background: #f4f7f2;
  color: var(--cart-ink);
}
.spec-note-input {
  width: min(340px, 100%);
  padding: 7px 12px;
  border: 1px solid var(--cart-ink);
  border-radius: 6px;
  background: #fff;
  color: var(--cart-ink);
  font-size: 13px;
  outline: none;
  box-shadow: 0 0 0 3px rgba(31, 59, 44, 0.08);
}
.status-badge {
  padding: 4px 10px;
  border-radius: 999px;
  background: #e8f5e9;
  color: #2e7d32;
  font-size: 12px;
  white-space: nowrap;
}
.cart-actions {
  grid-area: actions;
  display: flex;
  align-items: center;
  gap: 26px;
  flex-shrink: 0;
}
.quantity-control {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  border: 1px solid #eee;
  border-radius: 10px;
  background: #f9f9f9;
}
.quantity-control button {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: var(--cart-ink);
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  transition:
    background 0.2s,
    color 0.2s,
    transform 0.2s;
}
.quantity-control button:hover:not(:disabled) {
  background: var(--cart-ink);
  color: #fff;
}
.quantity-control button:active:not(:disabled) {
  transform: scale(0.88);
}
.quantity-control button:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.qty-num {
  min-width: 32px;
  color: var(--cart-ink);
  font-size: 15px;
  font-weight: 600;
  text-align: center;
  animation: qtyPop 0.28s var(--cart-ease);
}
@keyframes qtyPop {
  from {
    opacity: 0.2;
    transform: translateY(-6px) scale(0.8);
  }
  to {
    opacity: 1;
    transform: none;
  }
}
.remove-cart {
  position: relative;
  padding: 8px 4px;
  border: 0;
  background: transparent;
  color: var(--cart-muted);
  font-size: 14px;
  transition: color 0.3s;
}
.remove-cart::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 4px;
  width: 0;
  height: 1px;
  background: var(--cart-red);
  transform: translateX(-50%);
  transition: width 0.3s;
}
.remove-cart:hover {
  color: var(--cart-red);
  transform: none;
}
.remove-cart:hover::after {
  width: 80%;
}
.cart-checkout {
  position: fixed;
  left: 50%;
  bottom: 24px;
  z-index: 90;
  width: min(1220px, calc(100% - 48px));
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 28px;
  padding: 18px 28px;
  border: 1px solid rgba(255, 255, 255, 0.85);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(20px) saturate(1.15);
  -webkit-backdrop-filter: blur(20px) saturate(1.15);
  box-shadow: 0 20px 46px rgba(31, 59, 44, 0.12);
  transform: translateX(-50%);
  animation: checkoutIn 0.7s var(--cart-ease) 0.2s backwards;
}
@keyframes checkoutIn {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(90px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}
.cart-checkout .cart-summary {
  display: flex;
  flex: 1;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 14px;
  color: var(--cart-muted);
  font-size: 14px;
}
.cart-count b {
  display: inline-block;
  color: var(--cart-ink);
  font-weight: 700;
  animation: qtyPop 0.3s var(--cart-ease);
}
.cart-total {
  color: var(--cart-red);
  font-family: 'Noto Serif SC', serif;
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
  animation: pricePulse 0.45s var(--cart-ease);
}
.cart-summary small {
  flex-basis: 100%;
  color: var(--cart-red);
  font-size: 12px;
}
@keyframes pricePulse {
  0% {
    transform: scale(1);
  }
  45% {
    transform: scale(1.12);
    color: #e4491f;
  }
  100% {
    transform: scale(1);
  }
}
.cart-checkout button {
  min-width: 132px;
  padding: 13px 26px;
  border-radius: 999px;
  font-size: 15px;
}
.clear-cart {
  min-width: auto !important;
  border: 1px solid var(--cart-line);
  background: transparent;
  color: var(--cart-muted);
}
.clear-cart:hover:not(:disabled) {
  border-color: var(--cart-ink);
  color: var(--cart-ink);
}
.checkout-submit {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  overflow: hidden;
  border: 0 !important;
  background: var(--cart-red) !important;
  box-shadow: 0 10px 24px rgba(193, 62, 51, 0.28) !important;
}
.checkout-submit::after {
  content: '';
  position: absolute;
  top: 0;
  left: -120%;
  width: 45%;
  height: 100%;
  background: linear-gradient(
    to right,
    rgba(255, 255, 255, 0),
    rgba(255, 255, 255, 0.35),
    rgba(255, 255, 255, 0)
  );
  transform: skewX(-25deg);
  transition: left 0.6s var(--cart-ease);
}
.checkout-submit:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 14px 30px rgba(193, 62, 51, 0.36) !important;
}
.checkout-submit:hover:not(:disabled)::after {
  left: 150%;
}
.checkout-submit:disabled,
.clear-cart:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  transform: none;
}
.cart-empty {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 80px 20px;
  text-align: center;
}
.cart-empty-mark {
  width: 84px;
  height: 84px;
  display: grid;
  place-items: center;
  border: 1px solid var(--cart-line);
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 12px 30px rgba(31, 59, 44, 0.06);
  font-size: 34px;
  animation: emptyPop 0.5s var(--cart-ease) both;
}
.cart-empty b {
  color: var(--cart-ink);
  font-family: 'Noto Serif SC', serif;
  font-size: 20px;
}
.cart-empty p {
  margin: 0 0 8px;
  color: var(--cart-muted);
}
@keyframes emptyPop {
  from {
    opacity: 0;
    transform: scale(0.85);
  }
  to {
    opacity: 1;
    transform: none;
  }
}
@media (max-width: 1100px) {
  .cart-item {
    grid-template-columns: 24px minmax(0, 1fr) auto;
    grid-template-areas:
      'select basic actions'
      'specs specs specs';
    gap: 16px 22px;
  }
  .cart-specs {
    padding: 14px 0 0;
    border-top: 1px solid var(--cart-line);
    border-right: 0;
    border-left: 0;
  }
}
@media (max-width: 700px) {
  .cart-page {
    padding-top: 30px;
    padding-bottom: 230px;
  }
  .cart-page .page-heading h1 {
    font-size: 34px;
  }
  .cart-item {
    grid-template-columns: 22px minmax(0, 1fr);
    grid-template-areas:
      'select basic'
      'specs specs'
      'actions actions';
    gap: 14px 14px;
    padding: 18px;
  }
  .cart-thumb {
    width: 68px;
    height: 68px;
    flex-basis: 68px;
  }
  .cart-info b {
    font-size: 16px;
  }
  .cart-actions {
    justify-content: flex-end;
    gap: 18px;
  }
  .cart-checkout {
    bottom: 12px;
    width: calc(100% - 24px);
    flex-wrap: wrap;
    gap: 10px;
    padding: 14px 16px;
    border-radius: 16px;
  }
  .cart-checkout .cart-summary {
    flex-basis: 100%;
    justify-content: space-between;
  }
  .cart-total {
    font-size: 26px;
  }
  .cart-checkout button {
    flex: 1;
    min-width: 0;
    padding: 12px 16px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .cart-page *,
  .cart-page *::before,
  .cart-page *::after {
    animation-duration: 0.001ms !important;
    animation-delay: 0s !important;
    transition-duration: 0.001ms !important;
  }
}
.address-drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: flex;
  justify-content: flex-end;
  background: rgba(42, 31, 23, 0.42);
  backdrop-filter: blur(3px);
}
.address-drawer {
  width: min(520px, 94vw);
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 28px;
  background: #fffaf3;
  box-shadow: -18px 0 48px rgba(65, 41, 24, 0.2);
  animation: addressDrawerIn 0.28s ease-out;
}
@keyframes addressDrawerIn {
  from {
    transform: translateX(100%);
  }
  to {
    transform: translateX(0);
  }
}
.address-drawer header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--line);
}
.address-drawer header h2 {
  margin: 7px 0 0;
}
.address-drawer header > button {
  width: 36px;
  height: 36px;
  border: 0;
  background: transparent;
  color: var(--muted);
  font-size: 28px;
  cursor: pointer;
}
.address-loading {
  padding: 36px 0;
  color: var(--muted);
  text-align: center;
}
.checkout-address-list {
  flex: 1;
  display: grid;
  align-content: start;
  gap: 12px;
  overflow-y: auto;
  padding: 22px 2px;
}
.checkout-address-list > label {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  gap: 13px;
  padding: 17px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
}
.checkout-address-list > label.selected {
  border-color: var(--red);
  box-shadow: 0 0 0 1px var(--red);
}
.checkout-address-list input {
  accent-color: var(--red);
}
.checkout-address-list span {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.checkout-address-list em {
  color: var(--muted);
  font-style: normal;
}
.checkout-address-list i {
  padding: 2px 7px;
  border-radius: 10px;
  background: #fff0e8;
  color: var(--red);
  font-size: 11px;
  font-style: normal;
}
.checkout-address-list small {
  flex-basis: 100%;
  color: var(--muted);
  line-height: 1.6;
}
.checkout-address-empty {
  flex: 1;
  display: grid;
  place-content: center;
  justify-items: center;
  text-align: center;
}
.checkout-address-empty > span {
  color: var(--red);
  font-size: 44px;
}
.checkout-address-empty h3 {
  margin: 12px 0 4px;
}
.checkout-address-empty p {
  margin: 0 0 20px;
  color: var(--muted);
}
.address-drawer footer {
  display: grid;
  grid-template-columns: 1fr auto auto;
  align-items: center;
  gap: 10px;
  padding-top: 18px;
  border-top: 1px solid var(--line);
}
.address-drawer footer > button {
  padding: 10px 14px;
}
.saved-designs {
  margin-top: 28px;
  padding-top: 22px;
  border-top: 1px solid var(--line);
}
.saved-designs-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.saved-designs-heading h3 {
  margin: 0;
}
.saved-designs-heading span {
  color: var(--muted);
}
.saved-design-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-top: 15px;
}
.saved-design-card {
  display: flex;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fffaf3;
}
.saved-design-card img {
  width: 82px;
  height: 82px;
  object-fit: cover;
  border-radius: 8px;
}
.saved-design-card div {
  display: grid;
  gap: 5px;
  align-content: start;
}
.saved-design-card span,
.saved-design-card small {
  color: var(--muted);
}
.saved-design-price {
  color: var(--red);
  font-size: 16px;
}
.saved-design-actions {
  display: flex !important;
  grid-auto-flow: column;
  align-items: center;
  justify-content: start;
  gap: 9px !important;
}
.saved-design-cart-action {
  display: grid !important;
  justify-items: center;
  gap: 4px !important;
}
.saved-design-cart-action small {
  color: var(--red);
  font-size: 11px;
  white-space: nowrap;
}
.saved-design-card .saved-design-remove {
  width: fit-content;
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--red);
  cursor: pointer;
}
.saved-design-card .saved-design-cart {
  width: 28px;
  height: 28px;
  border: 1px solid var(--red);
  border-radius: 50%;
  padding: 0;
  background: var(--red);
  color: white;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
}
.saved-empty {
  color: var(--muted);
}
@media (max-width: 700px) {
  .address-drawer {
    padding: 22px 18px;
  }
  .address-drawer footer {
    grid-template-columns: 1fr 1fr;
  }
}
@media (max-width: 900px) {
  .customize-layout {
    grid-template-columns: 1fr;
  }
  .saved-design-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 560px) {
  .custom-choice-grid {
    grid-template-columns: 1fr;
  }
}
.confirm-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(24, 18, 14, 0.5);
}
.confirm-card {
  width: min(380px, 100%);
  padding: 26px 24px 22px;
  background: #fffaf3;
  border: 1px solid var(--line);
  border-radius: 16px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.22);
  text-align: center;
}
.confirm-card h3 {
  margin: 0 0 10px;
  font-size: 20px;
}
.confirm-card p {
  margin: 0 0 20px;
  color: var(--muted);
  line-height: 1.7;
}
.confirm-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}
.confirm-actions button {
  padding: 10px 24px;
  border-radius: 10px;
  cursor: pointer;
}
.confirm-actions .primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
