<script setup lang="ts">
import { onMounted, ref } from 'vue';
import SectionTitle from '../../components/SectionTitle.vue';
import PatternCard from '../../components/PatternCard.vue';
import ProductCard from '../../components/ProductCard.vue';
import { api, listFrom } from '../../api';
import { courses, patterns } from '../../data';

const courseItems = ref<any[]>(courses);
const patternItems = ref<any[]>(patterns);
const productItems = ref<any[]>([]);
function fallbackCourseImage(index: number) {
  return String(courses[index % courses.length]?.image || '/demo/pattern/peony-phoenix-pattern-01.jpg');
}
function courseImage(course: any, index: number) {
  return String(course.image || course.coverImage || course.imageUrl || fallbackCourseImage(index));
}
function useCourseFallback(event: Event, index: number) {
  const image = event.currentTarget as HTMLImageElement;
  const fallback = fallbackCourseImage(index);
  if (!image.src.endsWith(fallback)) image.src = fallback;
}
onMounted(async () => {
  const [homeResult, productResult] = await Promise.allSettled([
    api.home.detail(),
    api.products.list({ page: 1, pageSize: 100 })
  ]);
  if (homeResult.status === 'fulfilled') {
    const home: any = homeResult.value;
    const remoteCourses = listFrom(home?.courses || home?.courseList || home?.recommendCourses || []);
    const remotePatterns = listFrom(
      home?.patterns || home?.patternList || home?.recommendPatterns || []
    );
    if (remoteCourses.length) courseItems.value = remoteCourses.map((course, index) => ({
      ...courses[index % courses.length],
      ...course,
      image: courseImage(course, index)
    }));
    if (remotePatterns.length) patternItems.value = remotePatterns;
  }
  if (productResult.status === 'fulfilled') {
    productItems.value = listFrom(productResult.value);
  }
});
</script>
<template>
  <div class="home-page">
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
        ['▤', '非遗课堂', '系统学习，传承匠心'],
        ['✦', 'AI纹样生成', '输入灵感，一键生成'],
        ['◈', '版权存证', '守护每一份原创'],
        ['◇', '文创商品', '纹样应用，创意变现']
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
      to="/courses"
    />
    <div class="course-grid">
      <RouterLink
        v-for="(c, i) in courseItems"
        :key="c.id"
        class="course-link"
        :to="`/courses/${c.id}`"
        :aria-label="`查看课程：${c.title || c.name}`"
      >
        <article>
          <img :src="courseImage(c, i)" :alt="c.title || '非遗课程封面'" @error="useCourseFallback($event, i)" />
          <div>
            <span>{{ c.lessons || c.duration }} 节 · 精品课</span>
            <h3>{{ c.title || c.name }}</h3>
            <p>{{ c.desc || c.description || c.subtitle }}</p>
          </div>
        </article>
      </RouterLink>
    </div>
    <SectionTitle
      eyebrow="INSPIRED BY TRADITION"
      title="灵感纹样精选"
      action="发现更多"
      to="/patterns"
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
      to="/products"
    />
    <div class="product-grid">
      <ProductCard
        v-for="p in productItems.slice(0, 4)"
        :key="p.id"
        :p="p"
      />
    </div>
  </div>
  </div>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
  overflow: hidden;
  background:
    linear-gradient(rgba(255, 252, 244, 0.16), rgba(255, 252, 244, 0.28)),
    url('/images/home-ink-landscape.png') center / cover fixed no-repeat;
}

.home-page .hero {
  background: linear-gradient(
    90deg,
    rgba(255, 250, 241, 0.9) 0%,
    rgba(255, 250, 241, 0.68) 48%,
    rgba(255, 250, 241, 0.34) 100%
  );
}

.home-page .service-strip {
  background: rgba(255, 250, 243, 0.86);
  backdrop-filter: blur(10px);
}

.home-page :deep(.course-grid article),
.home-page :deep(.pattern-card),
.home-page :deep(.product-card) {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(6px);
}

@media (max-width: 900px) {
  .home-page {
    background-position: 62% center;
    background-attachment: scroll;
  }

  .home-page .hero {
    background: rgba(255, 250, 241, 0.78);
  }
}

.course-link, .course-link article { display: block; height: 100%; }
</style>
