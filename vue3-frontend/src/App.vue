<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { authState, isRealClient } from './auth';
import { readUserData, userDataEvent } from './userData';
import { generationActive, generationSnapshot, restoreActiveGeneration } from './generation';
const route = useRoute();
const router = useRouter();
const nav = [
  ['/', '首页'],
  ['/courses', '非遗课堂'],
  ['/generate', 'AI纹样生成'],
  ['/patterns', '我的纹样'],
  ['/products', '文创商品']
];
const inkBackgroundPaths = new Set(nav.map(([path]) => path));
const usesInkBackground = computed(() => inkBackgroundPaths.has(route.path));
const cartTarget = computed(() =>
  isRealClient.value ? '/cart' : { path: '/login', query: { redirect: '/cart' } }
);
const profileTarget = computed(() =>
  isRealClient.value ? '/profile' : { path: '/login', query: { redirect: '/profile' } }
);
const profileVersion = ref(0);
const headerProfile = computed(() => {
  profileVersion.value;
  return readUserData<any>('profile', {});
});
const headerAvatar = computed(() => String(headerProfile.value?.avatar || ''));
const avatarText = computed(
  () =>
    String(
      headerProfile.value?.nickname || authState.user?.nickname || authState.user?.username || '绣'
    )
      .trim()
      .slice(0, 1) || '绣'
);
function syncHeaderProfile(event: Event) {
  if ((event as CustomEvent).detail?.name === 'profile') profileVersion.value += 1;
}
onMounted(() => {
  window.addEventListener(userDataEvent, syncHeaderProfile);
  // 恢复未完成的 AI 生成任务轮询(刷新页面/重新打开后继续跟踪)
  restoreActiveGeneration();
});
onBeforeUnmount(() => window.removeEventListener(userDataEvent, syncHeaderProfile));
</script>
<template>
  <RouterView v-if="route.meta.layout === 'auth' || route.meta.layout === 'merchant'" />
  <div
    v-else
    class="shell"
    :class="{ 'ink-background': usesInkBackground }"
  >
    <header>
      <RouterLink
        to="/"
        class="brand"
        ><span class="brand-mark">绣</span
        ><span><b>绣纹智创</b><small>绣纹智创·AI非遗活态传承计划</small></span></RouterLink
      >
      <nav>
        <RouterLink
          v-for="[p, n] in nav"
          :key="p"
          :to="p"
          >{{ n }}</RouterLink
        >
      </nav>
      <div class="head-actions">
        <RouterLink to="/resources">创作资源</RouterLink
        ><RouterLink :to="cartTarget">🛒 购物车</RouterLink
        ><RouterLink
          v-if="!authState.token"
          class="login-pill"
          to="/login"
          >登录 / 注册</RouterLink
        ><RouterLink
          v-else
          class="header-avatar"
          :to="profileTarget"
          aria-label="个人中心"
          title="个人中心"
          ><img
            v-if="headerAvatar"
            :src="headerAvatar"
            alt="用户头像"
          /><span v-else>{{ avatarText }}</span></RouterLink
        >
      </div>
    </header>
    <main><RouterView /></main>
    <button
      v-if="generationActive && route.path !== '/generate'"
      class="ai-gen-fab"
      type="button"
      title="返回 AI 纹样生成页查看进度"
      @click="router.push('/generate')"
    >
      <span
        class="ai-gen-fab-spinner"
        aria-hidden="true"
      ></span>
      <span>✦ AI 正在作画… {{ generationSnapshot?.progress ?? 0 }}%</span>
    </button>
  </div>
</template>
