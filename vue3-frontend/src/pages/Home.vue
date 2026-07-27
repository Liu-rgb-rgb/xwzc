<script setup lang="ts">
import { onMounted, ref } from 'vue';
import SectionTitle from '../components/SectionTitle.vue';
import PatternCard from '../components/PatternCard.vue';
import ProductCard from '../components/ProductCard.vue';
import { api, listFrom } from '../api';
import { courses, patterns, products } from '../data';

const courseItems = ref<any[]>(courses);
const patternItems = ref<any[]>(patterns);
const productItems = ref<any[]>(products);
onMounted(async () => {
  try {
    const home: any = await api.home.detail();
    const remoteCourses = listFrom(home?.courses || home?.courseList || []);
    const remotePatterns = listFrom(home?.patterns || home?.patternList || []);
    const remoteProducts = listFrom(home?.products || home?.productList || []);
    if (remoteCourses.length) courseItems.value = remoteCourses;
    if (remotePatterns.length) patternItems.value = remotePatterns;
    if (remoteProducts.length) productItems.value = remoteProducts;
  } catch {}
});
</script>
<template>
  <section class="hero">
    <div class="hero-copy">
      <span class="eyebrow">国家级非物质文化遗产 · 广绣</span>
      <h1>让岭南绣艺<br /><em>在数字时代新生</em></h1>
      <p>以 AI 技术连接传统针法与现代设计，让每一份灵感都能成为独一无二的纹样与文创作品。</p>
      <div class="hero-buttons">
        <RouterLink
          class="primary"
          to="/generate"
          >开始创作</RouterLink
        ><RouterLink to="/courses">探索非遗</RouterLink>
      </div>
      <div class="hero-stats">
        <span><b>12,860+</b>原创纹样</span><span><b>6,280+</b>创作者</span
        ><span><b>128</b>精品课程</span>
      </div>
    </div>
    <div class="hero-art">
      <img :src="patternItems[0].image" />
      <div class="art-label"><span>今日灵感</span><b>牡丹呈祥 · 凤舞岭南</b></div>
    </div>
  </section>
  <section class="service-strip">
    <div
      v-for="x in [
        ['✦', 'AI纹样生成', '输入灵感，一键生成'],
        ['◇', '文创商品', '纹样应用，创意变现'],
        ['▤', '非遗课堂', '系统学习，传承匠心'],
        ['◈', '版权存证', '守护每一份原创']
      ]"
      :key="x[1]"
    >
      <i>{{ x[0] }}</i
      ><span
        ><b>{{ x[1] }}</b
        ><small>{{ x[2] }}</small></span
      >
    </div>
  </section>
  <div class="content">
    <SectionTitle
      eyebrow="HERITAGE ACADEMY"
      title="热门非遗课程"
      action="查看全部"
    />
    <div class="course-grid">
      <article
        v-for="c in courseItems"
        :key="c.id"
      >
        <img :src="c.image || c.coverImage" />
        <div>
          <span>{{ c.lessons || c.duration }} 节 · 精品课</span>
          <h3>{{ c.title }}</h3>
          <p>{{ c.desc || c.description || c.subtitle }}</p>
        </div>
      </article>
    </div>
    <SectionTitle
      eyebrow="INSPIRED BY TRADITION"
      title="灵感纹样精选"
      action="发现更多"
    />
    <div class="pattern-grid">
      <PatternCard
        v-for="p in patternItems.slice(0, 5)"
        :key="p.id"
        :p="p"
      />
    </div>
    <SectionTitle
      eyebrow="CULTURAL CREATIONS"
      title="把广绣带进日常"
      action="全部商品"
    />
    <div class="product-grid">
      <ProductCard
        v-for="p in productItems.slice(0, 4)"
        :key="p.id"
        :p="p"
      />
    </div>
  </div>
</template>
