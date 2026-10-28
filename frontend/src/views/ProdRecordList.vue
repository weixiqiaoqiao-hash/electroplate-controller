<script setup>
// 生产信息记录：只读台账，查询 + 导出（数据由扫码上料「发送」自动写入）
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { listProdRecords } from '../api/prodRecord'

const loading = ref(false)
const exporting = ref(false)
const records = ref([])
const total = ref(0)

// 筛选条件（时间区间为 [start, end] 字符串数组，格式 yyyy-MM-dd HH:mm:ss）
const filters = reactive({
  feedTimeRange: [],
  batchNo: '',
  rackNo: '',
  page: 1,
  size: 20
})

// 列定义：流水号 作业员 吊架号 投料槽位 投料时间 进线时间 出线时间 左批次6列 右批次6列 状态
const columns = [
  { prop: 'serialNo', label: '流水号', width: 150, fixed: 'left' },
  { prop: 'operator', label: '作业员', width: 80 },
  { prop: 'rackNo', label: '吊架号', width: 90 },
  { prop: 'feedSlot', label: '投料槽位', width: 90 },
  { prop: 'feedTime', label: '投料时间', width: 160 },
  { prop: 'inTime', label: '进线时间', width: 160 },
  { prop: 'outTime', label: '出线时间', width: 160 },
  { prop: 'batchNoL', label: '批号(左)', width: 120 },
  { prop: 'itemNoL', label: '料号(左)', width: 110 },
  { prop: 'specL', label: '规格(左)', width: 80 },
  { prop: 'resistanceL', label: '阻值(左)', width: 80 },
  { prop: 'toleranceL', label: '精度(左)', width: 80 },
  { prop: 'qtyL', label: '数量(左)', width: 80 },
  { prop: 'batchNoR', label: '批号(右)', width: 120 },
  { prop: 'itemNoR', label: '料号(右)', width: 110 },
  { prop: 'specR', label: '规格(右)', width: 80 },
  { prop: 'resistanceR', label: '阻值(右)', width: 80 },
  { prop: 'toleranceR', label: '精度(右)', width: 80 },
  { prop: 'qtyR', label: '数量(右)', width: 80 },
  { label: '状态', width: 90, fixed: 'right', slot: 'status' }
]

function buildQueryParams() {
  const params = {
    batchNo: filters.batchNo || undefined,
    rackNo: filters.rackNo || undefined
  }
  if (filters.feedTimeRange && filters.feedTimeRange.length === 2) {
    params.feedTimeStart = filters.feedTimeRange[0]
    params.feedTimeEnd = filters.feedTimeRange[1]
  }
  return params
}

async function load() {
  loading.value = true
  try {
    const res = await listProdRecords({ ...buildQueryParams(), page: filters.page, size: filters.size })
    records.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

function search() {
  filters.page = 1
  load()
}

function reset() {
  filters.feedTimeRange = []
  filters.batchNo = ''
  filters.rackNo = ''
  search()
}

// 导出遵循当前查询条件（不分页），blob 下载
async function exportExcel() {
  exporting.value = true
  try {
    const query = new URLSearchParams()
    const params = buildQueryParams()
    Object.keys(params).forEach((k) => {
      if (params[k] !== undefined) query.append(k, params[k])
    })
    const resp = await fetch('/api/prod-records/export?' + query.toString())
    if (!resp.ok) {
      let msg = '导出失败（HTTP ' + resp.status + '）'
      try {
        const body = await resp.json()
        if (body && body.message) msg = body.message
      } catch (e) { /* 非 JSON 响应体，使用默认文案 */ }
      ElMessage.error(msg)
      return
    }
    const blob = await resp.blob()
    // 文件名从 Content-Disposition 解析，解析失败用默认名
    let fileName = '生产记录.xlsx'
    const disposition = resp.headers.get('Content-Disposition') || ''
    const match = disposition.match(/filename\*=UTF-8''([^;]+)/i)
    if (match) {
      try {
        fileName = decodeURIComponent(match[1])
      } catch (e) { /* 保持默认名 */ }
    }
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } finally {
    exporting.value = false
  }
}

function show(v) {
  return v === null || v === undefined || v === '' ? '—' : v
}

onMounted(load)
</script>

<template>
  <div class="page">
    <el-form inline class="filter-bar" @submit.prevent>
      <el-form-item label="投料时间">
        <el-date-picker
          v-model="filters.feedTimeRange"
          type="datetimerange"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          :default-time="[new Date(2000, 0, 1, 0, 0, 0), new Date(2000, 0, 1, 23, 59, 59)]"
          style="width: 360px"
        />
      </el-form-item>
      <el-form-item label="批号">
        <el-input v-model="filters.batchNo" placeholder="左/右批号任一命中" clearable style="width: 180px" @keyup.enter="search" />
      </el-form-item>
      <el-form-item label="吊架号">
        <el-input v-model="filters.rackNo" placeholder="精确查询" clearable style="width: 140px" @keyup.enter="search" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <span class="toolbar-title">生产记录</span>
      <el-button type="success" :icon="Download" :loading="exporting" @click="exportExcel">导出</el-button>
    </div>

    <el-table v-loading="loading" :data="records" border stripe>
      <el-table-column
        v-for="col in columns"
        :key="col.prop || col.label"
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :fixed="col.fixed"
        align="center"
      >
        <template v-if="col.slot === 'status'" #default="{ row }">
          <el-tag v-if="row.status === 'DONE'" type="success">已完成</el-tag>
          <el-tag v-else type="warning">正在加工</el-tag>
        </template>
        <template v-else #default="{ row }">
          {{ show(row[col.prop]) }}
        </template>
      </el-table-column>
      <template #empty>暂无生产记录</template>
    </el-table>

    <el-pagination
      v-model:current-page="filters.page"
      v-model:page-size="filters.size"
      class="pager"
      background
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      @size-change="search"
      @current-change="load"
    />
  </div>
</template>

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
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 12px 0;
}
.toolbar-title {
  font-size: 15px;
  font-weight: 600;
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
