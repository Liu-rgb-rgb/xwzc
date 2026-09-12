<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { authState, isRealClient } from './auth';
import { readUserData, userDataEvent } from './userData';
const route = useRoute();
const nav = [
  ['/', '首页'],
  ['/generate', 'AI纹样生成'],
  ['/products', '文创商品'],
  ['/patterns', '我的纹样'],
  ['/courses', '非遗课堂']
];
const cartTarget = computed(() => isRealClient.value ? '/cart' : { path: '/login', query: { redirect: '/cart' } });
const profileTarget = computed(() => isRealClient.value ? '/profile' : { path: '/login', query: { redirect: '/profile' } });
const profileVersion = ref(0);
const headerProfile = computed(() => {
  profileVersion.value;
  return readUserData<any>('profile', {});
});
const headerAvatar = computed(() => String(headerProfile.value?.avatar || ''));
const avatarText = computed(() => String(
  headerProfile.value?.nickname || authState.user?.nickname || authState.user?.username || '绣'
).trim().slice(0, 1) || '绣');
function syncHeaderProfile(event: Event) {
  if ((event as CustomEvent).detail?.name === 'profile') profileVersion.value += 1;
}
onMounted(() => window.addEventListener(userDataEvent, syncHeaderProfile));
onBeforeUnmount(() => window.removeEventListener(userDataEvent, syncHeaderProfile));
</script>
<template>
  <RouterView v-if="route.meta.layout === 'auth' || route.meta.layout === 'merchant'" />
  <div
    v-else
    class="shell"
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
          ><img v-if="headerAvatar" :src="headerAvatar" alt="用户头像" /><span v-else>{{ avatarText }}</span></RouterLink
        >
      </div>
    </header>
    <main><RouterView /></main>
    <footer>
      <div>
        <RouterLink
          to="/"
          class="brand"
          ><span class="brand-mark">绣</span
          ><span><b>绣纹智创</b><small>绣纹智创·AI非遗活态传承计划</small></span></RouterLink
        >
        <p>以 AI 赋能广绣设计，连接传统与商业，创造美，传承美。</p>
      </div>
      <div><b>平台服务</b><RouterLink to="/generate">AI纹样生成</RouterLink><RouterLink to="/products">文创商品</RouterLink><RouterLink to="/customize">纹样定制</RouterLink></div>
      <div><b>学习支持</b><RouterLink to="/courses">非遗课堂</RouterLink><RouterLink to="/resources">创作资源</RouterLink><RouterLink to="/resources">帮助中心</RouterLink></div>
      <div>
        <b>联系我们</b><span>020-8888 6888</span><span>service@xiuwen.com</span
        ><span>广州市越秀区中山五路</span>
      </div>
    </footer>
  </div>
</template>
