import { createRouter, createWebHashHistory } from 'vue-router'

// 使用 hash 路由：后端 jar 托管静态资源时无需服务端路由回退配置
const routes = [
  { path: '/', redirect: '/recipes' },
  { path: '/recipes', name: 'recipes', component: () => import('../views/RecipeList.vue') },
  { path: '/logs', name: 'logs', component: () => import('../views/OperationLog.vue') },
  { path: '/dicts', name: 'dicts', component: () => import('../views/DictManagement.vue') },
  { path: '/production', name: 'production', component: () => import('../views/ProductionSettings.vue') },
  { path: '/slots', name: 'slots', component: () => import('../views/SlotManage.vue') },
  { path: '/prod-records', name: 'prod-records', component: () => import('../views/ProdRecordList.vue') }
]

export default createRouter({
  history: createWebHashHistory(),
  routes
})
