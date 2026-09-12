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

function enterClientDemo() {
  setLogin('client-demo-token', {
    username: 'client-demo',
    nickname: '客户端体验账号',
    role: 'USER'
  });
  router.push('/');
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
        <div class="login-embroidery">
          <img src="/demo/pattern/peony-phoenix-pattern-01.jpg" alt="凤凰牡丹广绣纹样" />
        </div>
        <div class="login-visual-copy">
          <span>GUANGXIU · AI · INHERITANCE</span>
          <h1>锦绣入画<br />非遗新生</h1>
          <p>以针作笔，以线为墨。让千年广绣在数字时代延续温度与光彩。</p>
        </div>
        <span class="login-seal">绣</span>
      </aside>
      <div class="login-panel">
        <form class="login-card" @submit.prevent="login">
          <RouterLink to="/" class="login-brand">
            <span class="brand-mark">绣</span>
            <span><b>绣纹智创</b><small>AI 非遗活态传承计划</small></span>
          </RouterLink>
          <span class="login-eyebrow">WELCOME BACK</span>
          <h2>{{ merchantPortal ? '商家端登录' : '欢迎回来' }}</h2>
          <p>{{ merchantPortal ? '管理商品、订单、纹样与店铺内容' : '登录后开启您的广绣创意之旅' }}</p>
          <div class="portal-switch" aria-label="选择登录身份">
            <button type="button" :class="{ on: !merchantPortal }" @click="choosePortal('client')">客户端</button>
            <button type="button" :class="{ on: merchantPortal }" @click="choosePortal('merchant')">商家端</button>
          </div>
          <label class="login-field">
            <span>账号</span>
            <input v-model="username" autocomplete="username" placeholder="请输入账号" />
          </label>
          <label class="login-field">
            <span>密码</span>
            <input v-model="password" autocomplete="current-password" type="password" placeholder="请输入密码" />
          </label>
          <a class="forgot-link">忘记密码？</a>
          <small v-if="error" class="form-error">{{ error }}</small>
          <button class="primary login-submit" type="submit" :disabled="loading">
            {{ loading ? '登录中…' : '登录' }}
          </button>
          <p class="register-entry">还没有账号？<button type="button" @click="openRegister">立即注册</button></p>
          <div class="line">或</div>
          <button v-if="!merchantPortal" class="demo-entry" type="button" @click="enterClientDemo">客户端体验账号</button>
          <button v-else class="demo-entry" type="button" @click="enterMerchantDemo">商家端体验账号</button>
        </form>
      </div>
    </section>
  </div>
</template>
