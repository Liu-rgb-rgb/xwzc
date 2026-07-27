<script setup lang="ts">
defineProps<{ eyebrow: string; title: string; action?: string }>();
</script>

<template>
  <div class="section-title">
    <div>
      <span>{{ eyebrow }}</span>
      <h2>{{ title }}</h2>
    </div>
    <a v-if="action">{{ action }} →</a>
  </div>
</template>
