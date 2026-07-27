<script setup lang="ts">
defineProps<{ p: any }>();
</script>

<template>
  <article class="product-card">
    <div class="image-wrap">
      <img
        :src="p.image || p.coverImage || p.mockupImage"
        :alt="p.title || p.name"
      /><button aria-label="收藏">♡</button>
    </div>
    <div>
      <h3>{{ p.title || p.name }}</h3>
      <p>{{ p.desc || p.subtitle || p.description }}</p>
      <b>¥ {{ p.price }}</b
      ><button class="small">立即定制</button>
    </div>
  </article>
</template>
