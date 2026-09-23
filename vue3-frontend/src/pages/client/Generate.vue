<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import SectionTitle from '../../components/SectionTitle.vue';
import PatternCard from '../../components/PatternCard.vue';
import { patterns as demoPatterns } from '../../data';
import { api } from '../../api';
import { authState, isDemoAccount } from '../../auth';
import { readUserData, writeUserData } from '../../userData';
import {
  generationActive,
  generationFinished,
  generationSnapshot,
  restoreActiveGeneration,
  submitGenerationJob
} from '../../generation';
const router = useRouter();
const styles = ref<string[]>(['广绣经典']),
  elements = ref<string[]>(['牡丹']),
  palette = ref('国风雅韵'),
  scene = ref('文创商品'),
  count = ref(4),
  description = ref(''),
  loading = ref(false),
  patterns = ref<any[]>([]),
  notice = ref(''),
  enhanced = ref(false),
  referenceFile = ref<File | null>(null),
  referencePreview = ref(''),
  fileInput = ref<HTMLInputElement | null>(null);

const paletteOptions = [
  { name: '国风雅韵', className: 'p1', value: 'chinese_elegant' },
  { name: '富贵华彩', className: 'p2', value: 'red_gold' },
  { name: '清润素韵', className: 'p3', value: 'soft_elegant' }
];
const sceneValues: Record<string, string> = {
  文创商品: 'product',
  服饰刺绣: 'clothing',
  家居软装: 'home',
  礼品包装: 'package'
};
const resultToneClass = computed(
  () =>
    ({
      国风雅韵: 'tone-elegant',
      富贵华彩: 'tone-rich',
      清润素韵: 'tone-soft'
    })[palette.value] || 'tone-elegant'
);
const inspirationPatterns = computed(() => (patterns.value.length ? patterns.value : demoPatterns));
const offlineDemoToken = computed(() => authState.token === 'client-demo-token');
function useKeyword(value: string) {
  description.value = description.value ? `${description.value} ${value}` : value;
}
function toggleChoice(target: 'style' | 'element', value: string) {
  const choices = target === 'style' ? styles : elements;
  const wasSelected = choices.value.includes(value);
  choices.value = wasSelected
    ? choices.value.filter((item) => item !== value)
    : [...choices.value, value];
  if (!wasSelected) useKeyword(value);
}
const selectedStylesText = computed(() => styles.value.join('、'));
const selectedElementsText = computed(() => elements.value.join('、'));

function chooseReference(file?: File) {
  if (!file) return;
  if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 5 * 1024 * 1024) {
    notice.value = '请上传不超过 5MB 的 JPG 或 PNG 图片';
    return;
  }
  if (referencePreview.value) URL.revokeObjectURL(referencePreview.value);
  referenceFile.value = file;
  referencePreview.value = URL.createObjectURL(file);
  notice.value = `已选择参考图：${file.name}`;
}
function removeReference() {
  if (referencePreview.value) URL.revokeObjectURL(referencePreview.value);
  referenceFile.value = null;
  referencePreview.value = '';
  if (fileInput.value) fileInput.value.value = '';
  notice.value = '已移除参考图，将按文字描述生成';
}
/** 是否有生成任务正在进行(提交后由全局轮询驱动, 切页也不中断) */
const generating = computed(() => !!generationActive.value);
const progressText = computed(() => {
  const snap = generationSnapshot.value;
  if (!snap) return '正在提交生成任务…';
  if (snap.status === 'FAILED') return snap.errorMessage || '生成失败';
  return `已完成 ${snap.completedCount}/${snap.totalCount} 张（${snap.progress}%）`;
});

onMounted(() => restoreActiveGeneration());

async function run() {
  if (isDemoAccount.value) {
    await router.push({ path: '/login', query: { redirect: '/generate' } });
    return;
  }
  if (generating.value) return; // 任务进行中, 防止重复提交
  if (!styles.value.length || !elements.value.length) {
    notice.value = '请至少选择一种风格和一个元素';
    return;
  }
  loading.value = true;
  notice.value = '';
  try {
    let referenceImageUrl = '';
    if (referenceFile.value) {
      const formData = new FormData();
      formData.append('file', referenceFile.value);
      try {
        const upload: any = await api.files.upload(formData);
        referenceImageUrl = upload?.url || upload?.fileUrl || '';
      } catch {}
    }
    const selectedPalette = paletteOptions.find((item) => item.name === palette.value);
    // 异步提交: 后端立即返回 generationId, 生图在后台线程进行
    const submit: any = await api.patterns.generate({
      keyword: selectedElementsText.value,
      style: styles.value[0] || '广绣经典',
      elements: [...elements.value],
      colorTheme: selectedPalette?.value || 'chinese_elegant',
      usageScene: sceneValues[scene.value] || 'product',
      description: [
        styles.value.length > 1 ? `融合风格：${selectedStylesText.value}` : '',
        description.value
      ].filter(Boolean).join('；'),
      referenceImageUrl,
      generateCount: Number(count.value)
    });
    const generationId = submit?.generationId ?? submit?.id;
    if (!generationId) throw new Error('提交生成任务失败：未返回任务 ID');
    patterns.value = [];
    submitGenerationJob({
      id: Number(generationId),
      totalCount: Number(count.value),
      keyword: selectedElementsText.value,
      submittedAt: Date.now()
    });
    notice.value = '生成任务已提交，AI 正在作画…';
  } catch (reason: any) {
    patterns.value = [];
    notice.value = reason?.response?.data?.message || reason?.message || '提交生成任务失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

// 任务结束(成功/失败)后自动更新页面, 无论当时用户在哪个页面都生效
watch(generationFinished, (done) => {
  if (!done) return;
  if (done.ok) {
    patterns.value = (done.snapshot.patterns || []).map((p: any) => ({
      ...p,
      image: p.imageUrl || p.thumbnailUrl || p.image || ''
    }));
    notice.value = `已生成 ${patterns.value.length} 张纹样`;
  } else {
    notice.value = done.snapshot.errorMessage || '纹样生成失败，请稍后重试';
  }
});

function enhanceDetails() {
  enhanced.value = !enhanced.value;
  notice.value = enhanced.value ? '已增强纹样色彩与细节显示' : '已恢复原始显示效果';
}

function syncGeneratedPatterns() {
  const stamped = patterns.value.map((pattern) => ({
    ...pattern,
    createdAt: pattern.createdAt || new Date().toISOString()
  }));
  const saved = readUserData<any[]>('saved_patterns', []);
  const merged = [...stamped, ...saved];
  writeUserData(
    'saved_patterns',
    merged.filter(
      (item, index) => merged.findIndex((other) => String(other.id) === String(item.id)) === index
    )
  );
  const recent = readUserData<any[]>('recent_generations', []);
  const recentMerged = [...stamped, ...recent];
  writeUserData(
    'recent_generations',
    recentMerged
      .filter(
        (item, index) =>
          recentMerged.findIndex((other) => String(other.id) === String(item.id)) === index
      )
      .slice(0, 50)
  );
}

async function savePatterns() {
  syncGeneratedPatterns();
  if (!offlineDemoToken.value) {
    await Promise.allSettled(patterns.value.map((pattern) => api.patterns.save(pattern.id)));
  }
  notice.value = '纹样已保存到“我的纹样”';
}

async function favoritePatterns() {
  syncGeneratedPatterns();
  const ids = new Set(readUserData<string[]>('pattern_favorites', []));
  patterns.value.forEach((pattern) => ids.add(String(pattern.id)));
  writeUserData('pattern_favorites', [...ids]);
  if (!offlineDemoToken.value) {
    await Promise.allSettled(patterns.value.map((pattern) => api.patterns.favorite(pattern.id)));
  }
  notice.value = '本次生成的纹样已收藏';
}
</script>
<template>
  <div class="page-head">
    <span>AI PATTERN STUDIO</span>
    <h1>让传统纹样，遇见无限灵感</h1>
    <p>选择风格、元素与应用场景，生成可用于文创设计的广绣纹样。</p>
  </div>
  <div class="studio">
    <aside class="control-panel">
      <label>01 · 选择风格 <small class="choice-hint">可多选</small></label>
      <div class="chips">
        <button
          v-for="x in ['广绣经典', '新中式', '岭南花窗', '刺绣纹样']"
          :key="x"
          type="button"
          :class="{ on: styles.includes(x) }"
          :aria-pressed="styles.includes(x)"
          @click="toggleChoice('style', x)"
        >
          {{ x }}
        </button>
      </div>
      <label>02 · 选择元素 <small class="choice-hint">可多选</small></label>
      <div class="chips">
        <button
          v-for="x in ['牡丹', '凤凰', '花鸟', '祥云', '莲花', '醒狮']"
          :key="x"
          type="button"
          :class="{ on: elements.includes(x) }"
          :aria-pressed="elements.includes(x)"
          @click="toggleChoice('element', x)"
        >
          {{ x }}
        </button>
      </div>
      <label>03 · 配色方案</label>
      <div class="palettes">
        <button
          v-for="option in paletteOptions"
          :key="option.name"
          :class="{ on: palette === option.name }"
          @click="palette = option.name"
        >
          <i :class="option.className" />{{ option.name }}
        </button>
      </div>
      <label>04 · 应用场景</label
      ><select v-model="scene">
        <option>文创商品</option>
        <option>服饰刺绣</option>
        <option>家居软装</option>
        <option>礼品包装</option></select
      ><label>05 · 输入灵感描述</label
      ><textarea
        v-model="description"
        rows="4"
        :placeholder="`${selectedElementsText || '所选元素'}主题纹样的补充描述`"
      /><label>06 · 参考图（可选）</label>
      <input
        ref="fileInput"
        class="upload-input"
        type="file"
        accept="image/jpeg,image/png"
        @change="chooseReference(($event.target as HTMLInputElement).files?.[0])"
      />
      <button
        class="upload"
        type="button"
        @click="fileInput?.click()"
        @dragover.prevent
        @drop.prevent="chooseReference($event.dataTransfer?.files?.[0])"
      >
        <img
          v-if="referencePreview"
          :src="referencePreview"
          alt="参考图预览"
        />
        <template v-else>⇧<b>点击或拖拽上传参考图</b><small>JPG / PNG，不超过 5MB</small></template>
        <span
          v-if="referencePreview"
          class="upload-remove"
          role="button"
          aria-label="移除参考图"
          title="移除参考图"
          @click.stop="removeReference"
        >×</span>
      </button>
      <label
        >生成数量 <b>{{ count }} 张</b></label
      ><input
        type="range"
        min="1"
        max="4"
        v-model="count"
      /><button
        class="primary generate-btn"
        type="button"
        :disabled="loading || generating"
        @click="run"
      >
        {{ generating ? '✦ 生成中…' : '✦ 立即生成纹样' }}
      </button>
    </aside>
    <section class="results">
      <div class="result-head">
        <h2>生成结果</h2>
      </div>
      <div
        v-if="generating"
        class="gen-progress"
      >
        <span
          class="gen-spinner"
          aria-hidden="true"
        ></span>
        <b>AI 正在作画中，请稍候…</b>
        <p class="gen-progress-text">{{ progressText }}</p>
        <div
          class="gen-bar"
          role="progressbar"
          :aria-valuenow="generationSnapshot?.progress ?? 0"
          aria-valuemin="0"
          aria-valuemax="100"
        >
          <i
            v-if="generationSnapshot?.status !== 'FAILED'"
            :style="{ width: (generationSnapshot?.progress || 0) + '%' }"
          ></i>
        </div>
        <small>现在就可以去浏览其他页面，生成在后台继续，完成后结果会自动出现在这里和“我的纹样”。</small>
      </div>
      <div
        v-else-if="patterns.length"
        class="result-grid"
        :class="[resultToneClass, { enhanced }]"
      >
        <img
          class="featured"
          :src="patterns[0].image"
        /><img
          v-for="p in patterns.slice(1, 4)"
          :src="p.image"
        />
      </div>
      <div
        v-else
        class="result-empty"
      >
        <span>✦</span>
        <b>暂未生成纹样</b>
        <p>选择左侧条件后，点击“立即生成纹样”</p>
      </div>
      <div
        v-if="patterns.length"
        class="result-actions"
      >
        <button
          :disabled="loading || generating"
          @click="run"
        >
          ↻ 重新生成</button
        ><button
          :class="{ on: enhanced }"
          @click="enhanceDetails"
        >
          ✦ 细节增强</button
        ><button
          class="jade"
          @click="savePatterns"
        >
          ⇩ 保存纹样</button
        ><button @click="favoritePatterns">♡ 收藏纹样</button>
      </div>
      <p
        v-if="notice"
        class="form-notice result-notice"
      >
        {{ notice }}
      </p>
    </section>
  </div>
  <div class="content inspiration">
    <SectionTitle
      eyebrow="DAILY INSPIRATION"
      title="灵感推荐"
      action="查看更多"
      to="/patterns"
    />
    <div class="pattern-grid">
      <PatternCard
        v-for="p in inspirationPatterns.slice(2, 7)"
        :p="p"
      />
    </div>
  </div>
</template>
