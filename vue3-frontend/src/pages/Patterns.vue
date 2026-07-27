<script setup lang="ts">
import { onMounted, ref } from 'vue';
import PatternCard from '../components/PatternCard.vue';
import { api, listFrom } from '../api';
import { patterns } from '../data';

const patternItems = ref<any[]>(patterns);
onMounted(async () => {
  try {
    const result = listFrom(await api.patterns.mine({ page: 1, pageSize: 20 }));
    if (result.length) patternItems.value = result;
  } catch {}
});
</script>
<template>
  <div class="page-head compact">
    <span>MY CREATIVE ASSETS</span>
    <h1>我的纹样</h1>
    <p>管理生成、收藏与已应用的纹样资产。</p>
    <div class="metrics">
      <div><b>236</b><span>纹样总数</span></div>
      <div><b>48</b><span>收藏数量</span></div>
      <div><b>12</b><span>最近生成</span></div>
      <div><b>27</b><span>已应用商品</span></div>
    </div>
  </div>
  <div class="content">
    <div class="tabbar">
      <b>全部纹样</b><span>我的收藏</span><span>最近生成</span><span>已应用</span
      ><button class="primary">＋ 新建生成</button>
    </div>
    <div class="toolbar">
      <input placeholder="⌕ 搜索纹样名称、关键词" /><select>
        <option>全部风格</option>
        <option>广绣经典</option>
        <option>新中式</option></select
      ><select>
        <option>全部元素</option>
        <option>牡丹</option>
        <option>凤凰</option>
      </select>
    </div>
    <div class="pattern-grid library">
      <PatternCard
        v-for="p in patternItems"
        :key="p.id"
        :p="p"
      />
    </div>
  </div>
</template>
