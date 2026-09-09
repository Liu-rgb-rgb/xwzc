<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import PatternCard from '../../components/PatternCard.vue';
import { api, listFrom } from '../../api';
import { patterns as demoPatterns } from '../../data';
import { readUserData, userDataEvent } from '../../userData';

type Tab = 'all' | 'favorite' | 'recent' | 'applied';
const router = useRouter();
const patternItems = ref<any[]>([]);
const activeTab = ref<Tab>('all');
const query = ref('');
const selectedStyle = ref('');
const selectedElement = ref('');
const favoriteIds = ref<string[]>([]);
const recentItems = ref<any[]>([]);
const appliedItems = ref<any[]>([]);

function normalizePattern(item: any) {
  const image = item.image || item.thumbnailUrl || item.imageUrl || item.resultImageUrl || '';
  const demo = demoPatterns.find((p: any) => image && String(image).includes(String(p.image).split('/').pop()));
  const elements = Array.isArray(item.elements)
    ? item.elements
    : String(item.elements || item.element || demo?.elements?.join(',') || '')
        .split(/[,，/、]/)
        .map((value) => value.trim())
        .filter(Boolean);
  const style = item.style || item.styleName || demo?.style || '其他风格';
  const styleTags = [...new Set([
    ...(Array.isArray(item.styleTags) ? item.styleTags : []),
    style,
    ...((demo as any)?.styleTags || []),
    ...(demo ? ['新中式'] : [])
  ])];
  return {
    ...demo,
    ...item,
    id: item.id ?? item.patternId ?? demo?.id,
    title: item.title || item.name || item.patternName || demo?.title || '未命名纹样',
    image: image || demo?.image || '/demo/pattern/round-flower-pattern-01.jpg',
    style,
    styleTags,
    elements,
    meta: item.meta || `${style}${elements.length ? ` · ${elements.join(' / ')}` : ''}`
  };
}

function refreshLocalData() {
  favoriteIds.value = readUserData<string[]>('pattern_favorites', []);
  recentItems.value = readUserData<any[]>('recent_generations', []).map(normalizePattern);
  appliedItems.value = readUserData<any[]>('applied_designs', []).map(normalizePattern);
}

const tabItems = computed(() => {
  if (activeTab.value === 'favorite') return patternItems.value.filter((p) => favoriteIds.value.includes(String(p.id)));
  if (activeTab.value === 'recent') return recentItems.value;
  if (activeTab.value === 'applied') return appliedItems.value;
  return patternItems.value;
});
const styles = computed(() => [...new Set(patternItems.value.flatMap((p) => p.styleTags || [p.style]).filter(Boolean))].sort());
const elements = computed(() => [...new Set(patternItems.value.flatMap((p) => p.elements || []).filter(Boolean))].sort());
const visibleItems = computed(() => {
  const keyword = query.value.trim().toLowerCase();
  return tabItems.value.filter((p) => {
    const text = [p.title, p.meta, p.style, ...(p.elements || [])].join(' ').toLowerCase();
    return (!keyword || text.includes(keyword)) &&
      (!selectedStyle.value || p.styleTags?.includes(selectedStyle.value)) &&
      (!selectedElement.value || p.elements?.includes(selectedElement.value));
  });
});

function clearFilters() {
  query.value = '';
  selectedStyle.value = '';
  selectedElement.value = '';
}

onMounted(async () => {
  refreshLocalData();
  window.addEventListener(userDataEvent, refreshLocalData);
  try {
    const result = listFrom(await api.patterns.mine({ page: 1, pageSize: 100 }));
    patternItems.value = (result.length ? result : demoPatterns).map(normalizePattern);
  } catch {
    patternItems.value = demoPatterns.map(normalizePattern);
  }
});
onBeforeUnmount(() => window.removeEventListener(userDataEvent, refreshLocalData));
</script>

<template>
  <div class="content">
    <div class="tabbar">
      <button v-for="item in ([['all', '全部纹样'], ['favorite', '我的收藏'], ['recent', '最近生成'], ['applied', '已应用']] as const)"
        :key="item[0]" class="tab-button" :class="{ active: activeTab === item[0] }" @click="activeTab = item[0]">
        {{ item[1] }}
      </button>
      <button class="primary create-button" @click="router.push('/generate')">＋ 新建生成</button>
    </div>
    <div class="toolbar">
      <input v-model="query" placeholder="搜索纹样名称、关键词" />
      <select v-model="selectedStyle" aria-label="按风格筛选">
        <option value="">全部风格</option>
        <option v-for="value in styles" :key="value" :value="value">{{ value }}</option>
      </select>
      <select v-model="selectedElement" aria-label="按元素筛选">
        <option value="">全部元素</option>
        <option v-for="value in elements" :key="value" :value="value">{{ value }}</option>
      </select>
    </div>
    <div v-if="visibleItems.length" class="pattern-grid library">
      <PatternCard v-for="pattern in visibleItems" :key="pattern.id" :p="pattern" />
    </div>
    <div v-else class="result-empty">
      <b>暂无符合条件的纹样</b>
      <p>{{ activeTab === 'all' ? '请调整搜索词或筛选条件。' : '该分类暂时没有纹样。' }}</p>
      <button v-if="query || selectedStyle || selectedElement" @click="clearFilters">清除筛选</button>
    </div>
  </div>
</template>

<style scoped>
.tab-button { margin: 0; padding: 0 0 12px; border: 0; border-bottom: 2px solid transparent; border-radius: 0; background: transparent; color: inherit; cursor: pointer; font: inherit; }
.tab-button.active { border-bottom-color: var(--red); color: var(--red); }
.tabbar .create-button { margin-left: auto; padding: 11px 18px; border-bottom: 0; border-radius: 10px; }
.result-empty { min-height: 260px; }
</style>
