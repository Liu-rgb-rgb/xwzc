<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { readUserData, writeUserData } from '../userData';

const props = defineProps<{ p: any }>();
const router = useRouter();
const savedIds = () => readUserData<string[]>('product_favorites', []);
const favorite = ref(savedIds().includes(String(props.p.id)));
const title = computed(() => props.p.title || props.p.name);

function toggleFavorite() {
  favorite.value = !favorite.value;
  const ids = new Set(savedIds());
  favorite.value ? ids.add(String(props.p.id)) : ids.delete(String(props.p.id));
  writeUserData('product_favorites', [...ids]);
}
function customize() {
  router.push({ path: '/customize', query: { productId: props.p.id } });
}
</script>

<template>
  <article class="product-card">
    <div class="image-wrap">
      <img
        :src="p.image || p.coverImage || p.mockupImage"
        :alt="p.title || p.name"
      /><button
        :aria-label="favorite ? '取消收藏' : '收藏'"
        :aria-pressed="favorite"
        :title="favorite ? '取消收藏' : '收藏'"
        @click="toggleFavorite"
      >{{ favorite ? '♥' : '♡' }}</button>
    </div>
    <div>
      <h3><RouterLink :to="`/products/${p.id}`">{{ title }}</RouterLink></h3>
      <p>{{ p.desc || p.subtitle || p.description }}</p>
      <b>¥ {{ p.price }}</b
      ><button class="small" @click="customize">立即定制</button>
    </div>
  </article>
</template>
