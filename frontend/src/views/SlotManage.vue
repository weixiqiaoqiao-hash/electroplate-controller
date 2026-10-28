<script setup>
// 槽位管理页：槽位基础信息的增删改查（删除为物理删除）
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listSlots, deleteSlot } from '../api/slot'
import SlotFormDialog from '../components/SlotFormDialog.vue'

const loading = ref(false)
const slots = ref([])

const dialogVisible = ref(false)
const editingSlot = ref(null)

async function load() {
  loading.value = true
  try {
    const res = await listSlots()
    slots.value = res.data
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingSlot.value = null
  dialogVisible.value = true
}

function openEdit(row) {
  editingSlot.value = row
  dialogVisible.value = true
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `删除「${row.slotName}」后不可恢复，其名称与工序序号将被释放。确认删除？`,
      '删除槽位',
      { type: 'warning', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' }
    )
  } catch (e) {
    return
  }
  try {
    await deleteSlot(row.id)
    ElMessage.success('已删除')
  } finally {
    load()
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <span class="toolbar-title">槽位列表</span>
      <el-button type="primary" @click="openCreate">新增槽位</el-button>
    </div>

    <el-table v-loading="loading" :data="slots" border stripe>
      <el-table-column prop="sortNo" label="工序序号" width="90" align="center" />
      <el-table-column prop="slotName" label="槽位名称" min-width="140" />
      <el-table-column label="类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.slotType === 1 ? 'primary' : 'info'">
            {{ row.slotType === 1 ? '镀槽' : '普通槽' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="timeSeconds" label="时间设定值(Sec)" width="130" align="center" />
      <el-table-column label="是否展示时间" width="110" align="center">
        <template #default="{ row }">
          {{ row.showTime ? '是' : '否' }}
        </template>
      </el-table-column>
      <el-table-column label="开关状态" width="90" align="center">
        <template #default="{ row }">
          <template v-if="row.slotType === 1">
            <el-tag v-if="row.switchOn" type="success" effect="light">开</el-tag>
            <el-tag v-else type="danger" effect="light">关</el-tag>
          </template>
          <template v-else>—</template>
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="修改时间" width="170" />
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>暂无槽位，点击右上角新增</template>
    </el-table>

    <SlotFormDialog v-model="dialogVisible" :slot="editingSlot" @saved="load" />
  </div>
</template>

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
</style>
