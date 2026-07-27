<script setup lang="ts">
import { onMounted, ref } from 'vue';
import SectionTitle from '../components/SectionTitle.vue';
import { api, listFrom } from '../api';
import { courses } from '../data';

const courseItems = ref<any[]>(courses);
const resourceItems = ref<any[]>([]);
onMounted(async () => {
  const [courseResult, resourceResult] = await Promise.allSettled([
    api.courses.list({ page: 1, pageSize: 12 }),
    api.resources.list({ page: 1, pageSize: 8 })
  ]);
  if (courseResult.status === 'fulfilled') {
    const list = listFrom(courseResult.value);
    if (list.length) courseItems.value = list;
  }
  if (resourceResult.status === 'fulfilled') resourceItems.value = listFrom(resourceResult.value);
});
</script>
<template>
  <div class="academy-hero">
    <div>
      <span>HERITAGE ACADEMY</span>
      <h1>非遗课堂</h1>
      <p>从一针一线开始，系统学习广绣知识与技法。</p>
      <div class="academy-stats">
        <b>128+<small>精品课程</small></b
        ><b>36+<small>非遗传承人</small></b
        ><b>5868+<small>学习者</small></b>
      </div>
    </div>
    <img src="/demo/product/round-coaster-set-cover.jpg" />
  </div>
  <div class="content">
    <div class="toolbar">
      <input placeholder="⌕ 搜索课程、老师或关键词" />
      <div class="chips">
        <button class="on">全部课程</button><button>历史文化</button><button>纹样解析</button
        ><button>针法基础</button><button>创作实践</button>
      </div>
    </div>
    <SectionTitle
      eyebrow="CURATED COURSES"
      title="精选推荐"
      action="查看全部"
    />
    <div class="course-grid large">
      <article
        v-for="(c, i) in [...courseItems, ...courseItems]"
        :key="i"
      >
        <img :src="c.image || c.coverImage" />
        <div>
          <span>{{ i % 2 ? '初级' : '热门' }} · {{ c.lessons || c.duration }} 节</span>
          <h3>{{ c.title }}</h3>
          <p>{{ c.desc || c.description || c.subtitle }}</p>
          <button class="primary small">进入学习</button>
        </div>
      </article>
    </div>
    <SectionTitle
      eyebrow="LEARNING RESOURCES"
      title="创作资源 / 学习资料"
      action="更多资源"
    />
    <div class="resource-list">
      <div
        v-for="(x, i) in [
          '广绣常用针法图解手册',
          '广绣经典纹样图谱',
          '广绣配色参考手册',
          '广绣针法演示视频'
        ]"
      >
        <b>{{ i === 3 ? '▶' : 'PDF' }}</b
        ><span
          >{{ x }}<small>{{ i === 3 ? 'MP4 · 256MB' : 'PDF · 12.8MB' }}</small></span
        ><button class="small">⇩ 下载</button>
      </div>
    </div>
  </div>
</template>
