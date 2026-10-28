<template>
  <div class="page">
    <!-- 筛选区：配方编号支持从配方列表“日志”入口带入 -->
    <el-form inline class="filter-bar" @submit.prevent>
      <el-form-item label="配方编号">
        <el-input v-model="filters.recipeCode" placeholder="如 REC20260101-0001" clearable style="width: 190px" />
      </el-form-item>
      <el-form-item label="动作">
        <el-select v-model="filters.action" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="(m, k) in ACTION_MAP" :key="k" :label="m.label" :value="k" />
        </el-select>
      </el-form-item>
      <el-form-item label="操作时间">
        <el-date-picker
          v-model="filters.timeRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 360px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" border stripe empty-text="暂无操作日志">
      <el-table-column prop="createdAt" label="操作时间" min-width="160" />
      <el-table-column prop="recipeCode" label="配方编号" min-width="150" />
      <el-table-column label="动作" width="90">
        <template #default="{ row }">
          <el-tag :type="(ACTION_MAP[row.action] && ACTION_MAP[row.action].type) || 'info'">
            {{ (ACTION_MAP[row.action] && ACTION_MAP[row.action].label) || row.action }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作人" width="110">
        <template #default="{ row }">{{ row.operator || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90">
        <template #default="{ row }">
          <el-button link type="primary" @click="showDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pager"
      background
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      :page-sizes="[10, 20, 50, 100]"
      @size-change="search"
      @current-change="load"
    />

    <!-- 前后快照对比抽屉 -->
    <el-drawer v-model="drawerVisible" size="720px" :title="drawerTitle">
      <div v-if="current" class="drawer-meta">
        <span>配方编号：<b>{{ current.recipeCode }}</b></span>
        <span>动作：<b>{{ (ACTION_MAP[current.action] && ACTION_MAP[current.action].label) || current.action }}</b></span>
        <span>操作时间：{{ current.createdAt }}</span>
        <span>操作人：{{ current.operator || '—' }}</span>
      </div>
      <el-table :data="diffRows" border :row-class-name="rowClassName" size="small">
        <el-table-column prop="label" label="字段" width="170" />
        <el-table-column v-if="current && current.before" label="变更前" min-width="120">
          <template #default="{ row }">{{ row.beforeVal }}</template>
        </el-table-column>
        <el-table-column label="变更后" min-width="120">
          <template #default="{ row }">{{ row.afterVal }}</template>
        </el-table-column>
      </el-table>
      <div class="drawer-tip">红色底色行表示本次修改发生变化的字段；新增动作无“变更前”数据。</div>
    </el-drawer>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { listLogs } from '../api/log'

const route = useRoute()

const ACTION_MAP = {
  CREATE: { label: '新增', type: 'success' },
  UPDATE: { label: '修改', type: 'primary' },
  ENABLE: { label: '启用', type: 'warning' },
  DISABLE: { label: '停用', type: 'info' }
}

const STATUS_LABEL = { ENABLED: '启用', DISABLED: '停用' }

const filters = reactive({
  recipeCode: (route.query.recipeCode && String(route.query.recipeCode)) || '',
  action: null,
  timeRange: null
})
const query = reactive({ page: 1, size: 20 })
const total = ref(0)
const records = ref([])
const loading = ref(false)

const drawerVisible = ref(false)
const current = ref(null)

const drawerTitle = computed(() => (current.value ? `操作详情 - ${current.value.recipeCode}` : '操作详情'))

// 快照字段清单（含镍/锡段嵌套 9 字段），CREATE 时 before 为 null 自动隐藏前栏
function sectionFields(sectionKey, sectionLabel) {
  return [
    { label: `${sectionLabel}·一段电流(A)`, get: r => r[sectionKey] && r[sectionKey].seg1Current },
    { label: `${sectionLabel}·一段时间(Sec)`, get: r => r[sectionKey] && r[sectionKey].seg1Time },
    { label: `${sectionLabel}·二段电流(A)`, get: r => r[sectionKey] && r[sectionKey].seg2Current },
    { label: `${sectionLabel}·二段时间(Sec)`, get: r => r[sectionKey] && r[sectionKey].seg2Time },
    { label: `${sectionLabel}·三段电流(A)`, get: r => r[sectionKey] && r[sectionKey].seg3Current },
    { label: `${sectionLabel}·三段时间(Sec)`, get: r => r[sectionKey] && r[sectionKey].seg3Time },
    { label: `${sectionLabel}·总时间(Sec)`, get: r => r[sectionKey] && r[sectionKey].totalTime },
    { label: `${sectionLabel}·保护电流(A)`, get: r => r[sectionKey] && r[sectionKey].protectCurrent },
    { label: `${sectionLabel}·报警电压(V)`, get: r => r[sectionKey] && r[sectionKey].alarmVoltage }
  ]
}

const DIFF_FIELDS = [
  { label: '类别', get: r => r.category },
  { label: '规格', get: r => r.spec },
  { label: '阻值下限(Ω)', get: r => r.resMin },
  { label: '阻值上限(Ω)', get: r => r.resMax },
  { label: '数量下限(pcs)', get: r => r.qtyMin },
  { label: '数量上限(pcs)', get: r => r.qtyMax },
  { label: '状态', get: r => r.status, fmt: v => STATUS_LABEL[v] || v },
  ...sectionFields('nickel', '镍段'),
  ...sectionFields('tin', '锡段')
]

// 空值显示占位符
function sv(v) {
  return v === null || v === undefined || v === '' ? '—' : v
}

const diffRows = computed(() => {
  if (!current.value) return []
  const before = current.value.before
  const after = current.value.after
  return DIFF_FIELDS.map(f => {
    const rawBefore = before ? f.get(before) : null
    const rawAfter = after ? f.get(after) : null
    const showBefore = f.fmt ? f.fmt(rawBefore) : rawBefore
    const showAfter = f.fmt ? f.fmt(rawAfter) : rawAfter
    return {
      label: f.label,
      beforeVal: sv(showBefore),
      afterVal: sv(showAfter),
      changed: before ? String(sv(showBefore)) !== String(sv(showAfter)) : false
    }
  })
})

function rowClassName({ row }) {
  return row.changed ? 'diff-row' : ''
}

async function load() {
  loading.value = true
  try {
    const params = {
      page: query.page,
      size: query.size
    }
    if (filters.recipeCode) params.recipeCode = filters.recipeCode.trim()
    if (filters.action) params.action = filters.action
    if (filters.timeRange && filters.timeRange.length === 2) {
      params.startTime = filters.timeRange[0]
      params.endTime = filters.timeRange[1]
    }
    const res = await listLogs(params)
    records.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function resetFilters() {
  filters.recipeCode = ''
  filters.action = null
  filters.timeRange = null
  search()
}

function showDetail(row) {
  current.value = row
  drawerVisible.value = true
}

onMounted(load)
</script>

<style scoped>
.page {
  background: #fff;
  padding: 16px;
  border-radius: 4px;
}
.filter-bar {
  padding: 12px 12px 0;
  background: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 12px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

.drawer-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 24px;
  margin-bottom: 12px;
  font-size: 14px;
  color: #606266;
}

.drawer-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

:deep(.diff-row) {
  background-color: #fef0f0;
}
</style>
