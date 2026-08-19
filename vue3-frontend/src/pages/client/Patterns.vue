<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import PatternCard from '../../components/PatternCard.vue';
import { api, listFrom } from '../../api';
import { patterns } from '../../data';
import { readUserData, userDataEvent } from '../../userData';

const patternItems = ref<any[]>(patterns);
const favoriteCount = ref(0);
const recentCount = ref(0);
const appliedCount = ref(0);
const serverTotal = ref(0);
const serverFavoriteCount = ref(0);
const serverRecentCount = ref(0);
const serverAppliedCount = ref(0);
const totalCount = computed(() => Math.max(serverTotal.value, patternItems.value.length));

function refreshMetrics() {
  favoriteCount.value = Math.max(serverFavoriteCount.value, readUserData<string[]>('pattern_favorites', []).length);
  recentCount.value = Math.max(serverRecentCount.value, readUserData<any[]>('recent_generations', []).length);
  appliedCount.value = Math.max(serverAppliedCount.value, readUserData<any[]>('applied_designs', []).length);
  const saved = readUserData<any[]>('saved_patterns', []);
  if (saved.length) {
    const merged = [...saved, ...patternItems.value];
    patternItems.value = merged.filter((item, index) =>
      merged.findIndex((other) => String(other.id) === String(item.id)) === index
    );
  }
}
onMounted(async () => {
  const [mineResult, generationResult, designResult] = await Promise.allSettled([
    api.patterns.mine({ page: 1, pageSize: 100 }),
    api.patterns.generations({ page: 1, pageSize: 100 }),
    api.customDesigns.mine({ page: 1, pageSize: 100 })
  ]);
  if (mineResult.status === 'fulfilled') {
    const response: any = mineResult.value;
    const result = listFrom(response);
    if (result.length) patternItems.value = result;
    serverTotal.value = Number(response?.total || response?.totalElements || result.length);
    serverFavoriteCount.value = result.filter((item: any) => item.favorite || item.favorited || item.isFavorite).length;
  }
  if (generationResult.status === 'fulfilled') serverRecentCount.value = listFrom(generationResult.value).length;
  if (designResult.status === 'fulfilled') serverAppliedCount.value = listFrom(designResult.value).length;
  refreshMetrics();
  window.addEventListener(userDataEvent, refreshMetrics);
});
onBeforeUnmount(() => window.removeEventListener(userDataEvent, refreshMetrics));
</script>
<template>
  <div class="page-head compact">
    <span>MY CREATIVE ASSETS</span>
    <h1>我的纹样</h1>
    <p>管理生成、收藏与已应用的纹样资产。</p>
    <div class="metrics">
      <div><b>{{ totalCount }}</b><span>纹样总数</span></div>
      <div><b>{{ favoriteCount }}</b><span>收藏数量</span></div>
      <div><b>{{ recentCount }}</b><span>最近生成</span></div>
      <div><b>{{ appliedCount }}</b><span>已应用商品</span></div>
    </div>
  </div>
  <div class="content">
    <div class="tabbar">
      <b>全部纹样</b><span>我的收藏</span><span>最近生成</span><span>已应用</span
      ><button class="primary">＋ 新建生成</button>
    </div>
    <div class="toolbar">
      <input placeholder="⌕ 搜索纹样名称、关键词" /><select>
        <option>全部风格</option>
        <option>广绣经典</option>
        <option>新中式</option></select
      ><select>
        <option>全部元素</option>
        <option>牡丹</option>
        <option>凤凰</option>
      </select>
    </div>
    <div class="pattern-grid library">
      <PatternCard
        v-for="p in patternItems"
        :key="p.id"
        :p="p"
      />
    </div>
  </div>
</template>
