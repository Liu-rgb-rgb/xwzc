<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { api } from '../../api';

type StatusTab = 'ALL' | 'PENDING' | 'IN_PRODUCTION' | 'COMPLETED';

const STITCH_LABELS: Record<string, string> = {
  OUTLINE: '轮廓走针',
  SATIN: '平包针',
  TATAMI: '他他米针',
  AUTO: 'AI智能混合'
};
const FABRIC_LABELS: Record<string, string> = {
  canvas: '帆布袋',
  cotton: '棉布',
  silk: '丝绸'
};
const STATUS_META: Record<string, { label: string; className: string }> = {
  PENDING: { label: '待接单', className: 'st-pending' },
  IN_PRODUCTION: { label: '生产中', className: 'st-in' },
  COMPLETED: { label: '已发货', className: 'st-done' }
};
const tabs: Array<{ key: StatusTab; label: string }> = [
  { key: 'ALL', label: '全部工单' },
  { key: 'PENDING', label: '待接单' },
  { key: 'IN_PRODUCTION', label: '生产中' },
  { key: 'COMPLETED', label: '已发货' }
];

const stats = ref({ total: 0, pending: 0, inProduction: 0, completed: 0 });
const orders = ref<any[]>([]);
const loading = ref(false);
const error = ref('');
const notice = ref('');
const activeTab = ref<StatusTab>('ALL');
const updatingIds = ref<Set<string>>(new Set());

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const [statsResult, ordersResult] = await Promise.all([
      api.factory.stats(),
      api.factory.orders(activeTab.value === 'ALL' ? {} : { status: activeTab.value })
    ]);
    const statsData: any = statsResult;
    const ordersData: any = ordersResult;
    stats.value = {
      total: Number(statsData?.total || 0),
      pending: Number(statsData?.pending || 0),
      inProduction: Number(statsData?.inProduction || 0),
      completed: Number(statsData?.completed || 0)
    };
    orders.value = Array.isArray(ordersData) ? ordersData : [];
  } catch (reason: any) {
    error.value =
      reason?.response?.data?.message || reason?.message || '工单加载失败，请确认后端已启动';
  } finally {
    loading.value = false;
  }
}

function switchTab(tab: StatusTab) {
  if (activeTab.value === tab) return;
  activeTab.value = tab;
  load();
}

function flashNotice(text: string) {
  notice.value = text;
  window.setTimeout(() => {
    if (notice.value === text) notice.value = '';
  }, 3000);
}

/** 接单：待接单 → 生产中（后端写入承接工坊名）；发货：生产中 → 已发货 */
async function advance(order: any) {
  const key = String(order.id);
  if (updatingIds.value.has(key)) return;
  const next = order.status === 'PENDING' ? 'IN_PRODUCTION' : 'COMPLETED';
  updatingIds.value.add(key);
  try {
    await api.factory.updateStatus(order.id, next);
    flashNotice(
      next === 'IN_PRODUCTION'
        ? '绣娘李阿姨已接单，正在为您刺绣（用户端已同步"生产中"）'
        : '已发货，用户端已同步"已发货"状态'
    );
    await load();
  } catch (reason: any) {
    error.value = reason?.response?.data?.message || reason?.message || '操作失败，请重试';
  } finally {
    updatingIds.value.delete(key);
  }
}

function stitchLabel(order: any) {
  return STITCH_LABELS[String(order.stitchType || '').toUpperCase()] || '轮廓走针';
}
function fabricLabel(order: any) {
  return FABRIC_LABELS[String(order.fabricType || '').toLowerCase()] || '';
}
function shortHash(h: string) {
  if (!h) return '';
  return h.length > 24 ? `${h.slice(0, 10)}…${h.slice(-6)}` : h;
}
function quoteOf(order: any) {
  return Number(order.quoteAmount || 0).toFixed(2);
}
function timeOf(order: any) {
  return String(order.createdAt || '')
    .replace('T', ' ')
    .slice(0, 16);
}
function onImageError(event: Event) {
  (event.target as HTMLImageElement).style.opacity = '0';
}

onMounted(load);
</script>
<template>
  <div class="workshop-page">
    <div class="workshop-head">
      <div>
        <span class="eyebrow">RURAL WORKSHOP HALL</span>
        <h2>乡村工坊接单大厅</h2>
      </div>
      <span class="workshop-sub">订单直达乡村 · 利润不留中间商 · 助力乡村振兴</span>
    </div>

    <p
      v-if="notice"
      class="workshop-notice"
    >
      ✔ {{ notice }}
    </p>

    <div class="workshop-stats">
      <article class="stat-total">
        <small>工单总数</small>
        <b>{{ stats.total }}</b>
        <i>累计接收打样</i>
      </article>
      <article class="stat-pending">
        <small>待接单</small>
        <b>{{ stats.pending }}</b>
        <i>等待绣娘接单</i>
      </article>
      <article class="stat-in">
        <small>生产中</small>
        <b>{{ stats.inProduction }}</b>
        <i>绣花机运行中</i>
      </article>
      <article class="stat-done">
        <small>已发货</small>
        <b>{{ stats.completed }}</b>
        <i>已交付用户</i>
      </article>
    </div>

    <div class="workshop-tabs">
      <button
        v-for="t in tabs"
        :key="t.key"
        type="button"
        :class="{ on: activeTab === t.key }"
        @click="switchTab(t.key)"
      >
        {{ t.label }}
      </button>
    </div>

    <p
      v-if="error"
      class="workshop-error"
    >
      {{ error }}
    </p>
    <p
      v-else-if="loading"
      class="workshop-state"
    >
      正在读取生产工单…
    </p>

    <div
      v-else-if="orders.length"
      class="workshop-list"
    >
      <article
        v-for="order in orders"
        :key="order.id"
        class="order-card"
      >
        <img
          :src="order.patternImageUrl"
          :alt="order.patternTitle || '纹样预览'"
          @error="onImageError"
        />
        <div class="order-body">
          <div class="order-head">
            <b class="order-no">{{ order.orderNo }}</b>
            <span
              class="status-pill"
              :class="STATUS_META[order.status]?.className"
            >
              {{ STATUS_META[order.status]?.label || order.status }}
            </span>
            <span
              v-if="order.factoryName && order.status !== 'PENDING'"
              class="factory-tag"
            >
              由 {{ order.factoryName }} 承接
            </span>
          </div>
          <h3>{{ order.patternTitle || 'AI 非遗纹样' }}</h3>
          <div class="order-meta">
            <span class="chip">✦ {{ stitchLabel(order) }}</span>
            <span
              v-if="fabricLabel(order)"
              class="chip"
            >
              🧵 {{ fabricLabel(order) }}</span
            >
            <span class="chip">预计针数 {{ order.stitchCount || 0 }} 针</span>
            <span class="chip">DST {{ order.fileSize || '—' }}</span>
            <span
              v-if="order.designHash"
              class="chip"
              :title="order.designHash"
            >
              🔏 指纹 {{ shortHash(order.designHash) }}</span
            >
            <span class="chip quote">预估工费 ¥ {{ quoteOf(order) }}</span>
          </div>
          <small class="order-sub"
            >{{ order.userNickname || '创作者' }} 提交 · {{ timeOf(order) }}</small
          >
        </div>
        <div class="order-actions">
          <a
            class="dst-link"
            :href="order.dstFileUrl"
            download
          >
            ⬇ 下载 DST 文件
          </a>
          <a
            v-if="order.stitchImageUrl"
            class="stitch-link"
            :href="order.stitchImageUrl"
            target="_blank"
            rel="noopener"
          >
            查看针脚图
          </a>
          <button
            v-if="order.status === 'PENDING'"
            class="advance"
            type="button"
            :disabled="updatingIds.has(String(order.id))"
            @click="advance(order)"
          >
            {{ updatingIds.has(String(order.id)) ? '处理中…' : '🧵 立即接单' }}
          </button>
          <button
            v-else-if="order.status === 'IN_PRODUCTION'"
            class="advance"
            type="button"
            :disabled="updatingIds.has(String(order.id))"
            @click="advance(order)"
          >
            {{ updatingIds.has(String(order.id)) ? '处理中…' : '📦 发货' }}
          </button>
          <span
            v-else
            class="done-tag"
          >
            已发货
          </span>
        </div>
      </article>
    </div>

    <div
      v-else
      class="workshop-empty"
    >
      <b>暂无{{ activeTab === 'ALL' ? '' : '该类' }}生产工单</b>
      <p>用户端完成 AI 打版并点击"提交给工厂"后，工单会实时出现在这里等待接单。</p>
    </div>
  </div>
</template>

<style scoped>
.workshop-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}
.workshop-head .eyebrow {
  color: #b7791f;
  letter-spacing: 2px;
  font-size: 12px;
}
.workshop-head h2 {
  margin: 4px 0 0;
  font-size: 26px;
  color: var(--ink);
}
.workshop-sub {
  color: var(--muted);
  font-size: 13px;
}
.workshop-notice {
  margin-top: 14px;
  padding: 11px 15px;
  border: 1px solid #bfe3cd;
  border-radius: 10px;
  background: #e2f5e9;
  color: #166b39;
  font-size: 14px;
  font-weight: 600;
}
.workshop-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-top: 18px;
}
.workshop-stats article {
  position: relative;
  overflow: hidden;
  padding: 16px 18px 14px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid var(--line);
  border-radius: 14px;
  box-shadow: var(--shadow);
}
.workshop-stats article::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 5px;
}
.stat-total::before {
  background: var(--ink);
}
.stat-pending::before {
  background: #d99a2b;
}
.stat-in::before {
  background: #2b6cb0;
}
.stat-done::before {
  background: #1a9c46;
}
.workshop-stats small {
  display: block;
  font-size: 12px;
  color: var(--muted);
  letter-spacing: 1px;
}
.workshop-stats b {
  display: block;
  margin: 5px 0 3px;
  font-size: 28px;
  line-height: 1;
  color: var(--ink);
}
.stat-pending b {
  color: #b7791f;
}
.stat-in b {
  color: #2b6cb0;
}
.stat-done b {
  color: #1a9c46;
}
.workshop-stats i {
  font-style: normal;
  font-size: 12px;
  color: var(--muted);
}
.workshop-tabs {
  display: flex;
  gap: 8px;
  margin: 18px 0 14px;
}
.workshop-tabs button {
  padding: 8px 18px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.8);
  color: var(--muted);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.workshop-tabs button.on {
  background: #07564f;
  border-color: #07564f;
  color: #fff;
  font-weight: 600;
}
.workshop-error {
  padding: 12px 15px;
  border: 1px solid #e8b4a8;
  border-radius: 10px;
  background: #fdf1ee;
  color: #a63a22;
  font-size: 14px;
}
.workshop-state {
  color: var(--muted);
  font-size: 14px;
}
.workshop-list {
  display: flex;
  flex-direction: column;
  gap: 13px;
}
.order-card {
  display: grid;
  grid-template-columns: 100px 1fr auto;
  gap: 16px;
  align-items: center;
  padding: 15px 17px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid var(--line);
  border-radius: 14px;
  box-shadow: var(--shadow);
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;
}
.order-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 40px rgba(87, 55, 27, 0.12);
}
.order-card img {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid var(--line);
  background: var(--paper);
}
.order-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.order-no {
  font-size: 13px;
  color: var(--muted);
  letter-spacing: 0.5px;
  font-family: 'Consolas', monospace;
}
.status-pill {
  padding: 2px 11px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}
.st-pending {
  background: #fbf0d9;
  color: #8a5b12;
  border: 1px solid #ecd9a8;
}
.st-in {
  background: #e3eefb;
  color: #1f4f83;
  border: 1px solid #bcd6f2;
}
.st-done {
  background: #e2f5e9;
  color: #166b39;
  border: 1px solid #bfe3cd;
}
.factory-tag {
  font-size: 12px;
  color: #0b625c;
  background: #e6f2ef;
  border: 1px solid #c4ded7;
  padding: 2px 10px;
  border-radius: 999px;
}
.order-body h3 {
  margin: 5px 0 7px;
  font-size: 16px;
  color: var(--ink);
}
.order-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.chip {
  padding: 3px 10px;
  border-radius: 8px;
  background: var(--paper);
  border: 1px solid var(--line);
  font-size: 12px;
  color: #5c5148;
}
.chip.quote {
  background: #fdf3e3;
  border-color: #ecd3a8;
  color: #8a5b12;
  font-weight: 700;
}
.order-sub {
  display: block;
  margin-top: 7px;
  font-size: 12px;
  color: var(--muted);
}
.order-actions {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
  min-width: 158px;
}
.dst-link,
.stitch-link,
.advance {
  display: block;
  padding: 8px 13px;
  border-radius: 9px;
  text-align: center;
  font-size: 13px;
  text-decoration: none;
  cursor: pointer;
}
.dst-link {
  background: #07564f;
  border: 0;
  color: #fff;
  font-weight: 600;
}
.dst-link:hover {
  background: #0b625c;
}
.stitch-link {
  background: transparent;
  border: 1px solid var(--line);
  color: var(--muted);
}
.stitch-link:hover {
  border-color: #07564f;
  color: #07564f;
}
.advance {
  background: linear-gradient(135deg, var(--red2), #a62216);
  border: 0;
  color: #fff;
  font-weight: 600;
}
.advance:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.done-tag {
  padding: 8px 13px;
  border-radius: 9px;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: #166b39;
  background: #e2f5e9;
  border: 1px solid #bfe3cd;
}
.workshop-empty {
  padding: 60px 20px;
  text-align: center;
  background: rgba(255, 255, 255, 0.7);
  border: 1px dashed var(--line);
  border-radius: 16px;
}
.workshop-empty b {
  display: block;
  font-size: 17px;
  color: var(--ink);
}
.workshop-empty p {
  margin-top: 8px;
  color: var(--muted);
  font-size: 14px;
}
@media (max-width: 900px) {
  .workshop-stats {
    grid-template-columns: repeat(2, 1fr);
  }
  .order-card {
    grid-template-columns: 80px 1fr;
  }
  .order-card img {
    width: 80px;
    height: 80px;
  }
  .order-actions {
    grid-column: 1 / -1;
    flex-direction: row;
    flex-wrap: wrap;
    min-width: 0;
  }
}
</style>
