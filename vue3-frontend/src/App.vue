<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router';
import { authState, logout } from './auth';
const route = useRoute();
const router = useRouter();
const nav = [
  ['/', '首页'],
  ['/generate', 'AI纹样生成'],
  ['/products', '文创商品'],
  ['/patterns', '我的纹样'],
  ['/courses', '非遗课堂']
];
function signOut() {
  logout();
  router.push('/');
}
</script>
<template>
  <RouterView v-if="route.meta.layout === 'auth' || route.meta.layout === 'admin'" />
  <div
    v-else
    class="shell"
  >
    <header>
      <RouterLink
        to="/"
        class="brand"
        ><span class="brand-mark">绣</span
        ><span><b>绣纹智创</b><small>广绣 AI 纹样设计与商家服务平台</small></span></RouterLink
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
        ><RouterLink to="/cart">🛒 购物车</RouterLink
        ><RouterLink
          v-if="!authState.token"
          class="login-pill"
          to="/login"
          >♙ 登录 / 注册</RouterLink
        ><template v-else
          ><RouterLink
            class="login-pill"
            to="/profile"
            >{{ authState.user?.nickname || '个人中心' }}</RouterLink
          ><button @click="signOut">退出</button></template
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
          ><span><b>绣纹智创</b><small>广绣 AI 纹样设计与商家服务平台</small></span></RouterLink
        >
        <p>以 AI 赋能广绣设计，连接传统与商业，创造美，传承美。</p>
      </div>
      <div><b>平台服务</b><span>AI纹样生成</span><span>文创商品</span><span>纹样定制</span></div>
      <div><b>学习支持</b><span>非遗课堂</span><span>创作资源</span><span>帮助中心</span></div>
      <div>
        <b>联系我们</b><span>020-8888 6888</span><span>service@xiuwen.com</span
        ><span>广州市越秀区中山五路</span>
      </div>
    </footer>
  </div>
</template>
