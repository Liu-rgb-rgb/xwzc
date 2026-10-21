<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import {
  musicState,
  musicTracks,
  nextTrack,
  playTrack,
  prevTrack,
  seekMusic,
  setVolume,
  toggleMusic
} from '../music';

const root = ref<HTMLElement | null>(null);
const open = ref(false);
const route = useRoute();
const previewIndex = ref(musicState.index);
const reducedMotion =
  typeof window !== 'undefined' &&
  !!window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
const displayIndex = computed(() => (musicState.playing ? musicState.index : previewIndex.value));
const displayTrack = computed(() => musicTracks[displayIndex.value]);
const showingCurrent = computed(() => displayIndex.value === musicState.index);
let carouselTimer: number | undefined;

function stopCarousel() {
  if (carouselTimer) window.clearInterval(carouselTimer);
  carouselTimer = undefined;
}
function startCarousel() {
  stopCarousel();
  if (!open.value || musicState.playing || reducedMotion || musicTracks.length < 2) return;
  carouselTimer = window.setInterval(() => {
    previewIndex.value = (previewIndex.value + 1) % musicTracks.length;
  }, 4200);
}
function togglePlay() {
  if (musicState.playing) {
    toggleMusic();
    return;
  }
  playTrack(displayIndex.value);
}
watch(
  () => musicState.index,
  (index) => {
    previewIndex.value = index;
  }
);
watch([open, () => musicState.playing], () => startCarousel());

function formatTime(value: number) {
  if (!Number.isFinite(value) || value <= 0) return '0:00';
  const minutes = Math.floor(value / 60);
  const seconds = Math.floor(value % 60);
  return `${minutes}:${String(seconds).padStart(2, '0')}`;
}
function togglePanel() {
  open.value = !open.value;
}
function closePanel() {
  open.value = false;
}
function onSeek(event: Event) {
  if (!showingCurrent.value) return;
  seekMusic(Number((event.target as HTMLInputElement).value));
}
function onVolume(event: Event) {
  setVolume(Number((event.target as HTMLInputElement).value));
}
function onDocumentPointerDown(event: PointerEvent) {
  if (!open.value) return;
  if (root.value && !root.value.contains(event.target as Node)) closePanel();
}
function onDocumentKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && open.value) closePanel();
}

watch(
  () => route.fullPath,
  () => closePanel()
);

onMounted(() => {
  document.addEventListener('pointerdown', onDocumentPointerDown);
  document.addEventListener('keydown', onDocumentKeydown);
});
onBeforeUnmount(() => {
  stopCarousel();
  document.removeEventListener('pointerdown', onDocumentPointerDown);
  document.removeEventListener('keydown', onDocumentKeydown);
});
</script>

<template>
  <div
    ref="root"
    class="music-player"
  >
    <button
      class="music-trigger"
      :class="{ on: musicState.playing }"
      type="button"
      title="背景音乐"
      aria-label="背景音乐"
      :aria-expanded="open"
      @click="togglePanel"
    >
      <span
        v-if="musicState.playing"
        class="music-bars"
        aria-hidden="true"
      ><i /><i /><i /></span>
      <svg
        v-else
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="1.7"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <path d="M9 18V6l10-2v12" />
        <circle
          cx="6.5"
          cy="18"
          r="2.5"
        />
        <circle
          cx="16.5"
          cy="16"
          r="2.5"
        />
      </svg>
    </button>

    <Transition name="music-pop">
      <div
        v-if="open"
        class="music-panel"
        role="dialog"
        aria-label="背景音乐播放器"
      >
        <div class="music-head">
          <b>背景音乐</b>
          <span>3 首 · 自动轮播</span>
        </div>

        <div class="music-stage">
          <span
            class="music-vinyl"
            :class="{ spin: musicState.playing }"
            aria-hidden="true"
          />
          <div class="music-covers">
            <img
              v-for="(track, index) in musicTracks"
              :key="track.id"
              class="music-cover"
              :class="{ show: index === displayIndex }"
              :src="track.cover"
              :alt="track.title"
            />
          </div>
          <div class="music-dots">
            <button
              v-for="(track, index) in musicTracks"
              :key="track.id"
              type="button"
              :class="{ on: index === displayIndex }"
              :aria-label="`播放 ${track.title}`"
              @click="playTrack(index)"
            />
          </div>
        </div>

        <div class="music-info">
          <b>{{ displayTrack.title }}</b>
          <span>{{ displayTrack.artist }}</span>
        </div>

        <div class="music-progress">
          <input
            type="range"
            min="0"
            :max="showingCurrent ? musicState.duration || 0 : 0"
            step="0.1"
            :value="showingCurrent ? musicState.currentTime : 0"
            :disabled="!showingCurrent || !musicState.duration"
            aria-label="播放进度"
            @input="onSeek"
          />
          <div class="music-times">
            <span>{{ formatTime(showingCurrent ? musicState.currentTime : 0) }}</span>
            <span>{{ formatTime(showingCurrent ? musicState.duration : 0) }}</span>
          </div>
        </div>

        <div class="music-controls">
          <button
            type="button"
            title="上一首"
            aria-label="上一首"
            @click="prevTrack"
          >
            <svg
              viewBox="0 0 24 24"
              fill="currentColor"
              aria-hidden="true"
            >
              <path d="M7 5h2v14H7zM19 5v14L9.5 12z" />
            </svg>
          </button>
          <button
            class="music-play"
            type="button"
            :title="musicState.playing ? '暂停' : '播放'"
            :aria-label="musicState.playing ? '暂停' : '播放'"
            @click="togglePlay"
          >
            <svg
              v-if="musicState.playing"
              viewBox="0 0 24 24"
              fill="currentColor"
              aria-hidden="true"
            >
              <path d="M7 5h4v14H7zM13 5h4v14h-4z" />
            </svg>
            <svg
              v-else
              viewBox="0 0 24 24"
              fill="currentColor"
              aria-hidden="true"
            >
              <path d="M8 5.5v13l11-6.5z" />
            </svg>
          </button>
          <button
            type="button"
            title="下一首"
            aria-label="下一首"
            @click="nextTrack()"
          >
            <svg
              viewBox="0 0 24 24"
              fill="currentColor"
              aria-hidden="true"
            >
              <path d="M15 5h2v14h-2zM5 5v14l9.5-7z" />
            </svg>
          </button>
          <div class="music-volume">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.6"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <path d="M4 9h4l5-4v14l-5-4H4z" />
              <path d="M17 8.5a5 5 0 0 1 0 7" />
            </svg>
            <input
              type="range"
              min="0"
              max="1"
              step="0.01"
              :value="musicState.volume"
              aria-label="音量"
              @input="onVolume"
            />
          </div>
        </div>

        <div class="music-list">
          <button
            v-for="(track, index) in musicTracks"
            :key="track.id"
            class="music-row"
            :class="{ on: index === musicState.index }"
            type="button"
            @click="playTrack(index)"
          >
            <img
              :src="track.cover"
              alt=""
              loading="lazy"
            />
            <span class="music-row-text">
              <b>{{ track.title }}</b>
              <small>{{ track.artist }}</small>
            </span>
            <span class="music-row-state">{{
              index === musicState.index ? (musicState.playing ? '播放中' : '已暂停') : ''
            }}</span>
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.music-player {
  position: relative;
  display: flex;
  align-items: center;
  flex: 0 0 auto;
}
.music-trigger {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  padding: 0;
  border: 1px solid #e6ded2;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.72);
  color: #4a443d;
  line-height: 0;
  transition:
    border-color 0.3s,
    background 0.3s,
    color 0.3s;
}
.music-trigger:hover {
  border-color: #c9b8a4;
  background: #fff;
  color: #2c2a29;
  transform: none;
}
.music-trigger.on {
  border-color: rgba(138, 43, 43, 0.3);
  background: #fff;
  color: #8a2b2b;
}
.music-trigger > svg {
  width: 17px;
  height: 17px;
}
.music-bars {
  display: flex;
  align-items: flex-end;
  gap: 2.5px;
  height: 15px;
}
.music-bars i {
  width: 2px;
  height: 13px;
  border-radius: 2px;
  background: #8a2b2b;
  transform-origin: bottom;
  animation: music-eq 0.9s ease-in-out infinite;
}
.music-bars i:nth-child(1) {
  animation-delay: 0s;
}
.music-bars i:nth-child(2) {
  animation-delay: 0.18s;
}
.music-bars i:nth-child(3) {
  animation-delay: 0.36s;
}
@keyframes music-eq {
  0%,
  100% {
    transform: scaleY(0.35);
  }
  50% {
    transform: scaleY(1);
  }
}

.music-panel {
  position: absolute;
  top: calc(100% + 28px);
  right: 0;
  z-index: 90;
  width: 340px;
  max-height: min(600px, calc(100vh - 100px));
  overflow: auto;
  padding: 18px;
  border: 1px solid #eae6df;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 26px 60px rgba(45, 42, 38, 0.18);
  color: #2c2a29;
  font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  scrollbar-width: thin;
}
.music-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.music-head b {
  color: #8d8a84;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 2px;
}
.music-head span {
  color: #b3ada4;
  font-size: 11px;
  letter-spacing: 0.5px;
}
.music-stage {
  position: relative;
  height: 168px;
  margin-bottom: 12px;
}
.music-vinyl {
  position: absolute;
  top: 9px;
  right: 24px;
  width: 150px;
  height: 150px;
  border-radius: 50%;
  background:
    radial-gradient(circle at center, #3a3a3a 0 2%, #151515 3% 18%, #1b1b1b 19% 60%, #0d0d0d 61% 100%);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.05);
}
.music-vinyl::before {
  content: '';
  position: absolute;
  inset: 46px;
  border-radius: 50%;
  background: radial-gradient(circle, #a33b31 0 34%, #8a2b2b 36% 100%);
}
.music-vinyl::after {
  content: '';
  position: absolute;
  inset: 71px;
  border-radius: 50%;
  background: #f3ede4;
}
.music-vinyl.spin {
  animation: vinyl-spin 9s linear infinite;
}
@keyframes vinyl-spin {
  to {
    transform: rotate(360deg);
  }
}
.music-covers {
  position: absolute;
  top: 0;
  left: 26px;
  z-index: 1;
  width: 150px;
  height: 150px;
}
.music-cover {
  position: absolute;
  inset: 0;
  width: 150px;
  height: 150px;
  object-fit: cover;
  border-radius: 14px;
  box-shadow: 0 16px 32px rgba(30, 24, 18, 0.24);
  opacity: 0;
  transition: opacity 0.8s ease;
}
.music-cover.show {
  opacity: 1;
}
.music-dots {
  position: absolute;
  bottom: 2px;
  left: 50%;
  z-index: 2;
  display: flex;
  gap: 6px;
  transform: translateX(-50%);
}
.music-dots button {
  width: 6px;
  height: 6px;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: rgba(44, 42, 41, 0.2);
  transition:
    width 0.3s,
    background 0.3s;
}
.music-dots button:hover {
  border-color: transparent;
  background: rgba(44, 42, 41, 0.4);
  transform: none;
}
.music-dots button.on {
  width: 16px;
  background: #1f3b2c;
}
.music-info b {
  display: block;
  overflow: hidden;
  color: #2c2a29;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.music-info span {
  display: block;
  margin-top: 4px;
  color: #9a948b;
  font-size: 11px;
  letter-spacing: 1px;
}
.music-progress {
  margin: 14px 0 8px;
}
.music-progress input {
  width: 100%;
  height: 3px;
  accent-color: #1f3b2c;
  cursor: pointer;
}
.music-progress input:disabled {
  cursor: default;
  opacity: 0.45;
}
.music-times {
  display: flex;
  justify-content: space-between;
  margin-top: 2px;
  color: #a8a29a;
  font-size: 10px;
  letter-spacing: 0.5px;
}
.music-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}
.music-controls > button {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  padding: 0;
  border: 1px solid #ece7df;
  border-radius: 50%;
  background: #fff;
  color: #1f3b2c;
  line-height: 0;
  transition:
    border-color 0.3s,
    background 0.3s;
}
.music-controls > button:hover {
  border-color: #1f3b2c;
  background: #f7f5f1;
  transform: none;
}
.music-controls > button svg {
  width: 15px;
  height: 15px;
}
.music-controls > .music-play {
  width: 44px;
  height: 44px;
  border: 0;
  background: #1f3b2c;
  color: #fff;
}
.music-controls > .music-play:hover {
  border-color: transparent;
  background: #163026;
}
.music-play svg {
  width: 17px !important;
  height: 17px !important;
}
.music-volume {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
  color: #8d8a84;
}
.music-volume svg {
  width: 14px;
  height: 14px;
}
.music-volume input {
  width: 62px;
  height: 3px;
  accent-color: #1f3b2c;
  cursor: pointer;
}
.music-list {
  margin-top: 16px;
  padding-top: 10px;
  border-top: 1px solid #f2efe9;
}
.music-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 7px 8px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: #5f5c56;
  text-align: left;
}
.music-row:hover {
  border-color: transparent;
  background: rgba(31, 59, 44, 0.05);
  transform: none;
}
.music-row img {
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  object-fit: cover;
  border-radius: 7px;
}
.music-row-text {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.music-row-text b {
  overflow: hidden;
  color: #2c2a29;
  font-size: 12px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.music-row-text small {
  overflow: hidden;
  color: #a8a29a;
  font-size: 10px;
  letter-spacing: 0.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.music-row-state {
  flex: 0 0 auto;
  color: #1f3b2c;
  font-size: 10px;
  letter-spacing: 1px;
}
.music-row.on {
  background: rgba(31, 59, 44, 0.08);
}
.music-row.on .music-row-text b {
  font-weight: 600;
}

.music-pop-enter-active,
.music-pop-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s cubic-bezier(0.2, 0.8, 0.2, 1);
}
.music-pop-enter-from,
.music-pop-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.98);
}

@media (max-width: 680px) {
  .music-trigger {
    width: 34px;
    height: 34px;
    display: grid;
  }
  .music-panel {
    position: fixed;
    top: 84px;
    right: 16px;
    left: 16px;
    width: auto;
    max-height: calc(100vh - 110px);
  }
  .music-covers {
    left: 18px;
  }
  .music-vinyl {
    right: 18px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .music-bars i,
  .music-vinyl.spin {
    animation: none;
  }
  .music-pop-enter-active,
  .music-pop-leave-active {
    transition: none;
  }
}
</style>
