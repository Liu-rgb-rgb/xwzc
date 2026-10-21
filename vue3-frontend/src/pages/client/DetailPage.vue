<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api, listFrom } from '../../api';
import { courses, patterns, products } from '../../data';
import { isRealClient } from '../../auth';
import { readUserData, writeUserData } from '../../userData';

const route = useRoute();
const router = useRouter();
const kind = computed(() => String(route.meta.kind || 'product'));
const id = computed(() =>
  String(
    route.params.productId ||
      route.params.courseId ||
      route.params.patternId ||
      route.params.orderId ||
      1
  )
);
const remoteItem = ref<any>(null);
const loading = ref(false);
const courseVideoFailed = ref(false);
const quantity = ref(1);
const adding = ref(false);
const cartNotice = ref('');
const cartQty = ref(0);
const cartTotal = ref(0);
const cartItemId = ref<number | null>(null);
const styleLabels: Record<string, string> = {
  classic: '广绣经典',
  new_chinese: '新中式',
  lingnan_window: '岭南花窗',
  embroidery: '刺绣纹样'
};
const courseFallbackImages = [
  '/demo/pattern/peony-phoenix-pattern-01.jpg',
  '/demo/pattern/peony-phoenix-pattern-02.jpg',
  '/demo/product/round-coaster-set-cover.jpg',
  '/demo/product/peony-canvas-bag-cover.jpg'
];

const fallbackItem = computed(() =>
  kind.value === 'course'
    ? courses.find((x) => String(x.id) === id.value) || courses[0]
    : kind.value === 'pattern'
      ? patterns.find((x) => String(x.id) === id.value) || patterns[0]
    : products.find((x) => String(x.id) === id.value) || products[0]
);
const item = computed(() => {
  const fallback = (fallbackItem.value || {}) as any;
  const remote = remoteItem.value;
  if (!remote) return fallback;
  return {
    ...fallback,
    ...remote,
    title: remote.title || remote.name || fallback.title,
    desc: remote.subtitle || remote.description || remote.desc || fallback.desc,
    image: remote.image || remote.coverImage || remote.imageUrl || fallback.image
  };
});
const detailImage = computed(() => {
  const value = item.value as any;
  if (value?.image || value?.coverImage || value?.imageUrl) {
    return String(value.image || value.coverImage || value.imageUrl);
  }
  if (kind.value === 'course') {
    const index = Math.max(0, Number(id.value) - 1) % courseFallbackImages.length;
    return courseFallbackImages[index];
  }
  return '/demo/product/peony-canvas-bag-cover.jpg';
});
const detailStyle = computed(() => {
  const value = String(item.value?.style || '广绣纹样');
  return styleLabels[value] || value;
});
const stockKnown = computed(() => remoteItem.value != null && remoteItem.value.stock != null);
const stockCount = computed(() => Math.max(0, Number((item.value as any)?.stock ?? 0) || 0));
const outOfStock = computed(() => stockKnown.value && stockCount.value === 0);
const maxQuantity = computed(() => (stockKnown.value ? Math.max(1, stockCount.value) : 99));
const priceText = computed(() => {
  const value = Number((item.value as any)?.price);
  if (!Number.isFinite(value)) return String((item.value as any)?.price ?? '');
  return value.toLocaleString('zh-CN', { maximumFractionDigits: 2 });
});
const categoryChip = computed(() => {
  const value = item.value as any;
  return String(value?.categoryName || value?.category?.name || '广绣文创');
});

function useDetailFallback(event: Event) {
  const image = event.currentTarget as HTMLImageElement;
  const fallback = String(
    fallbackItem.value?.image || '/demo/pattern/peony-phoenix-pattern-01.jpg'
  );
  if (!image.src.endsWith(fallback)) image.src = fallback;
}

async function loadDetail() {
  loading.value = true;
  courseVideoFailed.value = false;
  try {
    if (kind.value === 'course') remoteItem.value = await api.courses.detail(id.value);
    else if (kind.value === 'pattern') remoteItem.value = await api.patterns.detail(id.value);
    else if (kind.value === 'order') remoteItem.value = await api.orders.detail(id.value);
    else remoteItem.value = await api.products.detail(id.value);
    if (kind.value === 'course' && isRealClient.value) {
      api.courses
        .study(id.value)
        .then((result: any) => {
          if (result?.studyCount != null && remoteItem.value) {
            remoteItem.value = { ...remoteItem.value, studyCount: result.studyCount };
          }
        })
        .catch(() => {});
    }
  } catch {
    remoteItem.value = null;
  } finally {
    loading.value = false;
  }
}

function refreshCartQty() {
  const cart = readUserData<any[]>('cart_items', []);
  cartTotal.value = cart.reduce((sum, entry) => sum + Number(entry.quantity || 0), 0);
  if (kind.value !== 'product') {
    cartQty.value = 0;
    return;
  }
  const existing = cart.find((entry) => String(entry.productId || entry.id) === id.value);
  cartQty.value = Number(existing?.quantity || 0);
  if (existing?.cartItemId != null) cartItemId.value = Number(existing.cartItemId);
}

async function syncCartItem() {
  if (!isRealClient.value) return;
  try {
    const serverCart = listFrom(await api.cart.items());
    const entry = serverCart.find(
      (row: any) => Number(row.productId) === Number(id.value) && row.customDesignId == null
    );
    if (!entry) {
      cartItemId.value = null;
      return;
    }
    cartItemId.value = entry.id ?? null;
    const serverQty = Number(entry.quantity || 0);
    // 本地打样单为空但服务端已有记录时，把数量写回本地，保证卡片角标、顶部角标与详情页一致。
    if (!cartQty.value && serverQty > 0) {
      const cart = readUserData<any[]>('cart_items', []);
      writeUserData('cart_items', [
        ...cart.filter((row) => String(row.productId || row.id) !== id.value),
        {
          ...(item.value as any),
          productId: Number(id.value),
          quantity: serverQty,
          cartItemId: entry.id
        }
      ]);
      refreshCartQty();
    }
  } catch {
    /* 后端不可用时沿用本地打样单数据 */
  }
}

function changeQuantity(delta: number) {
  const next = quantity.value + delta;
  quantity.value = Math.min(Math.max(1, next), maxQuantity.value);
}

async function addProductToCart() {
  if (outOfStock.value) {
    cartNotice.value = '该商品已售罄';
    return;
  }
  if (!isRealClient.value) {
    await router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }
  if (stockKnown.value && quantity.value > stockCount.value) {
    cartNotice.value = `库存仅剩 ${stockCount.value} 件`;
    return;
  }
  const target = quantity.value;
  const updating = cartQty.value > 0;
  adding.value = true;
  cartNotice.value = '';
  try {
    if (updating && cartItemId.value != null) {
      await api.cart.update(cartItemId.value, { quantity: target });
    } else {
      const created: any = await api.cart.add({
        productId: Number(id.value),
        quantity: target
      });
      if (created?.id != null) cartItemId.value = Number(created.id);
    }
  } catch (reason: any) {
    cartNotice.value = reason?.response?.data?.message || '更新打样需求失败，请稍后重试';
    adding.value = false;
    return;
  }
  const cart = readUserData<any[]>('cart_items', []);
  const existing = cart.find((entry) => String(entry.productId || entry.id) === id.value);
  const next = existing
    ? cart.map((entry) =>
        String(entry.productId || entry.id) === id.value
          ? { ...entry, quantity: target, cartItemId: cartItemId.value ?? entry.cartItemId }
          : entry
      )
    : [
        ...cart,
        {
          ...(item.value as any),
          productId: Number(id.value),
          quantity: target,
          cartItemId: cartItemId.value
        }
      ];
  writeUserData('cart_items', next);
  refreshCartQty();
  adding.value = false;
  cartNotice.value = updating ? `打样单中该商品已更新为 ${target} 件` : `已添加 ${target} 件到打样需求`;
}

watch(
  [kind, id],
  async () => {
    quantity.value = 1;
    adding.value = false;
    cartNotice.value = '';
    cartItemId.value = null;
    refreshCartQty();
    await loadDetail();
    await syncCartItem();
  },
  { immediate: true }
);

watch([cartQty, maxQuantity], () => {
  quantity.value = Math.min(Math.max(1, cartQty.value || 1), maxQuantity.value);
});
</script>
<template>
  <div
    v-if="kind === 'product'"
    class="product-detail"
  >
    <div
      class="paper-noise"
      aria-hidden="true"
    />
    <div class="product-detail-wrap">
      <div class="product-media">
        <img
          class="product-media-img"
          :src="detailImage"
          :alt="String(item.title || item.name)"
          @error="useDetailFallback"
        />
      </div>
      <div class="product-copy">
        <p class="product-eyebrow">CULTURAL CREATION</p>
        <h1>{{ item.title || item.name }}</h1>
        <p class="product-sub">{{ item.desc || item.description || item.summary }}</p>
        <ul class="product-chips">
          <li>
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2.4"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <path d="m5 12 4.5 4.5L19 7" />
            </svg>
            非遗工艺
          </li>
          <li>{{ categoryChip }}</li>
          <li>小批量打样</li>
        </ul>
        <div class="product-price">
          <span class="currency">¥</span>
          <span class="amount">{{ priceText }}</span>
          <span
            class="stock"
            :class="{ soldout: outOfStock }"
            >{{ stockKnown ? `库存 ${stockCount} 件` : '现货供应' }}</span
          >
        </div>
        <div class="product-qty">
          <span>打样数量</span>
          <div class="stepper">
            <button
              type="button"
              aria-label="减少数量"
              :disabled="quantity <= 1 || outOfStock"
              @click="changeQuantity(-1)"
            >
              −
            </button>
            <b>{{ quantity }}</b>
            <button
              type="button"
              aria-label="增加数量"
              :disabled="quantity >= maxQuantity || outOfStock"
              @click="changeQuantity(1)"
            >
              +
            </button>
          </div>
        </div>
        <button
          class="product-add"
          type="button"
          :disabled="outOfStock || adding"
          @click="addProductToCart"
        >
          {{ outOfStock ? '已售罄' : adding ? (cartQty ? '正在更新…' : '正在添加…') : cartQty ? '更新打样数量' : '添加到打样需求' }}
        </button>
        <p
          v-if="cartNotice"
          class="product-notice"
        >
          {{ cartNotice }}
          <RouterLink
            v-if="isRealClient"
            to="/cart"
            >查看打样单 →</RouterLink
          >
        </p>
        <p
          v-else-if="cartQty"
          class="product-cart-state"
        >
          打样单共 {{ cartTotal }} 件 · 本商品 {{ cartQty }} 件 ·
          <RouterLink to="/cart">查看打样单</RouterLink>
        </p>
        <div class="product-services">
          <span>
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.6"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <path d="M12 3.5 13.8 9l5.7 1.8-5.7 1.8L12 18.2l-1.8-5.6L4.5 10.8 10.2 9Z" />
            </svg>
            算法打版
          </span>
          <span>
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.6"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <path d="M3 20h18M5 20V11l5 3v-3l5 3V6h4v14" />
            </svg>
            源头工坊
          </span>
          <span>
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.6"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <circle
                cx="12"
                cy="12"
                r="8"
              />
              <path d="m8.5 12 2.5 2.5 4.5-5" />
            </svg>
            进度可查
          </span>
        </div>
      </div>
    </div>
  </div>
  <section
    v-else
    class="content detail-page"
  >
    <RouterLink
      v-if="kind === 'course'"
      class="detail-back-top"
      to="/courses"
      >← 返回非遗课堂</RouterLink
    >
    <div
      v-if="kind === 'order'"
      class="panel"
    >
      <span class="eyebrow">ORDER DETAIL</span>
      <h1>订单详情 #{{ id }}</h1>
      <p v-if="loading">正在加载订单...</p>
      <div class="timeline">
        <b>{{ item?.statusName || item?.status || '订单已提交' }}</b>
        <span>{{ item?.orderNo || '等待商家确认与制作' }}</span>
        <span>{{ item?.logisticsInfo || '物流信息将在发货后更新' }}</span>
      </div>
      <button class="primary">查看物流</button>
    </div>
    <template v-else>
      <video
        v-if="kind === 'course' && item.videoUrl && !courseVideoFailed"
        class="detail-cover detail-video"
        :src="item.videoUrl"
        :poster="detailImage"
        controls
        autoplay
        playsinline
        preload="metadata"
        @error="courseVideoFailed = true"
      ></video>
      <img
        v-else
        class="detail-cover"
        :src="detailImage"
        :alt="String(item.title || item.name)"
        @error="useDetailFallback"
      />
      <div class="detail-copy">
        <span class="eyebrow">{{ kind === 'course' ? 'HERITAGE COURSE' : 'PATTERN ARCHIVE' }}</span>
        <h1>{{ item.title || item.name }}</h1>
        <p>{{ item.desc || item.description || item.summary }}</p>
        <p v-if="kind === 'pattern'">
          {{ detailStyle }} ·
          {{ item.meta || item.elements?.join(' / ') || item.keyword || '传统纹样资料' }}
        </p>
        <RouterLink
          v-if="kind === 'pattern'"
          class="detail-back"
          to="/patterns"
          >返回我的纹样</RouterLink
        >
      </div>
      <section
        v-if="kind === 'course' && item.content"
        class="course-learning panel"
      >
        <span class="eyebrow">COURSE CONTENT</span>
        <h2>{{ item.title }}</h2>
        <div
          class="course-content"
          v-html="item.content"
        ></div>
      </section>
    </template>
  </section>
</template>

<style scoped>
.product-detail {
  --paper: #fcfbfa;
  --ink-green: #1f3b2c;
  --accent: #c13e33;
  --hair: #eae6df;
  --ease: cubic-bezier(0.2, 0.8, 0.2, 1);
  position: relative;
  min-height: 100vh;
  background: var(--paper);
  color: #333;
  font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}
.paper-noise {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  opacity: 0.6;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)' opacity='0.035'/%3E%3C/svg%3E");
}
.product-detail-wrap {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1.02fr 0.98fr;
  gap: clamp(40px, 5vw, 76px);
  align-items: center;
  max-width: 1280px;
  margin: 0 auto;
  padding: clamp(40px, 5vw, 66px) 48px 110px;
}
.product-media {
  display: grid;
  place-items: center;
  aspect-ratio: 1 / 1;
  padding: clamp(26px, 3.6vw, 56px);
  overflow: hidden;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 22px 60px rgba(24, 40, 32, 0.06);
}
.product-media-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.product-copy {
  animation: copy-rise 0.8s var(--ease) both;
}
.product-eyebrow {
  margin: 0 0 14px;
  color: #8d8a84;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 2.4px;
}
.product-copy h1 {
  margin: 0 0 14px;
  color: var(--ink-green);
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: clamp(34px, 3.4vw, 46px);
  font-weight: 900;
  line-height: 1.25;
  letter-spacing: 3px;
}
.product-sub {
  max-width: 46ch;
  margin: 0 0 22px;
  color: #7a7a7a;
  font-size: 14px;
  line-height: 1.9;
  letter-spacing: 1px;
}
.product-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 0 0 26px;
  padding: 0;
  list-style: none;
}
.product-chips li {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: 999px;
  background: #fbecea;
  color: #a33b31;
  font-size: 12px;
  letter-spacing: 0.5px;
}
.product-chips svg {
  width: 12px;
  height: 12px;
}
.product-price {
  display: flex;
  align-items: baseline;
  gap: 2px;
  margin: 0 0 26px;
}
.product-price .currency {
  color: var(--accent);
  font-size: 16px;
  font-weight: 600;
}
.product-price .amount {
  color: var(--accent);
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 38px;
  font-weight: 700;
  line-height: 1;
}
.product-price .stock {
  margin-left: 14px;
  color: #a8a29a;
  font-size: 12px;
  letter-spacing: 0.5px;
}
.product-price .stock.soldout {
  color: var(--accent);
}
.product-qty {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 0 0 18px;
}
.product-qty > span {
  color: #6f665a;
  font-size: 13px;
  letter-spacing: 1px;
}
.stepper {
  display: inline-flex;
  align-items: center;
  overflow: hidden;
  border: 1px solid var(--hair);
  border-radius: 999px;
  background: #fff;
}
.stepper button {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: var(--ink-green);
  font-size: 18px;
  line-height: 1;
}
.stepper button:hover:not(:disabled) {
  border-color: transparent;
  background: rgba(31, 59, 44, 0.06);
  transform: none;
}
.stepper button:disabled {
  color: #c9c3ba;
  cursor: not-allowed;
}
.stepper b {
  min-width: 38px;
  color: var(--ink-green);
  font-size: 14px;
  font-weight: 600;
  text-align: center;
}
.product-add {
  width: 100%;
  height: 52px;
  border: 0;
  border-radius: 999px;
  background: var(--ink-green);
  color: #fff;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 2px;
  transition:
    background 0.3s var(--ease),
    box-shadow 0.3s var(--ease);
}
.product-add:hover:not(:disabled) {
  border-color: transparent;
  background: #163026;
  box-shadow: 0 12px 26px rgba(31, 59, 44, 0.18);
  transform: none;
}
.product-add:disabled {
  background: #c9c3ba;
  cursor: not-allowed;
}
.product-notice,
.product-cart-state {
  margin: 14px 0 0;
  color: #0b625c;
  font-size: 13px;
  letter-spacing: 0.5px;
}
.product-notice a,
.product-cart-state a {
  color: var(--ink-green);
  text-decoration: underline;
  text-underline-offset: 3px;
}
.product-services {
  display: flex;
  flex-wrap: wrap;
  gap: 26px;
  margin-top: 26px;
  padding-top: 22px;
  border-top: 1px solid #f0ece5;
  color: #8d8a84;
  font-size: 12px;
  letter-spacing: 0.5px;
}
.product-services span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}
.product-services svg {
  width: 15px;
  height: 15px;
  color: #9a948b;
}
.detail-back {
  display: inline-flex;
  align-items: center;
  padding: 10px 20px;
  border: 1px solid #eae6df;
  border-radius: 999px;
  color: #1f3b2c;
  font-size: 14px;
  transition:
    border-color 0.3s,
    background 0.3s;
}
.detail-back:hover {
  border-color: #1f3b2c;
  background: rgba(31, 59, 44, 0.05);
}
.detail-video {
  object-fit: contain;
  background: #171412;
}
.detail-back-top {
  grid-column: 1 / -1;
  justify-self: start;
  display: inline-flex;
  align-items: center;
  padding: 8px 16px;
  border: 1px solid #eae6df;
  border-radius: 999px;
  background: #fff;
  color: #6f665a;
  font-size: 13px;
  letter-spacing: 0.5px;
  transition:
    border-color 0.3s,
    color 0.3s,
    background 0.3s;
}
.detail-back-top:hover {
  border-color: #1f3b2c;
  background: rgba(31, 59, 44, 0.04);
  color: #1f3b2c;
}
@keyframes copy-rise {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}
.course-learning {
  grid-column: 1 / -1;
  width: 100%;
  scroll-margin-top: 110px;
}
.course-learning h2 {
  margin: 12px 0 22px;
  font-size: 30px;
}
.course-content {
  color: var(--ink);
  line-height: 1.9;
}
@media (max-width: 960px) {
  .product-detail-wrap {
    grid-template-columns: 1fr;
    gap: 32px;
    padding: 40px 24px 82px;
  }
  .product-media {
    width: 100%;
    max-width: 560px;
    margin: 0 auto;
  }
}
@media (max-width: 600px) {
  .product-detail-wrap {
    padding: 28px 16px 66px;
  }
  .product-copy h1 {
    letter-spacing: 2px;
  }
  .product-services {
    gap: 16px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .product-copy {
    animation: none;
  }
}
</style>
