<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '../../api';
import { courses, products } from '../../data';

const route = useRoute();
const router = useRouter();
const kind = computed(() => String(route.meta.kind || 'product'));
const id = computed(() =>
  String(route.params.productId || route.params.courseId || route.params.orderId || 1)
);
const remoteItem = ref<any>(null);
const loading = ref(false);
const studyLoading = ref(false);
const studying = ref(false);
const studyMessage = ref('');

const fallbackItem = computed(() =>
  kind.value === 'course'
    ? courses.find((x) => String(x.id) === id.value) || courses[0]
    : products.find((x) => String(x.id) === id.value) || products[0]
);
const item = computed(() => {
  const fallback = (fallbackItem.value || {}) as any;
  const remote = remoteItem.value;
  if (!remote) return fallback;
  return {
    ...fallback,
    ...remote,
    image: remote.image || remote.coverImage || remote.imageUrl || fallback.image
  };
});

function useDetailFallback(event: Event) {
  const image = event.currentTarget as HTMLImageElement;
  const fallback = String(
    fallbackItem.value?.image || '/demo/pattern/peony-phoenix-pattern-01.jpg'
  );
  if (!image.src.endsWith(fallback)) image.src = fallback;
}

async function loadDetail() {
  loading.value = true;
  studying.value = false;
  studyMessage.value = '';
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
async function primaryAction() {
  if (kind.value === 'product') {
    router.push({ path: '/customize', query: { productId: id.value } });
    return;
  }
  if (studyLoading.value) return;
  studyLoading.value = true;
  studyMessage.value = '';
  try {
    const result: any = await api.courses.study(id.value);
    studying.value = true;
    if (result?.studyCount != null && remoteItem.value) {
      remoteItem.value = { ...remoteItem.value, studyCount: result.studyCount };
    }
    await new Promise((resolve) => window.setTimeout(resolve));
    document.querySelector('.course-learning')?.scrollIntoView({ behavior: 'smooth' });
  } catch (reason: any) {
    studyMessage.value = reason?.response?.data?.message || '课程服务暂时不可用，请稍后重试';
  } finally {
    studyLoading.value = false;
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
        @error="useDetailFallback"
      />
      <div class="detail-copy">
        <span class="eyebrow">{{
          kind === 'course' ? 'HERITAGE COURSE' : 'CULTURAL CREATION'
        }}</span>
        <h1>{{ item.title || item.name }}</h1>
        <p>{{ item.desc || item.description || item.summary }}</p>
        <strong v-if="kind === 'product'">¥ {{ item.price }}</strong>
        <p v-else>{{ item.lessons || item.lessonCount || 0 }} · 精品课程</p>
        <button
          class="primary"
          :disabled="studyLoading"
          @click="primaryAction"
        >
          {{
            kind === 'course'
              ? studyLoading
                ? '正在进入…'
                : studying
                  ? '继续学习'
                  : '立即学习'
              : '立即定制'
          }}
        </button>
        <p
          v-if="studyMessage"
          class="course-error"
        >
          {{ studyMessage }}
        </p>
      </div>
      <section
        v-if="kind === 'course' && studying"
        class="course-learning panel"
      >
        <span class="eyebrow">COURSE CONTENT</span>
        <h2>{{ item.title }}</h2>
        <video
          v-if="item.videoUrl"
          class="course-video"
          :src="item.videoUrl"
          controls
          preload="metadata"
        ></video>
        <div
          v-if="item.content"
          class="course-content"
          v-html="item.content"
        ></div>
        <p v-else>{{ item.description || '课程内容暂未发布。' }}</p>
      </section>
    </template>
  </section>
</template>

<style scoped>
.course-learning {
  grid-column: 1 / -1;
  width: 100%;
  scroll-margin-top: 110px;
}
.course-learning h2 {
  margin: 12px 0 22px;
  font-size: 30px;
}
.course-video {
  display: block;
  width: min(100%, 960px);
  max-height: 540px;
  margin-bottom: 24px;
  border-radius: 14px;
  background: #1f1712;
}
.course-content {
  color: var(--ink);
  line-height: 1.9;
}
.course-error {
  color: var(--red) !important;
}
</style>
