<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { api, listFrom } from '../../api';
import { courses } from '../../data';
import { downloadResource } from '../../resourceFiles';

const courseItems = ref<any[]>(courses);
const router = useRouter();
const keyword = ref('');
const viewMode = ref<'grid' | 'list'>('grid');
const category = ref('全部课程');
const categories = ['全部课程', '历史文化', '纹样解析', '针法基础', '创作实践'];
const categoryByIndex = ['历史文化', '纹样解析', '针法基础', '创作实践'];
const catalogRoot = ref<HTMLElement | null>(null);
const theaterRoot = ref<HTMLElement | null>(null);
const gridRoot = ref<HTMLElement | null>(null);
const promoOpen = ref(false);
const promoVideo = ref<HTMLVideoElement | null>(null);
const heroVideoFailed = ref(false);
const activeFeature = ref(0);
const carouselPaused = ref(false);
const progressKey = ref(0);
let carouselTimer: number | undefined;
let revealObserver: IntersectionObserver | null = null;
const reducedMotion =
  typeof window !== 'undefined' &&
  !!window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
const HERO_VIDEO = 'https://xwzc.oss-cn-beijing.aliyuncs.com/1.1.mp4';

const resourceItems = ref<any[]>([]);
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
const featuredCourses = computed(() => courseItems.value.slice(0, 5));
const heroPoster = computed(() => courseImage(courseItems.value[0] || courses[0], 0));
const sideFeatures = computed(() => {
  const list = featuredCourses.value;
  if (list.length < 2) return [];
  return Array.from({ length: Math.min(2, list.length - 1) }, (_, offset) => {
    const index = (activeFeature.value + offset + 1) % list.length;
    return { course: list[index], index };
  });
});

function openCourse(course: any) {
  router.push(`/courses/${course.id}`);
}
function courseTag(index: number) {
  return categoryByIndex[index % 4];
}
function nextFeature(step = 1) {
  const total = featuredCourses.value.length;
  if (!total) return;
  activeFeature.value = (activeFeature.value + step + total) % total;
}
function selectFeature(index: number) {
  activeFeature.value = index % Math.max(1, featuredCourses.value.length);
}
function stopCarousel() {
  carouselPaused.value = true;
  if (carouselTimer) window.clearInterval(carouselTimer);
  carouselTimer = undefined;
}
function startCarousel() {
  if (carouselTimer) window.clearInterval(carouselTimer);
  carouselTimer = undefined;
  carouselPaused.value = false;
  progressKey.value += 1;
  if (reducedMotion || featuredCourses.value.length < 2) return;
  carouselTimer = window.setInterval(() => nextFeature(1), 6000);
}
function previewVideoOf(target: EventTarget | null) {
  const root = target as HTMLElement | null;
  if (!root || reducedMotion) return null;
  return (root.querySelector('.theater-slide.active video') ||
    root.querySelector('video')) as HTMLVideoElement | null;
}
function playPreview(event: Event) {
  const video = previewVideoOf(event.currentTarget);
  if (!video) return;
  video.currentTime = 0;
  video.play().catch(() => {});
}
function pausePreview(event: Event) {
  const video = previewVideoOf(event.currentTarget);
  if (!video) return;
  video.pause();
  video.currentTime = 0;
}
function openPromo() {
  promoOpen.value = true;
  document.body.style.overflow = 'hidden';
  nextTick(() => promoVideo.value?.play().catch(() => {}));
}
function closePromo() {
  promoOpen.value = false;
  document.body.style.overflow = '';
  if (promoVideo.value) {
    promoVideo.value.pause();
    promoVideo.value.currentTime = 0;
  }
}
function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && promoOpen.value) closePromo();
}
function scrollToCatalog() {
  catalogRoot.value?.scrollIntoView({
    behavior: reducedMotion ? 'auto' : 'smooth',
    block: 'start'
  });
}
function tiltMove(event: PointerEvent) {
  if (reducedMotion) return;
  const el = event.currentTarget as HTMLElement;
  const rect = el.getBoundingClientRect();
  if (!rect.width || !rect.height) return;
  const px = (event.clientX - rect.left) / rect.width - 0.5;
  const py = (event.clientY - rect.top) / rect.height - 0.5;
  el.style.setProperty('--tilt-x', `${(-py * 2.6).toFixed(2)}deg`);
  el.style.setProperty('--tilt-y', `${(px * 2.6).toFixed(2)}deg`);
}
function tiltReset(event: PointerEvent) {
  const el = event.currentTarget as HTMLElement;
  el.style.setProperty('--tilt-x', '0deg');
  el.style.setProperty('--tilt-y', '0deg');
}
function leaveCard(event: PointerEvent) {
  tiltReset(event);
  pausePreview(event);
}
function observeReveals() {
  const roots = [theaterRoot.value, gridRoot.value].filter(Boolean) as HTMLElement[];
  const items = roots.flatMap((root) => [...root.querySelectorAll<HTMLElement>('.reveal')]);
  if (!items.length) return;
  if (reducedMotion || !('IntersectionObserver' in window)) {
    items.forEach((item) => item.classList.add('is-in'));
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
    { threshold: 0.08, rootMargin: '0px 0px -5% 0px' }
  );
  items.forEach((item, index) => {
    if (item.classList.contains('is-in')) return;
    item.style.setProperty('--reveal-delay', `${Math.min(index, 6) * 55}ms`);
    revealObserver?.observe(item);
  });
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

watch(filteredCourses, () => nextTick(observeReveals), { flush: 'post' });
watch(
  () => featuredCourses.value.length,
  (length) => {
    if (activeFeature.value >= length) activeFeature.value = 0;
    startCarousel();
  }
);

onMounted(async () => {
  document.addEventListener('keydown', handleKeydown);
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
  await nextTick();
  observeReveals();
  startCarousel();
});

onBeforeUnmount(() => {
  stopCarousel();
  revealObserver?.disconnect();
  revealObserver = null;
  document.removeEventListener('keydown', handleKeydown);
  document.body.style.overflow = '';
});
</script>

<template>
  <div class="aca-page">
    <div class="aca-wrap">
      <!-- 首屏：左文案 + 右宣传片 -->
      <section class="aca-hero">
        <div class="aca-hero-copy">
          <p class="aca-eyebrow">
            <span
              class="aca-rule"
              aria-hidden="true"
            />HERITAGE ACADEMY
          </p>
          <h1>非遗课堂</h1>
          <p class="aca-lede">
            从一针一线开始，系统学习广绣知识与技法。高清影像记录，捕捉每一丝色彩的流转。
          </p>
          <div class="aca-stats">
            <div>
              <b>128<span>+</span></b>
              <small>精品课程</small>
            </div>
            <div>
              <b class="ink">36<span>+</span></b>
              <small>非遗传承人</small>
            </div>
            <div>
              <b class="ink">5868<span>+</span></b>
              <small>学习者</small>
            </div>
          </div>
          <button
            class="aca-promo-btn"
            type="button"
            @click="openPromo"
          >
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <circle
                cx="12"
                cy="12"
                r="8.5"
              />
              <path d="M10.2 8.8 15.4 12l-5.2 3.2Z" />
            </svg>
            观看宣传片
          </button>
        </div>
        <button
          class="aca-hero-media"
          type="button"
          aria-label="观看宣传片"
          @click="openPromo"
        >
          <video
            v-if="!heroVideoFailed"
            :autoplay="!reducedMotion"
            muted
            loop
            playsinline
            :poster="heroPoster"
            :src="HERO_VIDEO"
            @error="heroVideoFailed = true"
          ></video>
          <img
            v-else
            :src="heroPoster"
            alt=""
          />
          <span
            class="aca-hero-shade"
            aria-hidden="true"
          />
          <span class="aca-hero-tag">广绣宣传片</span>
        </button>
      </section>

      <!-- 精选剧场：1 大 + 2 小，轮播 -->
      <section
        ref="theaterRoot"
        class="aca-theater"
        aria-label="精选推荐"
      >
        <div class="aca-section-head reveal">
          <div>
            <p class="aca-eyebrow">
              <span
                class="aca-rule"
                aria-hidden="true"
              />CURATED COURSES
            </p>
            <h2>精选推荐</h2>
          </div>
          <button
            class="aca-link"
            type="button"
            @click="scrollToCatalog"
          >
            查看全部 →
          </button>
        </div>

        <div
          class="aca-theater-grid"
          @mouseenter="stopCarousel"
          @mouseleave="startCarousel"
        >
          <div class="aca-stage-slot reveal">
            <div
              class="aca-stage"
              @pointermove="tiltMove"
              @pointerenter="playPreview"
              @pointerleave="leaveCard"
            >
              <article
                v-for="(course, index) in featuredCourses"
                :key="course.id"
                class="theater-slide"
                :class="{ active: index === activeFeature }"
                @click="openCourse(course)"
              >
                <img
                  :src="courseImage(course, index)"
                  :alt="course.title || '课程封面'"
                  @error="useFallbackImage($event, index)"
                />
                <video
                  v-if="!reducedMotion && course.videoUrl"
                  muted
                  loop
                  playsinline
                  preload="metadata"
                  :poster="courseImage(course, index)"
                  :src="course.videoUrl"
                ></video>
                <span
                  class="theater-shade"
                  aria-hidden="true"
                />
                <div class="theater-caption">
                  <span class="theater-badge">{{ courseTag(index) }}</span>
                  <h3>{{ course.title }}</h3>
                  <p>{{ course.desc || course.description || course.subtitle }}</p>
                  <span class="theater-meta">进入学习 →</span>
                </div>
              </article>

              <button
                class="theater-arrow prev"
                type="button"
                aria-label="上一门课程"
                @click.stop="nextFeature(-1)"
              >
                ‹
              </button>
              <button
                class="theater-arrow next"
                type="button"
                aria-label="下一门课程"
                @click.stop="nextFeature(1)"
              >
                ›
              </button>

              <div
                class="theater-dots"
                @click.stop
              >
                <button
                  v-for="(course, index) in featuredCourses"
                  :key="course.id"
                  type="button"
                  :class="{ on: index === activeFeature }"
                  :aria-label="`切换到${course.title}`"
                  @click="selectFeature(index)"
                />
              </div>
              <span
                v-if="!reducedMotion && featuredCourses.length > 1"
                :key="`${activeFeature}-${progressKey}`"
                class="theater-progress"
                :class="{ paused: carouselPaused }"
                aria-hidden="true"
              />
            </div>
          </div>

          <div class="aca-sides">
            <div
              v-for="item in sideFeatures"
              :key="item.course.id"
              class="side-slot"
            >
              <article
                class="side-card"
                @click="selectFeature(item.index)"
                @pointermove="tiltMove"
                @pointerenter="playPreview"
                @pointerleave="leaveCard"
              >
                <img
                  :src="courseImage(item.course, item.index)"
                  :alt="item.course.title || '课程封面'"
                  @error="useFallbackImage($event, item.index)"
                />
                <video
                  v-if="!reducedMotion && item.course.videoUrl"
                  muted
                  loop
                  playsinline
                  preload="metadata"
                  :poster="courseImage(item.course, item.index)"
                  :src="item.course.videoUrl"
                ></video>
                <span
                  class="side-shade"
                  aria-hidden="true"
                />
                <div class="side-caption">
                  <h4>{{ item.course.title }}</h4>
                </div>
                <span
                  class="side-play"
                  aria-hidden="true"
                >
                  <svg
                    viewBox="0 0 24 24"
                    fill="currentColor"
                  >
                    <path d="M8 5.5v13l11-6.5Z" />
                  </svg>
                </span>
              </article>
            </div>
          </div>
        </div>
      </section>

      <!-- 全部课程：筛选 + 网格/列表 -->
      <section
        ref="catalogRoot"
        class="aca-catalog"
      >
        <div class="aca-toolbar">
          <label class="aca-search">
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
              placeholder="搜索课程、老师或关键词"
              aria-label="搜索课程"
            />
          </label>
          <div class="aca-chips">
            <button
              v-for="item in categories"
              :key="item"
              type="button"
              :class="{ on: category === item }"
              @click="category = item"
            >
              {{ item }}
            </button>
          </div>
        </div>

        <div class="aca-section-head">
          <div>
            <p class="aca-eyebrow">
              <span
                class="aca-rule"
                aria-hidden="true"
              />ALL COURSES
            </p>
            <h2>全部课程</h2>
          </div>
          <div
            class="aca-view-switch"
            aria-label="课程显示方式"
          >
            <button
              type="button"
              :class="{ active: viewMode === 'grid' }"
              title="网格视图"
              @click="viewMode = 'grid'"
            >
              <svg
                viewBox="0 0 16 16"
                aria-hidden="true"
              >
                <rect
                  x="1.5"
                  y="1.5"
                  width="5.2"
                  height="5.2"
                  rx="1.2"
                />
                <rect
                  x="9.3"
                  y="1.5"
                  width="5.2"
                  height="5.2"
                  rx="1.2"
                />
                <rect
                  x="1.5"
                  y="9.3"
                  width="5.2"
                  height="5.2"
                  rx="1.2"
                />
                <rect
                  x="9.3"
                  y="9.3"
                  width="5.2"
                  height="5.2"
                  rx="1.2"
                />
              </svg>
              网格
            </button>
            <button
              type="button"
              :class="{ active: viewMode === 'list' }"
              title="列表视图"
              @click="viewMode = 'list'"
            >
              <svg
                viewBox="0 0 16 16"
                aria-hidden="true"
              >
                <path
                  d="M2 3.5h12M2 8h12M2 12.5h12"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.6"
                  stroke-linecap="round"
                />
              </svg>
              列表
            </button>
          </div>
        </div>

        <div
          ref="gridRoot"
          class="aca-grid"
          :class="{ 'is-list': viewMode === 'list' }"
        >
          <article
            v-for="(course, index) in filteredCourses"
            :key="course.id"
            class="aca-course reveal"
            tabindex="0"
            @click="openCourse(course)"
            @keydown.enter="openCourse(course)"
          >
            <div class="aca-course-card">
              <div class="aca-course-media">
                <img
                  :src="courseImage(course, index)"
                  :alt="course.title || '非遗课程封面'"
                  loading="lazy"
                  @error="useFallbackImage($event, index)"
                />
                <span class="aca-course-tag">{{ courseTag(index) }}</span>
                <span
                  class="aca-course-play"
                  aria-hidden="true"
                >
                  <svg
                    viewBox="0 0 24 24"
                    fill="currentColor"
                  >
                    <path d="M8 5.5v13l11-6.5Z" />
                  </svg>
                </span>
              </div>
              <div class="aca-course-body">
                <h3>{{ course.title }}</h3>
                <p>{{ course.desc || course.description || course.subtitle }}</p>
                <div class="aca-course-foot">
                  <b>进入学习 →</b>
                </div>
              </div>
            </div>
          </article>
          <div
            v-if="!filteredCourses.length"
            class="aca-empty"
          >
            <b>没有找到匹配的课程</b>
            <span>换个关键词，或切换分类看看。</span>
          </div>
        </div>
      </section>
    </div>

    <!-- 宣传片弹层 -->
    <Teleport to="body">
      <div
        v-if="promoOpen"
        class="aca-promo-modal"
        role="dialog"
        aria-modal="true"
        aria-label="广绣宣传片"
        @click.self="closePromo"
      >
        <div class="aca-promo-box">
          <button
            class="aca-promo-close"
            type="button"
            aria-label="关闭宣传片"
            @click="closePromo"
          >
            ×
          </button>
          <video
            ref="promoVideo"
            :src="HERO_VIDEO"
            :poster="heroPoster"
            controls
            autoplay
            playsinline
          ></video>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.aca-page {
  --bg: #f6f4f0;
  --ink: #2d2a26;
  --red: #8a2b2b;
  --jade: #0f5f55;
  --muted: #7c7873;
  --line: #e3ded6;
  --ease: cubic-bezier(0.2, 0.8, 0.2, 1);
  min-height: 100vh;
  background: var(--bg);
  color: var(--ink);
  font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}
.aca-wrap {
  max-width: 1500px;
  margin: 0 auto;
  padding: 32px clamp(20px, 4vw, 40px) 96px;
}

/* ===== 通用 ===== */
.aca-eyebrow {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 0 12px;
  color: var(--red);
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 2.4px;
}
.aca-rule {
  width: 28px;
  height: 1px;
  background: var(--red);
}
.aca-section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin: 0 0 26px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--line);
}
.aca-section-head h2 {
  margin: 0;
  color: var(--ink);
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 30px;
  font-weight: 600;
  letter-spacing: 2px;
}
.aca-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--muted);
  font-size: 12px;
  letter-spacing: 1.5px;
  transition: color 0.3s;
}
.aca-link:hover {
  border-color: transparent;
  color: var(--ink);
  transform: none;
}

/* ===== 首屏 ===== */
.aca-hero {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.05fr);
  gap: clamp(36px, 5vw, 72px);
  align-items: center;
  margin-bottom: 76px;
  padding-top: 18px;
}
.aca-hero-copy h1 {
  margin: 0 0 18px;
  color: var(--ink);
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: clamp(44px, 5vw, 64px);
  font-weight: 500;
  letter-spacing: 6px;
  line-height: 1.12;
}
.aca-lede {
  max-width: 34em;
  margin: 0 0 34px;
  color: var(--muted);
  font-size: 15px;
  line-height: 1.95;
  letter-spacing: 0.5px;
}
.aca-stats {
  display: flex;
  gap: 44px;
  margin-bottom: 34px;
}
.aca-stats b {
  display: block;
  color: var(--red);
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 32px;
  font-weight: 600;
  line-height: 1;
}
.aca-stats b span {
  font-size: 20px;
}
.aca-stats b.ink {
  color: var(--ink);
}
.aca-stats small {
  display: block;
  margin-top: 8px;
  color: #a39d95;
  font-size: 12px;
  letter-spacing: 1px;
}
.aca-promo-btn {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 13px 26px;
  border: 1px solid var(--ink);
  border-radius: 999px;
  background: transparent;
  color: var(--ink);
  font-size: 13px;
  letter-spacing: 2px;
  transition:
    background 0.35s var(--ease),
    color 0.35s var(--ease);
}
.aca-promo-btn svg {
  width: 17px;
  height: 17px;
}
.aca-promo-btn:hover {
  border-color: var(--ink);
  background: var(--ink);
  color: #fff;
  transform: none;
}

.aca-hero-media {
  position: relative;
  display: block;
  width: 100%;
  aspect-ratio: 4 / 3;
  padding: 0;
  overflow: hidden;
  border: 0;
  border-radius: 30px;
  background: #eae7df;
  box-shadow: 0 32px 70px rgba(45, 42, 38, 0.16);
  cursor: pointer;
}
.aca-hero-media video,
.aca-hero-media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 1.2s var(--ease);
}
.aca-hero-media:hover video {
  transform: scale(1.03);
}
.aca-hero-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(20, 18, 16, 0.42), rgba(20, 18, 16, 0.04) 55%);
  pointer-events: none;
}
.aca-hero-tag {
  position: absolute;
  left: 26px;
  bottom: 24px;
  padding: 7px 14px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(8px);
  color: #fff;
  font-size: 12px;
  letter-spacing: 2px;
}

/* ===== 精选剧场 ===== */
.aca-theater {
  margin-bottom: 66px;
}
.aca-theater-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  gap: 22px;
  align-items: stretch;
}
.aca-stage-slot {
  min-width: 0;
}
.aca-stage {
  position: relative;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  border-radius: 28px;
  background: #eae7df;
  box-shadow: 0 26px 60px rgba(45, 42, 38, 0.14);
  transform: perspective(1400px) rotateX(var(--tilt-x, 0deg)) rotateY(var(--tilt-y, 0deg));
  transition: transform 0.5s var(--ease);
}
.theater-slide {
  position: absolute;
  inset: 0;
  opacity: 0;
  pointer-events: none;
  cursor: pointer;
  transition: opacity 0.9s ease;
}
.theater-slide.active {
  opacity: 1;
  pointer-events: auto;
}
.theater-slide img,
.theater-slide video {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.theater-slide img {
  transform: scale(1.02);
  transition: transform 7s linear;
}
.theater-slide.active img {
  transform: scale(1.08);
}
.theater-slide video {
  z-index: 1;
  opacity: 0;
  transition: opacity 0.45s ease;
}
.theater-slide.active:hover video {
  opacity: 1;
}
.theater-shade {
  position: absolute;
  inset: 0;
  z-index: 2;
  background: linear-gradient(to top, rgba(18, 16, 14, 0.78), rgba(18, 16, 14, 0) 58%);
  pointer-events: none;
}
.theater-caption {
  position: absolute;
  right: 34px;
  bottom: 30px;
  left: 34px;
  z-index: 3;
  color: #fff;
  pointer-events: none;
}
.theater-badge {
  display: inline-block;
  margin-bottom: 12px;
  padding: 5px 12px;
  border-radius: 999px;
  background: var(--red);
  font-size: 10px;
  letter-spacing: 2px;
}
.theater-caption h3 {
  margin: 0 0 8px;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: clamp(22px, 2.3vw, 30px);
  font-weight: 600;
  letter-spacing: 1.5px;
}
.theater-caption p {
  max-width: 46em;
  margin: 0 0 12px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
  line-height: 1.7;
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}
.theater-meta {
  color: rgba(255, 255, 255, 0.58);
  font-size: 11px;
  letter-spacing: 1.5px;
}
.theater-arrow {
  position: absolute;
  top: 50%;
  z-index: 4;
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.36);
  border-radius: 50%;
  background: rgba(25, 22, 19, 0.28);
  backdrop-filter: blur(6px);
  color: #fff;
  font-size: 20px;
  line-height: 1;
  opacity: 0;
  transform: translateY(-50%);
  transition:
    opacity 0.35s var(--ease),
    background 0.35s var(--ease);
}
.theater-arrow.prev {
  left: 18px;
}
.theater-arrow.next {
  right: 18px;
}
.aca-stage:hover .theater-arrow {
  opacity: 1;
}
.theater-arrow:hover {
  border-color: rgba(255, 255, 255, 0.6);
  background: rgba(25, 22, 19, 0.48);
  transform: translateY(-50%);
}
.theater-dots {
  position: absolute;
  bottom: 18px;
  left: 50%;
  z-index: 4;
  display: flex;
  gap: 7px;
  transform: translateX(-50%);
}
.theater-dots button {
  width: 7px;
  height: 7px;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.36);
  transition:
    width 0.35s var(--ease),
    background 0.35s var(--ease);
}
.theater-dots button:hover {
  border-color: transparent;
  background: rgba(255, 255, 255, 0.62);
  transform: none;
}
.theater-dots button.on {
  width: 22px;
  background: #fff;
}
.theater-progress {
  position: absolute;
  bottom: 0;
  left: 0;
  z-index: 5;
  width: 0;
  height: 2px;
  background: rgba(255, 255, 255, 0.7);
  animation: theater-progress 6s linear forwards;
}
.theater-progress.paused {
  animation-play-state: paused;
}
@keyframes theater-progress {
  from {
    width: 0;
  }
  to {
    width: 100%;
  }
}

.aca-sides {
  display: grid;
  grid-template-rows: repeat(2, minmax(0, 1fr));
  gap: 22px;
  min-width: 0;
}
.side-slot {
  min-height: 0;
}
.side-card {
  position: relative;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  border-radius: 22px;
  background: #eae7df;
  cursor: pointer;
  transform: perspective(1200px) rotateX(var(--tilt-x, 0deg)) rotateY(var(--tilt-y, 0deg));
  transition: transform 0.5s var(--ease);
}
.side-card img,
.side-card video {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.side-card img {
  transition: transform 0.9s var(--ease);
}
.side-card:hover img {
  transform: scale(1.05);
}
.side-card video {
  z-index: 1;
  opacity: 0;
  transition: opacity 0.4s ease;
}
.side-card:hover video {
  opacity: 1;
}
.side-shade {
  position: absolute;
  inset: 0;
  z-index: 2;
  background: linear-gradient(to top, rgba(18, 16, 14, 0.72), rgba(18, 16, 14, 0) 62%);
  pointer-events: none;
}
.side-caption {
  position: absolute;
  right: 20px;
  bottom: 18px;
  left: 20px;
  z-index: 3;
  color: #fff;
  pointer-events: none;
}
.side-caption h4 {
  margin: 0 0 5px;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
}
.side-caption span {
  color: rgba(255, 255, 255, 0.6);
  font-size: 11px;
  letter-spacing: 1px;
}
.side-play {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 3;
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(8px);
  color: #fff;
  opacity: 0;
  transform: translate(-50%, -50%) scale(0.85);
  transition:
    opacity 0.35s var(--ease),
    transform 0.35s var(--ease);
  pointer-events: none;
}
.side-play svg {
  width: 16px;
  height: 16px;
  margin-left: 2px;
}
.side-card:hover .side-play {
  opacity: 1;
  transform: translate(-50%, -50%) scale(1);
}

/* ===== 筛选栏 ===== */
.aca-catalog {
  scroll-margin-top: 108px;
}
.aca-toolbar {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 42px;
}
.aca-search {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
}
.aca-search svg {
  position: absolute;
  left: 20px;
  width: 17px;
  height: 17px;
  color: #aaa49c;
  pointer-events: none;
}
.aca-search input {
  width: 100%;
  height: 56px;
  padding: 0 22px 0 50px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: #fff;
  color: var(--ink);
  font-size: 14px;
  outline: none;
  transition:
    border-color 0.3s,
    box-shadow 0.3s;
}
.aca-search input::placeholder {
  color: #aaa49c;
}
.aca-search input:focus {
  border-color: var(--jade);
  box-shadow: 0 0 0 3px rgba(15, 95, 85, 0.08);
}
.aca-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.aca-chips button {
  padding: 12px 18px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: #fff;
  color: #6b6862;
  font-size: 13px;
  letter-spacing: 0.5px;
  white-space: nowrap;
  transition:
    background 0.3s var(--ease),
    color 0.3s var(--ease),
    border-color 0.3s var(--ease);
}
.aca-chips button:hover {
  border-color: #cfc9c0;
  color: var(--ink);
  transform: none;
}
.aca-chips button.on {
  border-color: var(--jade);
  background: var(--jade);
  color: #fff;
}
.aca-chips button.on:hover {
  border-color: var(--jade);
  background: var(--jade);
  color: #fff;
}

/* ===== 视图切换 ===== */
.aca-view-switch {
  display: inline-flex;
  flex: 0 0 auto;
  padding: 3px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fff;
}
.aca-view-switch button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: 0;
  border-radius: 9px;
  background: transparent;
  color: var(--muted);
  font-size: 13px;
  transition:
    background 0.3s var(--ease),
    color 0.3s var(--ease);
}
.aca-view-switch button svg {
  width: 14px;
  height: 14px;
  fill: currentColor;
}
.aca-view-switch button:hover {
  border-color: transparent;
  color: var(--ink);
  transform: none;
}
.aca-view-switch button.active {
  background: var(--jade);
  color: #fff;
}

/* ===== 课程网格 ===== */
.aca-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 24px;
}
.aca-course {
  min-width: 0;
  outline: none;
}
.aca-course-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgba(45, 42, 38, 0.06);
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 6px 24px rgba(45, 42, 38, 0.04);
  cursor: pointer;
  transition:
    transform 0.45s var(--ease),
    box-shadow 0.45s var(--ease),
    border-color 0.45s var(--ease);
}
.aca-course:hover .aca-course-card,
.aca-course:focus-visible .aca-course-card {
  transform: translateY(-5px);
  border-color: rgba(45, 42, 38, 0.1);
  box-shadow: 0 20px 44px rgba(45, 42, 38, 0.1);
}
.aca-course-media {
  position: relative;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  background: #eae7df;
}
.aca-course-media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.8s var(--ease);
}
.aca-course:hover .aca-course-media img {
  transform: scale(1.04);
}
.aca-course-tag {
  position: absolute;
  top: 14px;
  left: 14px;
  padding: 5px 11px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: var(--red);
  font-size: 10px;
  letter-spacing: 1.5px;
  backdrop-filter: blur(6px);
}
.aca-course-play {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 50%;
  background: rgba(20, 18, 16, 0.28);
  backdrop-filter: blur(8px);
  color: #fff;
  opacity: 0;
  transform: translate(-50%, -50%) scale(0.85);
  transition:
    opacity 0.35s var(--ease),
    transform 0.35s var(--ease);
}
.aca-course-play svg {
  width: 17px;
  height: 17px;
  margin-left: 2px;
}
.aca-course:hover .aca-course-play {
  opacity: 1;
  transform: translate(-50%, -50%) scale(1);
}
.aca-course-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  padding: 20px 22px 22px;
}
.aca-course-body h3 {
  margin: 0 0 8px;
  color: var(--ink);
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 19px;
  font-weight: 600;
  letter-spacing: 0.5px;
}
.aca-course-body p {
  display: -webkit-box;
  min-height: 40px;
  margin: 0 0 18px;
  overflow: hidden;
  color: #8d8880;
  font-size: 13px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.aca-course-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px solid #f2efe9;
  color: #a39d95;
  font-size: 12px;
  letter-spacing: 0.5px;
}
.aca-course-foot b {
  color: var(--red);
  font-weight: 500;
  letter-spacing: 1px;
  transition: letter-spacing 0.35s var(--ease);
}
.aca-course:hover .aca-course-foot b {
  letter-spacing: 2px;
}
.aca-empty {
  grid-column: 1 / -1;
  min-height: 240px;
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 10px;
  text-align: center;
  color: #8d8880;
  border: 1px dashed var(--line);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.55);
}
.aca-empty b {
  color: var(--ink);
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 20px;
  font-weight: 600;
}

/* ===== 列表视图 ===== */
.aca-grid.is-list {
  grid-template-columns: 1fr;
  gap: 14px;
}
.aca-grid.is-list .aca-course-card {
  flex-direction: row;
  min-height: 156px;
}
.aca-grid.is-list .aca-course-media {
  flex: 0 0 250px;
  aspect-ratio: auto;
}
.aca-grid.is-list .aca-course-body {
  justify-content: center;
  padding: 20px 28px;
}
.aca-grid.is-list .aca-course-body p {
  min-height: 0;
  margin-bottom: 14px;
}
.aca-grid.is-list .aca-course-foot {
  justify-content: flex-end;
  gap: 26px;
  margin-top: 0;
  padding-top: 0;
  border-top: 0;
}
.aca-grid.is-list .aca-course:hover .aca-course-card {
  transform: translateX(4px);
}

/* ===== 滚动渐显 ===== */
.reveal {
  opacity: 0;
  transform: translateY(16px);
}
.reveal.is-in {
  opacity: 1;
  transform: none;
  transition:
    opacity 0.7s var(--ease) var(--reveal-delay, 0ms),
    transform 0.7s var(--ease) var(--reveal-delay, 0ms);
}

/* ===== 宣传片弹层 ===== */
.aca-promo-modal {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: grid;
  place-items: center;
  padding: 32px;
  background: rgba(18, 16, 14, 0.72);
  backdrop-filter: blur(6px);
}
.aca-promo-box {
  position: relative;
  width: min(920px, 100%);
}
.aca-promo-box video {
  display: block;
  width: 100%;
  max-height: 76vh;
  border-radius: 18px;
  background: #111;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.4);
}
.aca-promo-close {
  position: absolute;
  top: -14px;
  right: -14px;
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.36);
  border-radius: 50%;
  background: rgba(20, 18, 16, 0.72);
  color: #fff;
  font-size: 20px;
  line-height: 1;
}
.aca-promo-close:hover {
  border-color: rgba(255, 255, 255, 0.6);
  background: rgba(20, 18, 16, 0.9);
  transform: none;
}

/* ===== 响应式 ===== */
@media (max-width: 1180px) {
  .aca-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .aca-toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  .aca-theater-grid {
    grid-template-columns: 1fr;
  }
  .aca-sides {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-template-rows: none;
  }
  .side-card {
    min-height: 200px;
  }
}
@media (max-width: 960px) {
  .aca-hero {
    grid-template-columns: 1fr;
    gap: 30px;
    margin-bottom: 56px;
  }
  .aca-hero-media {
    aspect-ratio: 16 / 10;
  }
  .aca-theater-grid {
    grid-template-columns: 1fr;
  }
  .aca-stage {
    aspect-ratio: 4 / 3;
  }
  .aca-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 640px) {
  .aca-wrap {
    padding: 20px 16px 72px;
  }
  .aca-hero-copy h1 {
    font-size: 40px;
    letter-spacing: 3px;
  }
  .aca-stats {
    gap: 28px;
  }
  .aca-section-head h2 {
    font-size: 24px;
  }
  .aca-chips {
    overflow-x: auto;
    flex-wrap: nowrap;
    padding-bottom: 4px;
    scrollbar-width: none;
  }
  .aca-chips::-webkit-scrollbar {
    display: none;
  }
  .aca-grid {
    grid-template-columns: 1fr;
  }
  .aca-grid.is-list .aca-course-card {
    flex-direction: column;
  }
  .aca-grid.is-list .aca-course-media {
    flex-basis: auto;
    aspect-ratio: 16 / 10;
  }
  .theater-caption {
    right: 20px;
    bottom: 24px;
    left: 20px;
  }
  .theater-caption h3 {
    font-size: 20px;
  }
  .theater-caption p {
    display: none;
  }
  .aca-sides {
    grid-template-columns: 1fr;
  }
}
@media (prefers-reduced-motion: reduce) {
  .reveal {
    opacity: 1;
    transform: none;
  }
  .theater-slide img,
  .aca-hero-media video {
    transition: none;
  }
  .aca-stage,
  .side-card {
    transform: none;
  }
}
</style>
