<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import ProductCard from '../../components/ProductCard.vue';
import { products as demoProducts } from '../../data';
import { api, listFrom } from '../../api';
type CategoryChoice = { id: number | string; name: string };
const categoryNames = ['帆布袋', '明信片', '丝巾', '杯垫', '摆件', '笔记本', '钥匙扣', '冰箱贴'];
const cat = ref<number | string>('all');
const products = ref(demoProducts);
const keyword = ref('');
const sort = ref('综合排序');
const priceRange = ref('全部价格');
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
    const matchesCategory = cat.value === 'all'
      || (typeof selected?.id === 'number' && Number(product.categoryId) === selected.id)
      || categoryOf(product) === selected?.name;
    const text = `${product.title || product.name || ''} ${product.desc || product.description || ''}`.toLowerCase();
    const matchesKeyword = !query || text.includes(query);
    const matchesPrice = priceRange.value === '全部价格'
      || (priceRange.value === '0-50元' && price <= 50)
      || (priceRange.value === '50-100元' && price > 50 && price <= 100);
    return matchesCategory && matchesKeyword && matchesPrice;
  });
  if (sort.value === '价格升序') return [...result].sort((a, b) => Number(a.price) - Number(b.price));
  if (sort.value === '销量优先') return [...result].sort((a, b) => salesOf(b) - salesOf(a));
  return result;
});
onMounted(async () => {
  try {
    const list = listFrom(await api.products.categories());
    if (list.length) {
      categories.value = [
        { id: 'all', name: '全部商品' },
        ...list.map((item: any) => ({ id: Number(item.id), name: String(item.name) }))
      ];
    }
  } catch {
    /* 后端未启动时保留预设分类 */
  }
  try {
    const list = listFrom(await api.products.list({ page: 1, pageSize: 50 }));
    products.value = list.map((p: any, i: number) => ({
      ...p,
      title: p.name,
      desc: p.subtitle || p.description,
      image: p.coverImage || p.mockupImage || p.imageUrl || demoProducts[i % demoProducts.length].image
    }));
  } catch {
    /* 后端未启动时保留演示商品 */
  }
});
</script>

<template>
  <div class="page-head compact">
    <span>CULTURAL CREATIONS</span>
    <h1>文创好物</h1>
    <p>让千年广绣走进日常生活，每一件作品都承载岭南之美。</p>
  </div>
  <div class="shop-layout">
    <aside class="categories">
      <h3>商品分类</h3>
      <button
        v-for="x in categories"
        :key="x.id"
        :class="{ on: String(cat) === String(x.id) }"
        @click="cat = x.id"
      >
        {{ x.name }}
      </button>
    </aside>
    <section class="catalog">
      <div class="promo">
        <div>
          <span>纹样定制 · 专属好物</span>
          <h2>把喜欢的纹样，变成看得见的作品</h2>
          <RouterLink class="primary" to="/customize">立即定制</RouterLink>
        </div>
        <img
          src="/demo/product/peony-canvas-bag-mockup.png"
          alt="牡丹帆布袋定制效果"
        />
      </div>
      <div class="toolbar">
        <input v-model="keyword" placeholder="⌕ 搜索商品名称或关键词" /><select v-model="sort">
          <option>综合排序</option>
          <option>销量优先</option>
          <option>价格升序</option></select
        ><select v-model="priceRange">
          <option>全部价格</option>
          <option>0-50元</option>
          <option>50-100元</option>
        </select>
      </div>
      <div class="product-grid wide">
        <ProductCard
          v-for="p in visibleProducts"
          :key="p.id"
          :p="p"
        />
        <div v-if="!visibleProducts.length" class="empty-products">
          <b>该分类暂无商品</b>
          <span>请选择其他分类或清除搜索条件</span>
        </div>
      </div>
    </section>
  </div>
</template>
