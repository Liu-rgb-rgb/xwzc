<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '../../api';
import { setLogin } from '../../auth';
const route = useRoute();
const router = useRouter();
const merchantPortal = computed(() => route.meta.portal === 'merchant');
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
    const user = result.userInfo || { username: username.value, role: 'USER' };
    const merchant = ['ADMIN', 'MERCHANT_ADMIN'].includes(user.role || '');
    if (merchantPortal.value && !merchant) {
      error.value = '该账号不是商家账号，请从客户端登录。';
      return;
    }
    if (!merchantPortal.value && merchant) {
      error.value = '该账号属于商家端，请选择商家端登录。';
      return;
    }
    if (result?.token) setLogin(result.token, user);
    const fallback = merchant ? '/merchant' : '/';
    router.push(String(router.currentRoute.value.query.redirect || fallback));
  } catch (reason: any) {
    error.value = reason?.response?.data?.message || '登录失败，请检查账号、密码和后端服务。';
  } finally {
    loading.value = false;
  }
}

function choosePortal(portal: 'client' | 'merchant') {
  error.value = '';
  router.replace(portal === 'merchant' ? '/merchant/login' : '/login');
}

async function enterClientDemo() {
  loading.value = true;
  error.value = '';
  try {
    setLogin('client-demo-token', {
      username: 'client-demo',
      nickname: '客户端体验账号',
      role: 'USER'
    });
    router.push('/');
  } catch (reason: any) {
    error.value = reason?.response?.data?.message || reason?.message || '体验账号登录失败，请确认后端服务已启动。';
  } finally {
    loading.value = false;
  }
}

function openRegister() {
  router.push({
    path: '/register',
    query: merchantPortal.value ? { portal: 'merchant' } : {}
  });
}

function enterMerchantDemo() {
  setLogin('merchant-demo-token', {
    username: 'merchant-demo',
    nickname: '商家体验账号',
    role: 'MERCHANT_ADMIN'
  });
  router.push('/merchant');
}
</script>

<template>
  <div class="login-page">
    <RouterLink class="login-home" to="/">← 返回首页</RouterLink>
    <section class="login-shell">
      <aside class="login-visual">
        <span class="login-vertical">一针一线 · 绣见岭南</span>
        <div class="login-calligraphy" aria-hidden="true">
          <span class="char">刺</span>
          <span class="char char-b">绣</span>
          <span class="calligraphy-seal">绣</span>
        </div>
        <div class="login-quote">
          <p>以针作笔，以线为墨<br />让千年广绣在数字时代延续温度与光彩</p>
        </div>
      </aside>
      <div class="login-panel">
        <form class="login-card" @submit.prevent="login">
          <span class="login-site-title">{{ merchantPortal ? '非遗刺绣网站 · 商家端' : '非遗刺绣网站' }}</span>
          <span class="login-site-sub">传承千年文化，守望匠心技艺</span>
          <h2>{{ merchantPortal ? '商家端登录' : '欢迎回来' }}</h2>
          <div class="portal-switch" aria-label="选择登录身份">
            <button type="button" :class="{ on: !merchantPortal }" @click="choosePortal('client')">客户端</button>
            <button type="button" :class="{ on: merchantPortal }" @click="choosePortal('merchant')">商家端</button>
          </div>
          <label class="login-field">
            <svg class="field-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" />
            </svg>
            <input v-model="username" autocomplete="username" placeholder="请输入账号" />
          </label>
          <label class="login-field">
            <svg class="field-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <rect x="3" y="11" width="18" height="11" rx="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" />
            </svg>
            <input v-model="password" autocomplete="current-password" type="password" placeholder="请输入密码" />
          </label>
          <small v-if="error" class="form-error">{{ error }}</small>
          <button class="primary login-submit" type="submit" :disabled="loading">
            {{ loading ? '登录中…' : '登 录' }}
          </button>
          <a class="forgot-link">忘记密码？</a>
          <p class="register-entry">还没有账号？<button type="button" @click="openRegister">立即注册</button></p>
          <div class="line">或</div>
          <button v-if="!merchantPortal" class="demo-entry" type="button" @click="enterClientDemo">客户端体验账号</button>
          <button v-else class="demo-entry" type="button" @click="enterMerchantDemo">商家端体验账号</button>
        </form>
      </div>
    </section>
  </div>
</template>
