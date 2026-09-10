<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { readUserData, writeUserData } from '../userData';
import { api } from '../api';

const props = defineProps<{ p: any }>();
const router = useRouter();
const savedIds = () => readUserData<string[]>('product_favorites', []);
const favorite = ref(savedIds().includes(String(props.p.id)));
const title = computed(() => props.p.title || props.p.name);
const cartNotice = ref('');

function toggleFavorite() {
  favorite.value = !favorite.value;
  const ids = new Set(savedIds());
  favorite.value ? ids.add(String(props.p.id)) : ids.delete(String(props.p.id));
  writeUserData('product_favorites', [...ids]);
}
function customize() {
  router.push({ path: '/customize', query: { productId: props.p.id } });
}
async function addToCart() {
  const cart = readUserData<any[]>('cart_items', []);
  const existing = cart.find((item) => String(item.productId || item.id) === String(props.p.id));
  const next = existing
    ? cart.map((item) => String(item.productId || item.id) === String(props.p.id)
        ? { ...item, quantity: Number(item.quantity || 1) + 1 }
        : item)
    : [...cart, { ...props.p, productId: props.p.id, quantity: 1 }];
  writeUserData('cart_items', next);
  cartNotice.value = '已加入购物车';
  window.setTimeout(() => (cartNotice.value = ''), 1500);
  try {
    await api.cart.add({ productId: props.p.id, quantity: 1 });
  } catch {
    /* 后端未启动时，本地购物车仍可正常使用。 */
  }
}
</script>

<template>
  <article class="product-card">
    <div class="image-wrap">
      <img
        :src="p.image || p.coverImage || p.mockupImage"
        :alt="p.title || p.name"
      />
    </div>
    <div>
      <h3><RouterLink :to="`/products/${p.id}`">{{ title }}</RouterLink></h3>
      <p>{{ p.desc || p.subtitle || p.description }}</p>
      <b>¥ {{ p.price }}</b>
      <div class="product-actions">
        <button class="small cart-button" @click="addToCart">🛒 加入购物车</button>
        <button class="small" @click="customize">立即定制</button>
      </div>
      <small v-if="cartNotice" class="cart-notice">{{ cartNotice }}</small>
    </div>
  </article>
</template>

<style scoped>
.product-actions { display: flex; gap: 8px; margin-top: 10px; }
.cart-button { color: var(--red); border-color: var(--red); }
.cart-notice { display: block; margin-top: 7px; color: var(--jade); }
</style>
