<script setup lang="ts">
import { onMounted, ref } from 'vue';
import ProductCard from '../../components/ProductCard.vue';
import { products as demoProducts } from '../../data';
import { api, listFrom } from '../../api';
const cat = ref('全部商品');
const products = ref(demoProducts);
onMounted(async () => {
  try {
    const list = listFrom(await api.products.list({ page: 1, pageSize: 12 }));
    if (list.length)
      products.value = list.map((p: any, i: number) => ({
        ...demoProducts[i % demoProducts.length],
        ...p,
        image:
          p.coverImage || p.mockupImage || p.imageUrl || demoProducts[i % demoProducts.length].image
      }));
  } catch {
    /* 后端未启动时保留完整演示数据 */
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
        v-for="x in [
          '全部商品',
          '帆布袋',
          '明信片',
          '丝巾',
          '杯垫',
          '摆件',
          '笔记本',
          '钥匙扣',
          '冰箱贴'
        ]"
        :key="x"
        :class="{ on: cat === x }"
        @click="cat = x"
      >
        {{ x }}
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
        <input placeholder="⌕ 搜索商品名称或关键词" /><select>
          <option>综合排序</option>
          <option>销量优先</option>
          <option>价格升序</option></select
        ><select>
          <option>全部价格</option>
          <option>0-50元</option>
          <option>50-100元</option>
        </select>
      </div>
      <div class="product-grid wide">
        <ProductCard
          v-for="p in products"
          :key="p.id"
          :p="p"
        />
      </div>
    </section>
  </div>
</template>
