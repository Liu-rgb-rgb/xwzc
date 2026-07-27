<script setup lang="ts">
defineProps<{ p: any }>();
</script>

<template>
  <article class="pattern-card">
    <div class="image-wrap">
      <img
        :src="p.image || p.thumbnailUrl || p.imageUrl"
        :alt="p.title"
      /><button aria-label="收藏">♡</button>
    </div>
    <div>
      <h3>{{ p.title }}</h3>
      <span>{{ p.meta || p.style || p.description }}</span>
    </div>
  </article>
</template>
