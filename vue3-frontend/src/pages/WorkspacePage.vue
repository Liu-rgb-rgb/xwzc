<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { api, listFrom } from '../api';
import { patterns, products } from '../data';

const route = useRoute();
const mode = computed(() => String(route.meta.mode || 'resources'));
const title = computed(() => String(route.meta.title || '功能中心'));
const items = ref<any[]>([]);
const profile = ref<any>({ nickname: '', phone: '', email: '', intro: '' });
const customProductId = ref<number>(Number(products[0]?.id || 501));
const customPatternId = ref<number>(Number(patterns[0]?.id || 301));
const customNote = ref('');
const notice = ref('');

onMounted(async () => {
  try {
    if (mode.value === 'cart') items.value = listFrom(await api.cart.items());
    else if (mode.value === 'orders') items.value = listFrom(await api.orders.mine());
    else if (mode.value === 'resources') items.value = listFrom(await api.resources.list());
    else if (mode.value === 'profile') profile.value = await api.user.profile();
  } catch {}
  if (!items.value.length && mode.value !== 'profile')
    items.value = mode.value === 'cart' ? products.slice(0, 3) : patterns.slice(0, 6);
});

async function saveProfile() {
  notice.value = '';
  try {
    await api.user.updateProfile(profile.value);
    notice.value = '资料保存成功';
  } catch {
    notice.value = '资料保存失败，请检查后端服务';
  }
}

async function createPreview() {
  notice.value = '';
  try {
    await api.customDesigns.create({
      productId: customProductId.value,
      patternId: customPatternId.value,
      designConfig: { x: 0.5, y: 0.48, scale: 0.75, rotation: 0 },
      remark: customNote.value
    });
    notice.value = '定制预览已提交';
  } catch {
    notice.value = '提交失败，请检查后端服务';
  }
}
</script>
<template>
  <section class="content workspace-page">
    <div class="page-heading">
      <span class="eyebrow">XIUWEN WORKSPACE</span>
      <h1>{{ title }}</h1>
      <p>统一管理您的广绣创作、学习与订单信息。</p>
    </div>
    <div
      v-if="mode === 'profile'"
      class="profile-layout"
    >
      <aside>
        <div class="avatar">绣</div>
        <b>绣纹爱好者</b><span>个人资料</span><span>收货地址</span><span>消息中心</span
        ><span>账号安全</span>
      </aside>
      <form
        class="panel"
        @submit.prevent="saveProfile"
      >
        <h2>完善个人资料</h2>
        <label>昵称<input v-model="profile.nickname" /></label
        ><label>手机号<input v-model="profile.phone" /></label
        ><label>邮箱<input v-model="profile.email" /></label
        ><label>个人简介<textarea v-model="profile.intro"></textarea></label
        ><button
          class="primary"
          type="submit"
        >
          保存资料
        </button>
        <p v-if="notice">{{ notice }}</p>
      </form>
    </div>
    <div
      v-else-if="mode === 'customize'"
      class="panel customize-panel"
    >
      <h2>文创商品定制</h2>
      <p>选择纹样、商品与数量，预览专属广绣文创作品。</p>
      <div class="option-row">
        <button
          v-for="x in products.slice(0, 4)"
          :key="x.id"
          :class="{ on: customProductId === Number(x.id) }"
          @click="customProductId = Number(x.id)"
        >
          {{ x.title }}
        </button>
      </div>
      <textarea
        v-model="customNote"
        placeholder="填写您的定制要求"
      ></textarea
      ><button
        class="primary"
        @click="createPreview"
      >
        生成定制预览
      </button>
      <p v-if="notice">{{ notice }}</p>
    </div>
    <div
      v-else
      class="workspace-grid"
    >
      <article
        v-for="item in items"
        :key="item.id"
      >
        <img
          :src="
            item.image ||
            item.coverImage ||
            item.imageUrl ||
            item.productCoverImage ||
            item.previewImageUrl
          "
        />
        <div>
          <b>{{
            item.title || item.name || item.productName || item.orderNo || `订单 ${item.id}`
          }}</b>
          <p>{{ item.desc || item.meta || item.status || '广绣文化创意内容' }}</p>
          <button>查看详情</button>
        </div>
      </article>
    </div>
  </section>
</template>
