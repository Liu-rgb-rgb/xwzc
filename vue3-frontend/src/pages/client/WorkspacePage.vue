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
const updatingCartKeys = ref<Set<string>>(new Set());
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

function normalizeCustomProduct(product: any) {
  return {
    ...product,
    id: product.id,
    title: product.title || product.name || `商品 #${product.id}`,
    image: imageUrl(
      product.mockupImage,
      product.coverImage,
      product.imageUrl,
      product.image
    )
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

function probeImage(src: string) {
  return new Promise<boolean>((resolve) => {
    const image = new Image();
    // 与画布加载方式保持一致（跨域），探测通过的图片才保证能在画布上绘制
    image.crossOrigin = 'anonymous';
    image.onload = () => resolve(true);
    image.onerror = () => resolve(false);
    image.src = src;
  });
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
        product.image &&
        product.status === 'ON_SALE' &&
        Number(product.isCustomizable) === 1
    );
    const candidatePatterns = uniqueById(
      listFrom(patternResult).map((pattern, index) => normalizeCustomPattern(pattern, index))
    ).filter(
      (pattern) =>
        pattern.image &&
        pattern.generationId != null &&
        pattern.status === 'NORMAL'
    );
    // 图片实际加载失败的纹样（OSS 文件被删/不可访问）画布无法使用，直接从列表移除。
    const checkedPatterns = await Promise.all(
      candidatePatterns.map(async (pattern) => ({
        pattern,
        available: await probeImage(pattern.image)
      }))
    );
    const brokenCount = checkedPatterns.filter((item) => !item.available).length;
    customPatterns.value = checkedPatterns
      .filter((item) => item.available)
      .map((item) => item.pattern);
    if (brokenCount) notice.value = `已自动隐藏 ${brokenCount} 个图片不可用的纹样`;
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
});
watch(mode, (value) => {
  void loadItems();
  if (value === 'customize') void loadCustomizeOptions();
});
watch(
  () => route.query.productId,
  (value) => {
    if (mode.value !== 'customize') return;
    const requested = customProducts.value.find(
      (product) => String(product.id) === String(value)
    );
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
    .map((entry) =>
      cartItemKey(entry) === key
        ? { ...entry, quantity }
        : entry
    )
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

async function clearCart() {
  if (!items.value.length || clearingCart.value) return;
  if (!window.confirm('确定清空购物车中的全部商品吗？')) return;
  clearingCart.value = true;
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
      productName: product.title || product.name,
      productImage: product.previewImageUrl || product.image || product.coverImage || product.mockupImage,
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
    notice.value = reason?.response?.data?.message || reason?.message || '订单提交失败，请稍后重试';
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
    customProducts.value.find((product) => String(product.id) === String(design.productId)) ||
    {
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
    notice.value = '该商品已售罄，无法加入购物车';
    return;
  }
  const cart = readUserData<any[]>('cart_items', []);
  const existing = cart.find(
    (item) => String(item.customDesignId) === String(design.id)
  );
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
    notice.value = reason?.response?.data?.message || '加入购物车失败';
    return;
  }
  const next = existing
    ? cart.map((item) =>
        String(item.customDesignId) === String(design.id) ? { ...item, ...cartItem } : item
      )
    : [...cart, cartItem];
  writeUserData('cart_items', next);
  showAddedDesignTip(design.id);
  notice.value = `定制商品已加入购物车，价格 ¥${price.toFixed(2)}`;
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
    customPatterns.value.find(
      (pattern) => String(pattern.id) === String(design.patternId)
    ) ||
    {
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
  <section class="content workspace-page">
    <div class="page-heading">
      <span class="eyebrow">XIUWEN WORKSPACE</span>
      <h1>{{ title }}</h1>
      <p>统一管理您的广绣创作、学习与订单信息。</p>
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
      <h2>文创商品定制</h2>
      <p>选择后台在售商品和您真实生成的 AI 纹样，在画布中调整位置、大小与角度。</p>
      <p v-if="customizeLoading" class="customize-state">正在加载商品与 AI 纹样…</p>
      <p v-else-if="customizeError" class="customize-state error">{{ customizeError }}</p>
      <div class="customize-layout">
        <div class="customize-options">
          <section>
            <h3>选择商品</h3>
            <div v-if="customProducts.length" class="custom-choice-grid">
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
                  :src="product.image"
                  :alt="product.title"
                />
                <span>
                  <b>{{ product.title }}</b>
                  <small>{{ productStock(product) > 0 ? `库存 ${productStock(product)}` : '已售罄 · 不可定制' }}</small>
                </span>
              </button>
            </div>
            <p v-else class="custom-option-empty">暂无可定制的在售商品，请联系商家上架商品。</p>
          </section>
          <section>
            <h3>选择全部 AI 纹样</h3>
            <button class="generate-ai-pattern" type="button" @click="router.push('/generate')">
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
                  :src="pattern.image"
                  :alt="pattern.title"
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
        :disabled="generatingPreview || !selectedCustomProduct || !selectedCustomPattern || productStock(selectedCustomProduct) <= 0"
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
                    title="加入购物车"
                    aria-label="加入购物车"
                    @click="addDesignToCart(design)"
                  >+</button>
                  <small v-if="addedDesignIds.has(String(design.id))">已经加入购物车</small>
                </div>
                <button class="saved-design-remove" @click="removeDesign(design)">删除记录</button>
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
        <div class="cart-list">
          <article
            v-for="item in items"
            :key="cartItemKey(item)"
            class="cart-item"
          >
            <label
              class="cart-select"
              :title="isCartSelected(item) ? '取消选择' : '选择商品'"
            >
              <input
                class="cart-selector"
                type="checkbox"
                :checked="isCartSelected(item)"
                :aria-label="`选择${cartProduct(item).title || cartProduct(item).name || '商品'}`"
                @change="toggleCartSelection(item)"
              />
            </label>
            <img
              :src="
                cartProduct(item).image ||
                cartProduct(item).coverImage ||
                cartProduct(item).mockupImage
              "
              :alt="cartProduct(item).title || cartProduct(item).name"
            />
            <div class="cart-info">
              <b>{{ cartProduct(item).title || cartProduct(item).name }}</b>
              <span>¥ {{ cartProduct(item).price }}</span>
            </div>
            <div
              class="quantity-control"
              aria-label="商品数量"
            >
              <button
                aria-label="减少数量"
                :disabled="updatingCartKeys.has(cartItemKey(item))"
                @click="updateCartQuantity(item, -1)"
              >
                −
              </button>
              <span>{{ item.quantity || 1 }}</span>
              <button
                aria-label="增加数量"
                :disabled="updatingCartKeys.has(cartItemKey(item))"
                @click="updateCartQuantity(item, 1)"
              >
                ＋
              </button>
            </div>
            <button
              class="remove-cart"
              @click="removeFromCart(item)"
            >
              移除
            </button>
          </article>
        </div>
        <div class="cart-checkout">
          <div class="cart-summary">
            <small v-if="!selectedCartItems.length">请先选择要提交的商品</small>
            <span>已选 {{ selectedCartQuantity }} 件 · 商品合计</span>
            <strong>¥ {{ cartTotal.toFixed(2) }}</strong>
          </div>
          <button
            class="clear-cart"
            :disabled="clearingCart"
            @click="clearCart"
          >
            {{ clearingCart ? '正在清空…' : '清空购物车' }}
          </button>
          <button
            class="primary"
            :disabled="submittingOrder || !selectedCartItems.length"
            @click="openAddressDrawer"
          >
            {{ submittingOrder ? '正在提交…' : '提交订单' }}
          </button>
        </div>
      </template>
      <div
        v-else
        class="result-empty"
      >
        <b>购物车还是空的</b>
        <p>去文创商品页挑选喜欢的广绣好物吧。</p>
        <button
          class="primary"
          @click="router.push('/products')"
        >
          去选购商品
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
      <aside class="address-drawer" aria-label="选择收货地址">
        <header>
          <div><span class="eyebrow">DELIVERY ADDRESS</span><h2>选择收货地址</h2></div>
          <button type="button" aria-label="关闭" @click="addressDrawerOpen = false">×</button>
        </header>
        <p v-if="addressLoading" class="address-loading">正在读取收货地址…</p>
        <div v-else-if="addresses.length" class="checkout-address-list">
          <label
            v-for="address in addresses"
            :key="address.id"
            :class="{ selected: String(address.id) === selectedAddressId }"
          >
            <input v-model="selectedAddressId" type="radio" :value="String(address.id)" />
            <span>
              <b>{{ address.receiverName }}</b><em>{{ address.receiverPhone }}</em>
              <i v-if="Number(address.isDefault) === 1">默认</i>
              <small>{{ fullAddress(address) }}</small>
            </span>
          </label>
        </div>
        <div v-else class="checkout-address-empty">
          <span>⌖</span><h3>还没有收货地址</h3><p>新增地址后才能提交订单。</p>
          <button class="primary" type="button" @click="goAddAddress">＋ 新增收货地址</button>
        </div>
        <footer v-if="addresses.length">
          <button type="button" @click="goAddAddress">新增地址</button>
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
  opacity: .58;
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
.custom-choice-grid span small { display: block; }
.custom-choice-grid span small { margin-top: 5px; color: var(--muted); font-size: 12px; }
.custom-choice-grid button.soldout span small { color: var(--red); }
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
.cart-list {
  display: grid;
  gap: 14px;
}
.cart-item {
  display: grid;
  grid-template-columns: 26px 110px 1fr auto auto;
  gap: 22px;
  align-items: center;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fffaf3;
}
.cart-select {
  display: grid;
  place-items: center;
  cursor: pointer;
}
.cart-selector {
  appearance: none;
  width: 22px;
  height: 22px;
  margin: 0;
  border: 1.5px solid #c9a98d;
  border-radius: 50%;
  background: #fff;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease;
}
.cart-selector:hover {
  border-color: var(--red);
}
.cart-selector:checked {
  border-color: var(--red);
  background: var(--red);
  box-shadow: inset 0 0 0 5px #fff;
}
.cart-selector:focus-visible {
  outline: 2px solid color-mix(in srgb, var(--red) 45%, transparent);
  outline-offset: 3px;
}
.cart-item img {
  width: 110px;
  height: 90px;
  object-fit: cover;
  border-radius: 10px;
}
.cart-info {
  display: grid;
  gap: 10px;
}
.cart-info span {
  color: var(--red);
  font-weight: 700;
}
.quantity-control {
  display: flex;
  align-items: center;
  gap: 12px;
}
.quantity-control button {
  width: 32px;
  height: 32px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: white;
  cursor: pointer;
}
.remove-cart {
  border: 0;
  background: transparent;
  color: var(--red);
  cursor: pointer;
}
.cart-checkout {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 28px;
  margin-top: 20px;
  padding: 20px 24px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fffaf3;
  box-shadow: var(--shadow);
}
.cart-checkout .cart-summary {
  display: flex;
  align-items: baseline;
  gap: 12px;
  color: var(--muted);
}
.cart-summary small {
  color: var(--red);
}
.cart-checkout strong {
  color: var(--red);
  font-size: 28px;
}
.cart-checkout button {
  min-width: 150px;
}
.cart-checkout button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.clear-cart {
  min-width: auto !important;
  border: 1px solid var(--red);
  background: transparent;
  color: var(--red);
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
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}
.address-drawer header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--line);
}
.address-drawer header h2 { margin: 7px 0 0; }
.address-drawer header > button {
  width: 36px;
  height: 36px;
  border: 0;
  background: transparent;
  color: var(--muted);
  font-size: 28px;
  cursor: pointer;
}
.address-loading { padding: 36px 0; color: var(--muted); text-align: center; }
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
.checkout-address-list input { accent-color: var(--red); }
.checkout-address-list span { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.checkout-address-list em { color: var(--muted); font-style: normal; }
.checkout-address-list i {
  padding: 2px 7px;
  border-radius: 10px;
  background: #fff0e8;
  color: var(--red);
  font-size: 11px;
  font-style: normal;
}
.checkout-address-list small { flex-basis: 100%; color: var(--muted); line-height: 1.6; }
.checkout-address-empty { flex: 1; display: grid; place-content: center; justify-items: center; text-align: center; }
.checkout-address-empty > span { color: var(--red); font-size: 44px; }
.checkout-address-empty h3 { margin: 12px 0 4px; }
.checkout-address-empty p { margin: 0 0 20px; color: var(--muted); }
.address-drawer footer {
  display: grid;
  grid-template-columns: 1fr auto auto;
  align-items: center;
  gap: 10px;
  padding-top: 18px;
  border-top: 1px solid var(--line);
}
.address-drawer footer > button { padding: 10px 14px; }
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
  .cart-item {
    grid-template-columns: 24px 80px 1fr;
    gap: 12px;
  }
  .cart-item img {
    width: 80px;
    height: 72px;
  }
  .quantity-control {
    grid-column: 2 / 4;
    justify-self: end;
  }
  .remove-cart {
    grid-column: 3;
    justify-self: end;
  }
  .cart-checkout {
    align-items: stretch;
    flex-direction: column;
  }
  .cart-checkout .cart-summary {
    flex-wrap: wrap;
  }
  .address-drawer { padding: 22px 18px; }
  .address-drawer footer { grid-template-columns: 1fr 1fr; }
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
</style>
