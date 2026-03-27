<template>
  <div class="page-shell">
    <div class="surface-card not-found-card">
      <div class="page-title-back">
        <el-button class="back-chevron-btn" plain aria-label="返回首页" @click="$router.push('/index')">&lt;</el-button>
        <h2>页面不存在</h2>
      </div>
      <p>你访问的页面可能已被移动。</p>
    </div>
  </div>
</template>

<style scoped>
.not-found-card {
  padding: 36px;
}

.not-found-card p {
  margin: 16px 0 0 44px;
  color: var(--text-sub);
}
</style>
