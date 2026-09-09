<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api, listFrom } from '../../api';
import { patterns as demoPatterns } from '../../data';
import { readUserData, userDataEvent, writeUserData } from '../../userData';

type Tab = 'all' | 'favorite' | 'recent' | 'applied';
type ViewMode = 'grid' | 'list';
interface PatternItem {
  id: string | number;
  title: string;
  image: string;
  category: string;
  style: string;
  elements: string[];
  theme: string;
  createdAt?: string | number;
  favorite: boolean;
  applied: boolean;
  [key: string]: any;
}

const router = useRouter();
const route = useRoute();
const items = ref<PatternItem[]>([]);
const libraryItems = ref<PatternItem[]>([]);
const activeTab = ref<Tab>('all');
const viewMode = ref<ViewMode>('grid');
const keyword = ref('');
const style = ref('');
const element = ref('');
const theme = ref('');
const page = ref(1);
const pageSize = ref(8);
const total = ref(0);
const loading = ref(false);
const notice = ref('');
const errorMessage = ref('');
const batchMode = ref(false);
const selectedIds = ref<string[]>([]);
const detail = ref<PatternItem | null>(null);
const options = ref<{ styles: string[]; elements: string[]; themes: string[] }>({ styles: [], elements: [], themes: [] });

const fallbackImage = '/demo/pattern/round-flower-pattern-01.jpg';
const favoriteIds = () => readUserData<string[]>('pattern_favorites', []);
const getCount = (result: any, fallback = 0) => {
  const value = result?.total ?? result?.totalCount ?? result?.pagination?.total;
  return Number.isFinite(Number(value)) ? Number(value) : fallback;
};
const toArray = (value: any): string[] => Array.isArray(value)
  ? value.map(String).filter(Boolean)
  : String(value || '').split(/[,，/、]/).map((v) => v.trim()).filter(Boolean);

function normalize(item: any, index = 0): PatternItem {
  const demo = demoPatterns[index % demoPatterns.length] as any;
  const id = item?.id ?? item?.patternId ?? demo?.id ?? index + 1;
  const title = item?.title || item?.name || item?.patternName || demo?.title || `我的纹样 ${index + 1}`;
  const image = item?.image || item?.thumbnailUrl || item?.imageUrl || item?.previewImageUrl || demo?.image || fallbackImage;
  const styleValue = item?.style || item?.styleName || demo?.style || '广绣经典';
  const elements = toArray(item?.elements ?? item?.element ?? demo?.elements);
  const ids = favoriteIds();
  return {
    ...demo,
    ...item,
    id,
    title,
    image,
    category: item?.category || item?.categoryName || elements[0] || styleValue,
    style: styleValue,
    elements,
    theme: item?.theme || item?.themeName || '传统文化',
    createdAt: item?.createdAt || item?.generatedAt || item?.createTime || '',
    favorite: Boolean(item?.favorite ?? item?.isFavorite ?? item?.favorited ?? ids.includes(String(id))),
    applied: Boolean(item?.applied ?? item?.isApplied ?? item?.productId ?? item?.usedProductId)
  };
}

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)));
const pageNumbers = computed(() => {
  const max = Math.min(totalPages.value, 7);
  const start = Math.min(Math.max(1, page.value - 3), Math.max(1, totalPages.value - max + 1));
  return Array.from({ length: max }, (_, i) => start + i);
});
const selectedItems = computed(() => items.value.filter((item) => selectedIds.value.includes(String(item.id))));
const stats = computed(() => ({
  total: Math.max(total.value, libraryItems.value.length),
  favorites: libraryItems.value.filter((item) => item.favorite).length,
  recent: libraryItems.value.filter((item) => isRecent(item)).length,
  applied: libraryItems.value.filter((item) => item.applied).length
}));
const styleOptions = computed(() => unique([...options.value.styles, ...libraryItems.value.map((item) => item.style)]));
const elementOptions = computed(() => unique([...options.value.elements, ...libraryItems.value.flatMap((item) => item.elements)]));
const themeOptions = computed(() => unique([...options.value.themes, ...libraryItems.value.map((item) => item.theme)]));

function unique(values: string[]) { return [...new Set(values.filter(Boolean))].sort(); }
function isRecent(item: PatternItem) {
  const time = item.createdAt ? new Date(item.createdAt).getTime() : 0;
  return Boolean(time && Date.now() - time <= 7 * 24 * 60 * 60 * 1000);
}
function formatDate(value?: string | number) {
  if (!value) return '最近生成';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('zh-CN');
}

function dedupePatterns(values: PatternItem[]) {
  const merged = new Map<string, PatternItem>();
  values.forEach((item) => {
    const id = String(item.id);
    const current = merged.get(id);
    merged.set(id, current ? {
      ...current,
      ...item,
      favorite: current.favorite || item.favorite,
      applied: current.applied || item.applied
    } : item);
  });
  return [...merged.values()];
}
function localPatternLibrary() {
  const saved = readUserData<any[]>('saved_patterns', []);
  const recent = readUserData<any[]>('recent_generations', []);
  const base = [...saved, ...recent, ...demoPatterns].map(normalize);
  const applied = readUserData<any[]>('applied_designs', []).map((design, index) => {
    const source = design.pattern || base.find((item) => String(item.id) === String(design.patternId)) || {};
    return normalize({
      ...source,
      id: design.patternId ?? source.id ?? `applied-${design.id}`,
      title: design.patternTitle || source.title,
      image: design.patternImage || source.image,
      createdAt: design.createdAt || source.createdAt,
      applied: true,
      productId: design.productId
    }, index);
  });
  return dedupePatterns([...saved.map(normalize), ...recent.map(normalize), ...applied]);
}
function matchesFilters(item: PatternItem) {
  const text = [item.title, item.category, item.style, item.theme, ...item.elements].join(' ').toLowerCase();
  const query = keyword.value.trim().toLowerCase();
  return (!query || text.includes(query)) &&
    (!style.value || item.style === style.value) &&
    (!element.value || item.elements.includes(element.value)) &&
    (!theme.value || item.theme === theme.value);
}
function itemsForCurrentTab(source: PatternItem[]) {
  return source.filter((item) => {
    if (activeTab.value === 'favorite' && !item.favorite) return false;
    if (activeTab.value === 'recent' && !isRecent(item)) return false;
    if (activeTab.value === 'applied' && !item.applied) return false;
    return matchesFilters(item);
  });
}
function showNotice(message: string) {
  notice.value = message;
  window.setTimeout(() => { if (notice.value === message) notice.value = ''; }, 2200);
}

async function loadPatterns() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const result: any = await api.patterns.mine({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value.trim() || undefined,
      style: style.value || undefined,
      element: element.value || undefined,
      theme: theme.value || undefined,
      tab: activeTab.value === 'all' ? undefined : activeTab.value
    });
    const serverItems = listFrom(result).map(normalize);
    const localItems = localPatternLibrary();
    libraryItems.value = dedupePatterns([...serverItems, ...localItems]);
    items.value = itemsForCurrentTab(libraryItems.value);
    total.value = Math.max(getCount(result, serverItems.length), libraryItems.value.length);
  } catch {
    const localItems = localPatternLibrary();
    libraryItems.value = dedupePatterns(localItems.length ? localItems : demoPatterns.map(normalize));
    items.value = itemsForCurrentTab(libraryItems.value);
    total.value = libraryItems.value.length;
    errorMessage.value = '接口暂时不可用，已显示本地保存的纹样。';
  } finally {
    loading.value = false;
    selectedIds.value = [];
  }
}

async function loadOptions() {
  try {
    const result: any = await api.patterns.options();
    options.value = {
      styles: toArray(result?.styles ?? result?.styleList ?? result?.styleOptions),
      elements: toArray(result?.elements ?? result?.elementList ?? result?.elementOptions),
      themes: toArray(result?.themes ?? result?.themeList ?? result?.themeOptions)
    };
  } catch { /* 筛选项可由当前列表字段补齐。 */ }
}

function resetFilters() {
  keyword.value = '';
  style.value = '';
  element.value = '';
  theme.value = '';
}
function selectTab(tab: Tab) { activeTab.value = tab; }
function changePage(target: number) {
  page.value = Math.min(Math.max(1, target), totalPages.value);
  loadPatterns();
}
function changePageSize() { page.value = 1; loadPatterns(); }
function toggleSelect(item: PatternItem) {
  const id = String(item.id);
  selectedIds.value = selectedIds.value.includes(id)
    ? selectedIds.value.filter((value) => value !== id)
    : [...selectedIds.value, id];
}
function saveFavorites(ids: string[]) { writeUserData('pattern_favorites', ids); }

async function toggleFavorite(item: PatternItem) {
  item.favorite = !item.favorite;
  const ids = new Set(favoriteIds());
  item.favorite ? ids.add(String(item.id)) : ids.delete(String(item.id));
  saveFavorites([...ids]);
  try {
    await (item.favorite ? api.patterns.favorite(item.id) : api.patterns.unfavorite(item.id));
    showNotice(item.favorite ? '已收藏纹样' : '已取消收藏');
  } catch {
    showNotice(item.favorite ? '后端暂不可用，收藏已同步到本地纹样库' : '后端暂不可用，本地收藏已取消');
  }
}
async function savePattern(item: PatternItem) {
  const saved = readUserData<any[]>('saved_patterns', []);
  const merged = [{ ...item, createdAt: item.createdAt || new Date().toISOString() }, ...saved];
  writeUserData('saved_patterns', merged.filter((value, index) =>
    merged.findIndex((other) => String(other.id) === String(value.id)) === index
  ));
  try {
    await api.patterns.save(item.id);
    showNotice('纹样已保存');
  } catch { showNotice('后端暂不可用，纹样已保存到本地'); }
}
async function openDetail(item: PatternItem) {
  detail.value = item;
  try { detail.value = normalize(await api.patterns.detail(item.id)); } catch { /* 使用列表信息展示详情。 */ }
}
function applyToProduct(item: PatternItem) { router.push({ path: '/customize', query: { patternId: String(item.id) } }); }
async function downloadPattern(item: PatternItem) {
  try {
    const result: any = await api.patterns.download(item.id);
    const url = result?.url || result?.downloadUrl || result?.fileUrl || (typeof result === 'string' ? result : '');
    if (url) window.open(url, '_blank', 'noopener');
    showNotice('下载地址已准备');
  } catch { showNotice('下载失败，请稍后重试'); }
}
async function removePattern(item: PatternItem) {
  try { await api.patterns.remove(item.id); } catch { /* 后端不可用时仍同步本地列表。 */ }
  removeLocalPatterns(new Set([String(item.id)]));
  items.value = items.value.filter((value) => String(value.id) !== String(item.id));
  libraryItems.value = libraryItems.value.filter((value) => String(value.id) !== String(item.id));
  total.value = Math.max(0, total.value - 1);
  showNotice('纹样已删除');
}
function removeLocalPatterns(ids: Set<string>) {
  for (const key of ['saved_patterns', 'recent_generations']) {
    writeUserData(key, readUserData<any[]>(key, []).filter((item) => !ids.has(String(item.id ?? item.patternId))));
  }
  saveFavorites(favoriteIds().filter((id) => !ids.has(String(id))));
}
function startBatch() { batchMode.value = !batchMode.value; if (!batchMode.value) selectedIds.value = []; }
async function batchDelete() {
  if (!selectedItems.value.length || !window.confirm(`确认删除选中的 ${selectedItems.value.length} 个纹样吗？`)) return;
  await Promise.allSettled(selectedItems.value.map((item) => api.patterns.remove(item.id)));
  const ids = new Set(selectedIds.value);
  removeLocalPatterns(ids);
  items.value = items.value.filter((item) => !ids.has(String(item.id)));
  libraryItems.value = libraryItems.value.filter((item) => !ids.has(String(item.id)));
  total.value = Math.max(0, total.value - ids.size);
  selectedIds.value = [];
  showNotice('批量删除完成');
}
async function batchDownload() {
  if (!selectedItems.value.length) return showNotice('请先选择纹样');
  await Promise.allSettled(selectedItems.value.map((item) => downloadPattern(item)));
  showNotice('批量下载已提交');
}
function jumpPage(event: Event) {
  const value = Number((event.target as HTMLInputElement).value);
  if (value) changePage(value);
}
function handleLibraryChange(event: Event) {
  const name = (event as CustomEvent).detail?.name;
  if (['saved_patterns', 'recent_generations', 'pattern_favorites', 'applied_designs'].includes(name)) loadPatterns();
}

watch([activeTab, keyword, style, element, theme], () => { page.value = 1; loadPatterns(); });
onMounted(() => {
  const requestedTab = String(route.query.tab || '');
  if (['all', 'favorite', 'recent', 'applied'].includes(requestedTab)) activeTab.value = requestedTab as Tab;
  loadOptions();
  loadPatterns();
  window.addEventListener(userDataEvent, handleLibraryChange);
});
onBeforeUnmount(() => window.removeEventListener(userDataEvent, handleLibraryChange));
</script>

<template>
  <div class="patterns-page">
    <section class="patterns-hero content">
      <span class="eyebrow">MY PATTERN LIBRARY</span>
      <h1>我的纹样</h1>
      <p>管理您的纹样资产，收藏喜爱的设计，应用到商品创作，打造专属的文化作品。</p>
      <div class="pattern-stats">
        <article><i>▤</i><div><span>纹样总数</span><strong>{{ stats.total }}</strong><small>个纹样</small></div></article>
        <article><i>★</i><div><span>收藏数量</span><strong>{{ stats.favorites }}</strong><small>个收藏</small></div></article>
        <article><i>◷</i><div><span>最近生成</span><strong>{{ stats.recent }}</strong><small>近 7 天新增</small></div></article>
        <article><i>♧</i><div><span>已应用到商品</span><strong>{{ stats.applied }}</strong><small>累计应用</small></div></article>
      </div>
    </section>

    <main class="patterns-layout content">
      <section class="patterns-main">
        <div class="pattern-tabs">
          <button v-for="tab in ([['all','全部纹样'],['favorite','我的收藏'],['recent','最近生成'],['applied','已应用']] as [Tab,string][])" :key="tab[0]" :class="{ active: activeTab === tab[0] }" @click="selectTab(tab[0])">{{ tab[1] }}</button>
          <div class="view-switch"><button :class="{ active: viewMode === 'grid' }" @click="viewMode = 'grid'">▦ 网格视图</button><button :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'">☷ 列表视图</button></div>
        </div>
        <div class="pattern-filters">
          <label class="search-field">⌕<input v-model="keyword" placeholder="搜索纹样名称、关键词" /></label>
          <select v-model="style" aria-label="风格"><option value="">风格</option><option v-for="value in styleOptions" :key="value" :value="value">{{ value }}</option></select>
          <select v-model="element" aria-label="元素"><option value="">元素</option><option v-for="value in elementOptions" :key="value" :value="value">{{ value }}</option></select>
          <select v-model="theme" aria-label="主题"><option value="">主题</option><option v-for="value in themeOptions" :key="value" :value="value">{{ value }}</option></select>
          <button @click="resetFilters">重置</button>
        </div>
        <p v-if="errorMessage" class="inline-notice">{{ errorMessage }}</p>
        <div v-if="loading" class="result-empty">正在加载纹样…</div>
        <div v-else-if="items.length" class="pattern-library" :class="viewMode">
          <article v-for="item in items" :key="item.id" class="library-card" :class="{ selected: selectedIds.includes(String(item.id)) }">
            <div class="library-image"><img :src="item.image" :alt="item.title" /><label v-if="batchMode" class="select-box"><input type="checkbox" :checked="selectedIds.includes(String(item.id))" @change="toggleSelect(item)" />选择</label><button class="favorite-button" :class="{ on: item.favorite }" :aria-label="item.favorite ? '取消收藏' : '收藏'" @click="toggleFavorite(item)">{{ item.favorite ? '★' : '☆' }}</button></div>
            <div class="library-info"><h3>{{ item.title }}</h3><p>{{ item.category }} · {{ item.style }}</p><small>{{ formatDate(item.createdAt) }} 生成</small><div class="card-actions"><button @click="openDetail(item)">▣ 查看详情</button><button @click="applyToProduct(item)">♧ 应用到商品</button><button @click="savePattern(item)">⊙ 保存纹样</button><button @click="downloadPattern(item)">⇩ 下载纹样</button><button class="delete-link" @click="removePattern(item)">删除</button></div></div>
          </article>
        </div>
        <div v-else class="result-empty"><b>暂无纹样</b><p>调整筛选条件或先生成一个新纹样。</p></div>
        <div class="pagination"><span>共 {{ total }} 条</span><div><button :disabled="page <= 1" @click="changePage(page - 1)">‹</button><button v-for="value in pageNumbers" :key="value" :class="{ active: value === page }" @click="changePage(value)">{{ value }}</button><button :disabled="page >= totalPages" @click="changePage(page + 1)">›</button></div><select v-model.number="pageSize" @change="changePageSize"><option :value="8">8 条/页</option><option :value="12">12 条/页</option><option :value="20">20 条/页</option></select><label>跳至 <input type="number" min="1" :max="totalPages" :value="page" @change="jumpPage" /> 页</label></div>
      </section>
      <aside class="quick-actions"><h2>快捷操作</h2><button class="primary" @click="router.push('/generate')">✦ 新建生成</button><button @click="startBatch">▧ {{ batchMode ? '退出批量管理' : '批量管理' }}</button><button class="danger" :disabled="!selectedItems.length" @click="batchDelete">♧ 批量删除</button><button :disabled="!selectedItems.length" @click="batchDownload">⇩ 批量下载</button><hr /><h3>纹样管理提示</h3><p>收藏喜欢的纹样，方便日后快速查找和管理。</p><p>将纹样应用到商品，创作独一无二的文创作品。</p><p>支持批量管理，提高操作效率。</p></aside>
    </main>
    <div v-if="notice" class="toast">{{ notice }}</div>
    <div v-if="detail" class="detail-backdrop" @click.self="detail = null"><article class="detail-modal"><button class="close" aria-label="关闭" @click="detail = null">×</button><img :src="detail.image" :alt="detail.title" /><div><span class="eyebrow">PATTERN DETAIL</span><h2>{{ detail.title }}</h2><p>{{ detail.category }} · {{ detail.style }}</p><p>元素：{{ detail.elements.join('、') || '传统广绣元素' }}</p><small>生成时间：{{ formatDate(detail.createdAt) }}</small><div><button class="primary" @click="applyToProduct(detail); detail = null">应用到商品</button><button @click="savePattern(detail)">保存纹样</button><button @click="downloadPattern(detail)">下载纹样</button></div></div></article></div>
  </div>
</template>

<style scoped>
.patterns-page { min-height: 100vh; background: linear-gradient(180deg,#fbf1e3,#fffaf2 42%); }
.patterns-hero { max-width: 1380px; padding-top: 48px; padding-bottom: 25px; }
.patterns-hero h1 { margin: 10px 0 5px; font-size: 43px; }
.patterns-hero p { color: var(--muted); margin: 0; }
.pattern-stats { display: grid; grid-template-columns: repeat(4,1fr); gap: 15px; margin-top: 25px; }
.pattern-stats article { display:flex; align-items:center; gap:14px; padding:18px 20px; border:1px solid var(--line); border-radius:13px; background:rgba(255,255,255,.75); }
.pattern-stats i { width:42px;height:42px;display:grid;place-items:center;border-radius:50%;background:#e3f0eb;color:var(--jade);font-size:22px;font-style:normal; }
.pattern-stats article:nth-child(2) i { background:#fae8c9;color:#c58b29; }.pattern-stats article:nth-child(3) i { background:#f8dfd5;color:var(--red); }
.pattern-stats span,.pattern-stats small { display:block;color:var(--muted);font-size:12px; }.pattern-stats strong { font-size:28px; margin-right:5px; }
.patterns-layout { display:grid; grid-template-columns:minmax(0,1fr) 245px; gap:22px; align-items:start; padding-bottom:75px; }
.patterns-main { min-width:0; }.pattern-tabs { display:flex; align-items:center; gap:28px; border-bottom:1px solid var(--line); }.pattern-tabs button,.view-switch button { border:0;background:transparent;border-radius:0;padding:13px 2px;color:var(--muted); }.pattern-tabs button.active { color:var(--red); border-bottom:2px solid var(--red); }.view-switch { margin-left:auto; display:flex; gap:10px; }.view-switch button.active { color:var(--red); }
.pattern-filters { display:flex; gap:10px; margin:18px 0; }.search-field { flex:1;display:flex;align-items:center;gap:5px;padding:0 12px;border:1px solid var(--line);border-radius:8px;background:#fff; color:var(--muted); }.search-field input { width:100%;padding:10px 4px;border:0;outline:0;background:transparent; }.pattern-filters select,.pattern-filters>button { min-width:105px;padding:10px;border:1px solid var(--line);border-radius:8px;background:#fffaf4; }.pattern-filters>button { min-width:70px; }
.inline-notice { padding:9px 12px; margin:0 0 12px; color:var(--jade); background:#eef5ef; border-radius:7px; font-size:13px; }.pattern-library { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:14px; }.library-card { overflow:hidden;border:1px solid var(--line);border-radius:11px;background:#fff;box-shadow:0 5px 18px rgba(80,50,24,.05); }.library-card.selected { outline:2px solid var(--red); }.library-image { position:relative; }.library-image img { display:block;width:100%;height:150px;object-fit:cover; }.favorite-button { position:absolute;right:8px;top:8px;width:31px;height:31px;padding:0;border:0;border-radius:50%;background:rgba(255,255,255,.9);font-size:19px;color:#88735c; }.favorite-button.on { color:var(--red); }.select-box { position:absolute;left:8px;top:8px;padding:4px 7px;border-radius:6px;background:rgba(255,255,255,.9);font-size:12px; }.select-box input { vertical-align:middle; }.library-info { padding:12px; }.library-info h3 { margin:0 0 5px;font-size:16px; }.library-info p { margin:0;color:var(--muted);font-size:12px; }.library-info small { display:block;margin-top:6px;color:var(--muted);font-size:11px; }.card-actions { display:flex;gap:7px;flex-wrap:wrap;margin-top:11px; }.card-actions button { padding:5px 7px;border:0;background:transparent;color:var(--jade);font-size:12px; }.card-actions .delete-link { color:var(--red); }.pattern-library.list { display:flex;flex-direction:column; }.pattern-library.list .library-card { display:grid;grid-template-columns:180px 1fr; }.pattern-library.list .library-image img { height:125px; }.quick-actions { padding:20px;background:rgba(255,255,255,.78);border:1px solid var(--line);border-radius:13px; }.quick-actions h2 { margin:0 0 15px;font-size:21px; }.quick-actions button { display:block;width:100%;margin:9px 0; }.quick-actions .danger { color:var(--red); }.quick-actions button:disabled { opacity:.45;cursor:not-allowed; }.quick-actions hr { border:0;border-top:1px solid var(--line);margin:20px 0; }.quick-actions h3 { margin:0 0 12px; }.quick-actions p { color:var(--muted);font-size:12px;line-height:1.7;margin:9px 0; }.result-empty { min-height:260px;display:grid;place-content:center;text-align:center;color:var(--muted); }.result-empty b { color:var(--jade);font-size:18px; }.pagination { display:flex;align-items:center;gap:12px;margin-top:22px;color:var(--muted);font-size:13px; }.pagination>div { display:flex;gap:4px; }.pagination button { min-width:30px;padding:6px 9px; }.pagination button.active { color:#fff;background:var(--red);border-color:var(--red); }.pagination button:disabled { opacity:.4; }.pagination select,.pagination input { padding:6px;border:1px solid var(--line);border-radius:6px;background:#fff; }.pagination label { margin-left:auto; }.pagination input { width:44px; }
.toast { position:fixed;left:50%;bottom:28px;transform:translateX(-50%);z-index:50;padding:10px 18px;border-radius:8px;background:rgba(45,33,24,.92);color:#fff; }.detail-backdrop { position:fixed;inset:0;z-index:60;display:grid;place-items:center;padding:20px;background:rgba(40,28,18,.5); }.detail-modal { position:relative;display:grid;grid-template-columns:minmax(230px, .9fr) 1fr;width:min(680px,94vw);overflow:hidden;border-radius:15px;background:#fffaf3;box-shadow:var(--shadow); }.detail-modal>img { width:100%;height:100%;min-height:320px;object-fit:cover; }.detail-modal>div { padding:34px; }.detail-modal h2 { margin:12px 0 8px; }.detail-modal p { color:var(--muted);line-height:1.7; }.detail-modal button { margin:20px 8px 0 0; }.detail-modal .close { position:absolute;right:10px;top:10px;margin:0;width:31px;height:31px;padding:0;border:0;border-radius:50%;background:#fff;font-size:22px; }
@media (max-width:1000px) { .pattern-library { grid-template-columns:repeat(3,minmax(0,1fr)); }.patterns-layout { grid-template-columns:1fr; }.quick-actions { order:-1; }.pattern-stats { grid-template-columns:repeat(2,1fr); } }
@media (max-width:650px) { .pattern-stats,.pattern-library { grid-template-columns:1fr 1fr; }.pattern-filters { flex-wrap:wrap; }.search-field { flex-basis:100%; }.pattern-tabs { gap:10px;overflow:auto; }.pagination { flex-wrap:wrap; }.pagination label { margin-left:0; }.pattern-library.list .library-card { grid-template-columns:110px 1fr; }.pattern-library.list .library-image img { height:100%;min-height:120px; }.detail-modal { grid-template-columns:1fr;max-height:90vh;overflow:auto; }.detail-modal>img { height:200px;min-height:0; } }
</style>
