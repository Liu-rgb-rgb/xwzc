<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { api, listFrom } from '../../api';
import { authState, isDemoAccount } from '../../auth';
import { patterns as demoPatterns } from '../../data';
import { readLastFinishedGeneration } from '../../generation';
import { readUserData, userDataEvent } from '../../userData';

type StitchKey = 'tatami' | 'satin' | 'outline' | 'knot';
type LayerKey = 'fabric' | StitchKey;
type DemoMode = 'layers' | 'replay' | 'gloss';
interface PatternLite {
  id: string;
  title: string;
  image: string;
  style?: string;
  elements?: string[];
}
interface LayerMeta {
  key: LayerKey;
  name: string;
  part: string;
  desc: string;
  color: string;
  stitchOrder: number;
}

const route = useRoute();

/* ===== 工艺层定义：与 py/api.py 的 auto 打版分层同口径 ===== */
const layerMeta: LayerMeta[] = [
  {
    key: 'fabric',
    name: '底布层',
    part: '绣布基材',
    desc: '棉质绣布作为承力基材，经向与纬向的张力决定后续针脚的平整度。',
    color: '#b89a6a',
    stitchOrder: 0
  },
  {
    key: 'tatami',
    name: '他他米针层',
    part: '主体填充',
    desc: '大间距平行长针填充大面积色块，针迹扁平、省线，是工业绣花的主力填充针法，常用于花瓣、衣身、兽身等宽面。',
    color: '#2b6cb0',
    stitchOrder: 1
  },
  {
    key: 'satin',
    name: '平包针层',
    part: '细节窄条',
    desc: '在细长区域与线条上蛇形密排短针，紧贴轮廓形成饱满的光泽带，常用于枝干、鬃毛、笔画等窄条结构。',
    color: '#1a9c46',
    stitchOrder: 2
  },
  {
    key: 'outline',
    name: '轮廓走针层',
    part: '轮廓勾边',
    desc: '沿纹样外形与内部纹理线逐点落针，先填充后勾边，让形态边界清晰挺括。',
    color: '#b73722',
    stitchOrder: 3
  },
  {
    key: 'knot',
    name: '打籽颗粒层',
    part: '点缀颗粒',
    desc: '把丝线绕成颗粒固定在深色小面积处，形成高低错落的立体籽粒，是立体感最强的点缀层，常用于花蕊、眼睛、纹饰中心。',
    color: '#8a2b2b',
    stitchOrder: 4
  }
];
const stitchKeys: StitchKey[] = ['tatami', 'satin', 'outline', 'knot'];
const layerOrder: Record<StitchKey, number> = { tatami: 0, satin: 1, outline: 2, knot: 3 };
const modeMeta: Array<{ key: DemoMode; name: string; hint: string }> = [
  { key: 'layers', name: '结构分层', hint: '展开工艺层，查看每一层的针脚结构' },
  { key: 'replay', name: '绣制回放', hint: '按打版工序逐针生长，还原机器绣制过程' },
  { key: 'gloss', name: '丝线质感', hint: '增强丝线光泽与张力，贴近观察针脚起伏' }
];

const demoTone = ref<DemoMode>('layers');
const selectedLayer = ref<LayerKey | null>(null);
const annotations = ref(true);
const explode = ref(55);
const tension = ref(80);
const layerVisible = reactive<Record<LayerKey, boolean>>({
  fabric: true,
  tatami: true,
  satin: true,
  outline: true,
  knot: true
});
const layerCounts = reactive<Record<LayerKey, number>>({
  fabric: 0,
  tatami: 0,
  satin: 0,
  outline: 0,
  knot: 0
});

const explodeAmount = computed(() => explode.value / 100);
const totalStitches = computed(() =>
  stitchKeys.reduce((sum, key) => sum + layerCounts[key], 0)
);
const activeLayerCount = computed(
  () => stitchKeys.filter((key) => layerCounts[key] > 0).length
);
const activeMeta = computed(
  () => layerMeta.find((item) => item.key === selectedLayer.value) || null
);
const demoToneName = computed(
  () => modeMeta.find((item) => item.key === demoTone.value)?.name || ''
);

function shareOf(key: LayerKey) {
  if (key === 'fabric' || !totalStitches.value) return 0;
  return Math.round((layerCounts[key] / totalStitches.value) * 100);
}
function formatCount(value: number) {
  return Math.max(0, Math.round(value)).toLocaleString('zh-CN');
}

/* ===== 页面数据 ===== */
const library = ref<PatternLite[]>([]);
const serverPatterns = ref<PatternLite[]>([]);
const uploads = ref<PatternLite[]>([]);
const selectedId = ref('');
const currentTitle = ref('');
const rendering = ref(false);
const renderError = ref('');
const autoRotate = ref(false);
const notice = ref('');
const uploadInput = ref<HTMLInputElement | null>(null);
let noticeTimer: number | null = null;

const statusText = computed(() => {
  if (renderError.value) return '纹样解析失败';
  if (rendering.value) return '正在解析纹样工艺层…';
  if (demoTone.value === 'replay') {
    return replayDone.value
      ? `绣制完成 · ${formatCount(totalStitches.value)} 针`
      : `绣制回放 · ${formatCount(replaySewn.value)} / ${formatCount(totalStitches.value)} 针`;
  }
  if (demoTone.value === 'gloss') return `丝线质感 · 张力 ${tension.value}%`;
  return `结构分层 · ${activeLayerCount.value} 个工艺模块`;
});

function showNotice(text: string) {
  notice.value = text;
  if (noticeTimer) window.clearTimeout(noticeTimer);
  noticeTimer = window.setTimeout(() => {
    notice.value = '';
    noticeTimer = null;
  }, 2400);
}

/* ===== 纹样库：与「我的纹样」共享本地数据 ===== */
function normalizePattern(item: any, index = 0): PatternLite | null {
  const image =
    item?.imageUrl || item?.thumbnailUrl || item?.previewImageUrl || item?.image || '';
  if (!image) return null;
  return {
    id: String(item?.id ?? item?.patternId ?? `local-${index}`),
    title: String(item?.title || item?.name || item?.patternName || `纹样 ${index + 1}`),
    image,
    style: item?.style || item?.styleName || '',
    elements: Array.isArray(item?.elements) ? item.elements.map(String) : []
  };
}

function dedupePatterns(items: PatternLite[]): PatternLite[] {
  const seen = new Set<string>();
  const result: PatternLite[] = [];
  for (const item of items) {
    const key = String(item.id);
    if (seen.has(key)) continue;
    seen.add(key);
    result.push(item);
  }
  return result;
}

function refreshLibrary() {
  const saved = readUserData<any[]>('saved_patterns', []);
  const recent = readUserData<any[]>('recent_generations', []);
  const last = readLastFinishedGeneration();
  const finished = last?.snapshot?.patterns || [];
  const local = [...saved, ...recent, ...finished]
    .map((item, index) => normalizePattern(item, index))
    .filter(Boolean) as PatternLite[];
  let merged = dedupePatterns([...uploads.value, ...local, ...serverPatterns.value]);
  if (!merged.length) {
    merged = demoPatterns
      .map((item, index) => normalizePattern(item, index))
      .filter(Boolean) as PatternLite[];
  }
  library.value = merged;
}

async function loadServerPatterns() {
  if (!authState.token || isDemoAccount.value) return;
  try {
    const result: any = await api.patterns.mine({ page: 1, pageSize: 60 });
    serverPatterns.value = listFrom(result)
      .map((item, index) => normalizePattern(item, index))
      .filter(Boolean) as PatternLite[];
    refreshLibrary();
  } catch {
    /* 接口不可用时仅展示本地纹样 */
  }
}

function onUserDataChange(event: Event) {
  const name = (event as CustomEvent).detail?.name;
  if (
    ['saved_patterns', 'recent_generations', 'pattern_favorites', 'applied_designs'].includes(name)
  ) {
    refreshLibrary();
  }
}

function resolveInitialPattern(): PatternLite | null {
  const queryId = String(route.query.patternId ?? '');
  if (queryId) {
    const hit = library.value.find((item) => item.id === queryId);
    if (hit) return hit;
  }
  const last = readLastFinishedGeneration();
  const finished = last?.snapshot?.patterns?.[0];
  if (finished) {
    const normalized = normalizePattern(finished, 0);
    if (normalized && (!queryId || normalized.id === queryId || !library.value.length)) {
      return normalized;
    }
  }
  return library.value[0] || null;
}

/* ===== Three.js ===== */
const HOOP_R = 1.72;
const SAMPLE = 384;
const STEP_PX = 3.6;
const STITCH_HALF = HOOP_R * 0.98;
const BOARD_HALF = STITCH_HALF + 0.16;
const EDGE_DIST = 1.6;
const KNOT_LUM = 100;
const KNOT_DIST = 14;
const TATAMI_DIST = 4;
const REPLAY_RATE = 2400;
const LAYER_GAP = 0.5;
const LAYER_LIFT = 0.2;

type ViewMode = 'canvas' | 'hoop';
type DragMode = 'pan' | 'rotate';
const viewMode = ref<ViewMode>('canvas');
const dragMode = ref<DragMode>('pan');
const canvasHost = ref<HTMLDivElement | null>(null);
const calloutHost = ref<HTMLDivElement | null>(null);

interface RenderTuning {
  length: number;
  width: number;
  height: number;
  z: number;
}
const renderTuning: Record<StitchKey, RenderTuning> = {
  tatami: { length: 1.55, width: 1.06, height: 0.6, z: 0.004 },
  satin: { length: 0.98, width: 0.82, height: 1.05, z: 0.012 },
  outline: { length: 0.75, width: 0.5, height: 1.3, z: 0.022 },
  knot: { length: 1, width: 0.72, height: 1.5, z: 0.03 }
};

let renderer: THREE.WebGLRenderer | undefined;
let scene: THREE.Scene | undefined;
let camera: THREE.PerspectiveCamera | undefined;
let controls: OrbitControls | undefined;
let stageRoot: THREE.Group | undefined;
let stitchGroup: THREE.Group | undefined;
let fabricGroup: THREE.Group | undefined;
let layerGroups: Partial<Record<LayerKey, THREE.Group>> = {};
let replayOrder: Array<{ key: StitchKey; mesh: THREE.InstancedMesh; count: number }> = [];
let threadMaterials: THREE.MeshPhysicalMaterial[] = [];
let sweepLight: THREE.DirectionalLight | undefined;
let frameId = 0;
let resizeObserver: ResizeObserver | undefined;
let homeDistance = 5.2;
let cameraGoal: THREE.Vector3 | null = null;
let targetGoal: THREE.Vector3 | null = null;
const clock = new THREE.Clock();
const probeVector = new THREE.Vector3();
const tempVector = new THREE.Vector3();
const calloutWorld = new THREE.Vector3();
const zAxis = new THREE.Vector3(0, 0, 1);

const replayPlaying = ref(false);
const replayDone = ref(false);
const replaySewn = ref(0);
let replayElapsed = 0;

const replayPercent = computed(() => {
  if (!totalStitches.value) return 0;
  return Math.min(100, (replaySewn.value / totalStitches.value) * 100);
});

function computeHomePosition(hostWidth: number, hostHeight: number) {
  if (!camera) return;
  camera.aspect = hostWidth / Math.max(1, hostHeight);
  camera.updateProjectionMatrix();
  const baseRadius = viewMode.value === 'canvas' ? BOARD_HALF * 1.12 : HOOP_R + 0.16;
  const radius = baseRadius + (demoTone.value === 'layers' ? 0.55 : 0.1);
  const vFov = THREE.MathUtils.degToRad(camera.fov);
  const hFov = 2 * Math.atan(Math.tan(vFov / 2) * camera.aspect);
  const distance = Math.max(radius / Math.tan(vFov / 2), radius / Math.tan(hFov / 2)) * 1.05;
  homeDistance = distance;
  if (controls) {
    controls.minDistance = distance * 0.28;
    controls.maxDistance = distance * 1.9;
  }
}

function applyCameraPreset(mode: DemoMode, instant = false) {
  if (!camera) return;
  let azimuth = 0;
  let elevation = 0;
  let scale = 1;
  if (mode === 'layers') {
    azimuth = 0;
    elevation = 0;
    scale = 1;
  } else if (mode === 'gloss') {
    azimuth = THREE.MathUtils.degToRad(24);
    elevation = THREE.MathUtils.degToRad(12);
    scale = 0.74;
  } else {
    azimuth = 0;
    elevation = THREE.MathUtils.degToRad(8);
    scale = 1;
  }
  const distance = homeDistance * scale;
  const position = new THREE.Vector3(
    Math.sin(azimuth) * Math.cos(elevation),
    Math.sin(elevation),
    Math.cos(azimuth) * Math.cos(elevation)
  ).multiplyScalar(distance);
  cameraGoal = position;
  targetGoal = new THREE.Vector3(0, 0, 0);
  if (instant) {
    camera.position.copy(position);
    camera.lookAt(0, 0, 0);
    controls?.target.set(0, 0, 0);
    cameraGoal = null;
    targetGoal = null;
  }
}

function initScene() {
  const host = canvasHost.value;
  if (!host) return;
  const width = host.clientWidth || 800;
  const height = host.clientHeight || 600;

  scene = new THREE.Scene();
  camera = new THREE.PerspectiveCamera(40, width / height, 0.1, 100);

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
  renderer.setSize(width, height);
  renderer.outputColorSpace = THREE.SRGBColorSpace;
  renderer.toneMapping = THREE.ACESFilmicToneMapping;
  renderer.toneMappingExposure = 1.12;
  host.appendChild(renderer.domElement);

  controls = new OrbitControls(camera, renderer.domElement);
  controls.enableDamping = true;
  controls.dampingFactor = 0.08;
  controls.enablePan = true;
  controls.screenSpacePanning = true;
  controls.mouseButtons = {
    LEFT: dragMode.value === 'pan' ? THREE.MOUSE.PAN : THREE.MOUSE.ROTATE,
    MIDDLE: THREE.MOUSE.DOLLY,
    RIGHT: dragMode.value === 'pan' ? THREE.MOUSE.ROTATE : THREE.MOUSE.PAN
  };
  controls.touches = {
    ONE: dragMode.value === 'pan' ? THREE.TOUCH.PAN : THREE.TOUCH.ROTATE,
    TWO: THREE.TOUCH.DOLLY_ROTATE
  };
  controls.zoomToCursor = true;
  controls.rotateSpeed = 0.78;
  controls.zoomSpeed = 0.8;
  controls.autoRotateSpeed = 1.5;
  controls.minPolarAngle = Math.PI * 0.08;
  controls.maxPolarAngle = Math.PI * 0.96;
  controls.target.set(0, 0, 0);
  controls.addEventListener('start', () => {
    cameraGoal = null;
    targetGoal = null;
  });

  computeHomePosition(width, height);
  applyCameraPreset(demoTone.value, true);

  buildStage();
  buildLights();

  renderer.domElement.addEventListener('dblclick', focusAt);
  resizeObserver = new ResizeObserver((entries) => {
    const rect = entries[0]?.contentRect;
    if (!rect || !rect.width || !rect.height || !renderer || !camera) return;
    renderer.setSize(rect.width, rect.height);
    computeHomePosition(rect.width, rect.height);
  });
  resizeObserver.observe(host);

  animate();
}

function buildLights() {
  if (!scene) return;
  scene.add(new THREE.HemisphereLight(0xffffff, 0xd9ccb9, 1.25));
  const key = new THREE.DirectionalLight(0xfff8ef, 2.4);
  key.position.set(-3.2, 4.4, 5.2);
  scene.add(key);
  const fill = new THREE.DirectionalLight(0xffffff, 0.85);
  fill.position.set(4.2, -2.4, 3.4);
  scene.add(fill);
  const rim = new THREE.DirectionalLight(0xffffff, 0.6);
  rim.position.set(0, 2.4, -6);
  scene.add(rim);
  sweepLight = new THREE.DirectionalLight(0xffd9a8, 0);
  sweepLight.position.set(3, 4.4, 3.4);
  scene.add(sweepLight);
}

function makeShadowTexture(): THREE.CanvasTexture {
  const size = 256;
  const canvas = document.createElement('canvas');
  canvas.width = size;
  canvas.height = size;
  const ctx = canvas.getContext('2d')!;
  const gradient = ctx.createRadialGradient(
    size / 2,
    size / 2,
    size * 0.18,
    size / 2,
    size / 2,
    size * 0.5
  );
  gradient.addColorStop(0, 'rgba(60, 42, 26, 0.32)');
  gradient.addColorStop(0.62, 'rgba(60, 42, 26, 0.16)');
  gradient.addColorStop(1, 'rgba(60, 42, 26, 0)');
  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, size, size);
  const texture = new THREE.CanvasTexture(canvas);
  texture.colorSpace = THREE.SRGBColorSpace;
  return texture;
}

function makeFabricTexture(): THREE.CanvasTexture {
  const size = 256;
  const canvas = document.createElement('canvas');
  canvas.width = size;
  canvas.height = size;
  const ctx = canvas.getContext('2d')!;
  ctx.fillStyle = '#efe8dc';
  ctx.fillRect(0, 0, size, size);
  ctx.strokeStyle = 'rgba(126, 106, 84, 0.09)';
  ctx.lineWidth = 1;
  for (let i = -size; i < size * 2; i += 7) {
    ctx.beginPath();
    ctx.moveTo(i, 0);
    ctx.lineTo(i + size, size);
    ctx.stroke();
  }
  ctx.strokeStyle = 'rgba(255, 255, 255, 0.16)';
  for (let i = -size; i < size * 2; i += 7) {
    ctx.beginPath();
    ctx.moveTo(i, size);
    ctx.lineTo(i + size, 0);
    ctx.stroke();
  }
  for (let i = 0; i < 900; i++) {
    ctx.fillStyle = `rgba(90, 70, 50, ${Math.random() * 0.05})`;
    ctx.fillRect(Math.random() * size, Math.random() * size, 1.4, 1.4);
  }
  const texture = new THREE.CanvasTexture(canvas);
  texture.colorSpace = THREE.SRGBColorSpace;
  texture.wrapS = THREE.RepeatWrapping;
  texture.wrapT = THREE.RepeatWrapping;
  texture.repeat.set(7, 7);
  return texture;
}

/* ===== 展示底布：方形画布 / 圆形绣盘 ===== */
function disposeStage() {
  disposeStitchMeshes();
  if (!stageRoot) return;
  scene?.remove(stageRoot);
  stageRoot.traverse((child) => {
    const mesh = child as THREE.Mesh;
    mesh.geometry?.dispose();
    const material = mesh.material as THREE.Material | THREE.Material[] | undefined;
    const disposeOne = (item: THREE.Material) => {
      (item as THREE.MeshStandardMaterial).map?.dispose();
      item.dispose();
    };
    if (Array.isArray(material)) material.forEach(disposeOne);
    else if (material) disposeOne(material);
  });
  stageRoot = undefined;
  stitchGroup = undefined;
  fabricGroup = undefined;
  layerGroups = {};
  replayOrder = [];
  threadMaterials = [];
}

function buildStage() {
  if (!scene) return;
  disposeStage();
  const isCanvas = viewMode.value === 'canvas';
  const root = new THREE.Group();
  stageRoot = root;
  scene.add(root);

  const boardHalf = isCanvas ? BOARD_HALF : HOOP_R;
  const shadow = new THREE.Mesh(
    new THREE.PlaneGeometry(boardHalf * 2.9, boardHalf * 2.9),
    new THREE.MeshBasicMaterial({
      map: makeShadowTexture(),
      transparent: true,
      depthWrite: false,
      opacity: 0.9
    })
  );
  shadow.position.z = -0.16;
  root.add(shadow);

  const fabricLayer = new THREE.Group();
  fabricLayer.userData.layer = 'fabric';
  fabricGroup = fabricLayer;
  layerGroups.fabric = fabricLayer;
  root.add(fabricLayer);

  if (isCanvas) {
    const backing = new THREE.Mesh(
      new THREE.PlaneGeometry((BOARD_HALF + 0.07) * 2, (BOARD_HALF + 0.07) * 2),
      new THREE.MeshStandardMaterial({ color: 0xe6dac4, roughness: 0.92, metalness: 0 })
    );
    backing.position.z = -0.02;
    fabricLayer.add(backing);
  }

  const fabric = new THREE.Mesh(
    isCanvas
      ? new THREE.PlaneGeometry(BOARD_HALF * 2, BOARD_HALF * 2)
      : new THREE.CircleGeometry(HOOP_R, 96),
    new THREE.MeshStandardMaterial({
      color: 0xf3ece1,
      map: makeFabricTexture(),
      roughness: 0.96,
      metalness: 0
    })
  );
  fabric.position.z = -0.006;
  fabricLayer.add(fabric);

  stitchGroup = new THREE.Group();
  root.add(stitchGroup);

  if (!isCanvas) {
    const wood = new THREE.Mesh(
      new THREE.TorusGeometry(HOOP_R + 0.075, 0.078, 26, 180),
      new THREE.MeshStandardMaterial({ color: 0x6b4b37, roughness: 0.62, metalness: 0.06 })
    );
    fabricLayer.add(wood);

    const rim = new THREE.Mesh(
      new THREE.TorusGeometry(HOOP_R + 0.004, 0.02, 14, 160),
      new THREE.MeshStandardMaterial({ color: 0xd8c5a5, roughness: 0.42, metalness: 0.3 })
    );
    rim.position.z = 0.03;
    fabricLayer.add(rim);

    const innerShade = new THREE.Mesh(
      new THREE.RingGeometry(HOOP_R - 0.01, HOOP_R + 0.06, 120),
      new THREE.MeshBasicMaterial({
        color: 0x3a2a1c,
        transparent: true,
        opacity: 0.16,
        side: THREE.DoubleSide
      })
    );
    innerShade.position.z = 0.008;
    fabricLayer.add(innerShade);
  }
}

/* ===== 图像分析：Otsu 前景 + 距离变换，复刻后端 auto 分层 ===== */
function otsuThreshold(histogram: Uint32Array, total: number) {
  let sum = 0;
  for (let i = 0; i < 256; i++) sum += i * histogram[i];
  let sumB = 0;
  let weightB = 0;
  let maxVariance = 0;
  let threshold = 127;
  for (let t = 0; t < 256; t++) {
    weightB += histogram[t];
    if (!weightB) continue;
    const weightF = total - weightB;
    if (!weightF) break;
    sumB += t * histogram[t];
    const meanB = sumB / weightB;
    const meanF = (sum - sumB) / weightF;
    const variance = weightB * weightF * (meanB - meanF) * (meanB - meanF);
    if (variance > maxVariance) {
      maxVariance = variance;
      threshold = t;
    }
  }
  return threshold;
}

function morphOpen(mask: Uint8Array, width: number, height: number) {
  const eroded = new Uint8Array(mask.length);
  for (let y = 1; y < height - 1; y++) {
    for (let x = 1; x < width - 1; x++) {
      const i = y * width + x;
      if (
        mask[i] &&
        mask[i - 1] &&
        mask[i + 1] &&
        mask[i - width] &&
        mask[i + width] &&
        mask[i - width - 1] &&
        mask[i - width + 1] &&
        mask[i + width - 1] &&
        mask[i + width + 1]
      ) {
        eroded[i] = 1;
      }
    }
  }
  const dilated = new Uint8Array(mask.length);
  for (let y = 1; y < height - 1; y++) {
    for (let x = 1; x < width - 1; x++) {
      const i = y * width + x;
      if (
        eroded[i] ||
        eroded[i - 1] ||
        eroded[i + 1] ||
        eroded[i - width] ||
        eroded[i + width] ||
        eroded[i - width - 1] ||
        eroded[i - width + 1] ||
        eroded[i + width - 1] ||
        eroded[i + width + 1]
      ) {
        dilated[i] = 1;
      }
    }
  }
  return dilated;
}

function removeSmallComponents(
  mask: Uint8Array,
  width: number,
  height: number,
  minArea: number
) {
  const total = width * height;
  const seen = new Uint8Array(total);
  const queue = new Int32Array(total);
  const cells: number[] = [];
  for (let start = 0; start < total; start++) {
    if (!mask[start] || seen[start]) continue;
    let head = 0;
    let tail = 0;
    queue[tail++] = start;
    seen[start] = 1;
    cells.length = 0;
    while (head < tail) {
      const index = queue[head++];
      cells.push(index);
      const x = index % width;
      const y = (index / width) | 0;
      if (x > 0) {
        const next = index - 1;
        if (mask[next] && !seen[next]) {
          seen[next] = 1;
          queue[tail++] = next;
        }
      }
      if (x < width - 1) {
        const next = index + 1;
        if (mask[next] && !seen[next]) {
          seen[next] = 1;
          queue[tail++] = next;
        }
      }
      if (y > 0) {
        const next = index - width;
        if (mask[next] && !seen[next]) {
          seen[next] = 1;
          queue[tail++] = next;
        }
      }
      if (y < height - 1) {
        const next = index + width;
        if (mask[next] && !seen[next]) {
          seen[next] = 1;
          queue[tail++] = next;
        }
      }
    }
    if (cells.length < minArea) {
      for (const cell of cells) mask[cell] = 0;
    }
  }
}

function distanceTransform(mask: Uint8Array, width: number, height: number) {
  const total = width * height;
  const INF = 1e9;
  const dist = new Float32Array(total);
  for (let i = 0; i < total; i++) dist[i] = mask[i] ? INF : 0;
  const diagonal = 1.4142;
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      const i = y * width + x;
      if (!dist[i]) continue;
      let value = dist[i];
      if (x > 0) value = Math.min(value, dist[i - 1] + 1);
      if (y > 0) {
        value = Math.min(value, dist[i - width] + 1);
        if (x > 0) value = Math.min(value, dist[i - width - 1] + diagonal);
        if (x < width - 1) value = Math.min(value, dist[i - width + 1] + diagonal);
      }
      dist[i] = value;
    }
  }
  for (let y = height - 1; y >= 0; y--) {
    for (let x = width - 1; x >= 0; x--) {
      const i = y * width + x;
      if (!dist[i]) continue;
      let value = dist[i];
      if (x < width - 1) value = Math.min(value, dist[i + 1] + 1);
      if (y < height - 1) {
        value = Math.min(value, dist[i + width] + 1);
        if (x < width - 1) value = Math.min(value, dist[i + width + 1] + diagonal);
        if (x > 0) value = Math.min(value, dist[i + width - 1] + diagonal);
      }
      dist[i] = value;
    }
  }
  for (let x = 0; x < width; x++) {
    dist[x] = Math.min(dist[x], 1);
    dist[(height - 1) * width + x] = Math.min(dist[(height - 1) * width + x], 1);
  }
  for (let y = 0; y < height; y++) {
    dist[y * width] = Math.min(dist[y * width], 1);
    dist[y * width + width - 1] = Math.min(dist[y * width + width - 1], 1);
  }
  return dist;
}

interface ImageAnalysis {
  mask: Uint8Array;
  dist: Float32Array;
  lum: Uint8Array;
}

function analyzePixels(pixels: Uint8ClampedArray): ImageAnalysis {
  const total = SAMPLE * SAMPLE;
  const lum = new Uint8Array(total);
  const alpha = new Uint8Array(total);
  const saturation = new Uint8Array(total);
  const histogram = new Uint32Array(256);
  for (let i = 0; i < total; i++) {
    const offset = i * 4;
    const r = pixels[offset];
    const g = pixels[offset + 1];
    const b = pixels[offset + 2];
    alpha[i] = pixels[offset + 3];
    const l = (r * 299 + g * 587 + b * 114) / 1000;
    lum[i] = l;
    histogram[l | 0]++;
    const max = Math.max(r, g, b);
    const min = Math.min(r, g, b);
    saturation[i] = max ? ((max - min) / max) * 255 : 0;
  }
  const threshold = otsuThreshold(histogram, total);
  let mask = new Uint8Array(total);
  let count = 0;
  for (let i = 0; i < total; i++) {
    if (alpha[i] > 120 && (lum[i] < threshold || saturation[i] > 40)) {
      mask[i] = 1;
      count++;
    }
  }
  if (count < total * 0.02) {
    mask = new Uint8Array(total);
    count = 0;
    for (let i = 0; i < total; i++) {
      if (alpha[i] > 120 && (lum[i] < 232 || saturation[i] > 30)) {
        mask[i] = 1;
        count++;
      }
    }
  }
  if (count < 64) {
    mask = new Uint8Array(total);
    for (let i = 0; i < total; i++) {
      if (alpha[i] > 120) mask[i] = 1;
    }
  }
  mask = morphOpen(mask, SAMPLE, SAMPLE);
  removeSmallComponents(mask, SAMPLE, SAMPLE, 8);
  const dist = distanceTransform(mask, SAMPLE, SAMPLE);
  return { mask, dist, lum };
}

/* ===== 把纹样逐像素拆成工艺层针脚 ===== */
function disposeStitchMeshes() {
  if (!stitchGroup) return;
  for (const child of [...stitchGroup.children]) {
    stitchGroup.remove(child);
    child.traverse((object) => {
      const mesh = object as THREE.Mesh;
      mesh.geometry?.dispose();
      const material = mesh.material as THREE.Material | THREE.Material[] | undefined;
      const disposeOne = (item: THREE.Material) => {
        (item as THREE.MeshStandardMaterial).map?.dispose();
        item.dispose();
      };
      if (Array.isArray(material)) material.forEach(disposeOne);
      else if (material) disposeOne(material);
    });
  }
  layerGroups = fabricGroup ? { fabric: fabricGroup } : {};
  replayOrder = [];
  threadMaterials = [];
}

interface StitchData {
  x: number;
  y: number;
  angle: number;
  length: number;
  width: number;
  height: number;
  r: number;
  g: number;
  b: number;
  shade: number;
}

function setGroupOpacity(group: THREE.Group, opacity: number) {
  group.traverse((object) => {
    const mesh = object as THREE.Mesh;
    if (!mesh.isMesh) return;
    const materials = Array.isArray(mesh.material) ? mesh.material : [mesh.material];
    materials.forEach((material) => {
      if (material.userData.appliedOpacity === opacity) return;
      if (material.userData.baseOpacity === undefined) {
        material.userData.baseOpacity = material.opacity;
        material.userData.baseTransparent = material.transparent;
        material.userData.baseDepthWrite = material.depthWrite;
      }
      const dimmed = opacity < 1;
      material.userData.appliedOpacity = opacity;
      material.transparent =
        Boolean(material.userData.baseTransparent) || dimmed;
      material.opacity = Number(material.userData.baseOpacity) * opacity;
      material.depthWrite = dimmed ? false : Boolean(material.userData.baseDepthWrite);
      material.needsUpdate = true;
    });
  });
}

function refreshLayerStyles() {
  const selected = selectedLayer.value;
  for (const meta of layerMeta) {
    const group = layerGroups[meta.key];
    if (!group) continue;
    group.visible = layerVisible[meta.key];
    const dimmed = selected !== null && selected !== meta.key;
    setGroupOpacity(group, dimmed ? 0.14 : 1);
  }
}

function toggleLayerVisible(key: LayerKey) {
  layerVisible[key] = !layerVisible[key];
  refreshLayerStyles();
}

function applyGloss(enabled: boolean) {
  for (const material of threadMaterials) {
    material.clearcoat = enabled ? 0.85 : 0;
    material.clearcoatRoughness = enabled ? 0.18 : 0.3;
    material.roughness = enabled ? 0.24 : 0.42;
    material.sheen = enabled ? 1 : 0.6;
    material.needsUpdate = true;
  }
}

function applyTension() {
  if (stitchGroup) stitchGroup.scale.z = 0.58 + (tension.value / 100) * 0.72;
}

function buildStitches(bitmap: ImageBitmap) {
  if (!stitchGroup) return;

  const canvas = document.createElement('canvas');
  canvas.width = SAMPLE;
  canvas.height = SAMPLE;
  const ctx = canvas.getContext('2d', { willReadFrequently: true });
  if (!ctx) throw new Error('无法创建纹样解析画布');

  const side = Math.min(bitmap.width, bitmap.height);
  ctx.drawImage(
    bitmap,
    (bitmap.width - side) / 2,
    (bitmap.height - side) / 2,
    side,
    side,
    0,
    0,
    SAMPLE,
    SAMPLE
  );
  const pixels = ctx.getImageData(0, 0, SAMPLE, SAMPLE).data;
  const analysis = analyzePixels(pixels);

  const half = SAMPLE / 2;
  const radiusPx = half - 6;
  const worldHalf = viewMode.value === 'hoop' ? HOOP_R - 0.05 : STITCH_HALF;
  const pxToWorld = worldHalf / radiusPx;
  const baseLength = STEP_PX * pxToWorld * 1.62;
  const baseWidth = STEP_PX * pxToWorld * 0.8;
  const baseHeight = 0.02;

  const lumAt = (x: number, y: number) => {
    const ix = x | 0;
    const iy = y | 0;
    if (ix < 0 || iy < 0 || ix >= SAMPLE || iy >= SAMPLE) return 245;
    const o = (iy * SAMPLE + ix) * 4;
    return pixels[o] * 0.299 + pixels[o + 1] * 0.587 + pixels[o + 2] * 0.114;
  };

  const layers: Record<StitchKey, StitchData[]> = {
    tatami: [],
    satin: [],
    outline: [],
    knot: []
  };

  let row = 0;
  for (let gy = STEP_PX * 0.5; gy < SAMPLE; gy += STEP_PX, row++) {
    const rowOffset = (row % 2) * STEP_PX * 0.5;
    for (let gx = STEP_PX * 0.5 + rowOffset; gx < SAMPLE; gx += STEP_PX) {
      const jx = gx + (Math.random() - 0.5) * STEP_PX * 0.55;
      const jy = gy + (Math.random() - 0.5) * STEP_PX * 0.55;
      const dx = jx - half;
      const dy = jy - half;
      if (viewMode.value === 'hoop' && dx * dx + dy * dy > radiusPx * radiusPx) continue;
      const px = Math.min(SAMPLE - 1, Math.max(0, jx | 0));
      const py = Math.min(SAMPLE - 1, Math.max(0, jy | 0));
      const index = py * SAMPLE + px;
      if (!analysis.mask[index]) continue;

      const offset = index * 4;
      const luminance = analysis.lum[index];
      const thickness = analysis.dist[index];

      const d = 4;
      const gradientX = lumAt(jx + d, jy) - lumAt(jx - d, jy);
      const gradientY = lumAt(jx, jy + d) - lumAt(jx, jy - d);
      const magnitude = Math.hypot(gradientX, gradientY);
      let angle: number;
      if (magnitude < 7) {
        angle = -0.58 + 0.3 * Math.sin(jx * 0.02 + jy * 0.014);
      } else {
        angle = Math.atan2(gradientY, gradientX) + Math.PI / 2;
      }
      angle = -angle;
      angle = (((angle % Math.PI) + Math.PI + Math.PI / 2) % Math.PI) - Math.PI / 2;
      angle += (Math.random() - 0.5) * 0.13;

      let layer: StitchKey;
      if (thickness <= EDGE_DIST) {
        layer = 'outline';
      } else if (luminance < KNOT_LUM && thickness <= KNOT_DIST) {
        layer = 'knot';
      } else if (thickness >= TATAMI_DIST) {
        layer = 'tatami';
      } else {
        layer = 'satin';
      }

      layers[layer].push({
        x: dx * pxToWorld,
        y: -dy * pxToWorld,
        angle,
        length: baseLength * (0.86 + Math.random() * 0.32),
        width: baseWidth * (0.86 + Math.random() * 0.28),
        height: baseHeight * (0.65 + Math.random() * 0.7),
        r: pixels[offset],
        g: pixels[offset + 1],
        b: pixels[offset + 2],
        shade: 0.9 + Math.random() * 0.2
      });
    }
  }

  const total = stitchKeys.reduce((sum, key) => sum + layers[key].length, 0);
  if (!total) throw new Error('未能从纹样中识别出可绣花的前景针脚');

  buildLayerMeshes(layers);
  if (demoTone.value === 'replay') startReplay();
  else restoreFullCounts();
}

function buildLayerMeshes(layers: Record<StitchKey, StitchData[]>) {
  if (!stitchGroup) return;
  disposeStitchMeshes();

  const matrix = new THREE.Matrix4();
  const quaternion = new THREE.Quaternion();
  const position = new THREE.Vector3();
  const scale = new THREE.Vector3();
  const color = new THREE.Color();

  for (const key of stitchKeys) {
    const list = layers[key];
    layerCounts[key] = list.length;
    const group = new THREE.Group();
    group.userData.layer = key;
    stitchGroup.add(group);
    layerGroups[key] = group;
    if (!list.length) continue;

    const tuning = renderTuning[key];
    let geometry: THREE.BufferGeometry;
    if (key === 'knot') {
      geometry = new THREE.SphereGeometry(0.5, 6, 5);
    } else {
      geometry = new THREE.CapsuleGeometry(0.5, 1, 3, 8);
      geometry.rotateZ(Math.PI / 2);
    }
    const material = new THREE.MeshPhysicalMaterial({
      roughness: 0.42,
      metalness: 0.04,
      sheen: 0.6,
      sheenRoughness: 0.5,
      sheenColor: new THREE.Color(0xffffff)
    });
    threadMaterials.push(material);

    const mesh = new THREE.InstancedMesh(geometry, material, list.length);
    mesh.frustumCulled = false;

    list.forEach((stitch, index) => {
      const length = stitch.length * tuning.length;
      const width = stitch.width * tuning.width;
      const height = stitch.height * tuning.height;
      if (key === 'knot') {
        const radius = stitch.width * 0.95;
        position.set(stitch.x, stitch.y, tuning.z + radius * 0.55);
        quaternion.identity();
        scale.set(radius, radius, radius * 0.85);
      } else {
        quaternion.setFromAxisAngle(zAxis, stitch.angle);
        position.set(stitch.x, stitch.y, tuning.z + height / 2);
        scale.set(length / 2, width, height);
      }
      matrix.compose(position, quaternion, scale);
      mesh.setMatrixAt(index, matrix);
      color.setRGB(stitch.r / 255, stitch.g / 255, stitch.b / 255, THREE.SRGBColorSpace);
      color.multiplyScalar(stitch.shade);
      mesh.setColorAt(index, color);
    });
    mesh.instanceMatrix.needsUpdate = true;
    if (mesh.instanceColor) mesh.instanceColor.needsUpdate = true;

    group.add(mesh);
    replayOrder.push({ key, mesh, count: list.length });
  }

  layerCounts.fabric = 0;
  replayOrder.sort((a, b) => layerOrder[a.key] - layerOrder[b.key]);

  applyTension();
  applyGloss(demoTone.value === 'gloss');
  refreshLayerStyles();
}

function restoreFullCounts() {
  for (const entry of replayOrder) entry.mesh.count = entry.count;
  replayElapsed = totalStitches.value;
  replaySewn.value = totalStitches.value;
}

function applyReplayCounts(sewn: number) {
  let remaining = sewn;
  for (const entry of replayOrder) {
    entry.mesh.count = Math.max(0, Math.min(entry.count, Math.floor(remaining)));
    remaining -= entry.count;
  }
}

function startReplay() {
  replayElapsed = 0;
  replaySewn.value = 0;
  replayDone.value = false;
  if (!totalStitches.value) {
    replayPlaying.value = false;
    replayDone.value = true;
    return;
  }
  for (const entry of replayOrder) entry.mesh.count = 0;
  replayPlaying.value = true;
}

function toggleReplay() {
  if (replayDone.value || replayElapsed >= totalStitches.value) {
    startReplay();
    return;
  }
  replayPlaying.value = !replayPlaying.value;
}

function stopReplayFull() {
  replayPlaying.value = false;
  replayDone.value = true;
  restoreFullCounts();
}

function setDemoTone(mode: DemoMode) {
  if (demoTone.value === mode) return;
  demoTone.value = mode;
  applyModePreset(mode);
}

function applyModePreset(mode: DemoMode, instant = false) {
  if (mode === 'layers') {
    explode.value = 55;
    annotations.value = true;
    autoRotate.value = false;
  } else if (mode === 'replay') {
    explode.value = 0;
    annotations.value = false;
    autoRotate.value = false;
  } else {
    explode.value = 0;
    annotations.value = false;
    autoRotate.value = true;
  }
  selectedLayer.value = null;
  refreshLayerStyles();
  applyGloss(mode === 'gloss');
  if (mode === 'replay') startReplay();
  else stopReplayFull();
  const host = canvasHost.value;
  if (host) computeHomePosition(host.clientWidth || 800, host.clientHeight || 600);
  applyCameraPreset(mode, instant);
}

watch(tension, applyTension);
watch(selectedLayer, refreshLayerStyles);
watch(dragMode, (mode) => {
  if (!controls) return;
  controls.mouseButtons.LEFT = mode === 'pan' ? THREE.MOUSE.PAN : THREE.MOUSE.ROTATE;
  controls.mouseButtons.RIGHT = mode === 'pan' ? THREE.MOUSE.ROTATE : THREE.MOUSE.PAN;
  controls.touches.ONE = mode === 'pan' ? THREE.TOUCH.PAN : THREE.TOUCH.ROTATE;
});
watch(autoRotate, (value) => {
  if (controls) controls.autoRotate = value;
});

/* ===== 图片加载 ===== */
async function fetchBitmap(src: string): Promise<ImageBitmap> {
  let response: Response;
  try {
    response = await fetch(src, { cache: 'no-store', mode: 'cors' });
  } catch {
    throw new Error('图片读取失败（可能被浏览器跨域拦截），请使用「替换图片」上传本地纹样');
  }
  if (!response.ok) throw new Error(`图片读取失败（HTTP ${response.status}）`);
  const blob = await response.blob();
  return createImageBitmap(blob);
}

let currentSource = '';

async function renderFromSource(src: string) {
  renderError.value = '';
  rendering.value = true;
  await nextTick();
  await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()));
  let bitmap: ImageBitmap | null = null;
  try {
    bitmap = await fetchBitmap(src);
    buildStitches(bitmap);
  } catch (error: any) {
    renderError.value = error?.message || '纹样解析失败，请更换图片重试';
  } finally {
    bitmap?.close();
    rendering.value = false;
  }
}

async function applyPattern(pattern: PatternLite) {
  if (!pattern.image) return;
  selectedId.value = pattern.id;
  currentTitle.value = pattern.title;
  currentSource = pattern.image;
  selectedLayer.value = null;
  await renderFromSource(pattern.image);
}

async function switchViewMode(mode: ViewMode) {
  if (viewMode.value === mode) return;
  viewMode.value = mode;
  buildStage();
  const host = canvasHost.value;
  if (host) computeHomePosition(host.clientWidth, host.clientHeight);
  applyCameraPreset(demoTone.value);
  if (currentSource) await renderFromSource(currentSource);
}

function pickPattern(pattern: PatternLite) {
  if (selectedId.value === pattern.id) return;
  applyPattern(pattern);
}

function openUpload() {
  uploadInput.value?.click();
}

async function handleFile(file?: File | null) {
  if (!file) return;
  if (!file.type.startsWith('image/')) {
    showNotice('请选择图片文件（JPG / PNG / WEBP）');
    return;
  }
  const item: PatternLite = {
    id: `upload-${Date.now()}`,
    title: file.name.replace(/\.[^.]+$/, '') || '本地上传纹样',
    image: URL.createObjectURL(file)
  };
  uploads.value = [item, ...uploads.value];
  refreshLibrary();
  showNotice('已载入本地纹样，正在拆解工艺层');
  await applyPattern(item);
}

function onUploadChange(event: Event) {
  const input = event.target as HTMLInputElement;
  handleFile(input.files?.[0]);
  input.value = '';
}

function onDrop(event: DragEvent) {
  const file = event.dataTransfer?.files?.[0];
  handleFile(file);
}

function onThumbError(event: Event) {
  const image = event.currentTarget as HTMLImageElement;
  const fallback = '/demo/pattern/round-flower-pattern-01.jpg';
  if (!image.src.endsWith(fallback)) image.src = fallback;
}

/* ===== 视角与模块交互 ===== */
function selectStitch(key: LayerKey) {
  if (demoTone.value !== 'layers') setDemoTone('layers');
  if (selectedLayer.value === key) {
    selectedLayer.value = null;
    return;
  }
  selectedLayer.value = key;
  const group = layerGroups[key];
  if (group) {
    const zScale = stitchGroup?.scale.z || 1;
    targetGoal = new THREE.Vector3(0, group.position.y, group.position.z * zScale);
  }
}

function resetView() {
  const host = canvasHost.value;
  if (host) computeHomePosition(host.clientWidth, host.clientHeight);
  applyCameraPreset(demoTone.value);
  targetGoal = new THREE.Vector3(0, 0, 0);
}

const focusRaycaster = new THREE.Raycaster();
const focusPlane = new THREE.Plane(new THREE.Vector3(0, 0, 1), 0);

function focusAt(event: MouseEvent) {
  if (!camera || !controls || !renderer) return;
  const rect = renderer.domElement.getBoundingClientRect();
  focusRaycaster.setFromCamera(
    new THREE.Vector2(
      ((event.clientX - rect.left) / rect.width) * 2 - 1,
      -((event.clientY - rect.top) / rect.height) * 2 + 1
    ),
    camera
  );
  const hit = new THREE.Vector3();
  if (!focusRaycaster.ray.intersectPlane(focusPlane, hit)) return;
  const offset = camera.position.clone().sub(controls.target);
  targetGoal = hit;
  cameraGoal = hit.clone().add(offset);
}

function updateCallouts() {
  if (!renderer || !camera) return;
  const host = calloutHost.value;
  if (!host) return;
  const rect = renderer.domElement.getBoundingClientRect();
  const show = demoTone.value === 'layers' && annotations.value;
  const fade = THREE.MathUtils.clamp((explode.value - 8) / 20, 0, 1);
  const stageHalf = viewMode.value === 'canvas' ? STITCH_HALF : HOOP_R;
  const elements = host.querySelectorAll<HTMLElement>('.s3d-callout');
  elements.forEach((element) => {
    const key = element.dataset.layer as LayerKey;
    const group = layerGroups[key];
    if (!show || !group || !layerVisible[key]) {
      element.style.visibility = 'hidden';
      return;
    }
    calloutWorld
      .set(
        stageHalf * 0.72 + group.position.x,
        stageHalf * 0.3 + group.position.y,
        0.12 + group.position.z * (stitchGroup?.scale.z || 1)
      );
    probeVector.copy(calloutWorld).project(camera!);
    const x = (probeVector.x * 0.5 + 0.5) * rect.width;
    const y = (-probeVector.y * 0.5 + 0.5) * rect.height;
    element.style.left = `${x}px`;
    element.style.top = `${y}px`;
    element.style.visibility = probeVector.z < 1 ? 'visible' : 'hidden';
    element.style.opacity = String(fade);
    element.classList.toggle('on', selectedLayer.value === key);
  });
}

function applyExplode(delta: number) {
  const amount = explodeAmount.value;
  const ease = Math.min(1, delta * 7);
  const zScale = stitchGroup?.scale.z || 1;
  for (const meta of layerMeta) {
    const group = layerGroups[meta.key];
    if (!group) continue;
    const step = (meta.stitchOrder - 2) * LAYER_GAP * amount;
    const targetY = step * LAYER_LIFT;
    const targetZ = step / zScale;
    group.position.y += (targetY - group.position.y) * ease;
    group.position.z += (targetZ - group.position.z) * ease;
  }
}

function animate() {
  frameId = requestAnimationFrame(animate);
  const delta = Math.min(clock.getDelta(), 0.1);

  if (cameraGoal && camera) {
    camera.position.lerp(cameraGoal, 0.1);
    if (camera.position.distanceTo(cameraGoal) < 0.01) cameraGoal = null;
  }
  if (targetGoal && controls) {
    controls.target.lerp(targetGoal, 0.1);
    if (controls.target.distanceTo(targetGoal) < 0.005) targetGoal = null;
  }
  applyExplode(delta);

  if (demoTone.value === 'replay' && replayPlaying.value) {
    replayElapsed = Math.min(replayElapsed + delta * REPLAY_RATE, totalStitches.value);
    applyReplayCounts(replayElapsed);
    const sewn = Math.floor(replayElapsed);
    if (sewn - replaySewn.value >= 16 || sewn >= totalStitches.value) {
      replaySewn.value = sewn;
    }
    if (replayElapsed >= totalStitches.value) {
      replayPlaying.value = false;
      replayDone.value = true;
    }
  }

  if (sweepLight) {
    const target = demoTone.value === 'gloss' ? 1.7 : 0;
    sweepLight.intensity += (target - sweepLight.intensity) * Math.min(1, delta * 6);
    if (demoTone.value === 'gloss') {
      const t = clock.elapsedTime;
      sweepLight.position.set(Math.sin(t * 0.8) * 5, 4.2, Math.cos(t * 0.8) * 5);
    }
  }

  controls?.update();
  if (renderer && scene && camera) renderer.render(scene, camera);
  updateCallouts();
}

/* ===== 生命周期 ===== */
onMounted(async () => {
  initScene();
  refreshLibrary();
  window.addEventListener(userDataEvent, onUserDataChange);
  const initial = resolveInitialPattern();
  if (initial) await applyPattern(initial);
  loadServerPatterns();
});

onBeforeUnmount(() => {
  window.removeEventListener(userDataEvent, onUserDataChange);
  if (noticeTimer) window.clearTimeout(noticeTimer);
  if (frameId) cancelAnimationFrame(frameId);
  resizeObserver?.disconnect();
  if (renderer) {
    renderer.domElement.removeEventListener('dblclick', focusAt);
    controls?.dispose();
    scene?.traverse((object: any) => {
      object.geometry?.dispose?.();
      const material = object.material;
      if (Array.isArray(material)) material.forEach((item: any) => item.dispose?.());
      else {
        material?.map?.dispose?.();
        material?.dispose?.();
      }
    });
    renderer.dispose();
    renderer.domElement.remove();
  }
  uploads.value.forEach((item) => {
    if (item.image.startsWith('blob:')) URL.revokeObjectURL(item.image);
  });
});
</script>

<template>
  <div
    class="s3d-page"
    @dragover.prevent
    @drop.prevent="onDrop"
  >
    <!-- 左：工艺模块清单 -->
    <aside class="s3d-left">
      <RouterLink
        to="/generate"
        class="s3d-back"
        >← 返回生成台</RouterLink
      >
      <h1>针法微观解析</h1>
      <p class="s3d-sub">
        按真实打版工序拆解为 {{ activeLayerCount }} 个工艺模块。点击模块可高亮定位，眼睛图标可单独显隐。
      </p>

      <div class="s3d-parts">
        <div
          v-for="meta in layerMeta"
          :key="meta.key"
          class="s3d-part"
          :class="{
            active: selectedLayer === meta.key,
            off: !layerVisible[meta.key],
            empty: meta.key !== 'fabric' && !layerCounts[meta.key]
          }"
          role="button"
          tabindex="0"
          @click="selectStitch(meta.key)"
          @keydown.enter.prevent="selectStitch(meta.key)"
        >
          <span
            class="s3d-part-thumb"
            :style="{ '--tone': meta.color }"
          >
            <svg
              v-if="meta.key === 'fabric'"
              viewBox="0 0 64 64"
              aria-hidden="true"
            >
              <rect
                width="64"
                height="64"
                fill="#f4ecdf"
              />
              <g
                stroke="currentColor"
                stroke-width="1.5"
                opacity="0.55"
              >
                <path d="M0 16h64M0 32h64M0 48h64M16 0v64M32 0v64M48 0v64" />
              </g>
              <rect
                x="12"
                y="12"
                width="40"
                height="40"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                opacity="0.8"
              />
            </svg>
            <svg
              v-else-if="meta.key === 'tatami'"
              viewBox="0 0 64 64"
              aria-hidden="true"
            >
              <rect
                width="64"
                height="64"
                fill="#f4ecdf"
              />
              <g
                stroke="currentColor"
                stroke-width="5"
                stroke-linecap="round"
                opacity="0.85"
              >
                <path d="M10 16h30M28 27h26M10 38h30M28 49h26" />
              </g>
            </svg>
            <svg
              v-else-if="meta.key === 'satin'"
              viewBox="0 0 64 64"
              aria-hidden="true"
            >
              <rect
                width="64"
                height="64"
                fill="#f4ecdf"
              />
              <g
                stroke="currentColor"
                stroke-width="4.5"
                stroke-linecap="round"
                opacity="0.85"
              >
                <path d="M-4 60 L30 26M8 64 L42 30M20 66 L54 32M32 68 L66 34" />
                <path d="M4 40 L38 6M16 44 L50 10" />
              </g>
            </svg>
            <svg
              v-else-if="meta.key === 'outline'"
              viewBox="0 0 64 64"
              aria-hidden="true"
            >
              <rect
                width="64"
                height="64"
                fill="#f4ecdf"
              />
              <path
                d="M14 14h36v36H14z"
                fill="none"
                stroke="currentColor"
                stroke-width="3"
                stroke-dasharray="7 4"
                stroke-linejoin="round"
                opacity="0.9"
              />
              <path
                d="M24 25c8-6 18 4 17 9s-9 12-17 7"
                fill="none"
                stroke="currentColor"
                stroke-width="2.4"
                opacity="0.6"
              />
            </svg>
            <svg
              v-else
              viewBox="0 0 64 64"
              aria-hidden="true"
            >
              <rect
                width="64"
                height="64"
                fill="#f4ecdf"
              />
              <g
                fill="currentColor"
                opacity="0.9"
              >
                <circle
                  cx="16"
                  cy="17"
                  r="4.2"
                />
                <circle
                  cx="32"
                  cy="14"
                  r="4.2"
                />
                <circle
                  cx="48"
                  cy="18"
                  r="4.2"
                />
                <circle
                  cx="23"
                  cy="32"
                  r="4.2"
                />
                <circle
                  cx="40"
                  cy="30"
                  r="4.2"
                />
                <circle
                  cx="14"
                  cy="47"
                  r="4.2"
                />
                <circle
                  cx="31"
                  cy="46"
                  r="4.2"
                />
                <circle
                  cx="48"
                  cy="49"
                  r="4.2"
                />
              </g>
            </svg>
          </span>
          <span class="s3d-part-main">
            <span class="s3d-part-title">
              <b>{{ meta.name }}</b>
              <em :style="{ '--chip': meta.color }">{{ meta.part }}</em>
            </span>
            <span class="s3d-part-meta">
              <template v-if="meta.key === 'fabric'">绣布基材 · 最底层</template>
              <template v-else
                >{{ formatCount(layerCounts[meta.key]) }} 针 · 占
                {{ shareOf(meta.key) }}%</template
              >
            </span>
          </span>
          <button
            type="button"
            class="s3d-part-eye"
            :class="{ off: !layerVisible[meta.key] }"
            :title="layerVisible[meta.key] ? '隐藏该层' : '显示该层'"
            @click.stop="toggleLayerVisible(meta.key)"
          >
            <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path
                d="M2 12s3.8-6.5 10-6.5S22 12 22 12s-3.8 6.5-10 6.5S2 12 2 12Z"
                fill="none"
                stroke="currentColor"
                stroke-width="1.6"
              />
              <circle
                cx="12"
                cy="12"
                r="3"
                fill="none"
                stroke="currentColor"
                stroke-width="1.6"
              />
              <path
                v-if="!layerVisible[meta.key]"
                d="M4 20 20 4"
                stroke="currentColor"
                stroke-width="1.6"
              />
            </svg>
          </button>
        </div>
      </div>

      <div class="s3d-detail">
        <template v-if="activeMeta">
          <div class="s3d-detail-head">
            <span>模块详情</span>
            <b :style="{ color: activeMeta.color }">{{ activeMeta.name }}</b>
          </div>
          <div class="s3d-detail-grid">
            <div>
              <span>针数</span>
              <b>{{
                activeMeta.key === 'fabric' ? '—' : formatCount(layerCounts[activeMeta.key])
              }}</b>
            </div>
            <div>
              <span>工序</span>
              <b>{{
                activeMeta.key === 'fabric' ? '基材' : `第 ${activeMeta.stitchOrder} 层`
              }}</b>
            </div>
            <div>
              <span>工艺职责</span>
              <b>{{ activeMeta.part }}</b>
            </div>
            <div>
              <span>占比</span>
              <b>{{ activeMeta.key === 'fabric' ? '—' : `${shareOf(activeMeta.key)}%` }}</b>
            </div>
          </div>
          <p class="s3d-detail-subject">纹样 · {{ currentTitle }}</p>
          <p>{{ activeMeta.desc }}</p>
        </template>
        <p
          v-else
          class="s3d-detail-empty"
        >
          点击左侧任一模块，查看它的针数、工序层序与工艺说明。
        </p>
      </div>

      <div class="s3d-left-footer">
        <div class="s3d-slider-head">
          <span>拆解程度</span><b>{{ explode }}%</b>
        </div>
        <input
          v-model.number="explode"
          class="s3d-range"
          type="range"
          min="0"
          max="100"
          step="1"
          :disabled="demoTone !== 'layers'"
          aria-label="拆解程度"
        />
        <div class="s3d-slider-head">
          <span>丝线张力</span><b>{{ tension }}%</b>
        </div>
        <input
          v-model.number="tension"
          class="s3d-range"
          type="range"
          min="0"
          max="100"
          step="1"
          aria-label="丝线张力模拟"
        />
        <div class="s3d-meta">
          <div>
            <span>展示底布</span><b>{{ viewMode === 'canvas' ? '方形画布' : '圆形绣盘' }}</b>
          </div>
          <div><span>演示模式</span><b>{{ demoToneName }}</b></div>
        </div>
      </div>
    </aside>

    <!-- 中：3D 拆解舞台 -->
    <section class="s3d-stage">
      <div
        ref="canvasHost"
        class="s3d-canvas"
      ></div>

      <div class="s3d-status">
        <i :class="{ busy: rendering || !!renderError }"></i>
        <span>{{ statusText }}</span>
      </div>

      <div
        class="s3d-modes"
        role="group"
        aria-label="演示模式"
        @dblclick.stop
      >
        <button
          v-for="mode in modeMeta"
          :key="mode.key"
          type="button"
          :class="{ on: demoTone === mode.key }"
          :title="mode.hint"
          @click="setDemoTone(mode.key)"
        >
          {{ mode.name }}
        </button>
        <span class="s3d-modes-divider"></span>
        <button
          type="button"
          class="s3d-anno-btn"
          :class="{ on: annotations && demoTone === 'layers' }"
          :disabled="demoTone !== 'layers'"
          title="显示 / 隐藏模块标注"
          @click="annotations = !annotations"
        >
          标注
        </button>
      </div>

      <div
        class="s3d-toolbar"
        @dblclick.stop
      >
        <div
          class="s3d-segment"
          role="group"
          aria-label="展示形态"
        >
          <button
            type="button"
            :class="{ on: viewMode === 'canvas' }"
            :aria-pressed="viewMode === 'canvas'"
            title="方形绣布画布，无边缘遮挡"
            @click="switchViewMode('canvas')"
          >
            画布
          </button>
          <button
            type="button"
            :class="{ on: viewMode === 'hoop' }"
            :aria-pressed="viewMode === 'hoop'"
            title="圆形木框绣盘"
            @click="switchViewMode('hoop')"
          >
            绣盘
          </button>
        </div>
        <div
          class="s3d-segment"
          role="group"
          aria-label="左键拖动方式"
        >
          <button
            type="button"
            :class="{ on: dragMode === 'pan' }"
            :aria-pressed="dragMode === 'pan'"
            title="左键拖动平移画布"
            @click="dragMode = 'pan'"
          >
            平移
          </button>
          <button
            type="button"
            :class="{ on: dragMode === 'rotate' }"
            :aria-pressed="dragMode === 'rotate'"
            title="左键拖动旋转观察"
            @click="dragMode = 'rotate'"
          >
            旋转
          </button>
        </div>
        <button
          type="button"
          :class="{ on: autoRotate }"
          :aria-pressed="autoRotate"
          title="自动 360° 旋转"
          @click="autoRotate = !autoRotate"
        >
          ↻ 自动旋转
        </button>
        <button
          type="button"
          title="复位视角"
          @click="resetView"
        >
          ⟲ 复位
        </button>
        <button
          type="button"
          class="s3d-toolbar-primary"
          title="替换当前展示的纹样图片"
          @click="openUpload"
        >
          ⇧ 替换图片
        </button>
      </div>

      <div
        ref="calloutHost"
        class="s3d-callouts"
      >
        <button
          v-for="meta in layerMeta"
          :key="meta.key"
          type="button"
          class="s3d-callout"
          :class="{ on: selectedLayer === meta.key }"
          :style="{ '--tone': meta.color }"
          :data-layer="meta.key"
          @click="selectStitch(meta.key)"
        >
          <i class="s3d-callout-dot"></i>
          <span class="s3d-callout-label">
            <b>{{ meta.name }}</b>
            <small>{{ meta.part }}</small>
          </span>
        </button>
      </div>

      <div
        v-if="demoTone === 'replay'"
        class="s3d-replay"
      >
        <button
          type="button"
          class="s3d-replay-btn"
          :title="replayPlaying ? '暂停' : '播放'"
          @click="toggleReplay"
        >
          {{ replayPlaying ? '❚❚' : '▶' }}
        </button>
        <button
          type="button"
          class="s3d-replay-btn"
          title="从头重播"
          @click="startReplay"
        >
          ↺
        </button>
        <div class="s3d-replay-track"><i :style="{ width: `${replayPercent}%` }"></i></div>
        <span>{{ formatCount(replaySewn) }} / {{ formatCount(totalStitches) }} 针</span>
      </div>

      <div class="s3d-hint">
        <template v-if="demoTone === 'replay'">
          <div><span class="accent"></span>按工序逐针生长</div>
          <div><span></span>可暂停 / 重播</div>
        </template>
        <template v-else-if="demoTone === 'gloss'">
          <div><span class="accent"></span>丝线光泽与张力实时渲染</div>
          <div><span></span>滚轮贴近观察针脚起伏</div>
        </template>
        <template v-else>
          <div><span></span>{{ dragMode === 'pan' ? '左键拖动平移' : '左键拖动旋转' }}</div>
          <div><span></span>滚轮缩放</div>
          <div><span class="accent"></span>点击模块或标注查看分层</div>
        </template>
      </div>

      <div
        v-if="rendering"
        class="s3d-loading"
      >
        <span class="s3d-spinner"></span>
        <b>正在把纹样拆成工艺层…</b>
        <small>提取前景轮廓 → 识别针法分层 → 生成 3D 针脚</small>
      </div>

      <div
        v-if="renderError && !rendering"
        class="s3d-error"
      >
        <b>纹样加载失败</b>
        <p>{{ renderError }}</p>
        <button
          type="button"
          @click="openUpload"
        >
          上传本地纹样
        </button>
      </div>

      <div
        v-if="notice"
        class="s3d-toast"
      >
        {{ notice }}
      </div>
    </section>

    <!-- 右：我的纹样（与「我的纹样」数据联动） -->
    <aside class="s3d-right">
      <div class="s3d-right-head">
        <div class="s3d-right-title">
          <h2>我的纹样</h2>
          <span>{{ library.length }}</span>
        </div>
        <p>与「我的纹样」实时同步 · 点击任意纹样即刻拆解</p>
      </div>

      <div class="s3d-thumbs">
        <button
          v-for="item in library"
          :key="item.id"
          type="button"
          class="s3d-thumb"
          :class="{ on: selectedId === item.id }"
          :title="item.title"
          @click="pickPattern(item)"
        >
          <span class="s3d-thumb-media">
            <img
              :src="item.image"
              :alt="item.title"
              loading="lazy"
              decoding="async"
              @error="onThumbError"
            />
          </span>
          <span class="s3d-thumb-name">{{ item.title }}</span>
          <i
            v-if="selectedId === item.id"
            class="s3d-thumb-check"
            >✓</i
          >
        </button>
        <button
          type="button"
          class="s3d-thumb s3d-thumb-upload"
          @click="openUpload"
        >
          <span>＋</span>
          <small>上传纹样</small>
        </button>
      </div>

      <div class="s3d-right-foot">
        <p>支持上传自己的纹样图片，系统会自动拆解出填充、勾边与打籽等工艺层。</p>
        <button
          type="button"
          @click="openUpload"
        >
          ⇧ 替换展示图片
        </button>
      </div>

      <input
        ref="uploadInput"
        type="file"
        accept="image/*"
        hidden
        @change="onUploadChange"
      />
    </aside>
  </div>
</template>

<style scoped>
.s3d-page {
  height: calc(100vh - 82px);
  min-height: 720px;
  display: grid;
  grid-template-columns: 348px minmax(0, 1fr) 264px;
  align-items: stretch;
  background: #f8f6f2;
  font-family: 'Noto Sans SC', 'Microsoft YaHei', 'PingFang SC', sans-serif;
  color: #3a352f;
}
/* ===== 左侧面板 ===== */
.s3d-left {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 24px 22px 18px;
  background: #fff;
  border-right: 1px solid #eae5dd;
  box-shadow: 4px 0 24px rgba(60, 40, 20, 0.02);
  z-index: 2;
}
.s3d-back {
  align-self: flex-start;
  color: #9a948a;
  font-size: 12px;
  letter-spacing: 0.5px;
  transition: color 0.2s;
}
.s3d-back:hover {
  color: #8a2b2b;
}
.s3d-left h1 {
  margin: 18px 0 8px;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 26px;
  font-weight: 600;
  letter-spacing: 2px;
  color: #2d2a26;
}
.s3d-sub {
  margin: 0 0 16px;
  color: #9a948a;
  font-size: 11.5px;
  line-height: 1.7;
}
.s3d-parts {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1 1 auto;
  min-height: 120px;
  overflow-y: auto;
  padding-right: 3px;
}
.s3d-parts::-webkit-scrollbar {
  width: 3px;
}
.s3d-parts::-webkit-scrollbar-thumb {
  background: #e5e0d8;
  border-radius: 3px;
}
.s3d-part {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 0 0 auto;
  min-height: 62px;
  padding: 8px 10px;
  border: 1px solid #eae5dd;
  border-radius: 11px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: 0.22s ease;
  outline: none;
}
.s3d-part:hover,
.s3d-part:focus-visible {
  border-color: #cfc6b8;
  box-shadow: 0 6px 16px rgba(80, 50, 24, 0.07);
}
.s3d-part.active {
  border-color: rgba(183, 55, 34, 0.42);
  background: #fffaf7;
  box-shadow: 0 6px 18px rgba(183, 55, 34, 0.08);
}
.s3d-part.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 10px;
  bottom: 10px;
  width: 3px;
  border-radius: 3px;
  background: #b73722;
}
.s3d-part.off {
  opacity: 0.58;
}
.s3d-part.empty {
  opacity: 0.42;
}
.s3d-part-thumb {
  flex: 0 0 46px;
  height: 46px;
  display: block;
  overflow: hidden;
  border: 1px solid #f0e8dc;
  border-radius: 8px;
  background: #f7efe3;
  color: var(--tone);
}
.s3d-part-thumb svg {
  display: block;
  width: 100%;
  height: 100%;
}
.s3d-part-main {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.s3d-part-title {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
.s3d-part-title b {
  font-size: 13px;
  font-weight: 600;
  color: #3a352f;
  white-space: nowrap;
}
.s3d-part-title em {
  padding: 1px 5px;
  border: 1px solid color-mix(in srgb, var(--chip) 32%, #fff);
  border-radius: 4px;
  background: color-mix(in srgb, var(--chip) 7%, #fff);
  color: var(--chip);
  font-size: 9px;
  font-style: normal;
  letter-spacing: 0.4px;
  white-space: nowrap;
}
.s3d-part-meta {
  color: #9a948a;
  font-size: 10.5px;
  letter-spacing: 0.3px;
  white-space: nowrap;
}
.s3d-part-eye {
  flex: 0 0 auto;
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: #a8a094;
}
.s3d-part-eye:hover {
  transform: none;
  background: #f6efe6;
  border-color: transparent;
  color: #8a2b2b;
}
.s3d-part-eye.off {
  color: #cfc6b8;
}
.s3d-part-eye svg {
  width: 16px;
  height: 16px;
}
.s3d-detail {
  flex: 0 0 auto;
  margin-top: 12px;
  padding: 11px 12px 12px;
  border: 1px solid #efe7db;
  border-radius: 12px;
  background: linear-gradient(145deg, #fffdf9, #faf4ea);
}
.s3d-detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #a09a90;
  font-size: 9.5px;
  letter-spacing: 1px;
}
.s3d-detail-head b {
  font-size: 12.5px;
  font-weight: 600;
  letter-spacing: 0.5px;
}
.s3d-detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}
.s3d-detail-grid div {
  padding: 6px 8px;
  border: 1px solid #f2ece2;
  border-radius: 7px;
  background: #fff;
}
.s3d-detail-grid span {
  display: block;
  color: #a09a90;
  font-size: 9px;
  letter-spacing: 0.5px;
}
.s3d-detail-grid b {
  display: block;
  margin-top: 2px;
  color: #3a352f;
  font-size: 12px;
  font-weight: 600;
}
.s3d-detail p {
  margin: 8px 0 0;
  color: #8f887e;
  font-size: 10.5px;
  line-height: 1.6;
}
.s3d-detail p.s3d-detail-subject {
  margin-top: 10px;
  color: #6f665a;
  font-weight: 600;
}
.s3d-detail-empty {
  margin: 0;
  padding: 8px 2px;
  color: #a09a90;
  font-size: 11px;
  line-height: 1.7;
  text-align: center;
}
.s3d-left-footer {
  flex: 0 0 auto;
  padding-top: 12px;
  margin-top: 12px;
  border-top: 1px solid #f0ebe3;
}
.s3d-slider-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin: 10px 0 5px;
  color: #8f887e;
  font-size: 11.5px;
}
.s3d-slider-head:first-child {
  margin-top: 0;
}
.s3d-slider-head b {
  color: #2d2a26;
  font-size: 12px;
  font-weight: 600;
}
.s3d-range {
  width: 100%;
  accent-color: #8a2b2b;
  cursor: pointer;
}
.s3d-range:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.s3d-meta {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 12px;
}
.s3d-meta div {
  padding: 7px 9px;
  border: 1px solid #f0ebe3;
  border-radius: 8px;
  background: #fdfbf8;
}
.s3d-meta span {
  display: block;
  color: #a09a90;
  font-size: 9.5px;
  letter-spacing: 0.5px;
}
.s3d-meta b {
  display: block;
  margin-top: 2px;
  color: #3a352f;
  font-size: 12px;
  font-weight: 600;
}

/* ===== 中间 3D 舞台 ===== */
.s3d-stage {
  position: relative;
  min-width: 0;
  min-height: 720px;
  overflow: hidden;
  background: radial-gradient(circle at 50% 38%, #fffdf8 0%, #f6f1e7 56%, #eae2d4 100%);
}
.s3d-stage::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 1;
  background-image:
    linear-gradient(rgba(150, 120, 80, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(150, 120, 80, 0.05) 1px, transparent 1px);
  background-size: 46px 46px;
  -webkit-mask-image: radial-gradient(circle at 50% 45%, #000 30%, transparent 78%);
  mask-image: radial-gradient(circle at 50% 45%, #000 30%, transparent 78%);
  pointer-events: none;
}
.s3d-canvas {
  position: absolute;
  inset: 0;
  z-index: 0;
}
.s3d-canvas :deep(canvas) {
  display: block;
  width: 100% !important;
  height: 100% !important;
  cursor: grab;
}
.s3d-canvas :deep(canvas:active) {
  cursor: grabbing;
}
.s3d-status {
  position: absolute;
  top: 18px;
  left: 18px;
  z-index: 5;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 14px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 999px;
  background: rgba(45, 42, 38, 0.66);
  backdrop-filter: blur(10px);
  color: #fff;
  font-size: 11px;
  letter-spacing: 0.8px;
  white-space: nowrap;
}
.s3d-status i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #4ade80;
  box-shadow: 0 0 6px rgba(74, 222, 128, 0.9);
  animation: s3d-pulse 1.8s infinite;
}
.s3d-status i.busy {
  background: #f0b44a;
  box-shadow: 0 0 6px rgba(240, 180, 74, 0.9);
}
@keyframes s3d-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.35;
  }
}
.s3d-modes {
  position: absolute;
  top: 16px;
  left: 50%;
  z-index: 5;
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 4px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.84);
  backdrop-filter: blur(12px);
  box-shadow: 0 10px 24px -10px rgba(60, 40, 20, 0.35);
  transform: translateX(-50%);
}
.s3d-modes button {
  padding: 7px 14px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #7d7264;
  font-size: 11px;
  letter-spacing: 0.6px;
  white-space: nowrap;
  transition: 0.2s;
}
.s3d-modes button:hover {
  transform: none;
  background: #fff;
  color: #8a2b2b;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}
.s3d-modes button.on {
  background: #2d2a26;
  color: #fff;
  box-shadow: 0 4px 10px rgba(45, 42, 38, 0.28);
}
.s3d-modes button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.s3d-modes-divider {
  width: 1px;
  height: 16px;
  margin: 0 4px;
  background: #eae2d6;
}
.s3d-toolbar {
  position: absolute;
  bottom: 16px;
  left: 50%;
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 4px;
  max-width: calc(100% - 32px);
  padding: 5px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(12px);
  box-shadow: 0 10px 24px -10px rgba(60, 40, 20, 0.35);
  transform: translateX(-50%);
}
.s3d-toolbar button {
  padding: 8px 13px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #6b6257;
  font-size: 11px;
  letter-spacing: 0.5px;
  white-space: nowrap;
  transition: 0.2s;
}
.s3d-toolbar button:hover {
  transform: none;
  background: #fff;
  color: #8a2b2b;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}
.s3d-toolbar button.on {
  background: #fff;
  color: #8a2b2b;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}
.s3d-segment {
  display: flex;
  gap: 1px;
  padding: 2px;
  border-radius: 999px;
  background: #f1ece3;
}
.s3d-segment button {
  padding: 6px 12px;
}
.s3d-segment button:not(.on) {
  color: #8d8478;
}
.s3d-toolbar .s3d-toolbar-primary {
  background: #2d2a26;
  color: #fff;
}
.s3d-toolbar .s3d-toolbar-primary:hover {
  background: #8a2b2b;
  color: #fff;
}
.s3d-callouts {
  position: absolute;
  inset: 0;
  z-index: 3;
  overflow: hidden;
  pointer-events: none;
}
.s3d-callout {
  position: absolute;
  left: 0;
  top: 0;
  display: flex;
  align-items: center;
  padding: 0;
  border: 0;
  background: transparent;
  opacity: 0;
  pointer-events: auto;
  transform: translate(16px, -50%);
  transition: opacity 0.25s ease;
  cursor: pointer;
}
.s3d-callout-dot {
  position: absolute;
  left: -20px;
  top: 50%;
  width: 7px;
  height: 7px;
  border: 2px solid #fff;
  border-radius: 50%;
  background: var(--tone);
  box-shadow: 0 0 10px color-mix(in srgb, var(--tone) 70%, transparent);
  transform: translate(-50%, -50%);
  transition: transform 0.2s ease;
}
.s3d-callout::before {
  content: '';
  position: absolute;
  left: -14px;
  top: 50%;
  width: 14px;
  height: 1px;
  background: color-mix(in srgb, var(--tone) 55%, #fff);
}
.s3d-callout-label {
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding: 5px 10px;
  border: 1px solid color-mix(in srgb, var(--tone) 30%, #fff);
  border-radius: 8px;
  background: rgba(255, 252, 246, 0.92);
  backdrop-filter: blur(6px);
  box-shadow: 0 8px 20px -8px rgba(70, 45, 20, 0.45);
  white-space: nowrap;
}
.s3d-callout-label b {
  color: #3a352f;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.4px;
}
.s3d-callout-label small {
  color: var(--tone);
  font-size: 9px;
  letter-spacing: 0.4px;
}
.s3d-callout.on .s3d-callout-label {
  border-color: var(--tone);
  background: #fff;
  box-shadow:
    0 0 0 1px var(--tone),
    0 10px 24px -8px rgba(70, 45, 20, 0.5);
}
.s3d-callout.on .s3d-callout-dot {
  transform: translate(-50%, -50%) scale(1.35);
}
.s3d-replay {
  position: absolute;
  left: 50%;
  bottom: 68px;
  z-index: 5;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 14px 7px 8px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(12px);
  box-shadow: 0 12px 28px -14px rgba(60, 40, 20, 0.45);
  transform: translateX(-50%);
  color: #6b6257;
  font-size: 10.5px;
  letter-spacing: 0.4px;
  white-space: nowrap;
}
.s3d-replay-btn {
  width: 26px;
  height: 26px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: #2d2a26;
  color: #fff;
  font-size: 10px;
}
.s3d-replay-btn:hover {
  transform: none;
  background: #8a2b2b;
}
.s3d-replay-track {
  width: 150px;
  height: 4px;
  overflow: hidden;
  border-radius: 999px;
  background: #ede6da;
}
.s3d-replay-track i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #c9953f, #b73722);
  transition: width 0.12s linear;
}
.s3d-hint {
  position: absolute;
  left: 50%;
  bottom: 68px;
  z-index: 4;
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 8px 18px;
  border: 1px solid rgba(234, 229, 221, 0.9);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(12px);
  box-shadow: 0 12px 28px -14px rgba(60, 40, 20, 0.45);
  color: #6b6257;
  font-size: 10.5px;
  white-space: nowrap;
  pointer-events: none;
  transform: translateX(-50%);
}
.s3d-hint div {
  display: flex;
  align-items: center;
  gap: 6px;
}
.s3d-hint span {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #b8b1a6;
}
.s3d-hint span.accent {
  background: #a83232;
}
.s3d-loading {
  position: absolute;
  inset: 0;
  z-index: 8;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgba(248, 246, 242, 0.72);
  backdrop-filter: blur(6px);
  color: #6b6257;
}
.s3d-loading b {
  color: #4a443c;
  font-size: 14px;
  font-weight: 600;
}
.s3d-loading small {
  color: #9a948a;
  font-size: 11px;
  letter-spacing: 0.5px;
}
.s3d-spinner {
  width: 34px;
  height: 34px;
  border: 2px solid rgba(138, 43, 43, 0.18);
  border-top-color: #8a2b2b;
  border-radius: 50%;
  animation: s3d-spin 0.9s linear infinite;
}
@keyframes s3d-spin {
  to {
    transform: rotate(360deg);
  }
}
.s3d-error {
  position: absolute;
  left: 50%;
  top: 50%;
  z-index: 8;
  width: min(360px, 86%);
  padding: 22px 24px;
  border: 1px solid #eadfd5;
  border-radius: 14px;
  background: rgba(255, 253, 249, 0.96);
  box-shadow: 0 24px 60px -20px rgba(60, 40, 20, 0.4);
  text-align: center;
  transform: translate(-50%, -50%);
}
.s3d-error b {
  color: #8a2b2b;
  font-size: 15px;
}
.s3d-error p {
  margin: 10px 0 16px;
  color: #8f887e;
  font-size: 12px;
  line-height: 1.7;
}
.s3d-error button {
  padding: 9px 20px;
  border: 0;
  border-radius: 9px;
  background: #8a2b2b;
  color: #fff;
  font-size: 12px;
}
.s3d-toast {
  position: absolute;
  left: 50%;
  bottom: 118px;
  z-index: 9;
  padding: 9px 18px;
  border-radius: 999px;
  background: rgba(45, 42, 38, 0.86);
  color: #fff;
  font-size: 12px;
  letter-spacing: 0.5px;
  white-space: nowrap;
  box-shadow: 0 12px 28px -10px rgba(0, 0, 0, 0.5);
  pointer-events: none;
  transform: translateX(-50%);
}

/* ===== 右侧纹样栏 ===== */
.s3d-right {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 22px 18px 18px;
  background: #fffdfa;
  border-left: 1px solid #eae5dd;
  z-index: 2;
}
.s3d-right-head {
  flex: 0 0 auto;
}
.s3d-right-title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.s3d-right-title h2 {
  margin: 0;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 19px;
  font-weight: 600;
  letter-spacing: 1.5px;
  color: #2d2a26;
}
.s3d-right-title span {
  min-width: 20px;
  padding: 1px 7px;
  border-radius: 999px;
  background: #f7ece6;
  color: #a83232;
  font-size: 11px;
  text-align: center;
}
.s3d-right-head p {
  margin: 8px 0 14px;
  color: #9a948a;
  font-size: 10.5px;
  line-height: 1.65;
}
.s3d-thumbs {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  align-content: start;
  padding: 2px 4px 8px 2px;
}
.s3d-thumbs::-webkit-scrollbar {
  width: 3px;
}
.s3d-thumbs::-webkit-scrollbar-thumb {
  background: #e5e0d8;
  border-radius: 3px;
}
.s3d-thumb {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 133px;
  padding: 0;
  overflow: hidden;
  border: 1px solid #ece7df;
  border-radius: 10px;
  background: #fff;
  text-align: left;
  transition: 0.22s ease;
}
.s3d-thumb:hover {
  transform: translateY(-1px);
  border-color: #cfc6b8;
  box-shadow: 0 6px 16px rgba(80, 50, 24, 0.1);
}
.s3d-thumb.on {
  border-color: #a83232;
  box-shadow:
    0 0 0 1px #a83232,
    0 8px 20px -8px rgba(168, 50, 50, 0.45);
}
.s3d-thumb-media {
  position: relative;
  display: block;
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
  width: 100%;
  background: linear-gradient(135deg, #f6efe4, #efe6d8);
}
.s3d-thumb img {
  position: absolute;
  inset: 0;
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.s3d-thumb-name {
  display: block;
  padding: 6px 8px;
  color: #7d766c;
  font-size: 10.5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.s3d-thumb-check {
  position: absolute;
  top: 5px;
  right: 5px;
  width: 17px;
  height: 17px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #a83232;
  color: #fff;
  font-size: 10px;
  font-style: normal;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.25);
}
.s3d-thumb-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-height: 106px;
  border-style: dashed;
  border-color: #d9d1c5;
  background: #fffdfa;
  color: #a09a90;
}
.s3d-thumb-upload span {
  font-size: 22px;
  line-height: 1;
  color: #b5aea4;
}
.s3d-thumb-upload small {
  font-size: 10.5px;
  letter-spacing: 0.5px;
}
.s3d-right-foot {
  flex: 0 0 auto;
  padding-top: 14px;
  margin-top: 6px;
  border-top: 1px solid #f0ebe3;
}
.s3d-right-foot p {
  margin: 0 0 12px;
  color: #a09a90;
  font-size: 10.5px;
  line-height: 1.7;
}
.s3d-right-foot button {
  width: 100%;
  padding: 11px 14px;
  border: 0;
  border-radius: 10px;
  background: linear-gradient(135deg, #a83232, #7a2020);
  color: #fff;
  font-size: 12px;
  letter-spacing: 1px;
  box-shadow: 0 10px 20px -10px rgba(168, 50, 50, 0.6);
  transition: 0.22s;
}
.s3d-right-foot button:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 24px -10px rgba(168, 50, 50, 0.65);
}

/* ===== 响应式 ===== */
@media (max-width: 1280px) {
  .s3d-page {
    grid-template-columns: 320px minmax(0, 1fr) 242px;
  }
  .s3d-left {
    padding: 20px 18px 16px;
  }
  .s3d-thumb {
    height: 123px;
  }
}
@media (max-width: 1100px) {
  .s3d-page {
    height: auto;
    min-height: calc(100vh - 130px);
    grid-template-columns: 296px minmax(0, 1fr);
    grid-template-rows: minmax(560px, 68vh) auto;
    overflow: visible;
  }
  .s3d-stage {
    min-height: 560px;
  }
  .s3d-modes {
    left: 18px;
    transform: none;
  }
  .s3d-right {
    grid-column: 1 / -1;
    border-left: 0;
    border-top: 1px solid #eae5dd;
    padding: 18px 24px 20px;
  }
  .s3d-thumbs {
    display: flex;
    overflow-x: auto;
    overflow-y: hidden;
    gap: 10px;
    padding-bottom: 6px;
  }
  .s3d-thumb {
    flex: 0 0 124px;
    height: 151px;
  }
  .s3d-right-foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 18px;
    margin-top: 10px;
  }
  .s3d-right-foot p {
    margin: 0;
  }
  .s3d-right-foot button {
    width: auto;
    flex: 0 0 auto;
  }
}
@media (max-width: 760px) {
  .s3d-page {
    grid-template-columns: 1fr;
    grid-template-rows: auto auto auto;
  }
  .s3d-left {
    border-right: 0;
    border-bottom: 1px solid #eae5dd;
  }
  .s3d-parts {
    overflow: visible;
  }
  .s3d-stage {
    min-height: 480px;
  }
  .s3d-right {
    grid-column: auto;
    padding: 16px;
  }
  .s3d-toolbar button {
    padding: 7px 10px;
    font-size: 10px;
  }
  .s3d-modes {
    top: 56px;
  }
  .s3d-modes button {
    padding: 6px 10px;
    font-size: 10px;
  }
  .s3d-hint {
    gap: 10px;
    padding: 8px 12px;
    font-size: 10px;
  }
}
</style>
