<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue';

export type DesignConfig = {
  x: number;
  y: number;
  scale: number;
  rotation: number;
};

const props = defineProps<{
  productImage: string;
  patternImage: string;
  modelValue: DesignConfig;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: DesignConfig];
}>();

const canvas = ref<HTMLCanvasElement | null>(null);
const product = ref<HTMLImageElement | null>(null);
const pattern = ref<HTMLImageElement | null>(null);
const loading = ref(false);
const error = ref('');
let loadVersion = 0;
let dragging = false;
let pointerX = 0;
let pointerY = 0;

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value));
}

function updateConfig(partial: Partial<DesignConfig>) {
  emit('update:modelValue', { ...props.modelValue, ...partial });
}

function loadImage(src: string) {
  return new Promise<HTMLImageElement>((resolve, reject) => {
    const image = new Image();
    image.crossOrigin = 'anonymous';
    image.onload = () => resolve(image);
    image.onerror = () => reject(new Error('图片加载失败'));
    image.src = src;
  });
}

function drawContainedImage(
  context: CanvasRenderingContext2D,
  image: HTMLImageElement,
  width: number,
  height: number
) {
  const ratio = Math.min(width / image.naturalWidth, height / image.naturalHeight);
  const drawWidth = image.naturalWidth * ratio;
  const drawHeight = image.naturalHeight * ratio;
  context.drawImage(
    image,
    (width - drawWidth) / 2,
    (height - drawHeight) / 2,
    drawWidth,
    drawHeight
  );
}

function redraw() {
  const target = canvas.value;
  const productImage = product.value;
  const patternImage = pattern.value;
  if (!target || !productImage || !patternImage) return;

  const context = target.getContext('2d');
  if (!context) return;
  const width = target.width;
  const height = target.height;
  context.clearRect(0, 0, width, height);
  context.fillStyle = '#fffaf3';
  context.fillRect(0, 0, width, height);
  drawContainedImage(context, productImage, width, height);

  const baseSize = Math.min(width, height) * 0.42;
  const aspect = patternImage.naturalWidth / patternImage.naturalHeight || 1;
  const patternWidth = (aspect >= 1 ? baseSize : baseSize * aspect) * props.modelValue.scale;
  const patternHeight = (aspect >= 1 ? baseSize / aspect : baseSize) * props.modelValue.scale;
  const centerX = width * props.modelValue.x;
  const centerY = height * props.modelValue.y;

  context.save();
  context.translate(centerX, centerY);
  context.rotate((props.modelValue.rotation * Math.PI) / 180);
  context.drawImage(
    patternImage,
    -patternWidth / 2,
    -patternHeight / 2,
    patternWidth,
    patternHeight
  );
  context.restore();
}

async function loadLayers() {
  const version = ++loadVersion;
  error.value = '';
  if (!props.productImage || !props.patternImage) {
    product.value = null;
    pattern.value = null;
    error.value = '请选择商品和纹样';
    return;
  }
  loading.value = true;
  try {
    const [productImage, patternImage] = await Promise.all([
      loadImage(props.productImage),
      loadImage(props.patternImage)
    ]);
    if (version !== loadVersion) return;
    product.value = productImage;
    pattern.value = patternImage;
    await nextTick();
    redraw();
  } catch {
    if (version === loadVersion) error.value = '商品图或纹样图加载失败，请重新选择';
  } finally {
    if (version === loadVersion) loading.value = false;
  }
}

function pointerPosition(event: PointerEvent) {
  const target = canvas.value;
  if (!target) return null;
  const rect = target.getBoundingClientRect();
  return {
    x: clamp((event.clientX - rect.left) / rect.width, 0, 1),
    y: clamp((event.clientY - rect.top) / rect.height, 0, 1)
  };
}

function startDrag(event: PointerEvent) {
  dragging = true;
  pointerX = event.clientX;
  pointerY = event.clientY;
  canvas.value?.setPointerCapture(event.pointerId);
}

function drag(event: PointerEvent) {
  if (!dragging || !canvas.value) return;
  const rect = canvas.value.getBoundingClientRect();
  const deltaX = (event.clientX - pointerX) / rect.width;
  const deltaY = (event.clientY - pointerY) / rect.height;
  pointerX = event.clientX;
  pointerY = event.clientY;
  updateConfig({
    x: clamp(props.modelValue.x + deltaX, 0, 1),
    y: clamp(props.modelValue.y + deltaY, 0, 1)
  });
}

function stopDrag(event: PointerEvent) {
  dragging = false;
  if (canvas.value?.hasPointerCapture(event.pointerId))
    canvas.value.releasePointerCapture(event.pointerId);
}

function movePattern(event: PointerEvent) {
  const position = pointerPosition(event);
  if (position) updateConfig(position);
}

function zoom(event: WheelEvent) {
  event.preventDefault();
  const step = event.deltaY > 0 ? -0.05 : 0.05;
  updateConfig({ scale: Number(clamp(props.modelValue.scale + step, 0.1, 2).toFixed(2)) });
}

function updateScale(event: Event) {
  updateConfig({ scale: Number((event.target as HTMLInputElement).value) });
}

function updateRotation(event: Event) {
  updateConfig({ rotation: Number((event.target as HTMLInputElement).value) });
}

function exportBlob() {
  return new Promise<Blob>((resolve, reject) => {
    if (!canvas.value || !product.value || !pattern.value) {
      reject(new Error('预览尚未准备完成'));
      return;
    }
    try {
      canvas.value.toBlob((blob) => {
        if (blob) resolve(blob);
        else reject(new Error('定制预览导出失败'));
      }, 'image/png');
    } catch {
      reject(new Error('图片跨域配置异常，无法导出定制预览'));
    }
  });
}

watch(() => [props.productImage, props.patternImage], loadLayers, { immediate: true });
watch(() => props.modelValue, redraw, { deep: true });
onBeforeUnmount(() => {
  loadVersion += 1;
});

defineExpose({ exportBlob });
</script>

<template>
  <div class="pattern-composer">
    <div
      class="canvas-shell"
      :class="{ loading }"
    >
      <canvas
        ref="canvas"
        width="600"
        height="600"
        aria-label="定制商品实时预览"
        @pointerdown="startDrag"
        @pointermove="drag"
        @pointerup="stopDrag"
        @pointercancel="stopDrag"
        @dblclick="movePattern"
        @wheel="zoom"
      ></canvas>
      <span v-if="loading">正在加载预览…</span>
    </div>
    <p
      v-if="error"
      class="composer-error"
    >
      {{ error }}
    </p>
    <div class="composer-controls">
      <label>
        <span>缩放 {{ modelValue.scale.toFixed(2) }}</span>
        <input
          type="range"
          min="0.1"
          max="2"
          step="0.05"
          :value="modelValue.scale"
          @input="updateScale"
        />
      </label>
      <label>
        <span>旋转 {{ modelValue.rotation }}°</span>
        <input
          type="range"
          min="0"
          max="360"
          step="1"
          :value="modelValue.rotation"
          @input="updateRotation"
        />
      </label>
    </div>
    <small>拖动纹样调整位置，滚轮或滑杆调整大小。</small>
  </div>
</template>

<style scoped>
.pattern-composer {
  display: grid;
  gap: 14px;
}
.canvas-shell {
  position: relative;
  width: min(100%, 600px);
  aspect-ratio: 1;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: #fffaf3;
}
.canvas-shell canvas {
  display: block;
  width: 100%;
  height: 100%;
  cursor: grab;
  touch-action: none;
}
.canvas-shell canvas:active {
  cursor: grabbing;
}
.canvas-shell span {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgba(255, 250, 243, 0.72);
  color: var(--muted);
}
.composer-controls {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}
.composer-controls label {
  display: grid;
  gap: 8px;
  color: var(--ink);
}
.composer-controls input {
  width: 100%;
  accent-color: var(--red);
}
.pattern-composer small {
  color: var(--muted);
}
.composer-error {
  margin: 0;
  color: var(--red);
}
@media (max-width: 640px) {
  .composer-controls {
    grid-template-columns: 1fr;
  }
}
</style>
