import { ref } from 'vue';
import { api } from './api';
import { readUserData, userDataKey, writeUserData } from './userData';

/** 一次 AI 生成任务(提交后拿到 generationId) */
export interface GenerationJob {
  id: number;
  totalCount: number;
  keyword: string;
  submittedAt: number;
}

/** 轮询到的任务状态快照 */
export interface GenerationSnapshot {
  status: 'PROCESSING' | 'SUCCESS' | 'FAILED';
  /** 0-100 */
  progress: number;
  completedCount: number;
  totalCount: number;
  errorMessage: string;
  patterns: any[];
}

/** 最近一次结束的任务(供页面展示结果/失败提示) */
export interface GenerationFinished {
  job: GenerationJob;
  snapshot: GenerationSnapshot;
  ok: boolean;
}

const STORAGE_NAME = 'active_generation';
const LAST_FINISHED_NAME = 'last_generation';
const POLL_INTERVAL = 1000;

export const generationActive = ref<GenerationJob | null>(null);
export const generationSnapshot = ref<GenerationSnapshot | null>(null);
export const generationFinished = ref<GenerationFinished | null>(null);

let timer: number | null = null;
let polling = false;

function initialSnapshot(job: GenerationJob): GenerationSnapshot {
  return {
    status: 'PROCESSING',
    progress: 0,
    completedCount: 0,
    totalCount: job.totalCount,
    errorMessage: '',
    patterns: []
  };
}

function persistJob(job: GenerationJob | null) {
  if (job) {
    writeUserData(STORAGE_NAME, job);
  } else {
    localStorage.removeItem(userDataKey(STORAGE_NAME));
  }
}

/**
 * 提交生成任务成功后的统一入口: 记录任务、持久化(刷新/切页不丢)、启动轮询。
 */
export function submitGenerationJob(job: GenerationJob) {
  generationFinished.value = null;
  generationActive.value = job;
  generationSnapshot.value = initialSnapshot(job);
  persistJob(job);
  startPolling();
}

/**
 * 应用启动/进入生成页时调用: 恢复之前未完成的轮询(刷新页面、切走后回来都能接上)。
 */
export function restoreActiveGeneration() {
  if (generationActive.value || timer !== null) return;
  const job = readUserData<GenerationJob | null>(STORAGE_NAME, null);
  if (!job || !job.id) return;
  generationActive.value = job;
  generationSnapshot.value = initialSnapshot(job);
  startPolling();
}

function startPolling() {
  if (timer !== null) return;
  poll();
  timer = window.setInterval(poll, POLL_INTERVAL);
}

function stopPolling() {
  if (timer !== null) {
    window.clearInterval(timer);
    timer = null;
  }
}

async function poll() {
  const job = generationActive.value;
  if (!job || polling) return;
  polling = true;
  try {
    const data: any = await api.patterns.generationStatus(job.id);
    // 轮询期间任务已被新任务替换, 丢弃过期响应
    if (!generationActive.value || String(generationActive.value.id) !== String(job.id)) return;
    generationSnapshot.value = {
      status: data.status || 'PROCESSING',
      progress: Number(data.progress || 0),
      completedCount: Number(data.completedCount || 0),
      totalCount: Number(data.totalCount || job.totalCount),
      errorMessage: data.errorMessage || '',
      patterns: Array.isArray(data.patterns) ? data.patterns : []
    };
    if (data.status === 'SUCCESS' || data.status === 'FAILED') {
      finish(job, data.status);
    }
  } catch {
    // 网络抖动等瞬时错误: 静默等待下一轮轮询
  } finally {
    polling = false;
  }
}

function finish(job: GenerationJob, status: 'SUCCESS' | 'FAILED') {
  const snapshot = generationSnapshot.value || initialSnapshot(job);
  stopPolling();
  const finished: GenerationFinished = { job, snapshot, ok: status === 'SUCCESS' };
  generationFinished.value = finished;
  if (status === 'SUCCESS') {
    syncFinishedPatterns(snapshot);
    // 持久化最近一次成功结果: 用户在别的页面/刷新后再回生成页, 仍能恢复展示
    writeUserData(LAST_FINISHED_NAME, finished);
  }
  generationActive.value = null;
  generationSnapshot.value = null;
  persistJob(null);
}

/** 读取最近一次已结束并成功的任务结果(用于生成页恢复展示) */
export function readLastFinishedGeneration(): GenerationFinished | null {
  return readUserData<GenerationFinished | null>(LAST_FINISHED_NAME, null);
}

/** 成功后把结果写入本地数据, 触发"我的纹样"等页面自动刷新 */
function syncFinishedPatterns(snapshot: GenerationSnapshot) {
  const stamped = (snapshot.patterns || []).map((pattern: any) => ({
    ...pattern,
    image: pattern.imageUrl || pattern.thumbnailUrl || pattern.image || '',
    createdAt: pattern.createdAt || Date.now()
  }));
  const history = readUserData<any[]>('recent_generations', []);
  writeUserData('recent_generations', [...stamped, ...history].slice(0, 50));
}
