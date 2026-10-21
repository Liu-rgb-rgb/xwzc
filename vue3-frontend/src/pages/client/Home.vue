<script setup lang="ts">
import { onMounted, ref } from 'vue';
import SectionTitle from '../../components/SectionTitle.vue';
import PatternCard from '../../components/PatternCard.vue';
import { api, listFrom } from '../../api';
import { courses, patterns } from '../../data';

const courseItems = ref<any[]>(courses);
const patternItems = ref<any[]>(patterns);
function fallbackCourseImage(index: number) {
  return String(
    courses[index % courses.length]?.image || '/demo/pattern/peony-phoenix-pattern-01.jpg'
  );
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
  try {
    const home: any = await api.home.detail();
    const remoteCourses = listFrom(
      home?.courses || home?.courseList || home?.recommendCourses || []
    );
    const remotePatterns = listFrom(
      home?.patterns || home?.patternList || home?.recommendPatterns || []
    );
    if (remoteCourses.length)
      courseItems.value = remoteCourses.map((course, index) => ({
        ...courses[index % courses.length],
        ...course,
        image: courseImage(course, index)
      }));
    if (remotePatterns.length) patternItems.value = remotePatterns;
  } catch {
    /* 后端未启动时保留本地演示数据 */
  }
});
</script>
<template>
  <div class="home-page">
    <section class="hero">
      <div class="hero-copy">
        <span class="eyebrow">国家级非物质文化遗产 · 广绣 · 工业级数字打版</span>
        <h1>AI非遗纹样<br /><em>智造引擎</em></h1>
        <p>
          用户生成创意，算法自动将其转化为工业级 DST
          文件，直接对接生产端。从灵感针法到绣花机排产，一个平台闭环完成。
        </p>
        <div class="hero-buttons">
          <RouterLink
            class="primary"
            to="/generate"
            >生成纹样</RouterLink
          ><RouterLink to="/courses">探索非遗</RouterLink>
        </div>
        <div class="hero-stats">
          <span><b>12,860+</b>原创纹样</span><span><b>38,000+</b>工业级 DST 打版</span
          ><span><b>128</b>合作工厂</span>
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
          ['①', '生成纹样', 'AI 一键生成非遗纹样'],
          ['②', '一键打版', '工业级 DST 文件，针脚路径可下载'],
          ['③', '提交给工厂', '生产工单自动生成，直达产线']
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
        title="非遗精品课程"
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
            <img
              :src="courseImage(c, i)"
              :alt="c.title || '非遗课程封面'"
              @error="useCourseFallback($event, i)"
            />
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

.course-link,
.course-link article {
  display: block;
  height: 100%;
}
</style>
