<template>
  <div class="page">
    <!-- 筛选区 -->
    <el-form inline class="filter-bar" @submit.prevent>
      <el-form-item label="类别">
        <el-select v-model="filters.category" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="d in store.categories" :key="d.id" :label="d.value" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="规格">
        <el-select v-model="filters.spec" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="d in store.specs" :key="d.id" :label="d.value" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="阻值(Ω)">
        <el-input-number
          v-model="filters.resistance"
          :controls="false"
          :min="0"
          :precision="2"
          placeholder="按区间命中过滤"
          style="width: 150px"
        />
      </el-form-item>
      <el-form-item label="数量(pcs)">
        <el-input-number
          v-model="filters.qty"
          :controls="false"
          :min="0"
          :precision="0"
          placeholder="按区间命中过滤"
          style="width: 150px"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" style="width: 120px">
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
          <el-option label="全部" value="ALL" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <span class="toolbar-title">配方列表</span>
      <el-button type="primary" @click="openCreate">新增配方</el-button>
    </div>

    <el-table v-loading="loading" :data="records" border stripe empty-text="暂无配方，点击右上角新增录入">
      <el-table-column prop="recipeCode" label="配方编号" min-width="150" />
      <el-table-column prop="category" label="类别" width="90" />
      <el-table-column prop="spec" label="规格" width="90" />
      <el-table-column label="阻值区间(Ω)" min-width="140">
        <template #default="{ row }">{{ row.resMin }} ~ {{ row.resMax }}</template>
      </el-table-column>
      <el-table-column label="数量区间(pcs)" min-width="130">
        <template #default="{ row }">{{ row.qtyMin }} ~ {{ row.qtyMax }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">
            {{ row.status === 'ENABLED' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="镍段总时间(Sec)" width="130">
        <template #default="{ row }">{{ row.nickel ? row.nickel.totalTime : '—' }}</template>
      </el-table-column>
      <el-table-column label="锡段总时间(Sec)" width="130">
        <template #default="{ row }">{{ row.tin ? row.tin.totalTime : '—' }}</template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="修改时间" min-width="160" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link :type="row.status === 'ENABLED' ? 'danger' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 'ENABLED' ? '停用' : '启用' }}
          </el-button>
          <el-button link type="primary" @click="gotoLogs(row)">日志</el-button>
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

    <RecipeFormDialog v-model="dialogVisible" :recipe="editing" @saved="load" />
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listRecipes, changeStatus } from '../api/recipe'
import { useDictStore } from '../stores/dict'
import RecipeFormDialog from '../components/RecipeFormDialog.vue'

const router = useRouter()
const store = useDictStore()

const filters = reactive({
  category: null,
  spec: null,
  resistance: null,
  qty: null,
  status: 'ENABLED'
})
const query = reactive({ page: 1, size: 20 })
const total = ref(0)
const records = ref([])
const loading = ref(false)

const dialogVisible = ref(false)
const editing = ref(null)

async function load() {
  loading.value = true
  try {
    const res = await listRecipes({ ...filters, ...query })
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
  filters.category = null
  filters.spec = null
  filters.resistance = null
  filters.qty = null
  filters.status = 'ENABLED'
  search()
}

function openCreate() {
  editing.value = null
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = row
  dialogVisible.value = true
}

async function toggleStatus(row) {
  const enabling = row.status === 'DISABLED'
  const tip = enabling
    ? '确认启用该配方？启用前将重新校验区间重叠，启用后参与扫码匹配。'
    : '停用后该配方不再参与扫码匹配，可随时重新启用。确认停用？'
  try {
    await ElMessageBox.confirm(tip, enabling ? '启用配方' : '停用配方', { type: 'warning' })
  } catch {
    return
  }
  try {
    await changeStatus(row.id, enabling ? 'ENABLED' : 'DISABLED')
    ElMessage.success(enabling ? '已启用' : '已停用')
  } catch (e) {
    // 拦截器已提示（如启用时 40901 区间重叠）
  }
  load()
}

function gotoLogs(row) {
  router.push({ path: '/logs', query: { recipeCode: row.recipeCode } })
}

onMounted(async () => {
  await store.fetchDicts()
  load()
})
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
}

.toolbar {
  margin: 12px 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
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
