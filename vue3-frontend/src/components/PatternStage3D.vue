<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import * as THREE from 'three';

const props = defineProps<{ src: string }>();

const host = ref<HTMLDivElement | null>(null);
const ready = ref(false);
const failed = ref(false);

let renderer: THREE.WebGLRenderer | null = null;
let scene: THREE.Scene | null = null;
let camera: THREE.PerspectiveCamera | null = null;
let group: THREE.Group | null = null;
let mesh: THREE.Mesh | null = null;
let texture: THREE.Texture | null = null;
let sheen: THREE.PointLight | null = null;
let resizeObserver: ResizeObserver | null = null;
let frameId = 0;
const clock = new THREE.Clock();

let rotX = -0.06;
let rotY = 0.18;
let goalX = -0.06;
let goalY = 0.18;
let dragging = false;
let lastX = 0;
let lastY = 0;
let zoom = 1;
let goalZoom = 1;
const pointers = new Map<number, { x: number; y: number }>();
let pinchStart = 0;
let pinchZoom = 1;

const clampZoom = (value: number) => Math.min(2.6, Math.max(0.65, value));
const pointerDistance = () => {
  const points = [...pointers.values()];
  if (points.length < 2) return 0;
  return Math.hypot(points[0].x - points[1].x, points[0].y - points[1].y);
};

function onPointerDown(event: PointerEvent) {
  pointers.set(event.pointerId, { x: event.clientX, y: event.clientY });
  if (pointers.size === 2) {
    dragging = false;
    pinchStart = pointerDistance();
    pinchZoom = goalZoom;
  } else {
    dragging = true;
    lastX = event.clientX;
    lastY = event.clientY;
  }
  (event.currentTarget as HTMLElement).setPointerCapture(event.pointerId);
}
function onPointerMove(event: PointerEvent) {
  if (pointers.has(event.pointerId))
    pointers.set(event.pointerId, { x: event.clientX, y: event.clientY });
  if (pointers.size === 2) {
    const distance = pointerDistance();
    if (pinchStart > 0) goalZoom = clampZoom(pinchZoom * (distance / pinchStart));
    return;
  }
  if (!dragging) return;
  goalY += (event.clientX - lastX) * 0.006;
  goalX = THREE.MathUtils.clamp(goalX + (event.clientY - lastY) * 0.005, -0.5, 0.5);
  lastX = event.clientX;
  lastY = event.clientY;
}
function onPointerUp(event: PointerEvent) {
  pointers.delete(event.pointerId);
  if (pointers.size < 2) pinchStart = 0;
  if (pointers.size === 0) dragging = false;
  (event.currentTarget as HTMLElement).releasePointerCapture?.(event.pointerId);
}
function onWheel(event: WheelEvent) {
  event.preventDefault();
  goalZoom = clampZoom(goalZoom * (event.deltaY > 0 ? 0.9 : 1.1));
}

const OSS_HOSTS = [
  'https://xwzc.oss-cn-beijing.aliyuncs.com',
  'http://xwzc.oss-cn-beijing.aliyuncs.com'
];
// OSS 桶未配置 CORS，WebGL 读取纹样会被浏览器拦截；开发期改走同源 /oss 代理。
function textureUrl(url: string) {
  const host = OSS_HOSTS.find((value) => url.startsWith(`${value}/`));
  if (!host) return url;
  // 原图 3–4MB，贴图只需要 1000px 宽度，走 OSS 图片处理取 WebP 缩略图（约 300KB）。
  return `/oss/${url.slice(host.length + 1)}?x-oss-process=image/resize,w_1000/format,webp/quality,q_80`;
}

function buildScene(width: number, height: number) {
  if (!host.value) return;
  scene = new THREE.Scene();
  camera = new THREE.PerspectiveCamera(34, width / Math.max(1, height), 0.1, 50);
  camera.position.set(0, 0, 4);
  camera.lookAt(0, 0, 0);

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
  renderer.setSize(width, height);
  renderer.outputColorSpace = THREE.SRGBColorSpace;
  renderer.toneMapping = THREE.ACESFilmicToneMapping;
  renderer.toneMappingExposure = 1.06;
  host.value.appendChild(renderer.domElement);

  group = new THREE.Group();
  scene.add(group);

  const geometry = new THREE.PlaneGeometry(2, 2.5, 44, 54);
  const material = new THREE.MeshPhysicalMaterial({
    color: 0xffffff,
    roughness: 0.58,
    metalness: 0.04,
    clearcoat: 0.28,
    clearcoatRoughness: 0.5,
    side: THREE.DoubleSide
  });
  mesh = new THREE.Mesh(geometry, material);
  group.add(mesh);

  const loader = new THREE.TextureLoader();
  loader.setCrossOrigin('anonymous');
  loader.load(
    textureUrl(props.src),
    (loaded) => {
      texture = loaded;
      texture.colorSpace = THREE.SRGBColorSpace;
      const image = loaded.image as HTMLImageElement | HTMLCanvasElement;
      const imageAspect = image.width / Math.max(1, image.height);
      const planeAspect = 2 / 2.5;
      if (imageAspect > planeAspect) {
        texture.repeat.x = planeAspect / imageAspect;
        texture.offset.x = (1 - texture.repeat.x) / 2;
      } else {
        texture.repeat.y = imageAspect / planeAspect;
        texture.offset.y = (1 - texture.repeat.y) / 2;
      }
      material.map = texture;
      material.needsUpdate = true;
      ready.value = true;
    },
    undefined,
    () => {
      // 贴图被跨域策略拦下时，保留上层 <img> 展示原纹样，不替换成别的图。
      failed.value = true;
      material.visible = false;
    }
  );

  scene.add(new THREE.HemisphereLight(0xfffaf0, 0x6b5a48, 0.95));
  const key = new THREE.DirectionalLight(0xfff2de, 1.65);
  key.position.set(2.2, 3.2, 4);
  scene.add(key);
  const fill = new THREE.DirectionalLight(0xdfe8f5, 0.5);
  fill.position.set(-3, -1.5, 2.5);
  scene.add(fill);
  sheen = new THREE.PointLight(0xffd8a8, 1.4, 9, 2);
  sheen.position.set(0, 1.4, 2.4);
  scene.add(sheen);

  const canvas = renderer.domElement;
  canvas.style.touchAction = 'none';
  canvas.style.cursor = 'grab';
  canvas.addEventListener('pointerdown', onPointerDown);
  canvas.addEventListener('pointermove', onPointerMove);
  canvas.addEventListener('pointerup', onPointerUp);
  canvas.addEventListener('pointercancel', onPointerUp);
  canvas.addEventListener('wheel', onWheel, { passive: false });
}

function animate() {
  frameId = requestAnimationFrame(animate);
  if (!renderer || !scene || !camera || !group || !mesh) return;
  const delta = Math.min(clock.getDelta(), 0.06);
  const time = clock.elapsedTime;

  const idleY = dragging ? 0 : Math.sin(time * 0.32) * 0.1;
  rotY += (goalY + idleY - rotY) * Math.min(1, delta * 6);
  rotX += (goalX - rotX) * Math.min(1, delta * 6);
  group.rotation.set(rotX, rotY, Math.sin(time * 0.24) * 0.015);

  zoom += (goalZoom - zoom) * Math.min(1, delta * 9);
  camera.position.z = 4 / zoom;

  const position = mesh.geometry.attributes.position as THREE.BufferAttribute;
  const array = position.array as Float32Array;
  for (let i = 0; i < array.length; i += 3) {
    const x = array[i];
    const y = array[i + 1];
    array[i + 2] =
      Math.sin(x * 2.2 + time * 0.95) * 0.035 + Math.sin(y * 2.8 - time * 0.7) * 0.026;
  }
  position.needsUpdate = true;

  if (sheen) {
    sheen.position.set(Math.sin(time * 0.5) * 2.1, Math.cos(time * 0.34) * 1.2 + 0.4, 2.5);
    sheen.intensity = 1.15 + Math.sin(time * 0.9) * 0.35;
  }
  renderer.render(scene, camera);
}

onMounted(() => {
  const element = host.value;
  if (!element) return;
  buildScene(element.clientWidth || 640, element.clientHeight || 800);
  animate();
  resizeObserver = new ResizeObserver((entries) => {
    const rect = entries[0]?.contentRect;
    if (!rect || !renderer || !camera || !rect.width || !rect.height) return;
    renderer.setSize(rect.width, rect.height);
    camera.aspect = rect.width / rect.height;
    camera.updateProjectionMatrix();
  });
  resizeObserver.observe(element);
});

onBeforeUnmount(() => {
  cancelAnimationFrame(frameId);
  resizeObserver?.disconnect();
  resizeObserver = null;
  const canvas = renderer?.domElement;
  canvas?.removeEventListener('pointerdown', onPointerDown);
  canvas?.removeEventListener('pointermove', onPointerMove);
  canvas?.removeEventListener('pointerup', onPointerUp);
  canvas?.removeEventListener('pointercancel', onPointerUp);
  canvas?.removeEventListener('wheel', onWheel);
  if (mesh) {
    mesh.geometry.dispose();
    const material = mesh.material as THREE.MeshPhysicalMaterial;
    material.map = null;
    material.dispose();
  }
  texture?.dispose();
  texture = null;
  renderer?.dispose();
  renderer = null;
  scene = null;
  camera = null;
  group = null;
  mesh = null;
  sheen = null;
  clock.stop();
});
</script>

<template>
  <div
    ref="host"
    class="pattern-stage-3d"
    :class="{ 'is-ready': ready, 'is-failed': failed }"
  >
    <img
      class="stage-fallback"
      :src="src"
      alt=""
      draggable="false"
    />
  </div>
</template>

<style scoped>
.pattern-stage-3d {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}
.stage-fallback {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 1;
  transition: opacity 0.45s ease;
  user-select: none;
}
.pattern-stage-3d.is-ready .stage-fallback {
  opacity: 0;
}
.pattern-stage-3d :deep(canvas) {
  position: absolute;
  inset: 0;
  display: block;
  width: 100% !important;
  height: 100% !important;
  opacity: 0;
  transition: opacity 0.45s ease;
}
/* 纹样贴图就绪前先显示原图，避免加载期间看到空白布面。 */
.pattern-stage-3d.is-ready :deep(canvas) {
  opacity: 1;
}
</style>
