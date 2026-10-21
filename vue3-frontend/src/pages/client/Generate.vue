<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../../api';
import { authState, isDemoAccount, isRealClient } from '../../auth';
import { readUserData, writeUserData } from '../../userData';
import type { GenerationFinished } from '../../generation';
import {
  clearLastFinishedGeneration,
  generationActive,
  generationFinished,
  generationSnapshot,
  readLastFinishedGeneration,
  restoreActiveGeneration,
  submitGenerationJob
} from '../../generation';
const router = useRouter();
const styles = ref<string[]>(['广绣经典']),
  elements = ref<string[]>(['牡丹']),
  palette = ref('国风雅韵'),
  description = ref(''),
  loading = ref(false),
  patterns = ref<any[]>([]),
  notice = ref(''),
  enhanced = ref(false),
  referenceFile = ref<File | null>(null),
  referencePreview = ref(''),
  fileInput = ref<HTMLInputElement | null>(null),
  advancedOpen = ref(false),
  elapsedSeconds = ref<number | null>(null),
  savingPattern = ref(false),
  favoritingPattern = ref(false),
  savedIds = ref<Set<string>>(new Set()),
  favoriteIds = ref<Set<string>>(new Set()),
  actionToast = ref('');
let actionToastTimer: number | null = null;

const paletteOptions = [
  { name: '国风雅韵', className: 'p1', value: 'chinese_elegant' },
  { name: '富贵华彩', className: 'p2', value: 'red_gold' },
  { name: '清润素韵', className: 'p3', value: 'soft_elegant' },
  { name: '自定义调色', className: 'p4' }
];
// ===== 自定义调色（HSL 滑块 + CSS filter 实时预览，颜色经 description 参与生成）=====
interface HslColor {
  h: number;
  s: number;
  l: number;
}
const customColors = ref<HslColor[]>([]);
const colorModalVisible = ref(false),
  draftHue = ref(8),
  draftSat = ref(72),
  draftLight = ref(48),
  editingIndex = ref(-1),
  previousPalette = ref(''),
  paletteError = ref(''),
  customStyleChips = ref<string[]>([]),
  customElementChips = ref<string[]>([]),
  styleInput = ref(''),
  elementInput = ref(''),
  styleInputVisible = ref(false),
  elementInputVisible = ref(false),
  styleInputRef = ref<HTMLInputElement | null>(null),
  elementInputRef = ref<HTMLInputElement | null>(null),
  padRef = ref<HTMLDivElement | null>(null),
  hueRef = ref<HTMLDivElement | null>(null);
const resultToneClass = computed(
  () =>
    ({
      国风雅韵: 'tone-elegant',
      富贵华彩: 'tone-rich',
      清润素韵: 'tone-soft'
    })[palette.value] || 'tone-elegant'
);
// 自定义模式下屏蔽 tone-* 滤镜, 让 inline filter 独立生效
const imageToneClass = computed(() =>
  palette.value === '自定义调色' ? '' : resultToneClass.value
);
const draftColorCss = computed(
  () => `hsl(${draftHue.value} ${draftSat.value}% ${draftLight.value}%)`
);
const draftColorHex = computed(() => hslToHex(draftHue.value, draftSat.value, draftLight.value));
/** 弹窗打开时把正在调的草稿色并入调色板, 拖滑块即可实时影响预览 */
const effectiveColors = computed<HslColor[]>(() => {
  if (!colorModalVisible.value) return customColors.value;
  const draft: HslColor = { h: draftHue.value, s: draftSat.value, l: draftLight.value };
  if (editingIndex.value >= 0) {
    return customColors.value.map((c, i) => (i === editingIndex.value ? draft : c));
  }
  return customColors.value.length < 4 ? [...customColors.value, draft] : customColors.value;
});
const customFilterStyle = computed<{ filter: string } | undefined>(() => {
  if (palette.value !== '自定义调色' || !effectiveColors.value.length) return undefined;
  const colors = effectiveColors.value;
  const avgS = colors.reduce((sum, c) => sum + c.s, 0) / colors.length;
  const avgL = colors.reduce((sum, c) => sum + c.l, 0) / colors.length;
  // CSS filter 只能全图统一变换: 主色相为旋转基准, 饱和/明度均值以 50 为中性锚点线性映射
  const saturate = 0.5 + avgS / 100;
  const brightness = 0.7 + avgL / 165;
  let filter = `hue-rotate(${colors[0].h}deg) saturate(${saturate.toFixed(2)}) brightness(${brightness.toFixed(2)})`;
  if (enhanced.value) filter += ' saturate(1.16) contrast(1.08)';
  return { filter };
});
const paletteColorText = computed(() => {
  if (palette.value !== '自定义调色' || customColors.value.length < 2) return '';
  const roles = ['主色', '辅色', '点缀', '点缀'];
  const parts = customColors.value.map(
    (c, i) => `${roles[i]} ${hslToHex(c.h, c.s, c.l).toUpperCase()}`
  );
  return `配色要求：${parts.join('，')}，整体遵循以上色值搭配`;
});
// 自定义风格不能进 style 字段(后端白名单), 文字走 description 进提示词
const customStyleText = computed(() =>
  customStyleChips.value.filter((name) => styles.value.includes(name)).length
    ? `补充风格：${customStyleChips.value.filter((name) => styles.value.includes(name)).join('、')}`
    : ''
);
function hslToHex(h: number, s: number, l: number): string {
  const sat = s / 100;
  const lig = l / 100;
  const k = (n: number) => (n + h / 30) % 12;
  const a = sat * Math.min(lig, 1 - lig);
  const f = (n: number) => lig - a * Math.max(-1, Math.min(k(n) - 3, Math.min(9 - k(n), 1)));
  const toHex = (v: number) =>
    Math.round(255 * v)
      .toString(16)
      .padStart(2, '0');
  return `#${toHex(f(0))}${toHex(f(8))}${toHex(f(4))}`;
}
const offlineDemoToken = computed(() => authState.token === 'client-demo-token');
function toggleChoice(target: 'style' | 'element', value: string) {
  const choices = target === 'style' ? styles : elements;
  const wasSelected = choices.value.includes(value);
  choices.value = wasSelected
    ? choices.value.filter((item) => item !== value)
    : [...choices.value, value];
}

// ===== 自定义风格/元素 chip =====
function addCustomChip(target: 'style' | 'element') {
  if (target === 'style') {
    styleInputVisible.value = !styleInputVisible.value;
    if (styleInputVisible.value) setTimeout(() => styleInputRef.value?.focus(), 0);
  } else {
    elementInputVisible.value = !elementInputVisible.value;
    if (elementInputVisible.value) setTimeout(() => elementInputRef.value?.focus(), 0);
  }
}

function confirmCustomChip(target: 'style' | 'element') {
  const input = target === 'style' ? styleInput : elementInput;
  const chips = target === 'style' ? customStyleChips : customElementChips;
  const choices = target === 'style' ? styles : elements;
  const name = input.value.trim();
  if (!name) return;
  if (target === 'style' && ['广绣经典', '新中式', '岭南花窗', '刺绣纹样'].includes(name)) {
    toggleChoice('style', name);
    input.value = '';
    return;
  }
  if (target === 'element' && ['牡丹', '凤凰', '花鸟', '祥云', '莲花', '醒狮'].includes(name)) {
    toggleChoice('element', name);
    input.value = '';
    return;
  }
  if (!chips.value.includes(name)) chips.value = [...chips.value, name];
  if (!choices.value.includes(name)) choices.value = [...choices.value, name];
  input.value = '';
  if (target === 'style') styleInputVisible.value = false;
  else elementInputVisible.value = false;
}

function removeCustomChip(target: 'style' | 'element', index: number) {
  const chips = target === 'style' ? customStyleChips : customElementChips;
  const choices = target === 'style' ? styles : elements;
  const name = chips.value[index];
  chips.value = chips.value.filter((_, i) => i !== index);
  choices.value = choices.value.filter((item) => item !== name);
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
const statusText = computed(() => {
  if (generating.value) return `AI 正在作画中 · ${generationSnapshot.value?.progress ?? 0}%`;
  if (!patterns.value.length) return '';
  return elapsedSeconds.value ? `生成完成 · 耗时 ${elapsedSeconds.value.toFixed(1)}s` : '生成完成';
});
function setElapsed(done: GenerationFinished) {
  if (!done.finishedAt) return;
  const secs = (done.finishedAt - done.job.submittedAt) / 1000;
  if (secs > 0 && secs < 3600) elapsedSeconds.value = secs;
}
const customColorsGradient = computed(() => {
  if (!customColors.value.length) return '';
  const stops = customColors.value.map((c) => `hsl(${c.h} ${c.s}% ${c.l}%)`);
  return `linear-gradient(90deg, ${stops.join(', ')})`;
});
const restoreNotice = computed(() => (notice.value.startsWith('已恢复') ? notice.value : ''));
const currentPatternId = computed(() =>
  patterns.value[0]?.id != null ? String(patterns.value[0].id) : ''
);
const isSaved = computed(
  () => !!currentPatternId.value && savedIds.value.has(currentPatternId.value)
);
const isFavorited = computed(
  () => !!currentPatternId.value && favoriteIds.value.has(currentPatternId.value)
);

/** 画布上的轻提示：让保存/收藏等操作在按钮附近有可见反馈 */
function showActionToast(text: string) {
  actionToast.value = text;
  if (actionToastTimer) window.clearTimeout(actionToastTimer);
  actionToastTimer = window.setTimeout(() => {
    actionToast.value = '';
    actionToastTimer = null;
  }, 1800);
}

/** 从本地数据同步当前纹样的保存/收藏状态, 驱动按钮图标 */
function refreshPatternFlags() {
  savedIds.value = new Set(
    readUserData<any[]>('saved_patterns', []).map((item) => String(item.id))
  );
  favoriteIds.value = new Set(
    readUserData<string[]>('pattern_favorites', []).map((id) => String(id))
  );
}

onMounted(() => {
  refreshPatternFlags();
  restoreActiveGeneration();
  // 任务在本页不在期间已结束(且刷新过页面, 内存里没有结果)时, 从本地存储恢复上次结果
  if (!generationActive.value && !generationFinished.value) {
    const last = readLastFinishedGeneration();
    if (last) restoreLastFinishedResult(last);
  }
  const savedPalette = readUserData<HslColor[]>('custom_palette', []);
  if (Array.isArray(savedPalette) && savedPalette.length) customColors.value = savedPalette;
});

// 切回预设方案时收起调色弹窗, 滤镜随 palette 切换自动失效/恢复
watch(palette, () => {
  if (palette.value !== '自定义调色' && colorModalVisible.value) colorModalVisible.value = false;
});

// 针脚路径大图: ESC 关闭
onMounted(() => window.addEventListener('keydown', onKeydown));
onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown);
  if (actionToastTimer) window.clearTimeout(actionToastTimer);
});
function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') stitchZoomVisible.value = false;
}

/** 校验图片 URL 是否真实可加载, 避免把失效缓存渲染成破图 */
function imageLoadable(url: string): Promise<boolean> {
  return new Promise((resolve) => {
    const img = new Image();
    const done = (ok: boolean) => {
      img.onload = null;
      img.onerror = null;
      resolve(ok);
    };
    img.onload = () => done(true);
    img.onerror = () => done(false);
    img.src = url;
  });
}

/** 恢复上次成功结果: 过滤无图/图加载失败的纹样, 全部失效时清缓存回到空态 */
async function restoreLastFinishedResult(done: GenerationFinished) {
  const restored = (done.snapshot.patterns || []).map((p: any) => ({
    ...p,
    image: p.imageUrl || p.thumbnailUrl || p.image || ''
  }));
  if (!restored.length) {
    clearLastFinishedGeneration();
    return;
  }
  // 校验主图(第一张)是否可加载, 失效则整份丢弃, 避免展示半残结果
  const mainOk = await imageLoadable(restored[0].image);
  if (!mainOk) {
    clearLastFinishedGeneration();
    notice.value = '上次生成的纹样图片已失效，请重新生成';
    return;
  }
  // 当前产品规则是一次只生成一张纹样，旧缓存里的多张结果只保留第一张
  patterns.value = restored.slice(0, 1).filter((p: any) => p.image);
  refreshPatternFlags();
  setElapsed(done);
  notice.value = `已恢复上次生成的 ${patterns.value.length} 张纹样`;
}

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
  elapsedSeconds.value = null;
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
      usageScene: 'product',
      description: [
        styles.value.length > 1 ? `融合风格：${selectedStylesText.value}` : '',
        description.value,
        paletteColorText.value,
        customStyleText.value
      ]
        .filter(Boolean)
        .join('；'),
      referenceImageUrl,
      generateCount: 1
    });
    const generationId = submit?.generationId ?? submit?.id;
    if (!generationId) throw new Error('提交生成任务失败：未返回任务 ID');
    patterns.value = [];
    submitGenerationJob({
      id: Number(generationId),
      totalCount: 1,
      keyword: selectedElementsText.value,
      submittedAt: Date.now()
    });
    notice.value = '生成任务已提交，AI 正在作画…';
  } catch (reason: any) {
    patterns.value = [];
    notice.value =
      reason?.response?.data?.message || reason?.message || '提交生成任务失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

function applyFinishedResult(done: GenerationFinished) {
  if (done.ok) {
    patterns.value = (done.snapshot.patterns || []).map((p: any) => ({
      ...p,
      image: p.imageUrl || p.thumbnailUrl || p.image || ''
    }));
    refreshPatternFlags();
    notice.value = `已生成 ${patterns.value.length} 张纹样`;
  } else {
    notice.value = done.snapshot.errorMessage || '纹样生成失败，请稍后重试';
  }
}

// 任务结束(成功/失败)后自动更新页面, 无论当时用户在哪个页面都生效
// immediate: 切到其他页面时任务已结束, 回来时直接补上结果
watch(
  generationFinished,
  (done) => {
    if (!done) return;
    setElapsed(done);
    applyFinishedResult(done);
  },
  { immediate: true }
);

function enhanceDetails() {
  enhanced.value = !enhanced.value;
  notice.value = enhanced.value ? '已增强纹样色彩与细节显示' : '已恢复原始显示效果';
}

// 带着当前纹样进入 3D 针法解析页，页面会按 patternId 从本地纹样库取图
function openStitchAnalysis() {
  const pattern = patterns.value[0];
  router.push({
    path: '/stitch-3d',
    query: pattern?.id != null ? { patternId: String(pattern.id) } : {}
  });
}

// ===== 自定义调色交互 =====
function selectCustom() {
  if (palette.value !== '自定义调色' && customColors.value.length >= 2) {
    palette.value = '自定义调色';
    return;
  }
  previousPalette.value = palette.value;
  if (customColors.value.length) {
    const first = customColors.value[0];
    draftHue.value = first.h;
    draftSat.value = first.s;
    draftLight.value = first.l;
  }
  editingIndex.value = -1;
  paletteError.value = '';
  colorModalVisible.value = true;
}

function closeColorModal(confirm: boolean) {
  if (confirm) {
    if (effectiveColors.value.length < 2) {
      paletteError.value = '请至少加入 2 个颜色形成配色';
      return;
    }
    customColors.value = effectiveColors.value;
    palette.value = '自定义调色';
    writeUserData('custom_palette', customColors.value);
  } else if (!customColors.value.length && palette.value === '自定义调色') {
    palette.value = previousPalette.value || '国风雅韵';
  }
  colorModalVisible.value = false;
}

function addOrUpdateDraftColor() {
  const draft: HslColor = { h: draftHue.value, s: draftSat.value, l: draftLight.value };
  if (editingIndex.value >= 0) {
    customColors.value = customColors.value.map((c, i) => (i === editingIndex.value ? draft : c));
  } else if (customColors.value.length < 4) {
    customColors.value = [...customColors.value, draft];
  }
  editingIndex.value = -1;
  paletteError.value = '';
}

function editCustomColor(index: number) {
  const c = customColors.value[index];
  draftHue.value = c.h;
  draftSat.value = c.s;
  draftLight.value = c.l;
  editingIndex.value = index;
}

function removeCustomColor(index: number) {
  customColors.value = customColors.value.filter((_, i) => i !== index);
  editingIndex.value = -1;
  if (customColors.value.length < 2) {
    paletteError.value = '至少保留 2 个颜色才能应用配色';
  }
}

function resetCustomColors() {
  customColors.value = [];
  editingIndex.value = -1;
  paletteError.value = '';
  writeUserData('custom_palette', []);
  if (palette.value === '自定义调色') palette.value = '国风雅韵';
}

/** 把指针事件换算成 2D 色盘内的相对坐标, 驱动饱和度(横)与明度(纵) */
function pickFromPad(event: PointerEvent) {
  const pad = padRef.value;
  if (!pad) return;
  const rect = pad.getBoundingClientRect();
  const x = Math.min(Math.max((event.clientX - rect.left) / rect.width, 0), 1);
  const y = Math.min(Math.max((event.clientY - rect.top) / rect.height, 0), 1);
  draftSat.value = Math.round(x * 100);
  draftLight.value = Math.round((1 - y) * 100);
}

function padPointerDown(event: PointerEvent) {
  pickFromPad(event);
  const pad = padRef.value;
  if (!pad) return;
  pad.setPointerCapture(event.pointerId);
  const move = (e: PointerEvent) => pickFromPad(e);
  const up = () => {
    pad.removeEventListener('pointermove', move);
    pad.removeEventListener('pointerup', up);
    pad.removeEventListener('pointercancel', up);
  };
  pad.addEventListener('pointermove', move);
  pad.addEventListener('pointerup', up);
  pad.addEventListener('pointercancel', up);
}

/** 色相条上横向拖动选主色相 */
function pickFromHueBar(event: PointerEvent) {
  const bar = hueRef.value;
  if (!bar) return;
  const rect = bar.getBoundingClientRect();
  const x = Math.min(Math.max((event.clientX - rect.left) / rect.width, 0), 1);
  draftHue.value = Math.round(x * 360);
}

function huePointerDown(event: PointerEvent) {
  pickFromHueBar(event);
  const bar = hueRef.value;
  if (!bar) return;
  bar.setPointerCapture(event.pointerId);
  const move = (e: PointerEvent) => pickFromHueBar(e);
  const up = () => {
    bar.removeEventListener('pointermove', move);
    bar.removeEventListener('pointerup', up);
    bar.removeEventListener('pointercancel', up);
  };
  bar.addEventListener('pointermove', move);
  bar.addEventListener('pointerup', up);
  bar.addEventListener('pointercancel', up);
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
  if (!patterns.value.length || savingPattern.value) return;
  savingPattern.value = true;
  try {
    syncGeneratedPatterns();
    refreshPatternFlags();
    if (!offlineDemoToken.value) {
      await Promise.allSettled(patterns.value.map((pattern) => api.patterns.save(pattern.id)));
    }
    notice.value = '纹样已保存到“我的纹样”';
    showActionToast('✓ 已保存到「我的纹样」');
  } finally {
    savingPattern.value = false;
  }
}

/** 收藏/取消收藏当前纹样, 图标与本地收藏列表实时同步 */
async function toggleFavorite() {
  const id = currentPatternId.value;
  if (!id || favoritingPattern.value) return;
  favoritingPattern.value = true;
  try {
    const ids = new Set(
      readUserData<string[]>('pattern_favorites', []).map((item) => String(item))
    );
    const wasFavorited = ids.has(id);
    if (wasFavorited) ids.delete(id);
    else ids.add(id);
    writeUserData('pattern_favorites', [...ids]);
    favoriteIds.value = ids;
    if (!offlineDemoToken.value) {
      const target = patterns.value[0];
      await Promise.allSettled([
        wasFavorited ? api.patterns.unfavorite(target.id) : api.patterns.favorite(target.id)
      ]);
    }
    notice.value = wasFavorited ? '已取消收藏' : '本次生成的纹样已收藏';
    showActionToast(wasFavorited ? '♡ 已取消收藏' : '♥ 已收藏');
  } finally {
    favoritingPattern.value = false;
  }
}

// ===== 一键打版（生成 DST，三种针法均由 Python 打版引擎真实计算）=====
const DST_SERVICE_BASE = 'http://localhost:8000';
const dstLoading = ref(false);
const dstModalVisible = ref(false);
const stitchType = ref<DstMode>('outline');
const activeDstMode = ref<DstMode>('outline'); // 本次实际打版用的针法（弹窗/报价/工单以它为准）
const stitchImgSrc = ref('');
const dstDownloadUrl = ref('');
const stitchCount = ref(0);
const fileSize = ref('');
const stitchBreakdown = ref<Record<string, number>>({});
const fabricType = ref('canvas'); // 帆布/棉布/丝绸：打版张力补偿参数
const compensationMsg = ref('');
const designHash = ref(''); // 纹样设计数字指纹（SHA-256）
const dstHash = ref(''); // DST 打版文件指纹（SHA-256）
const genTime = ref(''); // 打版完成时间（存证时间）
const certVisible = ref(false);
const bgPercent = ref(0); // 底布留白占比（打版引擎真实计算值）
const factorySubmitted = ref(false);
const factorySubmitting = ref(false);
const lastOrderNo = ref('');
// 针脚路径图点击放大（lightbox）
const stitchZoomVisible = ref(false);

// ===== 动态报价：价格 = 基础材料费 + 针数 × 单价（按绣花机市场工时价折算）=====
const QUOTE_BASE_FEE = 15; // 基础材料费：绣线 + 水溶衬 + 底布
const QUOTE_PER_STITCH = 0.01; // 针数单价：0.01 元/针
const quoteBaseText = QUOTE_BASE_FEE.toFixed(2);
const quotePerStitchText = QUOTE_PER_STITCH.toFixed(2);
const quoteStitchCost = computed(() => (stitchCount.value * QUOTE_PER_STITCH).toFixed(2));
const quoteTotal = computed(() =>
  (QUOTE_BASE_FEE + stitchCount.value * QUOTE_PER_STITCH).toFixed(2)
);
const stitchTypeLabel = computed(
  () =>
    ({
      outline: '轮廓走针',
      satin: '平包针',
      tatami: '他他米针',
      auto: 'AI 智能混合（自动匹配）'
    })[activeDstMode.value] || '轮廓走针'
);
const matchedStitchCount = computed(
  () => Object.values(stitchBreakdown.value).filter((n) => Number(n || 0) > 0).length
);
const breakdownText = computed(() => {
  const order: Array<[string, string]> = [
    ['outline', '轮廓走针'],
    ['satin', '平包针'],
    ['tatami', '他他米针']
  ];
  return order
    .filter(([key]) => Number(stitchBreakdown.value[key] || 0) > 0)
    .map(([key, label]) => `${label} ${stitchBreakdown.value[key]} 针`)
    .join(' · ');
});

function openDstModal() {
  // 只重置操作状态；数据字段（针数/指纹/补偿等）由成功回调在打开前设置，
  // 不能在这里清，否则刚拿到的一次性结果会被立即抹掉
  factorySubmitted.value = false;
  lastOrderNo.value = '';
  certVisible.value = false;
  dstModalVisible.value = true;
}
function nowText() {
  const d = new Date();
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(
    d.getMinutes()
  )}:${p(d.getSeconds())}`;
}
function shortHash(h: string) {
  if (!h) return '';
  return h.length > 24 ? `${h.slice(0, 12)}…${h.slice(-8)}` : h;
}
function printCert() {
  window.print();
}
const FABRIC_LABELS: Record<string, string> = {
  canvas: '帆布袋',
  cotton: '棉布',
  silk: '丝绸'
};
const certPatternImage = computed(() => patterns.value[0]?.image || stitchImgSrc.value);
const certPatternTitle = computed(
  () => patterns.value[0]?.title || patterns.value[0]?.name || 'AI 非遗纹样'
);
const certFabricLabel = computed(() => FABRIC_LABELS[fabricType.value] || '帆布袋');
const certNo = computed(
  () =>
    `XWZC-${(genTime.value || '').replace(/[^0-9]/g, '').slice(0, 14) || '00000000000000'}-${(
      designHash.value || '000000'
    )
      .slice(0, 6)
      .toUpperCase()}`
);
async function submitToFactory() {
  if (factorySubmitting.value || factorySubmitted.value) return;
  if (!isRealClient.value) {
    notice.value = '请先登录账号，再提交生产工单';
    return;
  }
  factorySubmitting.value = true;
  try {
    const pattern = patterns.value[0] || {};
    const created: any = await api.factory.submit({
      patternId: pattern.id,
      patternTitle: pattern.title || pattern.name,
      patternImageUrl: pattern.image,
      style: styles.value.join('、'),
      stitchType: String(activeDstMode.value).toUpperCase(),
      fabricType: fabricType.value,
      designHash: designHash.value,
      dstHash: dstHash.value,
      stitchImageUrl: stitchImgSrc.value,
      dstFileUrl: dstDownloadUrl.value,
      stitchCount: stitchCount.value,
      fileSize: fileSize.value,
      quoteAmount: Number(quoteTotal.value)
    });
    lastOrderNo.value = String(created?.orderNo || '');
    factorySubmitted.value = true;
  } catch (reason: any) {
    notice.value =
      '提交工厂失败：' + (reason?.response?.data?.message || reason?.message || '请稍后重试');
  } finally {
    factorySubmitting.value = false;
  }
}

type DstMode = 'outline' | 'satin' | 'tatami' | 'auto';

async function handleGenerateDST(mode: DstMode) {
  // 记录本次实际使用的针法：点"一键打版"用下拉框值，点金色按钮用 auto
  activeDstMode.value = mode;
  // 显示 Loading
  dstLoading.value = true;
  notice.value = '';

  // 取当前展示的纹样图（第一张为精选主图）
  const imageUrl = patterns.value[0]?.image;
  if (!imageUrl) {
    dstLoading.value = false;
    notice.value = '请先生成纹样，再进行打版';
    return;
  }
  try {
    // 把纹样图转成文件流发给 DST 服务
    let blob: Blob;
    try {
      // no-store: 必须绕开缓存。OSS 响应不带 Cache-Control，<img> 先加载时
      // 缓存里存的是无 CORS 头的响应，fetch 复用会被浏览器拦截（Failed to fetch）
      const response = await fetch(imageUrl, { cache: 'no-store' });
      if (!response.ok) throw new Error(`图片链接返回 HTTP ${response.status}`);
      blob = await response.blob();
    } catch (imgError: any) {
      throw new Error(`纹样图读取失败（${imgError?.message || '跨域或链接失效'}），请刷新页面重试`);
    }
    const formData = new FormData();
    formData.append('file', blob, 'pattern.png');

    // 针法拼到 URL 参数里，打版引擎按针法真实计算（outline/satin/tatami）
    const res = await fetch(
      `${DST_SERVICE_BASE}/generate-dst?stitch_type=${mode}&fabric=${fabricType.value}`,
      {
        method: 'POST',
        body: formData
      }
    );
    const result = await res.json();
    if (result.code === 200 && result.data) {
      stitchImgSrc.value = result.data.stitch_image_url;
      dstDownloadUrl.value = result.data.dst_file_url;
      stitchCount.value = Number(result.data.stitch_count || 0);
      fileSize.value = result.data.file_size || '';
      stitchBreakdown.value = result.data.stitch_breakdown || {};
      bgPercent.value = Number(result.data.bg_percent || 0);
      compensationMsg.value = result.data.compensation_msg || '';
      designHash.value = String(result.data.design_hash || '');
      dstHash.value = String(result.data.dst_hash || '');
      genTime.value = nowText();
      openDstModal();
    } else {
      notice.value = 'DST 生成失败：' + (result.message || '未知错误');
    }
  } catch (error: any) {
    console.error(error);
    notice.value = error?.message || '网络错误，请检查 DST 服务（localhost:8000）是否已启动';
  } finally {
    dstLoading.value = false;
  }
}
</script>
<template>
  <div class="gen-page">
    <div class="gen-workspace">
      <aside class="gen-side">
        <div class="gen-side-scroll">
          <div class="gen-intro">
            <h1>让传统纹样<br />遇见无限灵感</h1>
            <p>选择参数，AI 将自动生成纹样并转化为工业级文件。</p>
          </div>
          <div class="gen-block">
            <div class="gen-block-title"><span>01</span> 选择风格</div>
            <div class="gen-chips">
              <button
                v-for="x in ['广绣经典', '新中式', '岭南花窗', '刺绣纹样']"
                :key="x"
                type="button"
                class="gen-chip"
                :class="{ on: styles.includes(x) }"
                :aria-pressed="styles.includes(x)"
                @click="toggleChoice('style', x)"
              >
                {{ x }}
              </button>
              <button
                type="button"
                class="gen-chip gen-chip-add"
                :class="{ on: customStyleChips.length > 0 }"
                @click="addCustomChip('style')"
              >
                ＋ 自定义
              </button>
            </div>
            <div
              v-if="customStyleChips.length"
              class="gen-custom-chips"
            >
              <span
                v-for="(name, i) in customStyleChips"
                :key="name"
                class="gen-custom-chip"
                :class="{ on: styles.includes(name) }"
                @click="toggleChoice('style', name)"
              >
                {{ name }}
                <button
                  class="gen-custom-chip-remove"
                  type="button"
                  @click.stop="removeCustomChip('style', i)"
                >
                  ×
                </button>
              </span>
            </div>
            <input
              v-if="styleInputVisible"
              ref="styleInputRef"
              v-model="styleInput"
              class="gen-text-input"
              maxlength="12"
              placeholder="输入自定义风格（12 字内），回车确认"
              @keydown.enter.prevent="confirmCustomChip('style')"
              @blur="styleInputVisible = false"
            />
          </div>
          <div class="gen-block">
            <div class="gen-block-title"><span>02</span> 选择元素</div>
            <div class="gen-chips">
              <button
                v-for="x in ['牡丹', '凤凰', '花鸟', '祥云', '莲花', '醒狮']"
                :key="x"
                type="button"
                class="gen-chip"
                :class="{ on: elements.includes(x) }"
                :aria-pressed="elements.includes(x)"
                @click="toggleChoice('element', x)"
              >
                {{ x }}
              </button>
              <button
                type="button"
                class="gen-chip gen-chip-add"
                :class="{ on: customElementChips.length > 0 }"
                @click="addCustomChip('element')"
              >
                ＋ 自定义
              </button>
            </div>
            <div
              v-if="customElementChips.length"
              class="gen-custom-chips"
            >
              <span
                v-for="(name, i) in customElementChips"
                :key="name"
                class="gen-custom-chip"
                :class="{ on: elements.includes(name) }"
                @click="toggleChoice('element', name)"
              >
                {{ name }}
                <button
                  class="gen-custom-chip-remove"
                  type="button"
                  @click.stop="removeCustomChip('element', i)"
                >
                  ×
                </button>
              </span>
            </div>
            <input
              v-if="elementInputVisible"
              ref="elementInputRef"
              v-model="elementInput"
              class="gen-text-input"
              maxlength="12"
              placeholder="输入自定义元素（12 字内），回车确认"
              @keydown.enter.prevent="confirmCustomChip('element')"
              @blur="elementInputVisible = false"
            />
          </div>
          <div class="gen-block">
            <div class="gen-block-title"><span>03</span> 配色方案</div>
            <div class="gen-palettes">
              <button
                v-for="option in paletteOptions.slice(0, 3)"
                :key="option.name"
                type="button"
                class="gen-palette"
                :class="{ on: palette === option.name }"
                @click="palette = option.name"
              >
                <span class="gen-palette-name"
                  ><span>{{ option.name }}</span
                  ><em v-if="palette === option.name">已选</em></span
                >
                <i
                  class="gen-palette-bar"
                  :class="option.className"
                />
              </button>
              <button
                type="button"
                class="gen-palette gen-palette-custom"
                :class="{ on: palette === '自定义调色' }"
                @click="selectCustom"
              >
                <span class="gen-palette-name"
                  ><span>✦ 自定义调色</span><em v-if="palette === '自定义调色'">已选</em></span
                >
                <i
                  v-if="customColors.length"
                  class="gen-palette-bar"
                  :style="{ background: customColorsGradient }"
                />
              </button>
            </div>
          </div>
          <div class="gen-advanced">
            <button
              type="button"
              class="gen-advanced-toggle"
              @click="advancedOpen = !advancedOpen"
            >
              <span>展开高级选项（灵感描述 / 参考图）</span>
              <span
                class="gen-advanced-arrow"
                :class="{ on: advancedOpen }"
                >▼</span
              >
            </button>
            <div
              v-if="advancedOpen"
              class="gen-advanced-body"
            >
              <label class="gen-field-label">补充描述</label>
              <textarea
                v-model="description"
                rows="3"
                class="gen-textarea"
                :placeholder="`${selectedElementsText || '所选元素'}主题纹样的补充描述`"
              />
              <label class="gen-field-label">上传参考图（可选）</label>
              <input
                ref="fileInput"
                class="upload-input"
                type="file"
                accept="image/jpeg,image/png"
                @change="chooseReference(($event.target as HTMLInputElement).files?.[0])"
              />
              <button
                class="gen-upload"
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
                <template v-else
                  >⇧<b>点击或拖拽上传参考图</b><small>JPG / PNG，不超过 5MB</small></template
                >
                <span
                  v-if="referencePreview"
                  class="upload-remove"
                  role="button"
                  aria-label="移除参考图"
                  title="移除参考图"
                  @click.stop="removeReference"
                  >×</span
                >
              </button>
            </div>
          </div>
        </div>
        <div class="gen-side-footer">
          <p
            v-if="notice && !restoreNotice"
            class="gen-side-notice"
          >
            {{ notice }}
          </p>
          <button
            class="gen-generate"
            type="button"
            :disabled="loading || generating"
            @click="run"
          >
            {{ generating ? '✦ 生成中…' : '✦ 立即生成纹样' }}
          </button>
        </div>
      </aside>
      <section class="gen-main">
        <div class="gen-canvas">
          <img
            v-if="patterns.length"
            class="gen-canvas-backdrop"
            :src="patterns[0].image"
            alt=""
            aria-hidden="true"
          />
          <img
            v-if="patterns.length"
            class="gen-canvas-img"
            :class="[imageToneClass, { enhanced }]"
            :src="patterns[0].image"
            :style="customFilterStyle"
            alt="生成的纹样"
          />
          <div
            v-else-if="generating"
            class="gen-canvas-progress"
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
            <small
              >现在可以浏览其他页面，生成在后台继续，完成后结果会自动出现在这里和“我的纹样”。</small
            >
          </div>
          <div
            v-else
            class="gen-canvas-empty"
          >
            <span>✦</span>
            <b>暂未生成纹样</b>
            <p>选择左侧条件后，点击“立即生成纹样”</p>
          </div>
          <div
            v-if="statusText"
            class="gen-status"
          >
            <i
              class="gen-status-dot"
              aria-hidden="true"
            ></i
            >{{ statusText }}
          </div>
          <div
            v-if="patterns.length"
            class="gen-toolbar"
          >
            <button
              type="button"
              title="重新生成"
              :disabled="loading || generating"
              @click="run"
            >
              ↻
            </button>
            <button
              type="button"
              title="细节增强"
              :class="{ on: enhanced }"
              :aria-pressed="enhanced"
              @click="enhanceDetails"
            >
              ✦
            </button>
            <button
              type="button"
              :title="isSaved ? '已保存到我的纹样' : '保存纹样'"
              :class="{ on: isSaved }"
              :disabled="savingPattern"
              @click="savePatterns"
            >
              {{ isSaved ? '✓' : '↓' }}
            </button>
            <button
              type="button"
              :title="isFavorited ? '取消收藏' : '收藏纹样'"
              :class="{ on: isFavorited }"
              :aria-pressed="isFavorited"
              :disabled="favoritingPattern"
              @click="toggleFavorite"
            >
              {{ isFavorited ? '♥' : '♡' }}
            </button>
          </div>
          <div
            v-if="actionToast"
            class="gen-toast"
            role="status"
          >
            {{ actionToast }}
          </div>
          <button
            v-if="patterns.length"
            type="button"
            class="gen-3d-entry"
            @click="openStitchAnalysis"
          >
            进入 3D 针法解析 →
          </button>
        </div>
        <div class="gen-dst-card">
          <div class="gen-dst-head">
            <div class="gen-block-title"><span>04</span> 工业打版配置</div>
            <span
              v-if="restoreNotice"
              class="gen-restore-badge"
              >{{ restoreNotice }}</span
            >
          </div>
          <div class="gen-dst-row">
            <label class="gen-dst-field">
              <span>针法选择</span>
              <select
                id="stitch-type-select"
                v-model="stitchType"
                :disabled="dstLoading"
              >
                <option value="outline">轮廓走针 · 用于勾边</option>
                <option value="satin">平包针 · 用于细长花瓣</option>
                <option value="tatami">他他米针 · 用于大面积填充</option>
              </select>
            </label>
            <label class="gen-dst-field">
              <span>面料选择</span>
              <select
                id="fabric-select"
                v-model="fabricType"
                :disabled="dstLoading"
              >
                <option value="canvas">帆布袋 · 稳定</option>
                <option value="cotton">棉布 · 中等缩水</option>
                <option value="silk">丝绸 · 高缩水</option>
              </select>
            </label>
            <button
              class="gen-dst-btn"
              :disabled="dstLoading"
              @click="handleGenerateDST(stitchType)"
            >
              {{ dstLoading ? '✦ 针脚计算中…' : '✦ 一键打版' }}
            </button>
          </div>
        </div>
      </section>
    </div>
  </div>
  <div
    v-if="dstModalVisible"
    class="dst-overlay"
    @click.self="dstModalVisible = false"
  >
    <div class="dst-modal">
      <h3>🎉 工业级针脚路径生成成功</h3>
      <div class="dst-modal-body">
        <div class="dst-modal-left">
          <img
            :src="stitchImgSrc"
            alt="针脚路径图"
            class="dst-zoomable"
            title="点击查看大图"
            @click="stitchZoomVisible = true"
          />
          <p class="dst-zoom-hint">🔍 点击图片可放大查看针脚细节</p>
          <div
            v-if="activeDstMode === 'auto'"
            class="dst-legend"
          >
            <span class="dst-legend-item"><i style="background: #b73722"></i>轮廓走针 · 勾边</span>
            <span class="dst-legend-item"
              ><i style="background: #1a9c46"></i>平包针 · 细长区域</span
            >
            <span class="dst-legend-item"
              ><i style="background: #2b6cb0"></i>他他米针 · 大面积填充</span
            >
            <span class="dst-legend-item"><i class="dst-legend-bg"></i>智能留白 · 保留底布</span>
          </div>
        </div>
        <div class="dst-modal-right">
          <p>
            针法：<strong>{{ stitchTypeLabel }}</strong>
          </p>
          <p>
            预计针数：<strong>{{ stitchCount }} 针</strong>
          </p>
          <p class="dst-tip">
            💡 已智能过滤空白背景<template v-if="bgPercent"
              >（本图约 {{ bgPercent }}% 为底布留白）</template
            >，仅计算有效刺绣面积，机器不空跑，帮您节省生产成本
          </p>
          <p
            v-if="compensationMsg"
            class="dst-comp"
          >
            ⚖ {{ compensationMsg }}
          </p>
          <div
            v-if="designHash"
            class="dst-hashbox"
          >
            <div class="hashbox-head">
              <b>🔏 数字指纹已存证</b><small>SHA-256 加密哈希 · 唯一且防篡改</small>
            </div>
            <div class="hashbox-row">
              <span>纹样指纹</span><code>{{ shortHash(designHash) }}</code>
            </div>
            <div class="hashbox-row">
              <span>打版文件</span><code>{{ shortHash(dstHash) }}</code>
            </div>
            <button
              type="button"
              @click="certVisible = true"
            >
              📜 下载版权存证证书
            </button>
          </div>
          <p
            v-if="activeDstMode === 'auto' && breakdownText"
            class="dst-auto-note"
          >
            <b>系统已为您自动匹配 {{ matchedStitchCount }} 种针法</b>
            <small>
              {{ breakdownText }} · 总针数 {{ stitchCount }} 针（红=轮廓走针 绿=平包针 蓝=他他米针）
            </small>
          </p>
          <p>
            文件大小：<strong>{{ fileSize }}</strong>
          </p>
          <div class="dst-quote">
            <p class="dst-quote-title">动态报价 · 按针数计价</p>
            <div class="dst-quote-row">
              <span>基础材料费（绣线 + 衬底）</span><b>¥ {{ quoteBaseText }}</b>
            </div>
            <div class="dst-quote-row">
              <span>针数费 {{ stitchCount }} 针 × ¥{{ quotePerStitchText }}/针</span
              ><b>¥ {{ quoteStitchCost }}</b>
            </div>
            <div class="dst-quote-row total">
              <span>预计总报价</span><b>¥ {{ quoteTotal }}</b>
            </div>
          </div>
          <p>状态：<span class="dst-status">已生成工业级文件</span></p>
        </div>
      </div>
      <div class="dst-modal-footer">
        <button
          class="dst-close"
          @click="dstModalVisible = false"
        >
          关闭</button
        ><a
          class="primary dst-download"
          :href="dstDownloadUrl"
          download
        >
          下载 DST 文件（供绣花机使用）
        </a>
        <button
          v-if="!factorySubmitted"
          class="dst-factory"
          type="button"
          :disabled="factorySubmitting"
          @click="submitToFactory"
        >
          {{ factorySubmitting ? '正在提交…' : '🏭 提交给工厂' }}
        </button>
        <p
          v-else
          class="dst-factory-done"
        >
          ✔ 工单已生成<template v-if="lastOrderNo">（{{ lastOrderNo }}）</template
          >，已派发至乡村工坊，可在「个人中心 · 生产工单」跟踪进度
        </p>
      </div>
    </div>
  </div>
  <!-- 针脚路径图放大查看：铺满视口，点击任意处关闭 -->
  <div
    v-if="stitchZoomVisible"
    class="stitch-lightbox"
    @click="stitchZoomVisible = false"
  >
    <img
      :src="stitchImgSrc"
      alt="针脚路径大图"
    />
    <span class="stitch-lightbox-close">× 点击任意处或按 ESC 关闭</span>
  </div>
  <div
    v-if="certVisible"
    class="cert-overlay"
    @click.self="certVisible = false"
  >
    <div class="cert-card">
      <div class="cert-inner">
        <span class="cert-eyebrow">XIUWEN ZHICHUANG · DIGITAL EVIDENCE</span>
        <h3>非遗纹样数字版权存证证书</h3>
        <p class="cert-no">证书编号：{{ certNo }}</p>
        <div class="cert-body">
          <img
            :src="certPatternImage"
            alt="纹样预览"
          />
          <div class="cert-fields">
            <p>
              <small>纹样名称</small><b>{{ certPatternTitle }}</b>
            </p>
            <p>
              <small>打版针法</small><b>{{ stitchTypeLabel }}</b>
            </p>
            <p>
              <small>承接口料</small><b>{{ certFabricLabel }}</b>
            </p>
            <p>
              <small>预计针数</small><b>{{ stitchCount }} 针</b>
            </p>
            <p>
              <small>存证时间</small><b>{{ genTime }}</b>
            </p>
            <p class="cert-hash">
              <small>纹样设计数字指纹（SHA-256）</small>
              <code>{{ designHash }}</code>
            </p>
            <p class="cert-hash">
              <small>DST 打版文件指纹（SHA-256）</small>
              <code>{{ dstHash }}</code>
            </p>
          </div>
        </div>
        <p class="cert-note">
          本证书基于 SHA-256
          加密哈希算法生成，数字指纹唯一且防篡改，可用于非遗文创作品的版权存证、防伪溯源与数字藏品交易。指纹与原始文件一一对应，任何改动都会导致指纹不一致。——
          绣纹智创 · 非遗数字智造平台
        </p>
      </div>
    </div>
    <div class="cert-actions">
      <button
        type="button"
        @click="certVisible = false"
      >
        关闭
      </button>
      <button
        class="primary"
        type="button"
        @click="printCert"
      >
        🖨 打印 / 另存为 PDF
      </button>
    </div>
  </div>
  <div
    v-if="colorModalVisible"
    class="palette-overlay"
    @click.self="closeColorModal(false)"
  >
    <div class="palette-modal">
      <h4>自定义调色</h4>
      <div class="palette-swatch-row">
        <div
          class="palette-swatch"
          :style="{ background: draftColorCss }"
        />
        <code>{{ draftColorHex.toUpperCase() }}</code>
      </div>
      <div
        ref="padRef"
        class="palette-pad"
        :style="{
          background: `linear-gradient(to top, #000, transparent), linear-gradient(to right, #fff, hsl(${draftHue} 100% 50%))`
        }"
        @pointerdown.prevent="padPointerDown"
      >
        <i
          class="palette-pad-cursor"
          :style="{ left: `${draftSat}%`, top: `${100 - draftLight}%` }"
        />
      </div>
      <div class="palette-hue-row">
        <div
          ref="hueRef"
          class="palette-hue-bar"
          @pointerdown.prevent="huePointerDown"
        >
          <i
            class="palette-hue-cursor"
            :style="{ left: `${(draftHue / 360) * 100}%` }"
          />
        </div>
      </div>
      <div class="palette-dots">
        <div
          v-for="(c, i) in customColors"
          :key="i"
          class="palette-dot-item"
        >
          <span
            class="palette-dot"
            :class="{ on: editingIndex === i }"
            :style="{ background: `hsl(${c.h} ${c.s}% ${c.l}%)` }"
            :title="`点击编辑第 ${i + 1} 个颜色`"
            @click="editCustomColor(i)"
          >
            <button
              class="palette-dot-remove"
              type="button"
              @click.stop="removeCustomColor(i)"
            >
              ×
            </button>
          </span>
          <code>{{ hslToHex(c.h, c.s, c.l).toUpperCase() }}</code>
        </div>
        <button
          v-if="customColors.length < 4"
          class="palette-dot-add"
          type="button"
          title="把当前调好的颜色加入配色"
          @click="addOrUpdateDraftColor"
        >
          ＋
        </button>
        <span
          v-if="!customColors.length"
          class="palette-dots-hint"
          >先拖动色盘调出颜色，点 ＋ 加入，共需 2-4 个色</span
        >
        <span
          v-else
          class="palette-dots-hint"
          >点色块可重新编辑</span
        >
      </div>
      <p
        v-if="paletteError"
        class="palette-notice"
      >
        {{ paletteError }}
      </p>
      <div class="palette-modal-footer">
        <button
          class="danger"
          type="button"
          @click="resetCustomColors"
        >
          重置
        </button>
        <button
          class="ghost"
          type="button"
          @click="closeColorModal(false)"
        >
          取消
        </button>
        <button
          class="primary"
          type="button"
          :disabled="effectiveColors.length < 2"
          @click="closeColorModal(true)"
        >
          确定使用（{{ effectiveColors.length }}/4）
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ===== 生成工作台 ===== */
.gen-page {
  min-height: calc(100vh - 82px);
  background: #f8f6f2;
  font-family: 'Noto Sans SC', 'Microsoft YaHei', 'PingFang SC', sans-serif;
}
.gen-workspace {
  max-width: 1648px;
  margin: 0 auto;
  padding: 22px 24px 24px;
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 26px;
}
.gen-side {
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.gen-side-scroll {
  overflow: visible;
  padding-right: 8px;
  padding-bottom: 8px;
}
.gen-side-scroll::-webkit-scrollbar {
  width: 4px;
}
.gen-side-scroll::-webkit-scrollbar-thumb {
  background: #e5e0d8;
  border-radius: 4px;
}
.gen-intro {
  margin: 6px 0 26px;
}
.gen-intro h1 {
  margin: 0 0 8px;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 25px;
  line-height: 1.45;
  font-weight: 600;
  color: #3a352f;
  letter-spacing: 1px;
}
.gen-intro p {
  margin: 0;
  font-size: 11px;
  color: #a09a90;
  line-height: 1.7;
  letter-spacing: 0.5px;
}
.gen-block {
  margin-bottom: 24px;
}
.gen-block-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 12px;
  font-weight: 600;
  color: #5c554c;
  letter-spacing: 1px;
}
.gen-block-title span {
  color: #8a2b2b;
  font-weight: 700;
}
.gen-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.gen-chip {
  padding: 7px 15px;
  border-radius: 999px;
  border: 1px solid #e6e0d6;
  background: #fff;
  color: #8f887e;
  font-size: 12px;
  letter-spacing: 0.5px;
  transition: 0.2s;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
}
.gen-chip:hover {
  transform: none;
  border-color: #c8beb0;
  color: #5c554c;
}
.gen-chip.on {
  background: #2d2a26;
  border-color: #2d2a26;
  color: #fff;
  box-shadow: 0 3px 8px rgba(45, 42, 38, 0.18);
}
.gen-chip-add {
  border-style: dashed;
  background: #fffdfa;
}
.gen-chip-add.on {
  background: #fff2ec;
  color: #8a2b2b;
  border-color: #8a2b2b;
}
.gen-custom-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}
.gen-custom-chip {
  position: relative;
  padding: 6px 28px 6px 13px;
  border: 1px solid #e6e0d6;
  border-radius: 999px;
  background: #fff;
  color: #8f887e;
  font-size: 12px;
  cursor: pointer;
  user-select: none;
  transition: 0.2s;
}
.gen-custom-chip.on {
  background: #2d2a26;
  border-color: #2d2a26;
  color: #fff;
}
.gen-custom-chip-remove {
  position: absolute;
  right: 7px;
  top: 50%;
  transform: translateY(-50%);
  width: 14px;
  height: 14px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.12);
  color: inherit;
  font-size: 10px;
  line-height: 1;
  display: grid;
  place-items: center;
  cursor: pointer;
}
.gen-custom-chip-remove:hover {
  transform: translateY(-50%);
  background: rgba(0, 0, 0, 0.25);
}
.gen-text-input {
  width: 100%;
  margin-top: 10px;
  padding: 9px 12px;
  border: 1px solid #e6e0d6;
  border-radius: 9px;
  background: #fff;
  font-size: 12px;
  color: #3a352f;
  outline: none;
}
.gen-text-input:focus {
  border-color: #8a2b2b;
}
.gen-palettes {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.gen-palette {
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 11px 12px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  text-align: left;
  cursor: pointer;
  transition: 0.2s;
}
.gen-palette:hover {
  transform: none;
  border-color: #e6e0d6;
}
.gen-palette.on {
  border-color: rgba(138, 43, 43, 0.35);
  box-shadow: 0 3px 10px rgba(138, 43, 43, 0.08);
}
.gen-palette-name {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #8f887e;
  letter-spacing: 0.5px;
}
.gen-palette.on .gen-palette-name {
  color: #8a2b2b;
  font-weight: 600;
}
.gen-palette-name em {
  font-style: normal;
  font-size: 10px;
  color: #8a2b2b;
}
.gen-palette-bar {
  display: block;
  height: 4px;
  width: 100%;
  border-radius: 4px;
}
.gen-palette-custom {
  justify-content: center;
  align-items: center;
  text-align: center;
}
.gen-palette-custom .gen-palette-name {
  justify-content: center;
}
.gen-advanced {
  margin-top: 6px;
  margin-bottom: 18px;
  padding-top: 18px;
  border-top: 1px solid #eae5dd;
}
.gen-advanced-toggle {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #8f887e;
  font-size: 12px;
  letter-spacing: 0.5px;
  cursor: pointer;
}
.gen-advanced-toggle:hover {
  transform: none;
  color: #5c554c;
}
.gen-advanced-arrow {
  font-size: 10px;
  transition: transform 0.2s;
}
.gen-advanced-arrow.on {
  transform: rotate(180deg);
}
.gen-advanced-body {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.gen-field-label {
  font-size: 11px;
  color: #a09a90;
}
.gen-textarea {
  width: 100%;
  height: 76px;
  padding: 10px 12px;
  border: 1px solid #e6e0d6;
  border-radius: 10px;
  background: #fff;
  resize: none;
  font-size: 12px;
  color: #3a352f;
  outline: none;
}
.gen-textarea:focus {
  border-color: #8a2b2b;
}
.gen-upload {
  position: relative;
  width: 100%;
  height: 86px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  padding: 5px;
  border: 1px dashed #d6cfc3;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.6);
  color: #a09a90;
  font-size: 11px;
}
.gen-upload:hover {
  transform: none;
  border-color: #b9af9f;
  background: #fff;
}
.gen-upload b {
  font-size: 11px;
  font-weight: 500;
  color: #8f887e;
}
.gen-upload small {
  font-size: 9px;
  color: #b5aea4;
}
.gen-upload img {
  width: 100%;
  height: 74px;
  object-fit: cover;
  border-radius: 8px;
}
.gen-side-footer {
  flex: 0 0 auto;
  padding-top: 14px;
  background: linear-gradient(180deg, rgba(248, 246, 242, 0), #f8f6f2 30%);
}
.gen-side-notice {
  margin: 0 0 8px;
  font-size: 11px;
  line-height: 1.5;
  color: #8f887e;
}
.gen-generate {
  width: 100%;
  padding: 13px 16px;
  border: 0;
  border-radius: 12px;
  background: linear-gradient(135deg, #a83232, #7a2020);
  color: #fff;
  font-size: 14px;
  letter-spacing: 3px;
  font-weight: 500;
  box-shadow: 0 10px 20px -6px rgba(168, 50, 50, 0.45);
  transition: 0.25s;
}
.gen-generate:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 15px 25px -6px rgba(168, 50, 50, 0.5);
}
.gen-generate:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.gen-main {
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  gap: 16px;
  min-height: calc(100vh - 150px);
  min-width: 0;
}
.gen-canvas {
  position: relative;
  min-height: 0;
  overflow: hidden;
  border-radius: 24px;
  background: #eae7df;
  box-shadow: inset 0 4px 20px rgba(0, 0, 0, 0.05);
  display: grid;
  place-items: center;
}
/* 主图用绝对定位铺满画布: 让 height:100% 相对画布解析, 配合 contain 完整显示不裁切 */
.gen-canvas-img {
  position: absolute;
  inset: 0;
  z-index: 1;
  width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: center;
  display: block;
}
/* 方图放进宽画布时两侧留白, 用同一张图的模糊副本填充背景 */
.gen-canvas-backdrop {
  position: absolute;
  inset: 0;
  z-index: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: blur(34px) saturate(1.05) brightness(0.92);
  transform: scale(1.12);
  opacity: 0.55;
  pointer-events: none;
}
.gen-canvas-img.enhanced {
  filter: saturate(1.16) contrast(1.08);
}
.gen-canvas-img.tone-rich:not(.enhanced) {
  filter: saturate(1.2) contrast(1.05);
}
.gen-canvas-img.tone-soft:not(.enhanced) {
  filter: saturate(0.78) brightness(1.08);
}
.gen-canvas-empty,
.gen-canvas-progress {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 24px;
  text-align: center;
  color: #a09a90;
}
.gen-canvas-empty span {
  font-size: 34px;
  color: #b73722;
  opacity: 0.75;
}
.gen-canvas-empty b {
  font-size: 17px;
  color: #6b6257;
  font-weight: 600;
}
.gen-canvas-empty p {
  margin: 0;
  font-size: 12px;
}
.gen-canvas-progress b {
  font-size: 15px;
  color: #5c554c;
}
.gen-canvas-progress p {
  margin: 0;
  font-size: 12px;
}
.gen-canvas-progress small {
  max-width: 360px;
  font-size: 11px;
  line-height: 1.6;
  color: #aca59a;
}
.gen-status {
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 14px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  background: rgba(45, 42, 38, 0.55);
  backdrop-filter: blur(10px);
  color: #fff;
  font-size: 11px;
  letter-spacing: 1px;
}
.gen-status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #4ade80;
  box-shadow: 0 0 6px rgba(74, 222, 128, 0.8);
  animation: gen-pulse 1.6s infinite;
}
@keyframes gen-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.4;
  }
}
.gen-toolbar {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(12px);
  box-shadow: 0 10px 20px -5px rgba(0, 0, 0, 0.12);
  opacity: 0;
  transform: translateY(-10px);
  transition: 0.3s ease;
}
.gen-canvas:hover .gen-toolbar,
.gen-canvas:focus-within .gen-toolbar {
  opacity: 1;
  transform: none;
}
.gen-toolbar button {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: #555;
  font-size: 16px;
  transition: 0.2s;
}
.gen-toolbar button:hover:not(:disabled) {
  transform: none;
  background: #fff;
  color: #8a2b2b;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}
.gen-toolbar button.on {
  background: #fff;
  color: #8a2b2b;
}
.gen-toolbar button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.gen-toast {
  position: absolute;
  top: 22px;
  left: 50%;
  z-index: 3;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  border-radius: 999px;
  background: rgba(45, 42, 38, 0.86);
  color: #fff;
  font-size: 12px;
  letter-spacing: 0.5px;
  white-space: nowrap;
  box-shadow: 0 10px 24px -8px rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(8px);
  pointer-events: none;
  animation: gen-toast-in 0.22s ease;
}
@keyframes gen-toast-in {
  from {
    opacity: 0;
    transform: translate(-50%, -8px);
  }
  to {
    opacity: 1;
    transform: translate(-50%, 0);
  }
}
.gen-3d-entry {
  position: absolute;
  right: 22px;
  bottom: 22px;
  z-index: 2;
  padding: 11px 22px;
  border: 0;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #2d2a26;
  font-size: 12px;
  letter-spacing: 1px;
  font-weight: 600;
  box-shadow: 0 10px 24px -6px rgba(0, 0, 0, 0.18);
  backdrop-filter: blur(8px);
  transition: 0.25s;
}
.gen-3d-entry:hover {
  transform: translateY(-2px);
  background: #fff;
  color: #8a2b2b;
}
.gen-dst-card {
  padding: 16px 18px;
  border: 1px solid #ebe7e0;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 8px 24px -16px rgba(87, 55, 27, 0.25);
}
.gen-dst-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  margin-bottom: 12px;
}
.gen-dst-head .gen-block-title {
  margin-bottom: 0;
}
.gen-restore-badge {
  padding: 3px 9px;
  border-radius: 5px;
  background: #f0fdf4;
  color: #2f855a;
  font-size: 10px;
  letter-spacing: 0.5px;
  white-space: nowrap;
}
.gen-dst-row {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 16px;
  align-items: end;
}
.gen-dst-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.gen-dst-field > span {
  font-size: 10px;
  color: #a09a90;
  letter-spacing: 0.5px;
}
.gen-dst-field select {
  width: 100%;
  padding: 9px 10px;
  border: 1px solid #eae5dd;
  border-radius: 8px;
  background: #fdfcfa;
  color: #3a352f;
  font-size: 12px;
  outline: none;
  cursor: pointer;
}
.gen-dst-field select:focus {
  border-color: #8a2b2b;
}
.gen-dst-field select:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.gen-dst-btn {
  padding: 10px 24px;
  border: 0;
  border-radius: 9px;
  background: #8a2b2b;
  color: #fff;
  font-size: 12px;
  letter-spacing: 1px;
  box-shadow: 0 6px 14px -6px rgba(138, 43, 43, 0.5);
  transition: 0.2s;
}
.gen-dst-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  background: #6b2020;
}
.gen-dst-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
@media (max-width: 1180px) {
  .gen-page {
    height: auto;
    min-height: calc(100vh - 82px);
    overflow: visible;
  }
  .gen-workspace {
    height: auto;
    grid-template-columns: 1fr;
    padding-bottom: 32px;
  }
  .gen-side-scroll {
    overflow: visible;
    padding-right: 0;
  }
  .gen-main {
    grid-template-rows: auto auto;
  }
  .gen-canvas {
    min-height: 460px;
  }
}
@media (max-width: 680px) {
  .gen-workspace {
    padding: 16px;
    gap: 18px;
  }
  .gen-canvas {
    min-height: 320px;
  }
  .gen-dst-row {
    grid-template-columns: 1fr 1fr;
  }
  .gen-dst-btn {
    grid-column: 1 / -1;
  }
}
@media (hover: none) {
  .gen-toolbar {
    opacity: 1;
    transform: none;
  }
}
.dst-row {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
}
.dst-type-label {
  margin: 0;
  font-size: 14px;
  color: var(--muted);
  white-space: nowrap;
}
.dst-type-select {
  flex: 1;
  min-width: 0;
  padding: 9px 12px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fffdfa;
  font-size: 14px;
  color: var(--ink);
  cursor: pointer;
}
.dst-type-select:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.dst-row .dst-btn {
  flex: 0 0 auto;
  grid-column: auto;
  margin-top: 0;
  padding: 10px 18px;
}
.dst-btn-auto {
  grid-column: 1 / -1;
  margin-top: 10px;
  padding: 13px 18px;
  border: 0;
  border-radius: 12px;
  background: linear-gradient(135deg, #f7d774, #d9a94f 45%, #b97b2a);
  color: #4a2c07;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 1px;
  cursor: pointer;
  box-shadow: 0 8px 22px rgba(217, 169, 79, 0.35);
  transition:
    filter 0.15s ease,
    transform 0.15s ease;
}
.dst-btn-auto:hover:not(:disabled) {
  filter: brightness(1.06);
  transform: translateY(-1px);
}
.dst-btn-auto:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.dst-auto-note {
  margin: 4px 0 10px;
  padding: 10px 12px;
  background: linear-gradient(135deg, #fdf6e3, #fbeecb);
  border: 1px solid #ecd3a8;
  border-radius: 10px;
  font-size: 13px;
  color: #6b4a12;
  line-height: 1.8;
}
.dst-auto-note small {
  color: #8a6a2f;
}
.dst-tip {
  margin: -4px 0 10px;
  font-size: 13px;
  color: #8a7b6a;
  line-height: 1.6;
}
.dst-comp {
  margin: 0 0 10px;
  padding: 8px 11px;
  border: 1px solid #c4ded7;
  border-radius: 9px;
  background: #e6f2ef;
  color: #0b625c;
  font-size: 13px;
  font-weight: 600;
}
.dst-hashbox {
  margin: 0 0 10px;
  padding: 11px 12px;
  border: 1px solid #ecd3a8;
  border-radius: 10px;
  background: linear-gradient(135deg, #fdf6e3, #fbeecb);
}
.hashbox-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 7px;
}
.hashbox-head b {
  font-size: 13px;
  color: #6b4a12;
}
.hashbox-head small {
  font-size: 11px;
  color: #a5844a;
}
.hashbox-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin: 4px 0;
  font-size: 12px;
  color: #6b5a3e;
}
.hashbox-row code {
  font-family: Consolas, monospace;
  font-size: 12px;
  color: #0b625c;
}
.dst-hashbox button {
  margin-top: 8px;
  width: 100%;
  padding: 8px 12px;
  border: 0;
  border-radius: 8px;
  background: linear-gradient(135deg, #c9953f, #a9741f);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.dst-hashbox button:hover {
  filter: brightness(1.06);
}
/* ===== 版权存证证书 ===== */
.cert-overlay {
  position: fixed;
  inset: 0;
  z-index: 1300;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 24px;
  background: rgba(24, 18, 14, 0.65);
}
.cert-card {
  width: min(780px, 100%);
  max-height: 82vh;
  overflow: auto;
  background: #fffdf8;
  border: 2px solid #c9953f;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.35);
  padding: 10px;
}
.cert-inner {
  border: 1px solid #e5d3ae;
  padding: 28px 32px 22px;
}
.cert-eyebrow {
  color: #c9953f;
  letter-spacing: 3px;
  font-size: 11px;
}
.cert-card h3 {
  margin: 6px 0 2px;
  font-size: 25px;
  letter-spacing: 4px;
  color: #2d2118;
}
.cert-no {
  margin: 4px 0 16px;
  font-family: Consolas, monospace;
  font-size: 13px;
  color: #8a7b6a;
}
.cert-body {
  display: grid;
  grid-template-columns: 168px 1fr;
  gap: 22px;
  align-items: start;
}
.cert-body img {
  width: 168px;
  height: 168px;
  object-fit: cover;
  border: 1px solid var(--line);
  border-radius: 10px;
}
.cert-fields p {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  margin: 7px 0;
  font-size: 14px;
}
.cert-fields small {
  color: #8a7b6a;
  flex: 0 0 auto;
}
.cert-fields b {
  color: #2d2118;
  text-align: right;
}
.cert-hash {
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  margin-top: 12px !important;
}
.cert-hash code {
  font-family: Consolas, monospace;
  font-size: 11px;
  color: #0b625c;
  word-break: break-all;
  text-align: left;
}
.cert-note {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed #d8c7ae;
  font-size: 12px;
  line-height: 1.7;
  color: #8a7b6a;
}
.cert-actions {
  display: flex;
  gap: 12px;
}
@media (max-width: 640px) {
  .cert-body {
    grid-template-columns: 1fr;
  }
  .cert-body img {
    width: 100%;
    max-width: 220px;
  }
}
.dst-legend {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px 14px;
  margin-top: 10px;
  font-size: 12px;
  color: #5c5148;
}
.dst-legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
}
.dst-legend-item i {
  width: 12px;
  height: 12px;
  flex: 0 0 12px;
  border-radius: 3px;
  display: inline-block;
}
.dst-legend-item .dst-legend-bg {
  background: #fff;
  border: 1px solid #c8c0b5;
}
.dst-quote {
  margin: 6px 0 10px;
  padding: 12px 14px;
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: 10px;
}
.dst-quote-title {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 700;
  color: #b7791f;
  letter-spacing: 0.5px;
}
.dst-quote-row {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin: 5px 0;
  font-size: 13px;
  color: #5c5148;
}
.dst-quote-row b {
  font-size: 14px;
  color: var(--ink);
}
.dst-quote-row.total {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #d8c7ae;
  font-weight: 700;
}
.dst-quote-row.total b {
  font-size: 18px;
  color: var(--red);
}
.dst-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(24, 18, 14, 0.55);
}
.dst-modal {
  width: 100%;
  max-width: 780px;
  padding: 22px 24px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.28);
}
.dst-modal h3 {
  margin: 0 0 16px;
  font-size: 20px;
}
.dst-modal-body {
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: 20px;
  align-items: center;
}
.dst-modal-left img {
  width: 100%;
  border: 1px solid #f0e6dc;
  border-radius: 8px;
  background: #fff;
}
/* 针脚路径图可点击放大 */
.dst-modal-left .dst-zoomable {
  cursor: zoom-in;
  transition:
    box-shadow 0.15s ease,
    transform 0.15s ease;
}
.dst-modal-left .dst-zoomable:hover {
  box-shadow: 0 6px 18px rgba(43, 108, 176, 0.25);
  transform: scale(1.01);
}
.dst-zoom-hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: #8a7b6a;
  text-align: center;
}
.stitch-lightbox {
  position: fixed;
  inset: 0;
  z-index: 1500;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 30px;
  background: rgba(24, 18, 14, 0.88);
  cursor: zoom-out;
}
.stitch-lightbox img {
  max-width: 94vw;
  max-height: 88vh;
  object-fit: contain;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.5);
}
.stitch-lightbox-close {
  color: #e8ddd0;
  font-size: 14px;
  letter-spacing: 1px;
}
.dst-modal-right p {
  margin: 12px 0;
  font-size: 15px;
  color: #5c5148;
}
.dst-status {
  color: #1a9c46;
  font-weight: 600;
}
.dst-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 18px;
}
.dst-close {
  padding: 10px 22px;
  background: #fff;
  border: 1px solid #e3d9ce;
  border-radius: 10px;
  cursor: pointer;
}
.dst-download {
  display: inline-flex;
  align-items: center;
  padding: 10px 20px;
  border-radius: 10px;
  text-decoration: none;
}
.dst-factory {
  padding: 10px 20px;
  border: 0;
  border-radius: 10px;
  background: linear-gradient(135deg, #1f6f50, var(--jade, #2e8b6a));
  color: #fff;
  cursor: pointer;
}
.dst-factory-done {
  margin: 0;
  align-self: center;
  color: #1a9c46;
  font-size: 14px;
  font-weight: 600;
}
@media (max-width: 720px) {
  .dst-modal-body {
    grid-template-columns: 1fr;
  }
}
</style>

<!-- 打印证书：只打印证书卡片，其余全部隐藏 -->
<style>
@media print {
  body * {
    visibility: hidden !important;
  }
  .cert-card,
  .cert-card * {
    visibility: visible !important;
  }
  .cert-card {
    position: fixed !important;
    inset: 0 !important;
    width: 100% !important;
    max-height: none !important;
    overflow: visible !important;
    margin: 0 !important;
    box-shadow: none !important;
    border: none !important;
  }
  .cert-actions {
    display: none !important;
  }
}
</style>
