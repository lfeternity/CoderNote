<template>
  <div class="page-shell">
    <div class="surface-card not-found-card">
      <div class="page-title-back">
        <el-button class="back-chevron-btn" plain aria-label="返回首页" @click="$router.push('/index')">&lt;</el-button>
        <h2>页面不存在</h2>
      </div>
      <p>你访问的页面可能已被移动或删除。</p>
      <el-button type="primary" @click="$router.push('/index')">回到首页</el-button>
    </div>
  </div>
</template>

<style scoped>
.not-found-card {
  padding: clamp(24px, 4vw, 40px);
  display: grid;
  gap: 14px;
}

.not-found-card p {
  margin: 0 0 0 44px;
  color: var(--text-sub);
}
</style>
