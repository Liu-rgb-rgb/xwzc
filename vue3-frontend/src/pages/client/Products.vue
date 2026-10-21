<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import ProductCard from '../../components/ProductCard.vue';
import CalmSelect from '../../components/CalmSelect.vue';
import { products as demoProducts } from '../../data';
import { api, listFrom } from '../../api';
type CategoryChoice = { id: number | string; name: string };
const categoryNames = ['帆布袋', '明信片', '丝巾', '杯垫', '摆件', '笔记本', '钥匙扣', '冰箱贴'];
const cat = ref<number | string>('all');
const products = ref<any[]>([]);
const keyword = ref('');
const sort = ref('综合排序');
const sortOptions = ['综合排序', '销量优先', '价格升序'];
const priceRange = ref('全部价格');
const priceRanges = ['全部价格', '100元以下', '100-300元', '300元以上'];
const loading = ref(true);
const gridRoot = ref<HTMLElement | null>(null);
let revealObserver: IntersectionObserver | null = null;
const categories = ref<CategoryChoice[]>([
  { id: 'all', name: '全部商品' },
  ...categoryNames.map((name) => ({ id: name, name }))
]);

function categoryOf(product: any) {
  return product.categoryName || product.category?.name || product.category || '';
}

function salesOf(product: any) {
  return Number(product.sales || product.salesCount || 0);
}

const visibleProducts = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  const result = products.value.filter((product: any) => {
    const price = Number(product.price || 0);
    const selected = categories.value.find((item) => String(item.id) === String(cat.value));
    const matchesCategory =
      cat.value === 'all' ||
      (typeof selected?.id === 'number' && Number(product.categoryId) === selected.id) ||
      categoryOf(product) === selected?.name;
    const text =
      `${product.title || product.name || ''} ${product.desc || product.description || ''}`.toLowerCase();
    const matchesKeyword = !query || text.includes(query);
    const matchesPrice =
      priceRange.value === '全部价格' ||
      (priceRange.value === '100元以下' && price < 100) ||
      (priceRange.value === '100-300元' && price >= 100 && price < 300) ||
      (priceRange.value === '300元以上' && price >= 300);
    return matchesCategory && matchesKeyword && matchesPrice;
  });
  if (sort.value === '价格升序')
    return [...result].sort((a, b) => Number(a.price) - Number(b.price));
  if (sort.value === '销量优先') return [...result].sort((a, b) => salesOf(b) - salesOf(a));
  return result;
});

function resetFilters() {
  cat.value = 'all';
  keyword.value = '';
  sort.value = '综合排序';
  priceRange.value = '全部价格';
}

function observeCards() {
  const root = gridRoot.value;
  if (!root) return;
  const cards = root.querySelectorAll<HTMLElement>('.grid-item:not(.skeleton)');
  if (!('IntersectionObserver' in window)) {
    cards.forEach((card) => card.classList.add('is-in'));
    return;
  }
  revealObserver ??= new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return;
        entry.target.classList.add('is-in');
        revealObserver?.unobserve(entry.target);
      });
    },
    { threshold: 0.08, rootMargin: '0px 0px -6% 0px' }
  );
  cards.forEach((card) => {
    if (!card.classList.contains('is-in')) revealObserver?.observe(card);
  });
}

watch(
  visibleProducts,
  () => {
    nextTick(observeCards);
  },
  { flush: 'post' }
);

onMounted(async () => {
  let apiCategories: CategoryChoice[] = [];
  try {
    const list = listFrom(await api.products.categories());
    apiCategories = list.map((item: any) => ({ id: Number(item.id), name: String(item.name) }));
  } catch {
    /* 后端未启动时保留预设分类 */
  }
  try {
    const list = listFrom(await api.products.list({ page: 1, pageSize: 100 }));
    products.value = list.map((p: any, i: number) => ({
      ...p,
      title: p.name,
      desc: p.subtitle || p.description,
      image:
        p.coverImage || p.mockupImage || p.imageUrl || demoProducts[i % demoProducts.length].image
    }));
  } catch {
    products.value = [];
  }
  if (apiCategories.length) {
    const usedIds = new Set(products.value.map((p: any) => Number(p.categoryId)));
    const present = apiCategories.filter(
      (item) => usedIds.has(Number(item.id)) || products.value.some((p) => categoryOf(p) === item.name)
    );
    const visibleCategories = products.value.length ? present : apiCategories;
    if (visibleCategories.length) {
      categories.value = [{ id: 'all', name: '全部商品' }, ...visibleCategories];
    }
  }
  loading.value = false;
  nextTick(observeCards);
});

onBeforeUnmount(() => {
  revealObserver?.disconnect();
  revealObserver = null;
});
</script>

<template>
  <div class="workshop-page">
    <div
      class="paper-noise"
      aria-hidden="true"
    />
    <div class="workshop-wrap">
      <section class="workshop-hero">
        <p class="eyebrow">
          <span
            class="rule"
            aria-hidden="true"
          />SMART MANUFACTURING WORKSHOP
        </p>
        <h1>智造工坊</h1>
        <p class="lede">从纹样到生产工单，每一件广绣作品都由算法打版后直达产线。</p>
      </section>

      <div class="workshop-layout">
        <aside class="workshop-side">
          <h3>商品分类</h3>
          <nav
            class="side-menu"
            aria-label="商品分类"
          >
            <button
              v-for="x in categories"
              :key="x.id"
              type="button"
              :class="{ on: String(cat) === String(x.id) }"
              @click="cat = x.id"
            >
              {{ x.name }}
            </button>
          </nav>
        </aside>

        <section class="workshop-catalog">
          <div class="workshop-bar">
            <label class="search-box">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
                aria-hidden="true"
              >
                <circle
                  cx="11"
                  cy="11"
                  r="7"
                />
                <path d="m20 20-3.6-3.6" />
              </svg>
              <input
                v-model="keyword"
                type="search"
                placeholder="搜索商品名称或关键词..."
                aria-label="搜索商品"
              />
            </label>
            <CalmSelect
              v-model="sort"
              :options="sortOptions"
              aria-label="排序方式"
            />
            <CalmSelect
              v-model="priceRange"
              :options="priceRanges"
              aria-label="价格区间"
            />
          </div>

          <div
            ref="gridRoot"
            class="workshop-grid"
          >
            <template v-if="loading">
              <div
                v-for="n in 6"
                :key="`skeleton-${n}`"
                class="grid-item skeleton"
                aria-hidden="true"
              >
                <div class="skeleton-stage" />
                <div class="skeleton-lines">
                  <span /><span /><span />
                </div>
              </div>
            </template>
            <template v-else>
              <div
                v-for="(p, i) in visibleProducts"
                :key="p.id"
                class="grid-item"
                :style="{ '--reveal-delay': `${Math.min(i, 8) * 60}ms` }"
              >
                <ProductCard :p="p" />
              </div>
              <div
                v-if="!visibleProducts.length"
                class="empty-products"
              >
                <b>没有找到匹配的商品</b>
                <span>换个关键词，或清除当前筛选条件。</span>
                <button
                  type="button"
                  @click="resetFilters"
                >
                  清除筛选
                </button>
              </div>
            </template>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.workshop-page {
  --paper: #fcfbfa;
  --ink-green: #1f3b2c;
  --muted2: #7a7a7a;
  --accent: #c13e33;
  --hair: #eae6df;
  --ease: cubic-bezier(0.2, 0.8, 0.2, 1);
  position: relative;
  min-height: 100vh;
  background: var(--paper);
  color: #333;
  font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}
.paper-noise {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  opacity: 0.6;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)' opacity='0.035'/%3E%3C/svg%3E");
}
.workshop-wrap {
  position: relative;
  z-index: 1;
  max-width: 1440px;
  margin: 0 auto;
  padding: 48px 48px 96px;
}

/* ===== 标题区 ===== */
.workshop-hero {
  margin-bottom: 48px;
  animation: hero-rise 0.8s var(--ease) both;
}
.workshop-hero .eyebrow {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 0 14px;
  color: #8d8a84;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 2px;
}
.workshop-hero .rule {
  width: 24px;
  height: 2px;
  background: var(--accent);
}
.workshop-hero h1 {
  margin: 0 0 12px;
  color: var(--ink-green);
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 42px;
  font-weight: 900;
  letter-spacing: 3px;
}
.workshop-hero .lede {
  margin: 0;
  color: var(--muted2);
  font-size: 14px;
  letter-spacing: 1px;
}
@keyframes hero-rise {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

/* ===== 左侧分类 ===== */
.workshop-layout {
  display: flex;
  align-items: flex-start;
  gap: 48px;
}
.workshop-side {
  flex: 0 0 200px;
  width: 200px;
  position: sticky;
  top: 116px;
}
.workshop-side h3 {
  margin: 6px 0 22px;
  padding-left: 16px;
  color: var(--ink-green);
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 1px;
}
.side-menu {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.side-menu button {
  display: block;
  width: 100%;
  padding: 12px 20px;
  border: 0;
  border-radius: 30px;
  background: transparent;
  color: #6b6862;
  font-size: 14px;
  letter-spacing: 0.5px;
  text-align: left;
  transition:
    background 0.35s var(--ease),
    color 0.35s var(--ease);
}
.side-menu button:hover {
  background: rgba(31, 59, 44, 0.05);
  border-color: transparent;
  color: var(--ink-green);
  transform: none;
}
.side-menu button.on {
  background: var(--ink-green);
  color: #fff;
  font-weight: 500;
  box-shadow: 0 6px 18px rgba(31, 59, 44, 0.18);
}
.side-menu button.on:hover {
  background: var(--ink-green);
  color: #fff;
}

/* ===== 右侧目录 ===== */
.workshop-catalog {
  flex: 1;
  min-width: 0;
}
.workshop-bar {
  display: flex;
  gap: 14px;
  margin-bottom: 30px;
}
.search-box {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
}
.search-box svg {
  position: absolute;
  left: 18px;
  width: 16px;
  height: 16px;
  color: #aaa49c;
  pointer-events: none;
}
.search-box input {
  width: 100%;
  padding: 14px 20px 14px 44px;
  border: 1px solid var(--hair);
  border-radius: 12px;
  background: #fff;
  color: var(--ink-green);
  font-size: 14px;
  outline: none;
  transition:
    border-color 0.3s,
    box-shadow 0.3s;
}
.search-box input::placeholder {
  color: #aaa49c;
}
.search-box input:focus {
  border-color: var(--ink-green);
  box-shadow: 0 0 0 3px rgba(31, 59, 44, 0.08);
}
.search-box input::-webkit-search-cancel-button {
  cursor: pointer;
}
.calm-select {
  flex: 0 0 168px;
  width: 168px;
}

/* ===== 商品网格 ===== */
.workshop-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 30px;
}
.grid-item {
  display: flex;
  min-width: 0;
  opacity: 0;
  transform: translateY(18px);
}
.grid-item.is-in {
  opacity: 1;
  transform: none;
  transition:
    opacity 0.7s var(--ease) var(--reveal-delay, 0ms),
    transform 0.7s var(--ease) var(--reveal-delay, 0ms);
}
.grid-item > * {
  width: 100%;
}

/* 骨架屏 */
.grid-item.skeleton {
  flex-direction: column;
  opacity: 1;
  transform: none;
  overflow: hidden;
  border: 1px solid rgba(31, 59, 44, 0.05);
  border-radius: 16px;
  background: #fff;
}
.skeleton-stage {
  aspect-ratio: 1 / 1;
  background: linear-gradient(100deg, #f4f2ee 20%, #fbfaf8 42%, #f4f2ee 64%);
  background-size: 220% 100%;
  animation: shimmer 1.6s ease-in-out infinite;
}
.skeleton-lines {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 22px 24px 26px;
}
.skeleton-lines span {
  height: 12px;
  border-radius: 6px;
  background: linear-gradient(100deg, #f4f2ee 20%, #fbfaf8 42%, #f4f2ee 64%);
  background-size: 220% 100%;
  animation: shimmer 1.6s ease-in-out infinite;
}
.skeleton-lines span:nth-child(1) {
  width: 62%;
  height: 18px;
}
.skeleton-lines span:nth-child(3) {
  width: 38%;
}
@keyframes shimmer {
  from {
    background-position: 120% 0;
  }
  to {
    background-position: -20% 0;
  }
}

/* 空状态 */
.empty-products {
  grid-column: 1 / -1;
  min-height: 280px;
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 10px;
  padding: 40px;
  text-align: center;
  color: #8d8a84;
  border: 1px dashed #e3ddd4;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.6);
}
.empty-products b {
  color: var(--ink-green);
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 20px;
  font-weight: 700;
}
.empty-products span {
  font-size: 13px;
}
.empty-products button {
  margin-top: 8px;
  padding: 10px 22px;
  border: 1px solid var(--hair);
  border-radius: 999px;
  background: #fff;
  color: var(--ink-green);
  font-size: 13px;
}
.empty-products button:hover {
  border-color: var(--ink-green);
  transform: none;
}

/* ===== 响应式 ===== */
@media (max-width: 1180px) {
  .workshop-layout {
    gap: 32px;
  }
  .workshop-side {
    flex-basis: 168px;
    width: 168px;
  }
  .workshop-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 900px) {
  .workshop-wrap {
    padding: 36px 24px 72px;
  }
  .workshop-layout {
    flex-direction: column;
    gap: 24px;
  }
  .workshop-side {
    position: static;
    flex-basis: auto;
    width: 100%;
  }
  .workshop-side h3 {
    display: none;
  }
  .side-menu {
    flex-direction: row;
    gap: 8px;
    overflow-x: auto;
    padding-bottom: 4px;
    scrollbar-width: none;
  }
  .side-menu::-webkit-scrollbar {
    display: none;
  }
  .side-menu button {
    flex: 0 0 auto;
    width: auto;
    padding: 10px 18px;
    white-space: nowrap;
  }
  .workshop-bar {
    flex-wrap: wrap;
  }
  .search-box {
    flex-basis: 100%;
  }
  .calm-select {
    flex: 1;
    width: auto;
    min-width: 132px;
  }
}
@media (max-width: 640px) {
  .workshop-wrap {
    padding: 28px 16px 64px;
  }
  .workshop-hero h1 {
    font-size: 34px;
    letter-spacing: 2px;
  }
  .workshop-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .workshop-hero,
  .skeleton-stage,
  .skeleton-lines span {
    animation: none;
  }
  .grid-item {
    opacity: 1;
    transform: none;
  }
}
</style>
