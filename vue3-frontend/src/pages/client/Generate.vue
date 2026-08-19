<script setup lang="ts">
import { ref } from 'vue';
import SectionTitle from '../../components/SectionTitle.vue';
import PatternCard from '../../components/PatternCard.vue';
import { patterns as demoPatterns } from '../../data';
import { api, listFrom } from '../../api';
import { readUserData, writeUserData } from '../../userData';
const style = ref('广绣经典'),
  element = ref('牡丹'),
  count = ref(4),
  description = ref(''),
  loading = ref(false),
  patterns = ref(demoPatterns),
  notice = ref('');
async function run() {
  loading.value = true;
  notice.value = '';
  try {
    const result: any = await api.patterns.generate({
      keyword: element.value,
      style: style.value,
      elements: [element.value],
      colorTheme: 'chinese_elegant',
      usageScene: 'product',
      description: description.value,
      generateCount: Number(count.value)
    });
    const list = listFrom(result?.patterns ?? result);
    if (list.length)
      patterns.value = list.map((p: any, i: number) => ({
        ...demoPatterns[i % demoPatterns.length],
        ...p,
        image: p.imageUrl || p.thumbnailUrl || demoPatterns[i % demoPatterns.length].image
      }));
  } catch {
    patterns.value = demoPatterns.slice(0, Number(count.value)).map((p, i) => ({
      ...p,
      id: Date.now() + i,
      title: `${element.value} · ${style.value} ${i + 1}`
    }));
  } finally {
    const history = readUserData<any[]>('recent_generations', []);
    writeUserData('recent_generations', [...patterns.value.map((p) => ({ ...p, createdAt: Date.now() })), ...history].slice(0, 50));
    notice.value = `已生成 ${patterns.value.length} 张纹样`;
    loading.value = false;
  }
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
        <button class="on"><i class="p1" />国风雅韵</button><button><i class="p2" />富贵华彩</button
        ><button><i class="p3" />清润素韵</button>
      </div>
      <label>04 · 应用场景</label
      ><select>
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
      <div class="upload">⇧<b>点击或拖拽上传参考图</b><small>JPG / PNG，不超过 5MB</small></div>
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
        <span>由 AI 生成，仅供设计参考</span>
      </div>
      <div class="result-grid">
        <img
          class="featured"
          :src="patterns[0].image"
        /><img
          v-for="p in patterns.slice(1, 4)"
          :src="p.image"
        />
      </div>
      <div class="result-actions">
        <button>↻ 重新生成</button><button>✦ 细节增强</button
        ><button class="jade" @click="savePatterns">⇩ 保存纹样</button><button @click="favoritePatterns">♡ 收藏纹样</button>
      </div>
      <p v-if="notice" class="form-notice">{{ notice }}</p>
    </section>
  </div>
  <div class="content inspiration">
    <SectionTitle
      eyebrow="DAILY INSPIRATION"
      title="灵感推荐"
      action="查看更多"
    />
    <div class="pattern-grid">
      <PatternCard
        v-for="p in patterns.slice(2, 7)"
        :p="p"
      />
    </div>
  </div>
</template>
