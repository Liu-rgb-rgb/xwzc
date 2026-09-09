<script setup lang="ts">
defineProps<{ eyebrow: string; title: string; action?: string; to?: string }>();
</script>

<template>
  <div class="section-title">
    <div>
      <span>{{ eyebrow }}</span>
      <h2>{{ title }}</h2>
    </div>
    <RouterLink v-if="action && to" :to="to">{{ action }} →</RouterLink>
    <span v-else-if="action">{{ action }} →</span>
  </div>
</template>
