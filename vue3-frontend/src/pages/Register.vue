<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../api';

const router = useRouter();
const form = reactive({ username: '', password: '', confirmPassword: '', nickname: '', phone: '' });
const error = ref('');
async function submit() {
  if (form.password !== form.confirmPassword) return (error.value = '两次输入的密码不一致');
  try {
    await api.auth.register(form);
    router.push('/login');
  } catch {
    error.value = '后端未启动或注册失败，请稍后重试';
  }
}
</script>
<template>
  <div class="auth-screen">
    <form
      class="auth-card"
      @submit.prevent="submit"
    >
      <span class="brand-mark">绣</span>
      <h1>注册绣纹智创</h1>
      <p>创建账号，开启您的广绣创意之旅</p>
      <input
        v-model="form.username"
        required
        placeholder="登录账号"
      />
      <input
        v-model="form.nickname"
        placeholder="昵称"
      />
      <input
        v-model="form.phone"
        placeholder="手机号"
      />
      <input
        v-model="form.password"
        required
        type="password"
        placeholder="密码"
      />
      <input
        v-model="form.confirmPassword"
        required
        type="password"
        placeholder="确认密码"
      />
      <small
        v-if="error"
        class="form-error"
        >{{ error }}</small
      >
      <button
        class="primary"
        type="submit"
      >
        注册账号
      </button>
      <RouterLink to="/login">已有账号？返回登录</RouterLink>
    </form>
  </div>
</template>
