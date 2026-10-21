<template>
  <div class="upgrade-page">
    <div
      class="paper-noise"
      aria-hidden="true"
    />
    <div class="upgrade-wrap">
      <p class="eyebrow">
        <span
          class="rule"
          aria-hidden="true"
        />AI CUSTOM STUDIO
      </p>
      <h1>AI 定制正在升级</h1>
      <p class="lede">
        旧的画布贴图方案已暂时下线。下一代定制会先用 AI 识别纹样与面料材质，再生成真实的光影与褶皱效果，避免"贴上去"的失真感。
      </p>
      <div class="upgrade-actions">
        <RouterLink
          class="primary"
          to="/products"
          >去挑选商品</RouterLink
        >
        <RouterLink
          class="ghost"
          to="/generate"
          >生成 AI 纹样</RouterLink
        >
      </div>
      <p class="note">如需打样，可先在智造工坊把商品加入打样需求，提交后由源头工坊排产。</p>
    </div>
  </div>
</template>

<style scoped>
.upgrade-page {
  --paper: #fcfbfa;
  --ink-green: #1f3b2c;
  --accent: #c13e33;
  --ease: cubic-bezier(0.2, 0.8, 0.2, 1);
  position: relative;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 64px 24px 110px;
  background: var(--paper);
  color: #333;
  font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}
.paper-noise {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  opacity: 0.6;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)' opacity='0.035'/%3E%3C/svg%3E");
}
.upgrade-wrap {
  position: relative;
  z-index: 1;
  width: min(620px, 100%);
  text-align: center;
  animation: rise 0.8s var(--ease) both;
}
.eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  margin: 0 0 18px;
  color: #8d8a84;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 2.4px;
}
.rule {
  width: 24px;
  height: 2px;
  background: var(--accent);
}
h1 {
  margin: 0 0 18px;
  color: var(--ink-green);
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: clamp(32px, 4vw, 44px);
  font-weight: 900;
  letter-spacing: 3px;
}
.lede {
  margin: 0 auto 30px;
  max-width: 52ch;
  color: #7a7a7a;
  font-size: 14px;
  line-height: 1.95;
  letter-spacing: 1px;
}
.upgrade-actions {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 14px;
}
.upgrade-actions a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 148px;
  height: 46px;
  padding: 0 24px;
  border-radius: 999px;
  font-size: 14px;
  letter-spacing: 1px;
  transition:
    background 0.3s var(--ease),
    border-color 0.3s var(--ease),
    color 0.3s var(--ease),
    box-shadow 0.3s var(--ease);
}
.upgrade-actions .primary {
  background: var(--ink-green);
  color: #fff;
}
.upgrade-actions .primary:hover {
  background: #163026;
  box-shadow: 0 12px 26px rgba(31, 59, 44, 0.18);
}
.upgrade-actions .ghost {
  border: 1px solid #e3ddd4;
  background: #fff;
  color: var(--ink-green);
}
.upgrade-actions .ghost:hover {
  border-color: var(--ink-green);
  background: rgba(31, 59, 44, 0.04);
}
.note {
  margin: 28px 0 0;
  color: #a8a29a;
  font-size: 12px;
  letter-spacing: 0.5px;
}
@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}
@media (prefers-reduced-motion: reduce) {
  .upgrade-wrap {
    animation: none;
  }
}
</style>
