<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import SectionTitle from '../../components/SectionTitle.vue';
import { api, listFrom } from '../../api';
import { courses } from '../../data';
import { downloadResource } from '../../resourceFiles';

const courseItems = ref<any[]>(courses);
const resourceItems = ref<any[]>([]);
const router = useRouter();
const keyword = ref('');
const viewMode = ref<'grid' | 'list'>('grid');
const category = ref('全部课程');
const categories = ['全部课程', '历史文化', '纹样解析', '针法基础', '创作实践'];
const categoryByIndex = ['历史文化', '纹样解析', '针法基础', '创作实践'];
const downloadingResourceId = ref<string | number | null>(null);
const resourceNotice = ref('');
const fallbackResources = [
  { id: 'local-1', title: '广绣常用针法图解手册', resourceType: 'PDF' },
  { id: 'local-2', title: '广绣经典纹样图谱', resourceType: 'PDF' },
  { id: 'local-3', title: '广绣配色参考手册', resourceType: 'PDF' },
  { id: 'local-4', title: '广绣针法演示视频', resourceType: 'VIDEO' }
];
const displayedResources = computed(() =>
  (resourceItems.value.length ? resourceItems.value : fallbackResources).slice(0, 4)
);
function fallbackCourseImage(index: number) {
  return String(
    courses[index % courses.length]?.image || '/demo/pattern/peony-phoenix-pattern-01.jpg'
  );
}
function courseImage(course: any, index: number) {
  return String(course.image || course.coverImage || course.imageUrl || fallbackCourseImage(index));
}
function useFallbackImage(event: Event, index: number) {
  const image = event.currentTarget as HTMLImageElement;
  const fallback = fallbackCourseImage(index);
  if (!image.src.endsWith(fallback)) image.src = fallback;
}
const filteredCourses = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  return courseItems.value.filter((course, index) => {
    const matchesCategory =
      category.value === '全部课程' || categoryByIndex[index % 4] === category.value;
    const text =
      `${course.title || ''} ${course.desc || course.description || course.subtitle || ''}`.toLowerCase();
    return matchesCategory && (!query || text.includes(query));
  });
});
function openCourse(course: any) {
  router.push(`/courses/${course.id}`);
}
async function startResourceDownload(resource: any, index: number) {
  downloadingResourceId.value = resource.id;
  resourceNotice.value = '';
  try {
    await downloadResource(resource, index);
    resourceNotice.value = '资源已开始下载';
  } catch {
    resourceNotice.value = '资源暂时无法下载，请稍后重试';
  } finally {
    downloadingResourceId.value = null;
  }
}
onMounted(async () => {
  const [courseResult, resourceResult] = await Promise.allSettled([
    api.courses.list({ page: 1, pageSize: 12 }),
    api.resources.list({ page: 1, pageSize: 8 })
  ]);
  if (courseResult.status === 'fulfilled') {
    const list = listFrom(courseResult.value);
    if (list.length)
      courseItems.value = list.map((course, index) => ({
        ...courses[index % courses.length],
        ...course,
        image: courseImage(course, index)
      }));
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
      <input
        v-model="keyword"
        placeholder="⌕ 搜索课程、老师或关键词"
      />
      <div class="chips">
        <button
          v-for="x in categories"
          :key="x"
          :class="{ on: category === x }"
          @click="category = x"
        >
          {{ x }}
        </button>
      </div>
    </div>
    <div class="course-heading">
      <SectionTitle
        eyebrow="CURATED COURSES"
        title="精选推荐"
        action="查看全部"
        to="/courses"
      />
      <div
        class="view-switch"
        aria-label="课程显示方式"
      >
        <button
          type="button"
          :class="{ active: viewMode === 'grid' }"
          title="网格视图"
          @click="viewMode = 'grid'"
        >
          <span aria-hidden="true">▦</span> 网格
        </button>
        <button
          type="button"
          :class="{ active: viewMode === 'list' }"
          title="列表视图"
          @click="viewMode = 'list'"
        >
          <span aria-hidden="true">☷</span> 列表
        </button>
      </div>
    </div>
    <div
      class="course-grid large"
      :class="{ 'list-view': viewMode === 'list' }"
    >
      <article
        v-for="(c, i) in filteredCourses"
        :key="c.id"
      >
        <img
          :src="courseImage(c, i)"
          :alt="c.title || '非遗课程封面'"
          @error="useFallbackImage($event, i)"
        />
        <div>
          <span>{{ i % 2 ? '初级' : '热门' }} · {{ c.lessons || c.duration }} 节</span>
          <h3>{{ c.title }}</h3>
          <p>{{ c.desc || c.description || c.subtitle }}</p>
          <button
            class="primary small"
            @click="openCourse(c)"
          >
            进入学习
          </button>
        </div>
      </article>
    </div>
    <SectionTitle
      eyebrow="LEARNING RESOURCES"
      title="创作资源 / 学习资料"
      action="更多资源"
      to="/resources"
    />
    <div class="resource-list">
      <div
        v-for="(resource, i) in displayedResources"
        :key="resource.id"
      >
        <b>{{ resource.resourceType === 'VIDEO' ? '▶' : resource.resourceType || 'PDF' }}</b
        ><span
          >{{ resource.title
          }}<small>{{ resource.subtitle || resource.resourceType || '学习资料' }}</small></span
        ><button
          class="small"
          type="button"
          :disabled="downloadingResourceId === resource.id"
          @click="startResourceDownload(resource, i)"
        >
          {{ downloadingResourceId === resource.id ? '下载中…' : '⇩ 下载' }}
        </button>
      </div>
    </div>
    <p
      v-if="resourceNotice"
      class="resource-notice"
    >
      {{ resourceNotice }}
    </p>
  </div>
</template>

<style scoped>
.course-heading {
  display: flex;
  align-items: center;
  gap: 20px;
}

.course-heading :deep(.section-title) {
  flex: 1;
}

.view-switch {
  display: inline-flex;
  flex: 0 0 auto;
  padding: 3px;
  border: 1px solid var(--line);
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.82);
}

.view-switch button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-width: 68px;
  padding: 7px 11px;
  border: 0;
  border-radius: 6px;
  color: var(--muted);
  background: transparent;
  cursor: pointer;
}

.view-switch button.active {
  color: #fff;
  background: var(--jade);
}

.course-grid.large.list-view {
  grid-template-columns: 1fr;
  gap: 12px;
}

.course-grid.large.list-view article {
  display: grid;
  grid-template-columns: 230px minmax(0, 1fr);
  min-height: 150px;
}

.course-grid.large.list-view article:hover {
  transform: translateX(3px);
}

.course-grid.large.list-view img {
  height: 100%;
  min-height: 150px;
}

.course-grid.large.list-view article > div {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  padding: 18px 24px;
}

.course-grid.large.list-view h3 {
  font-size: 20px;
}

.course-grid.large.list-view .primary {
  margin-top: 14px;
}

.resource-notice {
  margin-top: 12px;
  color: var(--jade);
  text-align: right;
}

@media (max-width: 720px) {
  .course-heading {
    align-items: flex-end;
  }

  .view-switch button {
    min-width: auto;
    padding: 7px 9px;
    font-size: 0;
  }

  .view-switch button span {
    font-size: 16px;
  }

  .course-grid.large.list-view article {
    grid-template-columns: 112px minmax(0, 1fr);
    min-height: 132px;
  }

  .course-grid.large.list-view img {
    min-height: 132px;
  }

  .course-grid.large.list-view article > div {
    padding: 13px;
  }

  .course-grid.large.list-view h3 {
    font-size: 16px;
  }

  .course-grid.large.list-view p {
    display: none;
  }
}
</style>
