<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { api, listFrom, type ApiQuery } from '../../api';

const route = useRoute();
const keyword = ref('');
const rows = ref<any[]>([]);
const loading = ref(false);
const error = ref('');
const moduleName = computed(() => String(route.path.split('/').pop() || 'dashboard'));
const title = computed(() => String(route.meta.title || '管理模块'));

const loaders: Record<string, (params?: ApiQuery) => Promise<any>> = {
  dashboard: () => api.admin.dashboard(),
  orders: (params) => api.admin.orders.list(params),
  products: (params) => api.admin.products.list(params),
  'product-categories': (params) => api.admin.productCategories.list(params),
  'custom-designs': (params) => api.admin.customDesigns.list(params),
  patterns: (params) => api.admin.patterns.list(params),
  'prompt-templates': (params) => api.admin.promptTemplates.list(params),
  courses: (params) => api.admin.courses.list(params),
  resources: (params) => api.admin.resources.list(params),
  users: (params) => api.admin.users.list(params),
  home: async (params) => {
    const [banners, recommends] = await Promise.all([
      api.admin.homeBanners.list(params),
      api.admin.homeRecommends.list(params)
    ]);
    return [...listFrom(banners), ...listFrom(recommends)];
  },
  shop: () => api.admin.shop.detail()
};

const visibleRows = computed(() =>
  rows.value.filter((row) =>
    String(row.name || row.title || row.orderNo || row.id || '').includes(keyword.value)
  )
);

async function loadRows() {
  const loader = loaders[moduleName.value];
  if (!loader) return;
  loading.value = true;
  error.value = '';
  try {
    const result = await loader({ page: 1, pageSize: 20, keyword: keyword.value });
    const list = listFrom(result);
    rows.value = list.length ? list : result && typeof result === 'object' ? [result] : [];
  } catch (reason: any) {
    rows.value = [];
    error.value = reason?.response?.data?.message || '接口暂时不可用，请检查后端服务。';
  } finally {
    loading.value = false;
  }
}

watch(() => route.path, loadRows, { immediate: true });
</script>
<template>
  <section class="admin-page">
    <div class="admin-title">
      <div>
        <span>MANAGEMENT</span>
        <h1>{{ title }}</h1>
      </div>
      <button>＋ 新增</button>
    </div>
    <div class="admin-toolbar">
      <input
        v-model="keyword"
        placeholder="搜索名称或关键词"
        @keyup.enter="loadRows"
      />
      <button @click="loadRows">搜索</button>
    </div>
    <p
      v-if="error"
      class="form-error"
    >
      {{ error }}
    </p>
    <div class="metric-row">
      <article>
        <span>全部数据</span><b>{{ rows.length }}</b>
      </article>
      <article>
        <span>当前模块</span><b>{{ title }}</b>
      </article>
      <article>
        <span>接口状态</span><b>{{ loading ? '加载中' : error ? '异常' : '正常' }}</b>
      </article>
    </div>
    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>名称</th>
          <th>状态</th>
          <th>更新时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="loading">
          <td colspan="5">正在加载...</td>
        </tr>
        <tr v-else-if="!visibleRows.length">
          <td colspan="5">暂无数据</td>
        </tr>
        <tr
          v-for="row in visibleRows"
          :key="row.id || row.orderId || row.productId"
        >
          <td>{{ row.id || row.orderId || row.productId || '-' }}</td>
          <td>{{ row.name || row.title || row.orderNo || row.nickname || title }}</td>
          <td>
            <i>{{ row.statusName || row.status || '正常' }}</i>
          </td>
          <td>{{ row.updateTime || row.updatedAt || row.createTime || '-' }}</td>
          <td><button>编辑</button><button>查看</button></td>
        </tr>
      </tbody>
    </table>
  </section>
</template>
