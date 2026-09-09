<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api, listFrom } from '../../api';
import { patterns, products } from '../../data';
import { readUserData, userDataEvent, writeUserData } from '../../userData';

const route = useRoute();
const router = useRouter();
const mode = computed(() => String(route.meta.mode || 'resources'));
const title = computed(() => String(route.meta.title || '功能中心'));
const items = ref<any[]>([]);
const profile = ref<any>({ nickname: '', phone: '', email: '', intro: '' });
const customProductId = ref<number>(Number(products[0]?.id || 501));
const customPatternId = ref<number>(Number(patterns[0]?.id || 301));
const customNote = ref('');
const notice = ref('');
const savedDesigns = ref<any[]>([]);
const selectedItem = ref<any>(null);
const detailLoading = ref(false);
const submittingOrder = ref(false);
const addresses = ref<any[]>([]); const messages = ref<any[]>([]); const addressForm = ref<any>({receiverName:'',receiverPhone:'',province:'',city:'',district:'',detailAddress:'',isDefault:0}); const editingAddress = ref<any>(null);

async function loadItems() {
  items.value = [];
  if (mode.value === 'cart') {
    const localCart = readUserData<any[]>('cart_items', []);
    items.value = localCart;
    try {
      const serverCart = listFrom(await api.cart.items());
      if (!localCart.length && serverCart.length) items.value = serverCart;
    } catch {
      /* 后端未启动时直接使用本地购物车。 */
    }
    return;
  }
  if (mode.value === 'orders') {
    const localOrders = readUserData<any[]>('user_orders', []);
    items.value = localOrders;
    try {
      const serverOrders = listFrom(await api.orders.mine());
      items.value = [...serverOrders, ...localOrders].filter((order, index, values) =>
        values.findIndex((other) => String(other.id || other.orderNo) === String(order.id || order.orderNo)) === index
      );
    } catch { /* 后端未启动时显示本地提交的订单。 */ }
    return;
  }
  try {
    if (mode.value === 'resources') items.value = listFrom(await api.resources.list());
    else if (mode.value === 'profile') { profile.value = await api.user.profile(); addresses.value = listFrom(await api.user.addresses()); messages.value = listFrom(await api.messages.list()); }
  } catch { if (mode.value === 'profile') profile.value = readUserData<any>('profile', profile.value); }
  if (!items.value.length && mode.value !== 'profile') {
    items.value = patterns.slice(0, 6);
  }
}
async function saveAddress(){ const data={...addressForm.value}; try { const r=editingAddress.value?await api.user.updateAddress(editingAddress.value.id,data):await api.user.createAddress(data); if(editingAddress.value) addresses.value=addresses.value.map(a=>a.id===editingAddress.value.id?(r||{...a,...data}):a); else addresses.value.push(r||{...data,id:Date.now()}); } catch { addresses.value.push({...data,id:Date.now()}); } editingAddress.value=null; addressForm.value={receiverName:'',receiverPhone:'',province:'',city:'',district:'',detailAddress:'',isDefault:0}; }
async function deleteAddress(a:any){ try{await api.user.deleteAddress(a.id);}catch{} addresses.value=addresses.value.filter(x=>x.id!==a.id); }
async function setDefault(a:any){ try{await api.user.setDefaultAddress(a.id);}catch{} addresses.value=addresses.value.map(x=>({...x,isDefault:x.id===a.id?1:0})); }
async function markAllMessages(){try{await api.messages.markAllRead();}catch{} messages.value=messages.value.map(m=>({...m,isRead:1}));}

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
  savedDesigns.value = readUserData<any[]>('applied_designs', []);
  window.addEventListener(userDataEvent, handleUserData);
});
onBeforeUnmount(() => window.removeEventListener(userDataEvent, handleUserData));
watch(mode, loadItems);

function cartProduct(item: any) {
  const source = products.find((product) => String(product.id) === String(item.productId || item.id));
  return { ...source, ...item };
}

function updateCartQuantity(item: any, change: number) {
  const cart = readUserData<any[]>('cart_items', []);
  const id = String(item.productId || item.id);
  const next = cart
    .map((entry) => String(entry.productId || entry.id) === id
      ? { ...entry, quantity: Math.max(0, Number(entry.quantity || 1) + change) }
      : entry)
    .filter((entry) => Number(entry.quantity || 0) > 0);
  writeUserData('cart_items', next);
}

function removeFromCart(item: any) {
  const id = String(item.productId || item.id);
  writeUserData('cart_items', readUserData<any[]>('cart_items', []).filter(
    (entry) => String(entry.productId || entry.id) !== id
  ));
}

const cartTotal = computed(() => items.value.reduce((sum, item) => {
  const product = cartProduct(item);
  return sum + Number(product.price || 0) * Number(item.quantity || 1);
}, 0));

async function submitOrder() {
  if (!items.value.length || submittingOrder.value) return;
  submittingOrder.value = true;
  const orderItems = items.value.map((item) => {
    const product = cartProduct(item);
    return {
      productId: item.productId || product.id,
      productName: product.title || product.name,
      productImage: product.image || product.coverImage || product.mockupImage,
      price: Number(product.price || 0),
      quantity: Number(item.quantity || 1)
    };
  });
  const localOrder = {
    id: `local-${Date.now()}`,
    orderNo: `XW${Date.now()}`,
    items: orderItems,
    totalAmount: cartTotal.value,
    status: '待付款',
    createdAt: new Date().toLocaleString('zh-CN')
  };
  const orders = readUserData<any[]>('user_orders', []);
  writeUserData('user_orders', [localOrder, ...orders]);
  void api.orders.create({ items: orderItems, totalAmount: cartTotal.value }).then((result: any) => {
    if (!result) return;
    const current = readUserData<any[]>('user_orders', []);
    writeUserData('user_orders', current.map((order) => order.id === localOrder.id ? { ...order, ...result } : order));
  }).catch(() => { /* 后端未启动时保留本地订单。 */ });
  void api.cart.clear().catch(() => { /* 本地购物车仍会清空。 */ });
  writeUserData('cart_items', []);
  items.value = [];
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
  notice.value = '';
  recordAppliedDesign();
  notice.value = '定制预览已保存到我的纹样和个人中心';
  void api.customDesigns.create({
    productId: customProductId.value,
    patternId: customPatternId.value,
    designConfig: { x: 0.5, y: 0.48, scale: 0.75, rotation: 0 },
    remark: customNote.value
  }).catch(() => { /* 后端未启动时保留本地定制记录。 */ });
}

function recordAppliedDesign() {
  const records = readUserData<any[]>('applied_designs', []);
  const patternSources = [
    ...readUserData<any[]>('saved_patterns', []),
    ...readUserData<any[]>('recent_generations', []),
    ...patterns
  ];
  const selectedPattern = patternSources.find((pattern) => String(pattern.id) === String(customPatternId.value)) || patterns[0];
  const selectedProduct = products.find((product) => String(product.id) === String(customProductId.value)) || products[0];
  const saved = {
    id: `design-${Date.now()}`,
    productId: customProductId.value,
    patternId: customPatternId.value,
    title: selectedProduct?.title,
    productName: selectedProduct?.title,
    productImage: selectedProduct?.image,
    patternTitle: selectedPattern?.title,
    patternImage: selectedPattern?.image,
    pattern: selectedPattern,
    remark: customNote.value,
    createdAt: new Date().toLocaleString('zh-CN')
  };
  writeUserData('applied_designs', [saved, ...records]);
  savedDesigns.value = [saved, ...records];
}

function designProduct(design: any) {
  return products.find((product) => String(product.id) === String(design.productId)) || products[0];
}

function designPattern(design: any) {
  return design.pattern ||
    readUserData<any[]>('saved_patterns', []).find((pattern) => String(pattern.id) === String(design.patternId)) ||
    readUserData<any[]>('recent_generations', []).find((pattern) => String(pattern.id) === String(design.patternId)) ||
    patterns.find((pattern) => String(pattern.id) === String(design.patternId)) || patterns[0];
}

function removeDesign(design: any) {
  const next = savedDesigns.value.filter((item) => item.id !== design.id);
  writeUserData('applied_designs', next);
  savedDesigns.value = next;
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
</script>
<template>
  <section class="content workspace-page">
    <div class="page-heading">
      <span class="eyebrow">XIUWEN WORKSPACE</span>
      <h1>{{ title }}</h1>
      <p>统一管理您的广绣创作、学习与订单信息。</p>
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
        <h3>收货地址</h3><div v-for="a in addresses" :key="a.id"><b>{{a.receiverName}}</b> {{a.receiverPhone}} {{a.province}}{{a.city}}{{a.district}}{{a.detailAddress}} <button @click="setDefault(a)">{{a.isDefault?'默认':'设为默认'}}</button><button @click="editingAddress=a;addressForm={...a}">修改</button><button @click="deleteAddress(a)">删除</button></div><input v-model="addressForm.receiverName" placeholder="收货人"/><input v-model="addressForm.receiverPhone" placeholder="电话"/><input v-model="addressForm.detailAddress" placeholder="详细地址"/><button @click="saveAddress">{{editingAddress?'保存修改':'新增地址'}}</button>
        <h3>消息中心 <button @click="markAllMessages">全部已读</button></h3><div v-for="m in messages" :key="m.id">{{m.title}} <span>{{m.isRead?'已读':'未读'}}</span></div>
      </form>
    </div>
    <div
      v-else-if="mode === 'customize'"
      class="panel customize-panel"
    >
      <h2>文创商品定制</h2>
      <p>选择纹样、商品与数量，预览专属广绣文创作品。</p>
      <div class="option-row">
        <button
          v-for="x in products.slice(0, 4)"
          :key="x.id"
          :class="{ on: customProductId === Number(x.id) }"
          @click="customProductId = Number(x.id); customNote = customNote ? `${customNote} ${x.title}` : String(x.title)"
        >
          {{ x.title }}
        </button>
      </div>
      <textarea
        v-model="customNote"
        placeholder="填写您的定制要求"
      ></textarea
      ><button
        class="primary"
        @click="createPreview"
      >
        生成定制预览
      </button>
      <p v-if="notice">{{ notice }}</p>
      <div class="saved-designs">
        <div class="saved-designs-heading">
          <h3>我的定制作品</h3>
          <span>{{ savedDesigns.length }} 件</span>
        </div>
        <div v-if="savedDesigns.length" class="saved-design-grid">
          <article v-for="design in savedDesigns" :key="design.id" class="saved-design-card">
            <img :src="String(designProduct(design).image || '')" :alt="String(designProduct(design).title || '')" />
            <div>
              <b>{{ designProduct(design).title }}</b>
              <span>纹样：{{ designPattern(design).title }}</span>
              <small v-if="design.remark">{{ design.remark }}</small>
              <button @click="removeDesign(design)">删除记录</button>
            </div>
          </article>
        </div>
        <p v-else class="saved-empty">生成预览后，定制作品会保存在这里。</p>
      </div>
    </div>
    <div v-else-if="mode === 'cart'" class="cart-panel">
      <template v-if="items.length">
        <div class="cart-list">
          <article v-for="item in items" :key="item.productId || item.id" class="cart-item">
            <img
              :src="cartProduct(item).image || cartProduct(item).coverImage || cartProduct(item).mockupImage"
              :alt="cartProduct(item).title || cartProduct(item).name"
            />
            <div class="cart-info">
              <b>{{ cartProduct(item).title || cartProduct(item).name }}</b>
              <span>¥ {{ cartProduct(item).price }}</span>
            </div>
            <div class="quantity-control" aria-label="商品数量">
              <button aria-label="减少数量" @click="updateCartQuantity(item, -1)">−</button>
              <span>{{ item.quantity || 1 }}</span>
              <button aria-label="增加数量" @click="updateCartQuantity(item, 1)">＋</button>
            </div>
            <button class="remove-cart" @click="removeFromCart(item)">移除</button>
          </article>
        </div>
        <div class="cart-checkout">
          <div><span>商品合计</span><strong>¥ {{ cartTotal.toFixed(2) }}</strong></div>
          <button class="primary" :disabled="submittingOrder" @click="submitOrder">{{ submittingOrder ? '正在提交…' : '提交订单' }}</button>
        </div>
      </template>
      <div v-else class="result-empty">
        <b>购物车还是空的</b>
        <p>去文创商品页挑选喜欢的广绣好物吧。</p>
        <button class="primary" @click="router.push('/products')">去选购商品</button>
      </div>
    </div>
    <div
      v-else
      class="workspace-grid"
    >
      <article
        v-for="item in items"
        :key="item.id"
      >
        <img
          :src="
            item.image ||
            item.coverImage ||
            item.imageUrl ||
            item.productCoverImage ||
            item.previewImageUrl
          "
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
      v-if="selectedItem"
      class="resource-detail-backdrop"
      @click.self="selectedItem = null"
    >
      <article class="resource-detail-card">
        <button class="detail-close" aria-label="关闭" @click="selectedItem = null">×</button>
        <img
          :src="selectedItem.image || selectedItem.coverImage || selectedItem.imageUrl"
          :alt="selectedItem.title || selectedItem.name"
        />
        <div>
          <span class="eyebrow">CREATIVE RESOURCE</span>
          <h2>{{ selectedItem.title || selectedItem.name || '资源详情' }}</h2>
          <p>{{ selectedItem.description || selectedItem.desc || selectedItem.meta || '广绣文化创作与学习资源。' }}</p>
          <small v-if="detailLoading">正在读取详细资料…</small>
          <button class="primary" @click="selectedItem = null">关闭详情</button>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.cart-list { display: grid; gap: 14px; }
.cart-item { display: grid; grid-template-columns: 110px 1fr auto auto; gap: 22px; align-items: center; padding: 16px; border: 1px solid var(--line); border-radius: 14px; background: #fffaf3; }
.cart-item img { width: 110px; height: 90px; object-fit: cover; border-radius: 10px; }
.cart-info { display: grid; gap: 10px; }
.cart-info span { color: var(--red); font-weight: 700; }
.quantity-control { display: flex; align-items: center; gap: 12px; }
.quantity-control button { width: 32px; height: 32px; border: 1px solid var(--line); border-radius: 8px; background: white; cursor: pointer; }
.remove-cart { border: 0; background: transparent; color: var(--red); cursor: pointer; }
.cart-checkout { display: flex; justify-content: flex-end; align-items: center; gap: 28px; margin-top: 20px; padding: 20px 24px; border: 1px solid var(--line); border-radius: 14px; background: #fffaf3; box-shadow: var(--shadow); }
.cart-checkout div { display: flex; align-items: baseline; gap: 12px; color: var(--muted); }
.cart-checkout strong { color: var(--red); font-size: 28px; }
.cart-checkout button { min-width: 150px; }
.cart-checkout button:disabled { opacity: .6; cursor: wait; }
.saved-designs { margin-top: 28px; padding-top: 22px; border-top: 1px solid var(--line); }
.saved-designs-heading { display: flex; align-items: center; justify-content: space-between; }
.saved-designs-heading h3 { margin: 0; }
.saved-designs-heading span { color: var(--muted); }
.saved-design-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin-top: 15px; }
.saved-design-card { display: flex; gap: 12px; padding: 12px; border: 1px solid var(--line); border-radius: 12px; background: #fffaf3; }
.saved-design-card img { width: 82px; height: 82px; object-fit: cover; border-radius: 8px; }
.saved-design-card div { display: grid; gap: 5px; align-content: start; }
.saved-design-card span, .saved-design-card small { color: var(--muted); }
.saved-design-card button { width: fit-content; border: 0; padding: 0; background: transparent; color: var(--red); cursor: pointer; }
.saved-empty { color: var(--muted); }
@media (max-width: 700px) { .cart-item { grid-template-columns: 80px 1fr; } .cart-item img { width: 80px; height: 72px; } .cart-checkout { align-items: stretch; flex-direction: column; } }
@media (max-width: 900px) { .saved-design-grid { grid-template-columns: 1fr; } }
</style>
