<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <main class="grid min-h-screen place-items-center bg-[rgb(var(--color-bg))] px-6">
    <div class="text-center">
      <p class="text-sm font-semibold text-[rgb(var(--color-brand))]">404</p>
      <h1 class="mt-3 text-3xl font-semibold text-slate-950 dark:text-white">页面不存在</h1>
      <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">请求的地址没有匹配到 EduVoyage 页面。</p>
      <RouterLink class="btn-primary focus-ring mt-6 inline-flex h-10 items-center px-4 text-sm" to="/">返回首页</RouterLink>
    </div>
  </main>
</template>
