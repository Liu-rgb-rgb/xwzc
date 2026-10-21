<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api, listFrom } from '../../api';
import { authState, isDemoAccount } from '../../auth';
import { patterns as demoPatterns } from '../../data';
import { readUserData, userDataEvent, writeUserData } from '../../userData';
import PatternStage3D from '../../components/PatternStage3D.vue';

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
const allPatternTotal = ref(0);
const recentPatternTotal = ref(0);
const loading = ref(false);
const notice = ref('');
const errorMessage = ref('');
const batchMode = ref(false);
const selectedIds = ref<string[]>([]);
const detail = ref<PatternItem | null>(null);
const options = ref<{ styles: string[]; elements: string[]; themes: string[] }>({
  styles: [],
  elements: [],
  themes: []
});
const offlineDemoToken = computed(() => authState.token === 'client-demo-token');

/* ===== 展示模式：静态风 / 沉浸式 / 3D 展示 ===== */
type DisplayMode = 'static' | 'immersive' | '3d';
const displayMode = ref<DisplayMode>('static');
const displayModeMeta: Array<{ key: DisplayMode; name: string; short: string; hint: string }> = [
  { key: 'static', name: '静态风', short: '画廊', hint: '精致画廊陈列，悬停预览，点击打开详情' },
  { key: 'immersive', name: '沉浸式', short: '展开', hint: '点击纹样，从卡片飞入中央舞台，鼠标轻移即可视差' },
  { key: '3d', name: '3D 展示', short: '立体', hint: '纹样落在会呼吸的布面上，拖拽旋转观察丝线光泽' }
];
const activeModeMeta = computed(
  () => displayModeMeta.find((mode) => mode.key === displayMode.value) ?? displayModeMeta[0]
);
const stageItem = ref<PatternItem | null>(null);
const stageFrame = ref<HTMLElement | null>(null);
const stageGhost = ref<{ x: number; y: number; width: number; height: number } | null>(null);
const stageClosing = ref(false);
const galleryRoot = ref<HTMLElement | null>(null);
let revealObserver: IntersectionObserver | null = null;
let stageTimer = 0;

const fallbackImage = '/demo/pattern/round-flower-pattern-01.jpg';
const styleLabels: Record<string, string> = {
  classic: '广绣经典',
  new_chinese: '新中式',
  lingnan_window: '岭南花窗',
  embroidery: '刺绣纹样'
};
const favoriteIds = () => readUserData<string[]>('pattern_favorites', []);
const getCount = (result: any, fallback = 0) => {
  const value = result?.total ?? result?.totalCount ?? result?.pagination?.total;
  return Number.isFinite(Number(value)) ? Number(value) : fallback;
};
const toArray = (value: any): string[] =>
  Array.isArray(value)
    ? value.map(String).filter(Boolean)
    : String(value || '')
        .split(/[,，/、]/)
        .map((v) => v.trim())
        .filter(Boolean);

function normalize(item: any, index = 0): PatternItem {
  const demo = demoPatterns[index % demoPatterns.length] as any;
  const id = item?.id ?? item?.patternId ?? demo?.id ?? index + 1;
  const title =
    item?.title || item?.name || item?.patternName || demo?.title || `我的纹样 ${index + 1}`;
  const image =
    item?.imageUrl ||
    item?.thumbnailUrl ||
    item?.previewImageUrl ||
    item?.image ||
    demo?.image ||
    fallbackImage;
  const rawStyle = String(item?.style || item?.styleName || demo?.style || '广绣经典');
  const styleValue = styleLabels[rawStyle] || rawStyle;
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
    favorite: Boolean(
      item?.favorite ?? item?.isFavorite ?? item?.favorited ?? ids.includes(String(id))
    ),
    applied: Boolean(item?.applied ?? item?.isApplied ?? item?.productId ?? item?.usedProductId)
  };
}

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)));
const currentPageItemCount = computed(() => items.value.length);
const pageSizeOptions = [8, 12, 20];
const pageNumbers = computed(() => {
  const max = Math.min(totalPages.value, 7);
  const start = Math.min(Math.max(1, page.value - 3), Math.max(1, totalPages.value - max + 1));
  return Array.from({ length: max }, (_, i) => start + i);
});
const selectedItems = computed(() =>
  items.value.filter((item) => selectedIds.value.includes(String(item.id)))
);
const stats = computed(() =>
  isDemoAccount.value
    ? { total: allPatternTotal.value || total.value, favorites: 0, recent: 0, applied: 0 }
    : {
        total: allPatternTotal.value || Math.max(total.value, libraryItems.value.length),
        favorites: libraryItems.value.filter((item) => item.favorite).length,
        recent: recentPatternTotal.value,
        applied: libraryItems.value.filter((item) => item.applied).length
      }
);
const styleOptions = computed(() =>
  unique([...options.value.styles, ...libraryItems.value.map((item) => item.style)])
);
const elementOptions = computed(() =>
  unique([...options.value.elements, ...libraryItems.value.flatMap((item) => item.elements)])
);
const themeOptions = computed(() =>
  unique([...options.value.themes, ...libraryItems.value.map((item) => item.theme)])
);

function unique(values: string[]) {
  return [...new Set(values.filter(Boolean))].sort();
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
    merged.set(
      id,
      current
        ? {
            ...item,
            ...current,
            favorite: current.favorite || item.favorite,
            applied: current.applied || item.applied
          }
        : item
    );
  });
  return [...merged.values()];
}
function localPatternLibrary() {
  const saved = readUserData<any[]>('saved_patterns', []);
  const recent = readUserData<any[]>('recent_generations', []);
  const base = [...saved, ...recent, ...demoPatterns].map(normalize);
  const applied = readUserData<any[]>('applied_designs', []).map((design, index) => {
    const source =
      design.pattern || base.find((item) => String(item.id) === String(design.patternId)) || {};
    return normalize(
      {
        ...source,
        id: design.patternId ?? source.id ?? `applied-${design.id}`,
        title: design.patternTitle || source.title,
        image: design.patternImage || source.image,
        createdAt: design.createdAt || source.createdAt,
        applied: true,
        productId: design.productId
      },
      index
    );
  });
  return dedupePatterns([...saved.map(normalize), ...recent.map(normalize), ...applied]);
}
function matchesFilters(item: PatternItem) {
  const text = [item.title, item.category, item.style, item.theme, ...item.elements]
    .join(' ')
    .toLowerCase();
  const query = keyword.value.trim().toLowerCase();
  return (
    (!query || text.includes(query)) &&
    (!style.value || item.style === style.value) &&
    (!element.value || item.elements.includes(element.value)) &&
    (!theme.value || item.theme === theme.value)
  );
}
function itemsForCurrentTab(source: PatternItem[]) {
  if (isDemoAccount.value && activeTab.value !== 'all') return [];
  return source.filter((item) => {
    if (activeTab.value === 'favorite' && !item.favorite) return false;
    if (activeTab.value === 'applied' && !item.applied) return false;
    return matchesFilters(item);
  });
}
function showNotice(message: string) {
  notice.value = message;
  window.setTimeout(() => {
    if (notice.value === message) notice.value = '';
  }, 2200);
}

function usePatternImageFallback(item: PatternItem, event: Event) {
  const image = event.currentTarget as HTMLImageElement;
  if (image.src.endsWith(fallbackImage)) return;
  image.src = fallbackImage;
  // 卡片显示兜底图后，详情/3D 舞台也要用同一张，否则 3D 贴图会 404。
  item.image = fallbackImage;
}

function showLocalPatterns() {
  const localItems = localPatternLibrary();
  libraryItems.value = dedupePatterns(localItems.length ? localItems : demoPatterns.map(normalize));
  items.value = itemsForCurrentTab(libraryItems.value);
  total.value = libraryItems.value.length;
}

async function loadPatterns() {
  loading.value = true;
  errorMessage.value = '';
  try {
    if (isDemoAccount.value && activeTab.value !== 'all') {
      libraryItems.value = [];
      items.value = [];
      total.value = 0;
      return;
    }
    const query = {
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value.trim() || undefined,
      style: style.value || undefined,
      element: element.value || undefined,
      theme: theme.value || undefined,
      tab: activeTab.value === 'all' ? undefined : activeTab.value
    };
    const loadingAllPatterns = activeTab.value === 'all';
    // “全部纹样”只展示当前登录用户自己的纹样（mine 接口按属主过滤）；
    // 演示账号没有真实后端用户，保留公共列表供其浏览。
    const usePublicList = loadingAllPatterns && isDemoAccount.value;
    const result: any = await (usePublicList ? api.patterns.list(query) : api.patterns.mine(query));
    const serverItems = listFrom(result).map((item, index) =>
      normalize(isDemoAccount.value ? { ...item, favorite: false, applied: false } : item, index)
    );
    if (loadingAllPatterns || activeTab.value === 'recent') {
      libraryItems.value = serverItems;
      items.value = itemsForCurrentTab(serverItems);
      total.value = getCount(result, serverItems.length);
      if (loadingAllPatterns) allPatternTotal.value = total.value;
      else recentPatternTotal.value = total.value;
      return;
    }
    const localItems = localPatternLibrary();
    libraryItems.value = dedupePatterns([...serverItems, ...localItems]);
    items.value = itemsForCurrentTab(libraryItems.value);
    total.value = Math.max(getCount(result, serverItems.length), items.value.length);
  } catch {
    if (isDemoAccount.value) {
      libraryItems.value = [];
      items.value = [];
      total.value = 0;
      errorMessage.value = '纹样列表暂时无法加载，请稍后刷新。';
    } else {
      showLocalPatterns();
      errorMessage.value = '接口暂时不可用，已显示本地保存的纹样。';
    }
  } finally {
    loading.value = false;
    selectedIds.value = [];
  }
}

async function loadRecentTotal() {
  if (isDemoAccount.value) {
    recentPatternTotal.value = 0;
    return;
  }
  try {
    const result: any = await api.patterns.mine({ tab: 'recent', page: 1, pageSize: 1 });
    recentPatternTotal.value = getCount(result, listFrom(result).length);
  } catch {
    recentPatternTotal.value = 0;
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
  } catch {
    /* 筛选项可由当前列表字段补齐。 */
  }
}

function resetFilters() {
  keyword.value = '';
  style.value = '';
  element.value = '';
  theme.value = '';
}
function selectTab(tab: Tab) {
  activeTab.value = tab;
}
function changePage(target: number) {
  page.value = Math.min(Math.max(1, target), totalPages.value);
  loadPatterns();
}
function changePageSize() {
  page.value = 1;
  loadPatterns();
}
function toggleSelect(item: PatternItem) {
  const id = String(item.id);
  selectedIds.value = selectedIds.value.includes(id)
    ? selectedIds.value.filter((value) => value !== id)
    : [...selectedIds.value, id];
}
function saveFavorites(ids: string[]) {
  writeUserData('pattern_favorites', ids);
}

/** 提取后端返回的真实错误信息，避免把业务错误统一显示成“后端不可用”。 */
function backendMessage(error: unknown, fallback: string): string {
  const message = (error as any)?.response?.data?.message;
  return typeof message === 'string' && message.trim() ? message : fallback;
}

/** 判断纹样是否属于当前登录用户；列表未携带属主信息时按本人纹样处理，保持原行为。 */
function isOwnPattern(item: PatternItem): boolean {
  const owner = item.userId ?? item.ownerId;
  const current = authState.user?.id;
  if (owner == null || current == null) return true;
  return String(owner) === String(current);
}

async function toggleFavorite(item: PatternItem) {
  if (isDemoAccount.value) {
    router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }
  item.favorite = !item.favorite;
  const ids = new Set(favoriteIds());
  item.favorite ? ids.add(String(item.id)) : ids.delete(String(item.id));
  saveFavorites([...ids]);
  // 后端收藏接口只允许操作本人纹样，他人纹样仅做本地收藏，避免必然失败。
  if (offlineDemoToken.value || !isOwnPattern(item)) {
    showNotice(item.favorite ? '已收藏纹样' : '已取消收藏');
    return;
  }
  try {
    await (item.favorite ? api.patterns.favorite(item.id) : api.patterns.unfavorite(item.id));
    showNotice(item.favorite ? '已收藏纹样' : '已取消收藏');
  } catch (error) {
    showNotice(backendMessage(error, '后端暂不可用，收藏已同步到本地纹样库'));
  }
}
function createPattern() {
  if (isDemoAccount.value) {
    router.push({ path: '/login', query: { redirect: '/generate' } });
    return;
  }
  router.push('/generate');
}
async function savePattern(item: PatternItem) {
  const saved = readUserData<any[]>('saved_patterns', []);
  const merged = [{ ...item, createdAt: item.createdAt || new Date().toISOString() }, ...saved];
  writeUserData(
    'saved_patterns',
    merged.filter(
      (value, index) => merged.findIndex((other) => String(other.id) === String(value.id)) === index
    )
  );
  // 后端保存接口只允许操作本人纹样，他人纹样仅做本地保存，避免必然失败。
  if (offlineDemoToken.value || !isOwnPattern(item)) {
    showNotice('纹样已保存');
    return;
  }
  try {
    await api.patterns.save(item.id);
    showNotice('纹样已保存');
  } catch (error) {
    showNotice(backendMessage(error, '后端暂不可用，纹样已保存到本地'));
  }
}
async function openDetail(item: PatternItem) {
  detail.value = item;
  if (offlineDemoToken.value) return;
  try {
    detail.value = normalize(await api.patterns.detail(item.id));
  } catch {
    /* 使用列表信息展示详情。 */
  }
}
async function applyToProduct(item: PatternItem) {
  const apply = (selected: PatternItem) => {
    const recent = readUserData<any[]>('recent_generations', []);
    const next = [selected, ...recent].filter(
      (value, index, all) =>
        all.findIndex((other) => String(other.id ?? other.patternId) === String(value.id)) === index
    );
    writeUserData('recent_generations', next.slice(0, 50));
    router.push({ path: '/customize', query: { patternId: String(item.id) } });
  };
  if (offlineDemoToken.value) {
    apply(item);
    return;
  }
  try {
    const detail: any = await api.patterns.detail(item.id);
    const selected = normalize({ ...item, ...detail, image: detail?.imageUrl || item.image });
    apply(selected);
  } catch {
    apply(item);
  }
}

async function downloadImage(item: PatternItem) {
  const response = await fetch(item.image);
  if (!response.ok) throw new Error('图片读取失败');
  const blobUrl = URL.createObjectURL(await response.blob());
  const link = document.createElement('a');
  link.href = blobUrl;
  link.download = `${item.title || '绣纹纹样'}.jpg`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(blobUrl), 1000);
}
async function downloadPattern(item: PatternItem) {
  if (offlineDemoToken.value) {
    try {
      await downloadImage(item);
      showNotice('纹样已开始下载');
    } catch {
      showNotice('下载失败，请稍后重试');
    }
    return;
  }
  try {
    const result: any = await api.patterns.download(item.id);
    const url =
      result?.url ||
      result?.downloadUrl ||
      result?.fileUrl ||
      (typeof result === 'string' ? result : '');
    if (url) {
      window.open(url, '_blank', 'noopener');
      showNotice('下载地址已准备');
    } else {
      await downloadImage(item);
      showNotice('纹样已开始下载');
    }
  } catch {
    try {
      await downloadImage(item);
      showNotice('纹样已开始下载');
    } catch {
      showNotice('下载失败，请稍后重试');
    }
  }
}
async function removePattern(item: PatternItem) {
  if (!window.confirm(`确认删除“${item.title}”吗？`)) return;
  try {
    await api.patterns.remove(item.id);
  } catch {
    /* 后端不可用时仍同步本地列表。 */
  }
  removeLocalPatterns(new Set([String(item.id)]));
  items.value = items.value.filter((value) => String(value.id) !== String(item.id));
  libraryItems.value = libraryItems.value.filter((value) => String(value.id) !== String(item.id));
  total.value = Math.max(0, total.value - 1);
  showNotice('纹样已删除');
}
function removeLocalPatterns(ids: Set<string>) {
  for (const key of ['saved_patterns', 'recent_generations']) {
    writeUserData(
      key,
      readUserData<any[]>(key, []).filter((item) => !ids.has(String(item.id ?? item.patternId)))
    );
  }
  saveFavorites(favoriteIds().filter((id) => !ids.has(String(id))));
}
function startBatch() {
  batchMode.value = !batchMode.value;
  if (!batchMode.value) selectedIds.value = [];
}
async function batchDelete() {
  if (
    !selectedItems.value.length ||
    !window.confirm(`确认删除选中的 ${selectedItems.value.length} 个纹样吗？`)
  )
    return;
  if (!offlineDemoToken.value) {
    await Promise.allSettled(selectedItems.value.map((item) => api.patterns.remove(item.id)));
  }
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
  if (
    ['saved_patterns', 'recent_generations', 'pattern_favorites', 'applied_designs'].includes(name)
  )
    loadPatterns();
}

/* ===== 三模式交互 ===== */
function activateCard(item: PatternItem, event: MouseEvent) {
  if (batchMode.value) {
    toggleSelect(item);
    return;
  }
  if (displayMode.value === 'static') {
    openDetail(item);
    return;
  }
  const card = event.currentTarget as HTMLElement | null;
  const from = (card?.querySelector('.library-image') ?? card)?.getBoundingClientRect();
  if (!from) return;
  stageGhost.value = { x: from.x, y: from.y, width: from.width, height: from.height };
  stageItem.value = item;
  stageClosing.value = false;
  document.body.style.overflow = 'hidden';
  nextTick(() => {
    const frame = stageFrame.value;
    const ghost = stageGhost.value;
    if (!frame || !ghost) return;
    const to = frame.getBoundingClientRect();
    frame.style.transition = 'none';
    frame.style.transformOrigin = '0 0';
    frame.style.transform = `translate(${ghost.x - to.x}px, ${ghost.y - to.y}px) scale(${
      ghost.width / to.width
    }, ${ghost.height / to.height})`;
    frame.style.opacity = '0.35';
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        if (!stageFrame.value) return;
        stageFrame.value.style.transition =
          'transform .64s cubic-bezier(.2,.8,.2,1), opacity .5s ease';
        stageFrame.value.style.transform = 'none';
        stageFrame.value.style.opacity = '1';
      });
    });
  });
}
function closeStage() {
  if (stageClosing.value) return;
  const frame = stageFrame.value;
  const ghost = stageGhost.value;
  if (!frame || !ghost) {
    stageItem.value = null;
    document.body.style.overflow = '';
    return;
  }
  stageClosing.value = true;
  const to = frame.getBoundingClientRect();
  frame.style.transition = 'transform .56s cubic-bezier(.2,.8,.2,1), opacity .42s ease';
  frame.style.transformOrigin = '0 0';
  frame.style.transform = `translate(${ghost.x - to.x}px, ${ghost.y - to.y}px) scale(${
    ghost.width / to.width
  }, ${ghost.height / to.height})`;
  frame.style.opacity = '0.15';
  window.clearTimeout(stageTimer);
  stageTimer = window.setTimeout(() => {
    stageItem.value = null;
    stageClosing.value = false;
    stageGhost.value = null;
    document.body.style.overflow = '';
  }, 580);
}
async function removeStageItem() {
  const item = stageItem.value;
  if (!item) return;
  await removePattern(item);
  closeStage();
}
function stageTilt(event: MouseEvent) {
  if (displayMode.value !== 'immersive') return;
  const plate = event.currentTarget as HTMLElement;
  const rect = plate.getBoundingClientRect();
  const px = (event.clientX - rect.left) / rect.width - 0.5;
  const py = (event.clientY - rect.top) / rect.height - 0.5;
  plate.style.setProperty('--tilt-y', `${(px * 11).toFixed(2)}deg`);
  plate.style.setProperty('--tilt-x', `${(-py * 8).toFixed(2)}deg`);
}
function stageTiltReset(event: MouseEvent) {
  const plate = event.currentTarget as HTMLElement;
  plate.style.setProperty('--tilt-y', '0deg');
  plate.style.setProperty('--tilt-x', '0deg');
}
function handleStageKey(event: KeyboardEvent) {
  if (event.key === 'Escape' && stageItem.value) closeStage();
}
function setDisplayMode(mode: DisplayMode) {
  displayMode.value = mode;
  if (stageItem.value) closeStage();
}function observeCards() {
  if (!revealObserver) {
    revealObserver = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return;
          entry.target.classList.add('reveal');
          revealObserver?.unobserve(entry.target);
          window.setTimeout(() => {
            (entry.target as HTMLElement).style.transitionDelay = '';
          }, 900);
        });
      },
      { threshold: 0.06, rootMargin: '0px 0px -40px 0px' }
    );
  }
  galleryRoot.value
    ?.querySelectorAll('.card:not(.reveal)')
    .forEach((card) => revealObserver?.observe(card));
}

watch([activeTab, keyword, style, element, theme], () => {
  page.value = 1;
  loadPatterns();
});
watch(
  items,
  () => {
    nextTick(observeCards);
  },
  { flush: 'post' }
);
onMounted(() => {
  const requestedTab = String(route.query.tab || '');
  if (['all', 'favorite', 'recent', 'applied'].includes(requestedTab))
    activeTab.value = requestedTab as Tab;
  const savedMode = readUserData<string>('pattern_display_mode', 'static');
  if (['static', 'immersive', '3d'].includes(savedMode))
    displayMode.value = savedMode as DisplayMode;
  loadOptions();
  loadPatterns();
  loadRecentTotal();
  window.addEventListener(userDataEvent, handleLibraryChange);
  window.addEventListener('keydown', handleStageKey);
  nextTick(observeCards);
});
watch(displayMode, (mode) => writeUserData('pattern_display_mode', mode));
onBeforeUnmount(() => {
  window.removeEventListener(userDataEvent, handleLibraryChange);
  window.removeEventListener('keydown', handleStageKey);
  revealObserver?.disconnect();
  revealObserver = null;
  window.clearTimeout(stageTimer);
  document.body.style.overflow = '';
});
</script>

<template>
  <div class="patterns-page">
    <div class="paper-noise" aria-hidden="true" />
    <div class="patterns-wrap">
      <section class="library-hero">
        <div class="title-area">
          <span class="eyebrow">MY PATTERN ASSETS</span>
          <h1>我的纹样资产</h1>
          <p>管理您的专属设计，应用到商品创作，打造独具匠心的文创作品。</p>
        </div>
        <div class="stats-area">
          <div class="stat-item">
            <span class="stat-num">{{ stats.total }}</span>
            <span class="stat-label">纹样总数</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">{{ stats.favorites }}</span>
            <span class="stat-label">我的收藏</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">{{ stats.recent }}</span>
            <span class="stat-label">最近生成</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">{{ stats.applied }}</span>
            <span class="stat-label">已应用</span>
          </div>
        </div>
      </section>

      <section class="library-controls">
        <div class="control-row">
          <label class="search-box">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.6"
              aria-hidden="true"
            >
              <circle
                cx="11"
                cy="11"
                r="7"
              />
              <line
                x1="16.5"
                y1="16.5"
                x2="21"
                y2="21"
              />
            </svg>
            <input
              v-model="keyword"
              placeholder="搜索纹样名称或关键词…"
            />
          </label>
          <div class="filters">
            <select
              v-model="style"
              class="filter-pill"
              aria-label="风格"
            >
              <option value="">全部风格</option>
              <option
                v-for="value in styleOptions"
                :key="value"
                :value="value"
              >
                {{ value }}
              </option>
            </select>
            <select
              v-model="element"
              class="filter-pill"
              aria-label="元素"
            >
              <option value="">元素筛选</option>
              <option
                v-for="value in elementOptions"
                :key="value"
                :value="value"
              >
                {{ value }}
              </option>
            </select>
            <select
              v-model="theme"
              class="filter-pill"
              aria-label="主题"
            >
              <option value="">主题</option>
              <option
                v-for="value in themeOptions"
                :key="value"
                :value="value"
              >
                {{ value }}
              </option>
            </select>
            <button
              v-if="keyword || style || element || theme"
              class="filter-pill ghost"
              @click="resetFilters"
            >
              重置
            </button>
          </div>
        </div>

        <div class="control-row secondary">
          <div class="tab-pills">
            <button
              v-for="tab in [
                ['all', '全部纹样'],
                ['favorite', '我的收藏'],
                ['recent', '最近生成'],
                ['applied', '已应用']
              ] as [Tab, string][]"
              :key="tab[0]"
              class="tab-pill"
              :class="{ active: activeTab === tab[0] }"
              @click="selectTab(tab[0])"
            >
              {{ tab[1] }}
            </button>
          </div>
          <div class="toolbar-right">
            <div
              class="mode-switch"
              role="tablist"
              aria-label="展示模式"
            >
              <button
                v-for="mode in displayModeMeta"
                :key="mode.key"
                role="tab"
                :aria-selected="displayMode === mode.key"
                :class="{ active: displayMode === mode.key }"
                :title="mode.hint"
                @click="setDisplayMode(mode.key)"
              >
                {{ mode.name }}
              </button>
            </div>
            <div class="view-switch">
              <button
                :class="{ active: viewMode === 'grid' }"
                title="网格视图"
                aria-label="网格视图"
                @click="viewMode = 'grid'"
              >
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.7"
                  stroke-linejoin="round"
                >
                  <rect
                    x="4"
                    y="4"
                    width="7"
                    height="7"
                    rx="1.4"
                  />
                  <rect
                    x="13"
                    y="4"
                    width="7"
                    height="7"
                    rx="1.4"
                  />
                  <rect
                    x="4"
                    y="13"
                    width="7"
                    height="7"
                    rx="1.4"
                  />
                  <rect
                    x="13"
                    y="13"
                    width="7"
                    height="7"
                    rx="1.4"
                  />
                </svg>
              </button>
              <button
                :class="{ active: viewMode === 'list' }"
                title="列表视图"
                aria-label="列表视图"
                @click="viewMode = 'list'"
              >
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.7"
                  stroke-linecap="round"
                >
                  <line
                    x1="4"
                    y1="6"
                    x2="20"
                    y2="6"
                  />
                  <line
                    x1="4"
                    y1="12"
                    x2="20"
                    y2="12"
                  />
                  <line
                    x1="4"
                    y1="18"
                    x2="20"
                    y2="18"
                  />
                </svg>
              </button>
            </div>
            <button
              class="manage-btn"
              :class="{ active: batchMode }"
              @click="startBatch"
            >
              {{ batchMode ? '退出批量' : '批量管理' }}
            </button>
          </div>
        </div>

        <p class="mode-hint">
          <span>{{ activeModeMeta.hint }}</span>
          <small v-if="displayMode !== 'static'">点击任意纹样试试</small>
        </p>
      </section>

      <div
        v-if="batchMode"
        class="batch-bar"
      >
        <span>已选 <b>{{ selectedIds.length }}</b> 个纹样</span>
        <div>
          <button
            :disabled="!selectedItems.length"
            @click="batchDownload"
          >
            批量下载
          </button>
          <button
            class="danger"
            :disabled="!selectedItems.length"
            @click="batchDelete"
          >
            批量删除
          </button>
        </div>
      </div>

      <p
        v-if="errorMessage"
        class="inline-notice"
      >
        {{ errorMessage }}
      </p>
      <div
        v-if="loading"
        class="result-empty"
      >
        正在加载纹样…
      </div>

      <div
        v-else-if="items.length && viewMode === 'grid'"
        ref="galleryRoot"
        class="gallery"
      >
        <article
          v-for="(item, index) in items"
          :key="item.id"
          class="card"
          :class="{ selected: selectedIds.includes(String(item.id)), 'batch-mode': batchMode }"
          :style="{ transitionDelay: `${Math.min(index, 10) * 45}ms` }"
          @click="activateCard(item, $event)"
        >
          <div class="library-image">
            <img
              :src="item.image"
              :alt="item.title"
              loading="lazy"
              @error="usePatternImageFallback(item, $event)"
            />
            <label
              v-if="batchMode"
              class="select-box"
              @click.stop
            >
              <input
                type="checkbox"
                :checked="selectedIds.includes(String(item.id))"
                @change="toggleSelect(item)"
              />
              选择
            </label>
            <div class="card-info">
              <h3>{{ item.title }}</h3>
              <p>{{ item.category }} · {{ item.style }}</p>
            </div>
            <div
              class="card-actions"
              @click.stop
            >
              <button
                class="action-btn"
                :class="{ on: item.favorite }"
                :aria-label="item.favorite ? '取消收藏' : '收藏'"
                @click="toggleFavorite(item)"
              >
                <svg
                  viewBox="0 0 24 24"
                  :fill="item.favorite ? 'currentColor' : 'none'"
                  stroke="currentColor"
                  stroke-width="1.8"
                >
                  <path
                    d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
                  />
                </svg>
              </button>
              <button
                class="action-btn"
                aria-label="下载纹样"
                @click="downloadPattern(item)"
              >
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.8"
                >
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                  <polyline points="7 10 12 15 17 10" />
                  <line
                    x1="12"
                    y1="15"
                    x2="12"
                    y2="3"
                  />
                </svg>
              </button>
            </div>
          </div>
        </article>
      </div>

      <div
        v-else-if="items.length"
        class="library-list"
      >
        <article
          v-for="item in items"
          :key="item.id"
          class="list-row"
          :class="{ selected: selectedIds.includes(String(item.id)) }"
          @click="activateCard(item, $event)"
        >
          <label
            v-if="batchMode"
            class="select-box"
            @click.stop
          >
            <input
              type="checkbox"
              :checked="selectedIds.includes(String(item.id))"
              @change="toggleSelect(item)"
            />
          </label>
          <div class="list-thumb library-image">
            <img
              :src="item.image"
              :alt="item.title"
              loading="lazy"
              @error="usePatternImageFallback(item, $event)"
            />
          </div>
          <div class="list-info">
            <h3>{{ item.title }}</h3>
            <p>{{ item.category }} · {{ item.style }} · {{ formatDate(item.createdAt) }} 生成</p>
          </div>
          <div class="list-actions" @click.stop>
            <button
              class="action-btn"
              :class="{ on: item.favorite }"
              :aria-label="item.favorite ? '取消收藏' : '收藏'"
              @click="toggleFavorite(item)"
            >
              {{ item.favorite ? '★' : '☆' }}
            </button>
            <button @click="openDetail(item)">详情</button>
            <button @click="savePattern(item)">保存</button>
            <button @click="downloadPattern(item)">下载</button>
            <button
              class="delete-link"
              @click="removePattern(item)"
            >
              删除
            </button>
          </div>
        </article>
      </div>

      <div
        v-else
        class="result-empty"
      >
        <b>暂无纹样</b>
        <p>调整筛选条件，或点击右下角「新建生成」创作一张。</p>
      </div>

      <div
        v-if="items.length"
        class="pagination"
      >
        <span>共 {{ total }} 条</span>
        <div class="page-buttons">
          <button
            :disabled="page <= 1"
            @click="changePage(page - 1)"
          >
            ‹
          </button>
          <button
            v-for="value in pageNumbers"
            :key="value"
            :class="{ active: value === page }"
            @click="changePage(value)"
          >
            {{ value }}
          </button>
          <button
            :disabled="page >= totalPages"
            @click="changePage(page + 1)"
          >
            ›
          </button>
        </div>
        <div class="page-tools">
          <select
            v-model.number="pageSize"
            @change="changePageSize"
          >
            <option
              v-if="currentPageItemCount !== pageSize"
              :value="pageSize"
            >
              {{ currentPageItemCount }} 条/页
            </option>
            <template
              v-for="size in pageSizeOptions"
              :key="size"
            >
              <option
                v-if="size !== pageSize || currentPageItemCount === pageSize"
                :value="size"
              >
                {{ size }} 条/页
              </option>
            </template>
          </select>
          <label>
            跳至
            <input
              type="number"
              min="1"
              :max="totalPages"
              :value="page"
              @change="jumpPage"
            />
            页
          </label>
        </div>
      </div>
    </div>

    <button
      class="fab-btn"
      @click="createPattern"
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
      >
        <line
          x1="12"
          y1="5"
          x2="12"
          y2="19"
        />
        <line
          x1="5"
          y1="12"
          x2="19"
          y2="12"
        />
      </svg>
      新建生成
    </button>

    <div
      v-if="notice"
      class="toast"
    >
      {{ notice }}
    </div>

    <div
      v-if="detail"
      class="detail-backdrop"
      @click.self="detail = null"
    >
      <article class="detail-modal">
        <button
          class="close"
          aria-label="关闭"
          @click="detail = null"
        >
          ×
        </button>
        <img
          :src="detail.image"
          :alt="detail.title"
          @error="usePatternImageFallback(detail, $event)"
        />
        <div>
          <span class="eyebrow">PATTERN DETAIL</span>
          <h2>{{ detail.title }}</h2>
          <p>{{ detail.category }} · {{ detail.style }}</p>
          <p>元素：{{ detail.elements.join('、') || '传统广绣元素' }}</p>
          <small>生成时间：{{ formatDate(detail.createdAt) }}</small>
          <div class="detail-actions">
            <button
              v-if="false"
              class="primary"
              @click="
                applyToProduct(detail);
                detail = null;
              "
            >
              应用到商品
            </button>
            <button @click="savePattern(detail)">保存纹样</button>
            <button @click="downloadPattern(detail)">下载纹样</button>
          </div>
        </div>
      </article>
    </div>

    <Teleport to="body">
      <div
        v-if="stageItem"
        class="stage-overlay"
        :class="{ closing: stageClosing }"
        @click.self="closeStage"
      >
        <button
          class="stage-close"
          aria-label="关闭"
          @click="closeStage"
        >
          ×
        </button>
        <div class="stage-body">
          <div
            ref="stageFrame"
            class="stage-plate"
            :class="{ 'is-3d': displayMode === '3d' }"
          >
            <div
              class="stage-plate-inner"
              @mousemove="stageTilt"
              @mouseleave="stageTiltReset"
            >
              <PatternStage3D
                v-if="displayMode === '3d'"
                :key="String(stageItem.id)"
                :src="stageItem.image"
              />
              <img
                v-else
                :src="stageItem.image"
                :alt="stageItem.title"
                @error="usePatternImageFallback(stageItem, $event)"
              />
              <span class="stage-sheen" aria-hidden="true" />
              <span
                v-if="displayMode === '3d'"
                class="stage-tip"
              >
                拖拽旋转 · 滚轮缩放 · 丝线随光流动
              </span>
            </div>
          </div>
          <aside class="stage-info">
            <span class="eyebrow">{{ activeModeMeta.name }} · PATTERN STAGE</span>
            <h2>{{ stageItem.title }}</h2>
            <p class="stage-sub">{{ stageItem.category }} · {{ stageItem.style }}</p>
            <dl>
              <div>
                <dt>元素</dt>
                <dd>{{ stageItem.elements.join('、') || '传统广绣元素' }}</dd>
              </div>
              <div>
                <dt>主题</dt>
                <dd>{{ stageItem.theme }}</dd>
              </div>
              <div>
                <dt>生成</dt>
                <dd>{{ formatDate(stageItem.createdAt) }}</dd>
              </div>
            </dl>
            <div class="stage-actions">
              <button
                class="primary"
                @click="toggleFavorite(stageItem)"
              >
                {{ stageItem.favorite ? '已收藏' : '收藏纹样' }}
              </button>
              <button @click="savePattern(stageItem)">保存</button>
              <button @click="downloadPattern(stageItem)">下载</button>
              <button
                class="danger"
                @click="removeStageItem"
              >
                删除
              </button>
            </div>
          </aside>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.patterns-page {
  --paper: #fcfbfa;
  --ink2: #2c2a29;
  --muted2: #8b857c;
  --accent: #c13e33;
  --hair: #eae6df;
  --ease: cubic-bezier(0.2, 0.8, 0.2, 1);
  position: relative;
  min-height: 100vh;
  background: var(--paper);
  color: var(--ink2);
  font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
.paper-noise {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  opacity: 0.6;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)' opacity='0.035'/%3E%3C/svg%3E");
}
.patterns-wrap {
  position: relative;
  z-index: 1;
  max-width: 1440px;
  margin: 0 auto;
  padding: 46px 48px 130px;
}

/* ===== 标题与统计 ===== */
.library-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 32px;
  margin-bottom: 46px;
  padding-bottom: 0;
  border: 0;
  flex-wrap: wrap;
}
.title-area .eyebrow {
  display: block;
  margin-bottom: 14px;
  color: #b3aba0;
  font-size: 11px;
  letter-spacing: 3px;
}
.title-area h1 {
  margin: 0 0 12px;
  font-family: 'Noto Serif SC', 'Songti SC', 'Microsoft YaHei', serif;
  font-size: 42px;
  font-weight: 700;
  letter-spacing: 3px;
}
.title-area p {
  margin: 0;
  color: var(--muted2);
  font-size: 14px;
  letter-spacing: 0.5px;
}
.stats-area {
  display: flex;
  gap: 56px;
}
.stat-item {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 7px;
}
.stat-num {
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 36px;
  font-weight: 600;
  line-height: 1;
}
.stat-label {
  color: var(--muted2);
  font-size: 12.5px;
  letter-spacing: 1px;
}

/* ===== 控制栏 ===== */
.library-controls {
  padding-bottom: 16px;
  margin-bottom: 26px;
  border-bottom: 1px solid var(--hair);
}
.control-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}
.control-row.secondary {
  margin-top: 16px;
}
.search-box {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 340px;
  max-width: 100%;
  padding: 2px 0;
  border-bottom: 1px solid #cfc8bd;
  color: #9a938a;
  transition: border-color 0.3s;
  cursor: text;
}
.search-box:focus-within {
  border-bottom-color: var(--ink2);
}
.search-box svg {
  width: 17px;
  height: 17px;
  flex: 0 0 auto;
}
.search-box input {
  flex: 1;
  min-width: 0;
  padding: 9px 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--ink2);
  font-size: 14px;
}
.search-box input::placeholder {
  color: #a9a29a;
}
.filters {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.filter-pill {
  appearance: none;
  padding: 8px 18px;
  border: 1px solid var(--hair);
  border-radius: 30px;
  background: transparent;
  color: #6f6a63;
  font-size: 13px;
  cursor: pointer;
  outline: none;
  transition: all 0.3s var(--ease);
}
.filter-pill:hover {
  background: #fff;
  border-color: #fff;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.06);
  color: var(--ink2);
}
select.filter-pill {
  padding-right: 34px;
  background-image: linear-gradient(45deg, transparent 50%, #9a938a 50%),
    linear-gradient(135deg, #9a938a 50%, transparent 50%);
  background-position: calc(100% - 17px) 52%, calc(100% - 12px) 52%;
  background-size: 5px 5px, 5px 5px;
  background-repeat: no-repeat;
}
.filter-pill.ghost {
  border-color: rgba(193, 62, 51, 0.28);
  color: var(--accent);
}
.tab-pills {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.tab-pill {
  padding: 7px 14px;
  border: 0;
  border-radius: 24px;
  background: transparent;
  color: #7b756d;
  font-size: 13.5px;
  cursor: pointer;
  transition: all 0.3s var(--ease);
}
.tab-pill:hover {
  color: var(--ink2);
}
.tab-pill.active {
  background: var(--ink2);
  color: #fff;
  box-shadow: 0 6px 16px rgba(44, 42, 41, 0.18);
}
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
  flex-wrap: wrap;
}
.mode-switch {
  display: flex;
  gap: 2px;
  padding: 3px;
  border: 1px solid var(--hair);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.6);
}
.mode-switch button {
  padding: 7px 14px;
  border: 0;
  border-radius: 22px;
  background: transparent;
  color: #77716a;
  font-size: 12.5px;
  cursor: pointer;
  transition: all 0.3s var(--ease);
}
.mode-switch button:hover {
  color: var(--ink2);
}
.mode-switch button.active {
  background: var(--accent);
  color: #fff;
  box-shadow: 0 6px 14px rgba(193, 62, 51, 0.28);
}
.view-switch {
  display: flex;
  border: 1px solid var(--hair);
  border-radius: 10px;
  overflow: hidden;
}
.view-switch button {
  display: grid;
  place-items: center;
  width: 34px;
  height: 32px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #8c857c;
  cursor: pointer;
  transition: background 0.25s, color 0.25s;
}
.view-switch button svg {
  width: 15px;
  height: 15px;
}
.view-switch button:hover {
  color: var(--ink2);
}
.view-switch button.active {
  background: #fff;
  color: var(--accent);
}
.manage-btn {
  padding: 8px 16px;
  border: 1px solid var(--hair);
  border-radius: 24px;
  background: transparent;
  color: #6f6a63;
  font-size: 12.5px;
  cursor: pointer;
  transition: all 0.3s;
}
.manage-btn:hover,
.manage-btn.active {
  border-color: var(--accent);
  color: var(--accent);
}
.mode-hint {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 14px 0 0;
  color: #a29a90;
  font-size: 12px;
}
.mode-hint small {
  padding: 3px 9px;
  border-radius: 12px;
  background: rgba(193, 62, 51, 0.08);
  color: var(--accent);
  letter-spacing: 0.5px;
}

/* ===== 批量条 ===== */
.batch-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
  padding: 12px 18px;
  border: 1px solid rgba(193, 62, 51, 0.16);
  border-radius: 14px;
  background: rgba(193, 62, 51, 0.06);
  color: #7a4a43;
  font-size: 13px;
  flex-wrap: wrap;
}
.batch-bar b {
  color: var(--accent);
}
.batch-bar div {
  display: flex;
  gap: 10px;
}
.batch-bar button {
  padding: 7px 14px;
  border: 1px solid rgba(193, 62, 51, 0.3);
  border-radius: 20px;
  background: #fff;
  color: var(--accent);
  font-size: 12.5px;
  cursor: pointer;
}
.batch-bar button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.batch-bar button.danger {
  border-color: transparent;
  background: var(--accent);
  color: #fff;
}

/* ===== 画廊 ===== */
.gallery {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(232px, 1fr));
  gap: 26px;
}
.card {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  background: #fff;
  cursor: pointer;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
  opacity: 0;
  transform: translateY(34px);
  transition: opacity 0.7s var(--ease), transform 0.7s var(--ease), box-shadow 0.5s var(--ease);
}
.card.reveal {
  opacity: 1;
  transform: translateY(0);
}
.card:hover {
  transform: translateY(-9px);
  box-shadow: 0 26px 46px rgba(0, 0, 0, 0.12);
}
.card.selected {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.library-image {
  position: relative;
  aspect-ratio: 4 / 5;
  overflow: hidden;
  background: #efe8dd;
}
.library-image img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.9s var(--ease);
}
.card:hover .library-image img {
  transform: scale(1.07);
}
.card .library-image::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(20, 16, 12, 0.66) 0%, rgba(20, 16, 12, 0) 42%);
  opacity: 0.85;
  transition: opacity 0.4s;
  pointer-events: none;
}
.card:hover .library-image::after {
  opacity: 1;
}
.card-info {
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: 16px;
  z-index: 2;
  color: #fff;
  transition: transform 0.45s var(--ease);
}
.card:hover .card-info {
  transform: translateY(-8px);
}
.card-info h3 {
  margin: 0 0 6px;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 17px;
  letter-spacing: 1px;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.4);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.card-info p {
  margin: 0;
  color: rgba(255, 255, 255, 0.78);
  font-size: 11.5px;
  letter-spacing: 0.8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.card-actions {
  position: absolute;
  right: 14px;
  bottom: 14px;
  z-index: 3;
  display: flex;
  gap: 8px;
  opacity: 0;
  transform: translateY(14px);
  transition: all 0.35s var(--ease);
}
.card:hover .card-actions,
.card:focus-within .card-actions {
  opacity: 1;
  transform: none;
}
.action-btn {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  padding: 0;
  box-sizing: border-box;
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(10px);
  color: #fff;
  line-height: 0;
  cursor: pointer;
  transition: background 0.25s, color 0.25s, transform 0.25s, border-color 0.25s;
}
.action-btn svg {
  display: block;
  width: 16px;
  height: 16px;
}
.action-btn:hover {
  background: #fff;
  color: var(--ink2);
  transform: scale(1.08);
}
.action-btn:active {
  transform: scale(0.94);
}
.action-btn.on {
  border-color: transparent;
  background: rgba(193, 62, 51, 0.88);
  color: #fff;
}
.action-btn.on:hover {
  background: var(--accent);
  color: #fff;
}
.select-box {
  position: absolute;
  left: 12px;
  top: 12px;
  z-index: 4;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  color: var(--ink2);
  font-size: 12px;
}

/* ===== 列表视图 ===== */
.library-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.list-row {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  border: 1px solid var(--hair);
  border-radius: 14px;
  background: #fff;
  cursor: pointer;
  transition: all 0.3s var(--ease);
}
.list-row:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.07);
}
.list-row.selected {
  outline: 2px solid var(--accent);
}
.list-thumb {
  width: 66px;
  height: 82px;
  flex: 0 0 auto;
  border-radius: 9px;
}
.list-thumb::after {
  display: none;
}
.list-thumb img {
  border-radius: 9px;
}
.list-info {
  flex: 1;
  min-width: 0;
}
.list-info h3 {
  margin: 0 0 5px;
  font-size: 15px;
}
.list-info p {
  margin: 0;
  color: var(--muted2);
  font-size: 12px;
}
.list-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.list-actions button {
  padding: 6px 13px;
  border: 1px solid var(--hair);
  border-radius: 20px;
  background: transparent;
  color: #6f6a63;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
}
.list-actions button:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.list-actions .action-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  border-radius: 50%;
  background: transparent;
  color: #9a8f80;
  font-size: 15px;
}
.list-actions .delete-link {
  border-color: transparent;
  color: #b23a2c;
}
.list-actions .delete-link:hover {
  background: rgba(193, 62, 51, 0.08);
}

/* ===== 空态 / 提示 ===== */
.result-empty {
  padding: 70px 20px;
  border: 1px dashed #ddd5c8;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.45);
  color: #a29a90;
  text-align: center;
}
.result-empty b {
  display: block;
  margin-bottom: 8px;
  color: #6f665c;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 18px;
}
.result-empty p {
  margin: 0;
  font-size: 13px;
}
.inline-notice {
  margin: 0 0 14px;
  padding: 10px 14px;
  border-radius: 10px;
  background: rgba(11, 98, 92, 0.07);
  color: var(--jade);
  font-size: 13px;
}

/* ===== 分页 ===== */
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-top: 34px;
  padding-top: 18px;
  border-top: 1px solid var(--hair);
  color: var(--muted2);
  font-size: 12.5px;
  flex-wrap: wrap;
}
.page-buttons {
  display: flex;
  gap: 6px;
}
.page-buttons button {
  width: 32px;
  height: 32px;
  border: 1px solid var(--hair);
  border-radius: 9px;
  background: transparent;
  color: #7b756d;
  cursor: pointer;
  transition: all 0.25s;
}
.page-buttons button:hover {
  border-color: var(--ink2);
  color: var(--ink2);
}
.page-buttons button.active {
  border-color: var(--ink2);
  background: var(--ink2);
  color: #fff;
}
.page-buttons button:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.page-tools {
  display: flex;
  align-items: center;
  gap: 14px;
}
.page-tools select,
.page-tools input {
  padding: 6px 8px;
  border: 1px solid var(--hair);
  border-radius: 8px;
  background: #fff;
  color: #6f6a63;
  font-size: 12px;
  outline: none;
}
.page-tools input {
  width: 52px;
  text-align: center;
}

/* ===== 悬浮新建 ===== */
.fab-btn {
  position: fixed;
  right: 44px;
  bottom: 44px;
  z-index: 70;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 17px 32px;
  border: 0;
  border-radius: 40px;
  background: var(--ink2);
  color: #fff;
  font-size: 15px;
  cursor: pointer;
  box-shadow: 0 14px 34px rgba(0, 0, 0, 0.22);
  transition: all 0.4s var(--ease);
}
.fab-btn svg {
  width: 17px;
  height: 17px;
}
.fab-btn:hover {
  transform: translateY(-5px);
  background: var(--accent);
  box-shadow: 0 18px 40px rgba(193, 62, 51, 0.35);
}

/* ===== 详情弹窗 ===== */
.detail-backdrop {
  position: fixed;
  inset: 0;
  z-index: 90;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(26, 22, 18, 0.55);
  backdrop-filter: blur(8px);
  animation: fade-in 0.25s ease;
}
.detail-modal {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 26px;
  width: min(860px, 94vw);
  max-height: 86vh;
  padding: 22px;
  border-radius: 18px;
  background: #fffdf9;
  box-shadow: 0 40px 90px rgba(0, 0, 0, 0.35);
  animation: pop-in 0.35s var(--ease);
}
.detail-modal > img {
  width: 100%;
  height: 100%;
  max-height: 70vh;
  border-radius: 12px;
  object-fit: cover;
  background: #efe8dd;
}
.detail-modal h2 {
  margin: 8px 0 6px;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 24px;
  letter-spacing: 1px;
}
.detail-modal p {
  margin: 4px 0;
  color: var(--muted2);
  font-size: 13px;
}
.detail-modal small {
  display: block;
  margin-top: 10px;
  color: #a29a90;
  font-size: 12px;
}
.detail-modal .close {
  position: absolute;
  right: 14px;
  top: 12px;
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  color: #6f665c;
  font-size: 19px;
  line-height: 1;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
.detail-actions {
  display: flex;
  gap: 10px;
  margin-top: 18px;
  flex-wrap: wrap;
}
.detail-actions button {
  padding: 9px 16px;
  border: 1px solid var(--hair);
  border-radius: 22px;
  background: transparent;
  color: #6f665c;
  font-size: 12.5px;
  cursor: pointer;
}
.detail-actions button.primary {
  border-color: transparent;
  background: var(--accent);
  color: #fff;
}

/* ===== Toast ===== */
.toast {
  position: fixed;
  left: 50%;
  bottom: 36px;
  z-index: 120;
  transform: translateX(-50%);
  padding: 11px 20px;
  border-radius: 24px;
  background: rgba(44, 42, 41, 0.92);
  color: #fff;
  font-size: 13px;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.25);
  animation: toast-in 0.3s var(--ease);
}

/* ===== 沉浸式 / 3D 舞台 ===== */
.stage-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: grid;
  place-items: center;
  padding: 40px;
  background: rgba(24, 20, 17, 0.62);
  backdrop-filter: blur(12px) saturate(0.92);
  animation: fade-in 0.3s ease;
}
.stage-overlay.closing {
  pointer-events: none;
}
.stage-body {
  display: grid;
  grid-template-columns: minmax(0, 520px) 320px;
  align-items: center;
  gap: 40px;
  width: min(1020px, 94vw);
  max-height: 88vh;
}
.stage-plate {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 5;
  max-height: 80vh;
  border-radius: 18px;
  overflow: hidden;
  background: #efe7db;
  box-shadow: 0 42px 90px rgba(0, 0, 0, 0.45);
  will-change: transform, opacity;
}
.stage-plate.is-3d {
  background: radial-gradient(circle at 50% 32%, #3d2d21 0%, #191310 72%);
}
.stage-plate-inner {
  position: absolute;
  inset: 0;
  transform: perspective(1200px) rotateX(var(--tilt-x, 0deg)) rotateY(var(--tilt-y, 0deg));
  transition: transform 0.22s ease-out;
}
.stage-plate-inner img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.stage-sheen {
  position: absolute;
  inset: -40%;
  pointer-events: none;
  background: linear-gradient(
    105deg,
    transparent 38%,
    rgba(255, 246, 226, 0.55) 48%,
    rgba(255, 255, 255, 0.18) 53%,
    transparent 62%
  );
  mix-blend-mode: soft-light;
  animation: sheen-sweep 6.5s ease-in-out infinite;
}
.stage-plate.is-3d .stage-sheen {
  opacity: 0;
  animation: none;
}
.stage-tip {
  position: absolute;
  left: 16px;
  bottom: 14px;
  padding: 6px 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(8px);
  color: rgba(255, 245, 230, 0.85);
  font-size: 11.5px;
  letter-spacing: 0.6px;
}
.stage-close {
  position: fixed;
  right: 40px;
  top: 32px;
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(10px);
  color: #fff;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  transition: all 0.3s;
}
.stage-close:hover {
  background: #fff;
  color: var(--ink2);
  transform: rotate(90deg);
}
.stage-info {
  color: #f6f1e8;
  animation: rise-in 0.6s var(--ease) 0.08s both;
}
.stage-info .eyebrow {
  display: block;
  margin-bottom: 14px;
  color: rgba(246, 241, 232, 0.5);
  font-size: 11px;
  letter-spacing: 3px;
}
.stage-info h2 {
  margin: 0 0 10px;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 34px;
  letter-spacing: 2px;
}
.stage-sub {
  margin: 0 0 22px;
  color: rgba(246, 241, 232, 0.66);
  font-size: 13px;
  letter-spacing: 0.6px;
}
.stage-info dl {
  display: grid;
  gap: 10px;
  margin: 0 0 26px;
  padding-top: 18px;
  border-top: 1px solid rgba(246, 241, 232, 0.16);
}
.stage-info dl div {
  display: flex;
  gap: 14px;
  font-size: 12.5px;
}
.stage-info dt {
  flex: 0 0 40px;
  color: rgba(246, 241, 232, 0.45);
  letter-spacing: 1px;
}
.stage-info dd {
  margin: 0;
  color: rgba(246, 241, 232, 0.88);
}
.stage-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.stage-actions button {
  padding: 10px 18px;
  border: 1px solid rgba(246, 241, 232, 0.24);
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(8px);
  color: #f6f1e8;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s var(--ease);
}
.stage-actions button:hover {
  background: #fff;
  color: var(--ink2);
  transform: translateY(-2px);
}
.stage-actions button.primary {
  border-color: transparent;
  background: var(--accent);
  color: #fff;
}
.stage-actions button.primary:hover {
  background: #d95043;
  color: #fff;
}
.stage-actions button.danger {
  border-color: rgba(255, 140, 124, 0.4);
  color: #ffb4a6;
}

@keyframes fade-in {
  from {
    opacity: 0;
  }
}
@keyframes pop-in {
  from {
    opacity: 0;
    transform: scale(0.96);
  }
}
@keyframes toast-in {
  from {
    opacity: 0;
    transform: translate(-50%, 12px);
  }
}
@keyframes rise-in {
  from {
    opacity: 0;
    transform: translateY(22px);
  }
}
@keyframes sheen-sweep {
  0%,
  100% {
    transform: translateX(-45%);
  }
  50% {
    transform: translateX(45%);
  }
}

@media (max-width: 1180px) {
  .gallery {
    grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
    gap: 20px;
  }
  .stats-area {
    gap: 38px;
  }
}
@media (max-width: 900px) {
  .patterns-wrap {
    padding: 32px 22px 110px;
  }
  .title-area h1 {
    font-size: 32px;
  }
  .stats-area {
    width: 100%;
    justify-content: space-between;
    gap: 12px;
  }
  .stat-num {
    font-size: 26px;
  }
  .stat-item {
    align-items: flex-start;
  }
  .stats-area {
    justify-content: flex-start;
    gap: 34px;
  }
  .search-box {
    width: 100%;
  }
  .toolbar-right {
    width: 100%;
    margin-left: 0;
  }
  .detail-modal {
    grid-template-columns: 1fr;
    overflow: auto;
  }
  .stage-overlay {
    padding: 18px;
    overflow-y: auto;
  }
  .stage-body {
    grid-template-columns: 1fr;
    gap: 22px;
    width: min(520px, 100%);
  }
  .stage-plate {
    max-height: 58vh;
  }
  .stage-info h2 {
    font-size: 26px;
  }
  .stage-close {
    right: 18px;
    top: 14px;
  }
  .fab-btn {
    right: 20px;
    bottom: 20px;
    padding: 14px 24px;
    font-size: 14px;
  }
}
</style>
