<script setup lang="ts">
const modules = [
  ['/merchant/dashboard', '数据总览'],
  ['/merchant/orders', '订单管理'],
  ['/merchant/products', '商品管理'],
  ['/merchant/product-categories', '商品分类'],
  ['/merchant/custom-designs', '定制设计'],
  ['/merchant/patterns', '纹样管理'],
  ['/merchant/prompt-templates', '提示词模板'],
  ['/merchant/courses', '课程管理'],
  ['/merchant/resources', '资源管理'],
  ['/merchant/users', '用户管理'],
  ['/merchant/home', '首页配置'],
  ['/merchant/shop', '店铺配置']
];
</script>
<template>
  <div class="admin-shell">
    <aside>
      <RouterLink
        to="/"
        class="admin-brand"
        >绣纹智创<small>商家管理后台</small></RouterLink
      ><RouterLink
        v-for="[path, label] in modules"
        :key="path"
        :to="path"
        >{{ label }}</RouterLink
      >
    </aside>
    <main>
      <header><b>绣纹智创商家中心</b><RouterLink to="/">返回客户端</RouterLink></header>
      <RouterView />
    </main>
  </div>
</template>
