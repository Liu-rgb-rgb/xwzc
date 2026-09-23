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
    await router.push({ path: '/login', query: { redirect: '/cart' } });
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
    cartNotice.value = reason?.response?.data?.message || '加入购物车失败';
    window.setTimeout(() => (cartNotice.value = ''), 1800);
    return;
  }
  const next = existing
    ? cart.map((item) => String(item.productId || item.id) === String(props.p.id)
        ? { ...item, quantity: Number(item.quantity || 1) + 1 }
        : item)
    : [...cart, { ...props.p, productId: props.p.id, quantity: 1 }];
  writeUserData('cart_items', next);
  refreshCartQty();
  cartNotice.value = '已加入购物车';
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
      <img
        :src="p.image || p.coverImage || p.mockupImage"
        :alt="p.title || p.name"
        @error="useImageFallback"
      />
    </RouterLink>
    <div>
      <h3><RouterLink :to="`/products/${p.id}`">{{ title }}</RouterLink></h3>
      <p>{{ p.desc || p.subtitle || p.description }}</p>
      <b>¥ {{ p.price }}</b>
      <span class="product-stock" :class="{ soldout: outOfStock }">
        库存：{{ stockCount }}<template v-if="outOfStock">（已售罄）</template>
      </span>
      <div class="product-actions">
        <button
          class="cart-button"
          type="button"
          :disabled="outOfStock"
          :aria-label="outOfStock ? '商品已售罄' : cartQty ? `已加入 ${cartQty} 件，点击继续加入` : '加入购物车'"
          :title="outOfStock ? '商品已售罄' : cartQty ? `已加入 ${cartQty} 件` : '加入购物车'"
          @click="addToCart"
        >+<i v-if="cartQty" class="cart-badge">{{ cartQty }}</i></button>
        <button
          class="small"
          type="button"
          :disabled="outOfStock"
          :title="outOfStock ? '商品已售罄' : '加入购物车'"
          @click="addToCart"
        >{{ outOfStock ? '已售罄' : '加入购物车' }}</button>
      </div>
      <small v-if="cartNotice" class="cart-notice">{{ cartNotice }}</small>
    </div>
  </article>
</template>

<style scoped>
.product-actions { display: flex; align-items: center; gap: 10px; margin-top: 10px; }
.cart-button {
  position: relative;
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 1px solid var(--red);
  border-radius: 50%;
  background: var(--red);
  color: #fff;
  font-size: 18px;
  font-weight: 400;
  line-height: 1;
}
.cart-badge {
  position: absolute;
  top: -8px;
  right: -9px;
  min-width: 17px;
  height: 17px;
  box-sizing: border-box;
  padding: 0 4px;
  display: grid;
  place-items: center;
  border: 2px solid #fff;
  border-radius: 999px;
  background: var(--red);
  color: #fff;
  font-size: 11px;
  font-style: normal;
  font-weight: 700;
  line-height: 1;
}
.cart-button:hover {
  background: var(--red2);
  border-color: var(--red2);
}
.cart-button:disabled {
  cursor: not-allowed;
  border-color: #c8c0b5;
  background: #c8c0b5;
  opacity: .75;
}
.product-actions .small:disabled {
  cursor: not-allowed;
  border-color: #c8c0b5;
  color: #9b9389;
  background: #f2ede6;
}
.product-stock { display: block; margin-top: 7px; color: var(--muted); font-size: 13px; }
.product-stock.soldout { color: var(--red); }
.cart-notice { display: block; margin-top: 7px; color: var(--jade); }
</style>
