<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { patterns } from '../../data';
import { api } from '../../api';
import { logout, setLogin } from '../../auth';
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
  } catch {
    error.value = '后端服务暂未连接，可点击体验版入口浏览完整页面。';
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

function enterAsGuest() {
  logout();
  router.push('/');
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
        <div class="portal-switch" aria-label="选择登录身份">
          <button type="button" :class="{ on: !merchantPortal }" @click="choosePortal('client')">客户端</button>
          <button type="button" :class="{ on: merchantPortal }" @click="choosePortal('merchant')">商家端</button>
        </div>
        <h2>{{ merchantPortal ? '商家端登录' : '客户端登录' }}</h2>
        <p>{{ merchantPortal ? '管理商品、订单、纹样与店铺内容' : '登录后开启您的广绣创意之旅' }}</p>
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
          v-if="!merchantPortal"
          type="button"
          @click="router.push('/register')"
        >
          注册账号</button
        ><button
          v-if="!merchantPortal"
          type="button"
          @click="enterClientDemo"
        >
          客户端体验账号</button
        ><button
          v-if="merchantPortal"
          type="button"
          @click="enterMerchantDemo"
        >
          商家体验版入口
        </button>
        <button class="guest-entry" type="button" @click="enterAsGuest">以游客身份浏览</button>
      </form>
    </div>
  </div>
</template>
