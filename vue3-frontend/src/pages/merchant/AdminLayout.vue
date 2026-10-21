<script setup lang="ts">
const modules = [
  ['/merchant/factory-orders', '工坊接单'],
  ['/merchant/dashboard', '数据总览'],
  ['/merchant/orders', '订单管理'],
  ['/merchant/products', '商品管理'],
  ['/merchant/product-categories', '商品分类'],
  ['/merchant/custom-designs', '定制设计'],
  ['/merchant/patterns', '纹样管理'],
  ['/merchant/pattern-generations', '生成记录'],
  ['/merchant/prompt-templates', '提示词模板'],
  ['/merchant/course-categories', '课程分类'],
  ['/merchant/courses', '课程管理'],
  // 【已隐藏】资源管理入口：需要恢复时取消下一行注释
  // ['/merchant/resources', '资源管理'],
  ['/merchant/users', '用户管理'],
  ['/merchant/home', '首页配置'],
  ['/merchant/shop', '店铺配置'],
  ['/merchant/messages', '消息管理']
];
</script>
<template>
  <div class="admin-shell">
    <aside>
      <RouterLink
        to="/"
        class="admin-brand"
        >绣纹智创<small>乡村共富工坊管理台</small></RouterLink
      ><RouterLink
        v-for="[path, label] in modules"
        :key="path"
        :to="path"
        >{{ label }}</RouterLink
      >
    </aside>
    <main>
      <header>
        <b>绣纹智创 · 乡村工坊端</b
        ><RouterLink to="/merchant/factory-orders">🧵 工坊接单大厅</RouterLink
        ><RouterLink
          to="/"
          title="回到客户端首页"
          >返回客户端</RouterLink
        >
      </header>
      <RouterView />
    </main>
  </div>
</template>
