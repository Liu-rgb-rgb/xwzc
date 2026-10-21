<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { authState, isMerchant, isRealClient } from './auth';
import { readUserData, userDataEvent } from './userData';
import { generationActive, generationSnapshot, restoreActiveGeneration } from './generation';
import MusicPlayer from './components/MusicPlayer.vue';
import { pauseMusic } from './music';
const route = useRoute();
const router = useRouter();
const nav = [
  ['/', '首页'],
  ['/courses', '非遗课堂'],
  ['/generate', 'AI纹样生成'],
  ['/stitch-3d', '3D针法'],
  ['/patterns', '我的纹样'],
  ['/products', '智造工坊']
];
const inkBackgroundPaths = new Set(nav.map(([path]) => path));
const paperBackgroundPaths = new Set(['/cart']);
const usesInkBackground = computed(() => inkBackgroundPaths.has(route.path));
const usesPaperBackground = computed(() => paperBackgroundPaths.has(route.path));
const cartTarget = computed(() =>
  isRealClient.value ? '/cart' : { path: '/login', query: { redirect: '/cart' } }
);
const profileTarget = computed(() =>
  isRealClient.value ? '/profile' : { path: '/login', query: { redirect: '/profile' } }
);
const workshopTarget = computed(() =>
  isMerchant.value ? '/merchant/factory-orders' : '/merchant/login'
);
const profileVersion = ref(0);
const cartVersion = ref(0);
const cartQty = computed(() => {
  cartVersion.value;
  return readUserData<any[]>('cart_items', []).reduce(
    (sum, item) => sum + Number(item.quantity || 0),
    0
  );
});
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
  const name = (event as CustomEvent).detail?.name;
  if (name === 'profile') profileVersion.value += 1;
  if (name === 'cart_items') cartVersion.value += 1;
}
onMounted(() => {
  window.addEventListener(userDataEvent, syncHeaderProfile);
  // 恢复未完成的 AI 生成任务轮询(刷新页面/重新打开后继续跟踪)
  restoreActiveGeneration();
});
onBeforeUnmount(() => window.removeEventListener(userDataEvent, syncHeaderProfile));
// 登录页、商家端等非前台外壳页面不再播放背景音乐。
watch(
  () => route.meta.layout,
  (layout) => {
    if (layout) pauseMusic();
  }
);
</script>
<template>
  <RouterView
    v-if="
      route.meta.layout === 'auth' ||
      route.meta.layout === 'merchant' ||
      route.meta.layout === 'factory'
    "
  />
  <div
    v-else
    class="shell"
    :class="{ 'ink-background': usesInkBackground, 'paper-background': usesPaperBackground }"
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
        <!-- 【已隐藏】创作资源入口：需要恢复时解开注释 -->
        <!-- <RouterLink to="/resources">创作资源</RouterLink> -->
        <RouterLink
          :to="workshopTarget"
          title="乡村工坊端 · 接单生产"
          >🧶 乡村工坊端</RouterLink
        ><RouterLink
          :to="cartTarget"
          class="cart-entry"
          title="查看打样需求"
          >🧵 打样需求<i
            v-if="cartQty"
            class="cart-entry-badge"
            >{{ cartQty }}</i
          ></RouterLink
        ><MusicPlayer /><RouterLink
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
