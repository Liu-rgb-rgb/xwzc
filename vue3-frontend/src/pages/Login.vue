<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { patterns } from '../data';
import { api } from '../api';
import { setLogin } from '../auth';
const router = useRouter();
const username = ref('');
const password = ref('');
const loading = ref(false);
const error = ref('');
async function login() {
  loading.value = true;
  error.value = '';
  try {
    const result: any = await api.auth.login({
      username: username.value,
      password: password.value
    });
    if (result?.token)
      setLogin(result.token, result.userInfo || { username: username.value, role: 'USER' });
    router.push(String(router.currentRoute.value.query.redirect || '/'));
  } catch {
    error.value = '后端服务暂未连接，可点击体验版入口浏览完整页面。';
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-visual">
      <RouterLink
        to="/"
        class="brand"
        ><span class="brand-mark">绣</span
        ><span><b>绣纹智创</b><small>广绣 AI 纹样设计与商家服务平台</small></span></RouterLink
      >
      <div>
        <span>广绣 · AI · 文创</span>
        <h1>绣承千年<br />智创未来</h1>
        <p>以人工智能赋能非遗广绣，让传统技艺在现代设计中焕发新生。</p>
      </div>
      <img
        :src="patterns[0].image"
        alt="广绣牡丹纹样"
      />
    </div>
    <div class="login-panel">
      <form
        class="login-card"
        @submit.prevent="login"
      >
        <span>WELCOME BACK</span>
        <h2>欢迎登录</h2>
        <p>登录后开启您的广绣创意之旅</p>
        <input
          v-model="username"
          autocomplete="username"
          placeholder="请输入账号"
        /><input
          v-model="password"
          autocomplete="current-password"
          type="password"
          placeholder="请输入密码"
        /><a>忘记密码？</a
        ><small
          v-if="error"
          class="form-error"
          >{{ error }}</small
        ><button
          class="primary"
          type="submit"
        >
          {{ loading ? '登录中…' : '登录' }}
        </button>
        <div class="line">还没有账号？</div>
        <button
          type="button"
          @click="router.push('/register')"
        >
          注册账号</button
        ><RouterLink to="/">体验版入口 · 无需登录</RouterLink>
      </form>
    </div>
  </div>
</template>
