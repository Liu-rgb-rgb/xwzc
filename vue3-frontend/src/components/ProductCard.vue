<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { readUserData, writeUserData } from '../userData';
import { api } from '../api';
import { isRealClient } from '../auth';

const props = defineProps<{ p: any }>();
const router = useRouter();
const savedIds = () => readUserData<string[]>('product_favorites', []);
const favorite = ref(savedIds().includes(String(props.p.id)));
const title = computed(() => props.p.title || props.p.name);
const stockCount = computed(() => Math.max(0, Number(props.p.stock ?? 0) || 0));
const outOfStock = computed(() => stockCount.value === 0);
const priceText = computed(() => {
  const value = Number(props.p.price);
  if (!Number.isFinite(value)) return String(props.p.price ?? '');
  return value.toLocaleString('zh-CN', { maximumFractionDigits: 2 });
});
const cartNotice = ref('');
const cartQty = ref(0);
function refreshCartQty() {
  const cart = readUserData<any[]>('cart_items', []);
  const existing = cart.find((item) => String(item.productId || item.id) === String(props.p.id));
  cartQty.value = Number(existing?.quantity || 0);
}
refreshCartQty();
const fallbackImages = [
  '/demo/product/peony-canvas-bag-cover.jpg',
  '/demo/product/phoenix-tote-cover.jpg',
  '/demo/product/lingnan-silk-scarf-cover.jpg',
  '/demo/product/lingnan-notebook-cover.jpg'
];
const fallbackImage = computed(
  () => fallbackImages[Math.abs(Number(props.p.id) || 0) % fallbackImages.length]
);

function useImageFallback(event: Event) {
  const image = event.currentTarget as HTMLImageElement;
  if (!image.src.endsWith(fallbackImage.value)) image.src = fallbackImage.value;
}

function toggleFavorite() {
  favorite.value = !favorite.value;
  const ids = new Set(savedIds());
  favorite.value ? ids.add(String(props.p.id)) : ids.delete(String(props.p.id));
  writeUserData('product_favorites', [...ids]);
}
async function addToCart() {
  if (outOfStock.value) {
    cartNotice.value = '该商品已售罄';
    window.setTimeout(() => (cartNotice.value = ''), 1500);
    return;
  }
  if (!isRealClient.value) {
    await router.push({ path: '/login', query: { redirect: `/products/${props.p.id}` } });
    return;
  }
  const cart = readUserData<any[]>('cart_items', []);
  const existing = cart.find((item) => String(item.productId || item.id) === String(props.p.id));
  if (Number(existing?.quantity || 0) >= stockCount.value) {
    cartNotice.value = '已达到库存上限';
    window.setTimeout(() => (cartNotice.value = ''), 1500);
    return;
  }
  try {
    await api.cart.add({ productId: props.p.id, quantity: 1 });
  } catch (reason: any) {
    cartNotice.value = reason?.response?.data?.message || '提交打样需求失败';
    window.setTimeout(() => (cartNotice.value = ''), 1800);
    return;
  }
  const next = existing
    ? cart.map((item) =>
        String(item.productId || item.id) === String(props.p.id)
          ? { ...item, quantity: Number(item.quantity || 1) + 1 }
          : item
      )
    : [...cart, { ...props.p, productId: props.p.id, quantity: 1 }];
  writeUserData('cart_items', next);
  refreshCartQty();
  cartNotice.value = '已提交打样需求';
  window.setTimeout(() => (cartNotice.value = ''), 1500);
}
</script>

<template>
  <article class="product-card">
    <RouterLink
      class="image-wrap"
      :to="`/products/${p.id}`"
      :aria-label="`查看商品：${title}`"
    >
      <span class="image-stage">
        <img
          :src="p.image || p.coverImage || p.mockupImage"
          :alt="p.title || p.name"
          loading="lazy"
          @error="useImageFallback"
        />
      </span>
    </RouterLink>
    <div class="card-body">
      <h3>
        <RouterLink :to="`/products/${p.id}`">{{ title }}</RouterLink>
      </h3>
      <p>{{ p.desc || p.subtitle || p.description }}</p>
      <div class="price-row">
        <span class="price-currency">¥</span>
        <span class="price-value">{{ priceText }}</span>
        <span
          class="stock-info"
          :class="{ soldout: outOfStock }"
        >
          库存 {{ stockCount }}<template v-if="outOfStock"> · 已售罄</template>
        </span>
      </div>
      <div class="product-actions">
        <button
          class="cart-button"
          type="button"
          :disabled="outOfStock"
          :aria-label="
            outOfStock
              ? '商品已售罄'
              : cartQty
                ? `已提交 ${cartQty} 件打样，点击继续提交`
                : '提交打样需求'
          "
          :title="outOfStock ? '商品已售罄' : cartQty ? `已提交 ${cartQty} 件打样` : '提交打样需求'"
          @click="addToCart"
        >
          +<i
            v-if="cartQty"
            class="cart-badge"
            >{{ cartQty }}</i
          >
        </button>
        <button
          class="small"
          type="button"
          :disabled="outOfStock"
          :title="outOfStock ? '商品已售罄' : '提交打样需求'"
          @click="addToCart"
        >
          {{ outOfStock ? '已售罄' : '提交打样需求' }}
        </button>
      </div>
      <small
        v-if="cartNotice"
        class="cart-notice"
        >{{ cartNotice }}</small
      >
    </div>
  </article>
</template>

<style scoped>
.product-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  border: 1px solid rgba(31, 59, 44, 0.06);
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 4px 20px rgba(24, 40, 32, 0.03);
  transition:
    transform 0.45s var(--ease, cubic-bezier(0.2, 0.8, 0.2, 1)),
    box-shadow 0.45s var(--ease, cubic-bezier(0.2, 0.8, 0.2, 1)),
    border-color 0.45s var(--ease, cubic-bezier(0.2, 0.8, 0.2, 1));
}
.product-card:hover {
  transform: translateY(-6px);
  border-color: rgba(31, 59, 44, 0.12);
  box-shadow: 0 18px 42px rgba(24, 40, 32, 0.09);
}

/* 图片展台：浅米色底 + 极缓呼吸 */
.product-card .image-wrap {
  position: relative;
  display: block;
  overflow: hidden;
  aspect-ratio: 1 / 1;
  padding: 26px;
  background: #f5f3ef;
}
.product-card .image-wrap::after {
  content: '';
  position: absolute;
  inset: -20% -40%;
  pointer-events: none;
  opacity: 0;
  background: linear-gradient(
    115deg,
    transparent 42%,
    rgba(255, 255, 255, 0.45) 50%,
    transparent 58%
  );
  transform: translateX(-55%);
  transition:
    transform 0.9s var(--ease, cubic-bezier(0.2, 0.8, 0.2, 1)),
    opacity 0.45s;
}
.product-card:hover .image-wrap::after {
  opacity: 1;
  transform: translateX(55%);
}
.image-stage {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  animation: stage-breathe 9s ease-in-out infinite alternate;
  animation-delay: var(--reveal-delay, 0ms);
}
.product-card:hover .image-stage {
  animation-play-state: paused;
}
.product-card .image-stage img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform 0.6s var(--ease, cubic-bezier(0.2, 0.8, 0.2, 1));
}
.product-card:hover .image-stage img {
  transform: scale(1.05);
}
@keyframes stage-breathe {
  from {
    transform: scale(1);
  }
  to {
    transform: scale(1.02);
  }
}

/* 商品信息 */
.product-card .card-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  padding: 22px 24px 24px;
}
.product-card .card-body h3 {
  margin: 0 0 8px;
  color: #1f3b2c;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
.product-card .card-body h3 a:hover {
  text-decoration: underline;
  text-decoration-thickness: 1px;
  text-underline-offset: 4px;
  text-decoration-color: rgba(31, 59, 44, 0.28);
}
.product-card .card-body p {
  display: -webkit-box;
  min-height: 42px;
  margin: 0 0 18px;
  overflow: hidden;
  color: #8a857e;
  font-size: 13px;
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.price-row {
  display: flex;
  align-items: baseline;
  margin-top: auto;
  padding-bottom: 18px;
}
.price-currency {
  margin-right: 2px;
  color: #1f3b2c;
  font-size: 13px;
  font-weight: 600;
}
.price-value {
  color: #1f3b2c;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
}
.stock-info {
  margin-left: 12px;
  color: #a8a29a;
  font-size: 12px;
  letter-spacing: 0.5px;
}
.stock-info.soldout {
  color: #c13e33;
}

/* 操作区 */
.product-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 18px;
  border-top: 1px solid #f2efe9;
}
.product-actions .cart-button {
  position: relative;
  flex: 0 0 42px;
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 1px solid #e6e2da;
  border-radius: 50%;
  background: #fff;
  color: #1f3b2c;
  font-size: 20px;
  font-weight: 400;
  line-height: 1;
  transition: all 0.3s var(--ease, cubic-bezier(0.2, 0.8, 0.2, 1));
}
.product-actions .cart-button:hover:not(:disabled) {
  border-color: #1f3b2c;
  background: #1f3b2c;
  color: #fff;
  transform: none;
}
.product-actions .cart-button:disabled {
  cursor: not-allowed;
  border-color: #e6e2da;
  background: #f5f3ef;
  color: #b9b2a8;
}
.cart-badge {
  position: absolute;
  top: -6px;
  right: -7px;
  min-width: 17px;
  height: 17px;
  box-sizing: border-box;
  padding: 0 4px;
  display: grid;
  place-items: center;
  border: 2px solid #fff;
  border-radius: 999px;
  background: #c13e33;
  color: #fff;
  font-size: 11px;
  font-style: normal;
  font-weight: 700;
  line-height: 1;
}
.product-actions .small {
  flex: 1;
  height: 42px;
  padding: 0 18px;
  float: none;
  border: 0;
  border-radius: 999px;
  background: rgba(31, 59, 44, 0.08);
  color: #1f3b2c;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s var(--ease, cubic-bezier(0.2, 0.8, 0.2, 1));
}
.product-actions .small:hover:not(:disabled) {
  border-color: transparent;
  background: #1f3b2c;
  color: #fff;
  transform: none;
}
.product-actions .small:disabled {
  cursor: not-allowed;
  background: #f2efe9;
  color: #a8a29a;
}
.cart-notice {
  display: block;
  margin-top: 10px;
  color: #0b625c;
  font-size: 12px;
}

@media (prefers-reduced-motion: reduce) {
  .image-stage {
    animation: none;
  }
  .product-card,
  .product-card .image-stage img {
    transition: none;
  }
}
</style>
