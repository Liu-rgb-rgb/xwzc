<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { api } from '../api';
import { courses, products } from '../data';

const route = useRoute();
const kind = computed(() => String(route.meta.kind || 'product'));
const id = computed(() =>
  String(route.params.productId || route.params.courseId || route.params.orderId || 1)
);
const remoteItem = ref<any>(null);
const loading = ref(false);

const fallbackItem = computed(() =>
  kind.value === 'course'
    ? courses.find((x) => String(x.id) === id.value) || courses[0]
    : products.find((x) => String(x.id) === id.value) || products[0]
);
const item = computed(() => remoteItem.value || fallbackItem.value);

async function loadDetail() {
  loading.value = true;
  try {
    if (kind.value === 'course') remoteItem.value = await api.courses.detail(id.value);
    else if (kind.value === 'order') remoteItem.value = await api.orders.detail(id.value);
    else remoteItem.value = await api.products.detail(id.value);
  } catch {
    remoteItem.value = null;
  } finally {
    loading.value = false;
  }
}
watch([kind, id], loadDetail, { immediate: true });
</script>
<template>
  <section class="content detail-page">
    <div
      v-if="kind === 'order'"
      class="panel"
    >
      <span class="eyebrow">ORDER DETAIL</span>
      <h1>订单详情 #{{ id }}</h1>
      <p v-if="loading">正在加载订单...</p>
      <div class="timeline">
        <b>{{ item?.statusName || item?.status || '订单已提交' }}</b>
        <span>{{ item?.orderNo || '等待商家确认与制作' }}</span>
        <span>{{ item?.logisticsInfo || '物流信息将在发货后更新' }}</span>
      </div>
      <button class="primary">查看物流</button>
    </div>
    <template v-else>
      <img
        class="detail-cover"
        :src="String(item.image || item.coverImage || item.imageUrl)"
        :alt="String(item.title || item.name)"
      />
      <div class="detail-copy">
        <span class="eyebrow">{{
          kind === 'course' ? 'HERITAGE COURSE' : 'CULTURAL CREATION'
        }}</span>
        <h1>{{ item.title || item.name }}</h1>
        <p>{{ item.desc || item.description || item.summary }}</p>
        <strong v-if="kind === 'product'">¥ {{ item.price }}</strong>
        <p v-else>{{ item.lessons || item.lessonCount || 0 }} · 精品课程</p>
        <button class="primary">{{ kind === 'course' ? '立即学习' : '立即定制' }}</button>
      </div>
    </template>
  </section>
</template>
