<script setup lang="ts">
import { ref } from 'vue';
import { readUserData, writeUserData } from '../userData';

const props = defineProps<{ p: any }>();
const savedIds = () => readUserData<string[]>('pattern_favorites', []);
const favorite = ref(savedIds().includes(String(props.p.id)));
function toggleFavorite() {
  favorite.value = !favorite.value;
  const ids = new Set(savedIds());
  favorite.value ? ids.add(String(props.p.id)) : ids.delete(String(props.p.id));
  writeUserData('pattern_favorites', [...ids]);
}
</script>

<template>
  <article class="pattern-card">
    <div class="image-wrap">
      <img
        :src="p.image || p.thumbnailUrl || p.imageUrl"
        :alt="p.title"
      /><button
        :aria-label="favorite ? '取消收藏' : '收藏'"
        :aria-pressed="favorite"
        :title="favorite ? '取消收藏' : '收藏'"
        @click="toggleFavorite"
      >{{ favorite ? '♥' : '♡' }}</button>
    </div>
    <div>
      <h3>{{ p.title }}</h3>
      <span>{{ p.meta || p.style || p.description }}</span>
    </div>
  </article>
</template>
