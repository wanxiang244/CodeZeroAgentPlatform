<template>
  <a-layout class="basic-layout">
    <!-- 顶部导航栏 -->
    <GlobalHeader />
    <!-- 主要内容区域 -->
    <a-layout-content class="main-content">
      <router-view />
    </a-layout-content>
    <!-- 底部版权信息 -->
    <GlobalFooter />
  </a-layout>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
</script>

<style scoped>
.basic-layout {
  min-height: 100vh;
  background: var(--color-bg-primary);
}

.main-content {
  max-width: var(--content-max-width);
  width: 100%;
  padding: var(--spacing-xxl) var(--spacing-lg);
  background: var(--color-bg-primary);
  margin: 0 auto;
  min-height: calc(100vh - var(--header-height) - var(--footer-height));
}
</style>
