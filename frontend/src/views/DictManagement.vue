<template>
  <div class="page">
    <el-tabs v-model="activeTab" @tab-change="loadType">
      <el-tab-pane label="类别字典（RC/AS/HP...）" name="CATEGORY" />
      <el-tab-pane label="规格字典（0402/0603...）" name="SPEC" />
    </el-tabs>

    <div class="toolbar">
      <span class="toolbar-title">字典列表</span>
      <div class="add-group">
        <el-input
          v-model="inputValue"
          :placeholder="activeTab === 'CATEGORY' ? '输入新类别，如 RC' : '输入新规格，如 0805'"
          maxlength="32"
          clearable
          style="width: 260px"
          @keyup.enter="handleAdd"
        />
        <el-button type="primary" :loading="submitting" @click="handleAdd">添加</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="items" border stripe empty-text="暂无字典项">
      <el-table-column type="index" label="#" width="60" />
      <el-table-column prop="value" :label="activeTab === 'CATEGORY' ? '类别值' : '规格值'" min-width="160" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="tip">
      提示：类别与规格只能从字典中选择；已被配方引用的字典项无法删除；新增配方页面会即时同步最新字典。
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDicts, addDict, deleteDict } from '../api/dict'
import { useDictStore } from '../stores/dict'

const store = useDictStore()
const activeTab = ref('CATEGORY')
const items = ref([])
const loading = ref(false)
const inputValue = ref('')
const submitting = ref(false)

async function loadType() {
  loading.value = true
  try {
    const res = await listDicts(activeTab.value)
    items.value = res.data
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

async function handleAdd() {
  const value = (inputValue.value || '').trim()
  if (!value) {
    ElMessage.warning('请输入字典值')
    return
  }
  try {
    submitting.value = true
    await addDict({ type: activeTab.value, value })
    ElMessage.success('添加成功')
    inputValue.value = ''
    await loadType()
    // 强制刷新配方表单/筛选下拉共用的字典缓存
    store.fetchDicts(true)
  } catch (e) {
    // 拦截器已提示（如字典值重复）
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除“${row.value}”？已被配方引用的字典项无法删除。`, '删除确认', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await deleteDict(row.id)
    ElMessage.success('删除成功')
  } catch (e) {
    // 拦截器已提示（40903 被引用不可删）
  }
  loadType()
  store.fetchDicts(true)
}

onMounted(loadType)
</script>

<style scoped>
.page {
  background: #fff;
  padding: 16px;
  border-radius: 4px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.toolbar-title {
  font-size: 15px;
  font-weight: 600;
}
.add-group {
  display: flex;
  gap: 12px;
}
.tip {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
}
</style>
