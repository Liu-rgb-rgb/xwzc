<script setup lang="ts">
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { isDemoAccount } from '../auth';
import { readUserData, writeUserData } from '../userData';

const props = defineProps<{ p: any }>();
const router = useRouter();
const route = useRoute();
const savedIds = () => readUserData<string[]>('pattern_favorites', []);
// 游客体验账号始终以未收藏状态展示，避免读取到浏览器中残留的收藏记录。
const favorite = ref(
  isDemoAccount.value ? false : savedIds().includes(String(props.p.id))
);
function toggleFavorite() {
  if (isDemoAccount.value) {
    router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }
  favorite.value = !favorite.value;
  const ids = new Set(savedIds());
  favorite.value ? ids.add(String(props.p.id)) : ids.delete(String(props.p.id));
  writeUserData('pattern_favorites', [...ids]);
}
function openDetail() {
  router.push(`/patterns/${props.p.id}`);
}
</script>

<template>
  <article
    class="pattern-card"
    role="link"
    tabindex="0"
    @click="openDetail"
    @keydown.enter="openDetail"
  >
    <div class="image-wrap">
      <img
        :src="p.image || p.thumbnailUrl || p.imageUrl"
        :alt="p.title"
      /><button
        :aria-label="favorite ? '取消收藏' : '收藏'"
        :aria-pressed="favorite"
        :title="favorite ? '取消收藏' : '收藏'"
        @click.stop="toggleFavorite"
      >{{ favorite ? '♥' : '♡' }}</button>
    </div>
    <div>
      <h3>{{ p.title }}</h3>
      <span>{{ p.meta || p.style || p.description }}</span>
    </div>
  </article>
</template>
