<script setup>
// 左侧大模块菜单 + 顶部 Tab 页切换模块内页面
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { List, Tickets, Collection, Notebook, Operation, Grid, Document } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// 本地资料库模块包含的页面，落在其中任一页时左侧模块菜单保持高亮
const LIBRARY_PATHS = ['/recipes', '/logs', '/dicts']
const activeModule = computed(() =>
  LIBRARY_PATHS.includes(route.path) ? '/recipes' : route.path
)

// 顶部 Tab 页签仅本地资料库模块内部使用（其他模块为单页）
const showTabs = computed(() => LIBRARY_PATHS.includes(route.path))

// 顶部 Tab 页签：name 即路由地址
const activeTab = computed(() => route.path)

function onTabChange(path) {
  router.push(path)
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="200px" class="aside">
      <div class="brand">玖维-电镀控制系统</div>
      <el-menu
        router
        :default-active="activeModule"
        class="menu"
        background-color="#001529"
        text-color="#a6adb4"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/recipes">
          <el-icon><Notebook /></el-icon>
          <span>本地资料库</span>
        </el-menu-item>
        <el-menu-item index="/production">
          <el-icon><Operation /></el-icon>
          <span>生产参数设置</span>
        </el-menu-item>
        <el-menu-item index="/slots">
          <el-icon><Grid /></el-icon>
          <span>槽位管理</span>
        </el-menu-item>
        <el-menu-item index="/prod-records">
          <el-icon><Document /></el-icon>
          <span>生产信息记录</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header v-if="showTabs" height="48px" class="header">
        <el-tabs :model-value="activeTab" class="tabs" @tab-change="onTabChange">
          <el-tab-pane label="配方管理" name="/recipes" />
          <el-tab-pane label="操作日志" name="/logs" />
          <el-tab-pane label="字典管理" name="/dicts" />
        </el-tabs>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  min-height: 100vh;
}
.aside {
  background: #001529;
  display: flex;
  flex-direction: column;
}
.brand {
  height: 48px;
  line-height: 48px;
  text-align: center;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
  background: #002766;
}
.menu {
  border-right: none;
  flex: 1;
}
.menu :deep(.el-menu-item.is-active) {
  background: #1890ff;
}
.header {
  display: flex;
  align-items: flex-end;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}
.tabs {
  --el-tabs-header-height: 47px;
}
.main {
  background: #f5f7fa;
}
</style>
