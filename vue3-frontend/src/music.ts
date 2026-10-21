import { reactive } from 'vue';

export type MusicTrack = {
  id: string;
  title: string;
  artist: string;
  src: string;
  cover: string;
};

export const musicTracks: MusicTrack[] = [
  {
    id: 'exo',
    title: "Baby, Don't Cry (인어의 눈물)",
    artist: 'EXO',
    src: '/music/exo-baby-dont-cry.mp3',
    cover: '/music/cover-exo.jpg'
  },
  {
    id: 'radwimps',
    title: '三葉のテーマ',
    artist: 'RADWIMPS',
    src: '/music/radwimps-mitsuha-theme.mp3',
    cover: '/music/cover-radwimps.jpg'
  },
  {
    id: 'chen',
    title: 'nop',
    artist: '陈越龙',
    src: '/music/chen-nop.mp3',
    cover: '/music/cover-chen.jpg'
  }
];

export const musicState = reactive({
  index: 0,
  playing: false,
  currentTime: 0,
  duration: 0,
  volume: 0.7,
  ready: false
});

const audio = new Audio();
audio.preload = 'metadata';
audio.volume = musicState.volume;
audio.src = musicTracks[0].src;

let pausedByVideo = false;

audio.addEventListener('timeupdate', () => {
  musicState.currentTime = audio.currentTime;
});
audio.addEventListener('loadedmetadata', () => {
  musicState.duration = Number.isFinite(audio.duration) ? audio.duration : 0;
  musicState.ready = true;
});
audio.addEventListener('durationchange', () => {
  musicState.duration = Number.isFinite(audio.duration) ? audio.duration : 0;
});
audio.addEventListener('play', () => {
  musicState.playing = true;
});
audio.addEventListener('pause', () => {
  musicState.playing = false;
});
audio.addEventListener('ended', () => {
  nextTrack(true);
});

function applyTrack(index: number) {
  musicState.index = index;
  musicState.currentTime = 0;
  musicState.duration = 0;
  musicState.ready = false;
  audio.src = musicTracks[index].src;
}

export function playTrack(index = musicState.index) {
  if (index !== musicState.index) applyTrack(index);
  pausedByVideo = false;
  audio.play().catch(() => {});
}

export function toggleMusic() {
  if (audio.paused) {
    playTrack();
    return;
  }
  pausedByVideo = false;
  audio.pause();
}

export function nextTrack(keepPlaying = !audio.paused) {
  applyTrack((musicState.index + 1) % musicTracks.length);
  if (keepPlaying) audio.play().catch(() => {});
}

export function prevTrack() {
  const keepPlaying = !audio.paused;
  applyTrack((musicState.index - 1 + musicTracks.length) % musicTracks.length);
  if (keepPlaying) audio.play().catch(() => {});
}

export function seekMusic(time: number) {
  if (!Number.isFinite(time)) return;
  const max = Number.isFinite(audio.duration) ? audio.duration : time;
  audio.currentTime = Math.max(0, Math.min(time, max));
  musicState.currentTime = audio.currentTime;
}

export function setVolume(value: number) {
  const volume = Math.max(0, Math.min(1, Number(value) || 0));
  musicState.volume = volume;
  audio.volume = volume;
}

export function pauseMusic() {
  pausedByVideo = false;
  audio.pause();
}

function isAudibleVideo(target: EventTarget | null) {
  return (
    target instanceof HTMLVideoElement &&
    !target.muted &&
    target.volume > 0 &&
    !target.ended
  );
}

function anyAudibleVideoPlaying() {
  return [...document.querySelectorAll('video')].some(
    (video) => !video.paused && !video.ended && isAudibleVideo(video)
  );
}

function onMediaPlay(event: Event) {
  if (!isAudibleVideo(event.target)) return;
  if (!audio.paused) {
    pausedByVideo = true;
    audio.pause();
  }
}

function onMediaStop(event: Event) {
  if (!isAudibleVideo(event.target) || !pausedByVideo) return;
  window.setTimeout(() => {
    if (!pausedByVideo || anyAudibleVideoPlaying() || !audio.paused) return;
    pausedByVideo = false;
    audio.play().catch(() => {});
  }, 150);
}

document.addEventListener('play', onMediaPlay, true);
document.addEventListener('pause', onMediaStop, true);
document.addEventListener('ended', onMediaStop, true);
