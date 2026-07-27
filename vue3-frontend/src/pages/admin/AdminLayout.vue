<script setup lang="ts">
const modules = [
  ['/admin/dashboard', '数据总览'],
  ['/admin/orders', '订单管理'],
  ['/admin/products', '商品管理'],
  ['/admin/product-categories', '商品分类'],
  ['/admin/custom-designs', '定制设计'],
  ['/admin/patterns', '纹样管理'],
  ['/admin/prompt-templates', '提示词模板'],
  ['/admin/courses', '课程管理'],
  ['/admin/resources', '资源管理'],
  ['/admin/users', '用户管理'],
  ['/admin/home', '首页配置'],
  ['/admin/shop', '店铺配置']
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
      <header><b>绣纹智创管理中心</b><RouterLink to="/">返回用户端</RouterLink></header>
      <RouterView />
    </main>
  </div>
</template>
