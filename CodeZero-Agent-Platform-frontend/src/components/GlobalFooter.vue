<template>
  <a-layout-footer class="footer">
    <div class="footer-content">
      <p class="copyright">
        <a
          href="https://www.codefather.cn"
          target="_blank"
          rel="noopener noreferrer"
          class="author-link"
        >
          编程导航原创项目 by 程序员鱼皮
        </a>
      </p>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts">
// 无需额外的响应式数据
</script>

<style scoped>
.footer {
  background: var(--color-bg-primary) !important;
  text-align: center;
  padding: var(--spacing-lg) !important;
  border-top: 1px solid var(--color-border-light);
}

.copyright {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.author-link {
  color: var(--color-text-secondary);
  text-decoration: none;
}

.author-link:hover {
  color: var(--color-text-primary);
  text-decoration: underline;
}
</style>
