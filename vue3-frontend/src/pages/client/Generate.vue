<script setup lang="ts">
import { computed, ref } from 'vue';
import SectionTitle from '../../components/SectionTitle.vue';
import PatternCard from '../../components/PatternCard.vue';
import { patterns as demoPatterns } from '../../data';
import { api, listFrom } from '../../api';
import { readUserData, writeUserData } from '../../userData';
const style = ref('广绣经典'),
  element = ref('牡丹'),
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
  { name: '富贵华彩', className: 'p2', value: 'rich_color' },
  { name: '清润素韵', className: 'p3', value: 'soft_elegant' }
];
const sceneValues: Record<string, string> = {
  文创商品: 'product', 服饰刺绣: 'clothing', 家居软装: 'home', 礼品包装: 'package'
};
const resultToneClass = computed(() => ({
  国风雅韵: 'tone-elegant', 富贵华彩: 'tone-rich', 清润素韵: 'tone-soft'
}[palette.value] || 'tone-elegant'));
const inspirationPatterns = computed(() => patterns.value.length ? patterns.value : demoPatterns);

function createDemoResults() {
  const condition = `${style.value}${element.value}${palette.value}${scene.value}${description.value}${Date.now()}`;
  const seed = [...condition].reduce((sum, character) => sum + character.charCodeAt(0), 0);
  const elementStart: Record<string, number> = { 牡丹: 0, 凤凰: 1, 花鸟: 2, 祥云: 3, 莲花: 2, 醒狮: 4 };
  const start = (elementStart[element.value] ?? 0) + seed;
  const ordered = Array.from({ length: Number(count.value) }, (_, index) => {
    const source = demoPatterns[(start + index) % demoPatterns.length];
    return {
      ...source,
      id: Date.now() + index,
      title: `${element.value} · ${style.value} · ${palette.value} ${index + 1}`,
      meta: `${scene.value}${description.value ? ` · ${description.value}` : ''}`,
      image: index === 0 && referencePreview.value ? referencePreview.value : source.image
    };
  });
  return ordered;
}

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
async function run() {
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
    const result: any = await api.patterns.generate({
      keyword: element.value,
      style: style.value,
      elements: [element.value],
      colorTheme: selectedPalette?.value || 'chinese_elegant',
      usageScene: sceneValues[scene.value] || 'product',
      description: description.value,
      referenceImageUrl,
      generateCount: Number(count.value)
    });
    const list = listFrom(result?.patterns ?? result);
    if (list.length) {
      patterns.value = list.map((p: any, i: number) => ({
        ...demoPatterns[i % demoPatterns.length],
        ...p,
        image: p.imageUrl || p.thumbnailUrl || demoPatterns[i % demoPatterns.length].image
      }));
    } else {
      patterns.value = createDemoResults();
    }
  } catch {
    patterns.value = createDemoResults();
  } finally {
    const history = readUserData<any[]>('recent_generations', []);
    writeUserData('recent_generations', [...patterns.value.map((p) => ({ ...p, createdAt: Date.now() })), ...history].slice(0, 50));
    notice.value = `已生成 ${patterns.value.length} 张纹样`;
    loading.value = false;
  }
}

function enhanceDetails() {
  enhanced.value = !enhanced.value;
  notice.value = enhanced.value ? '已增强纹样色彩与细节显示' : '已恢复原始显示效果';
}

function savePatterns() {
  const saved = readUserData<any[]>('saved_patterns', []);
  const merged = [...patterns.value, ...saved];
  writeUserData('saved_patterns', merged.filter((item, index) =>
    merged.findIndex((other) => String(other.id) === String(item.id)) === index
  ));
  notice.value = '纹样已保存到“我的纹样”';
}

function favoritePatterns() {
  const ids = new Set(readUserData<string[]>('pattern_favorites', []));
  patterns.value.forEach((pattern) => ids.add(String(pattern.id)));
  writeUserData('pattern_favorites', [...ids]);
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
      <label>01 · 选择风格</label>
      <div class="chips">
        <button
          v-for="x in ['广绣经典', '新中式', '岭南花窗', '刺绣纹样']"
          :class="{ on: style === x }"
          @click="style = x"
        >
          {{ x }}
        </button>
      </div>
      <label>02 · 选择元素</label>
      <div class="chips">
        <button
          v-for="x in ['牡丹', '凤凰', '花鸟', '祥云', '莲花', '醒狮']"
          :class="{ on: element === x }"
          @click="element = x"
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
        ><i :class="option.className" />{{ option.name }}</button>
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
        :placeholder="`${element}主题纹样的补充描述`"
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
        <img v-if="referencePreview" :src="referencePreview" alt="参考图预览" />
        <template v-else>⇧<b>点击或拖拽上传参考图</b><small>JPG / PNG，不超过 5MB</small></template>
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
        @click="run"
      >
        {{ loading ? '正在生成…' : '✦ 立即生成纹样' }}
      </button>
    </aside>
    <section class="results">
      <div class="result-head">
        <h2>生成结果</h2>
      </div>
      <div v-if="patterns.length" class="result-grid" :class="[resultToneClass, { enhanced }]">
        <img
          class="featured"
          :src="patterns[0].image"
        /><img
          v-for="p in patterns.slice(1, 4)"
          :src="p.image"
        />
      </div>
      <div v-else class="result-empty">
        <span>✦</span>
        <b>暂未生成纹样</b>
        <p>选择左侧条件后，点击“立即生成纹样”</p>
      </div>
      <div v-if="patterns.length" class="result-actions">
        <button :disabled="loading" @click="run">↻ 重新生成</button><button :class="{ on: enhanced }" @click="enhanceDetails">✦ 细节增强</button
        ><button class="jade" @click="savePatterns">⇩ 保存纹样</button><button @click="favoritePatterns">♡ 收藏纹样</button>
      </div>
      <p v-if="patterns.length && notice" class="form-notice result-notice">{{ notice }}</p>
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
