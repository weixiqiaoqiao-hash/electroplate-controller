<script setup>
// 生产参数设置页：按工序顺序纵向展示启用槽位，时间设定模式批量改时间，镀槽开关即时切换
import { computed, onMounted, ref } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listSlots, updateSlotTimes, switchSlot } from '../api/slot'

const loading = ref(false)
const slots = ref([])
// 时间设定模式：false 浏览态（输入框只读），true 编辑态
const editMode = ref(false)
// 进入编辑态时的基线快照 id -> timeSeconds
const baseline = ref(new Map())
const saving = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await listSlots('ENABLED')
    slots.value = res.data
  } finally {
    loading.value = false
  }
}

// 是否有未保存的时间修改
const hasChanges = computed(() =>
  slots.value.some(s => s.showTime && s.timeSeconds !== baseline.value.get(s.id))
)

function enterEdit() {
  baseline.value = new Map(slots.value.map(s => [s.id, s.timeSeconds]))
  editMode.value = true
}

async function saveTimes() {
  const items = slots.value
    .filter(s => s.showTime && s.timeSeconds !== baseline.value.get(s.id))
    .map(s => ({ id: s.id, timeSeconds: s.timeSeconds, version: s.version }))
  if (items.length === 0) {
    ElMessage.info('没有修改任何时间')
    editMode.value = false
    return
  }
  saving.value = true
  try {
    await updateSlotTimes(items)
    ElMessage.success('时间保存成功')
    editMode.value = false
    await load()
  } catch (e) {
    // 拦截器已提示（40902 冲突等），保持编辑态
  } finally {
    saving.value = false
  }
}

function cancelEdit() {
  // 恢复基线值
  slots.value.forEach(s => {
    if (s.showTime && baseline.value.has(s.id)) {
      s.timeSeconds = baseline.value.get(s.id)
    }
  })
  editMode.value = false
}

// 镀槽开关：切换即保存，失败回滚 UI
async function onSwitchChange(slot) {
  const target = slot.switchOn
  try {
    await switchSlot(slot.id, target)
    ElMessage.success(`「${slot.slotName}」已${target ? '打开' : '关闭'}`)
  } catch (e) {
    slot.switchOn = !target
  }
}

// 编辑态下离开页面提醒
onBeforeRouteLeave(() => {
  if (editMode.value && hasChanges.value) {
    return window.confirm('有未保存的时间修改，确定离开？')
  }
})

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="page">
    <div class="toolbar">
      <div class="title">生产参数设置</div>
      <div>
        <template v-if="!editMode">
          <el-button type="primary" @click="enterEdit">时间设定</el-button>
        </template>
        <template v-else>
          <el-button @click="cancelEdit">取消</el-button>
          <el-button type="primary" :loading="saving" @click="saveTimes">
            保存{{ hasChanges ? '（有未保存修改）' : '' }}
          </el-button>
        </template>
      </div>
    </div>
    <el-alert
      v-if="editMode"
      type="warning"
      :closable="false"
      show-icon
      title="时间设定模式：修改各槽位时间后，点击右上角保存统一生效；取消则放弃全部修改"
      style="margin-bottom: 12px"
    />

    <el-empty v-if="!loading && slots.length === 0" description="暂无启用槽位，请先在槽位管理中维护" />

    <el-table v-else :data="slots" border stripe :class="{ editing: editMode }">
      <el-table-column prop="sortNo" label="序号" width="70" align="center" />
      <el-table-column prop="slotName" label="槽位名称" min-width="160">
        <template #default="{ row }">
          <span class="slot-name">{{ row.slotName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.slotType === 1 ? 'primary' : 'info'">
            {{ row.slotType === 1 ? '镀槽' : '普通槽' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="时间(Sec)" width="240" align="center">
        <template #default="{ row }">
          <el-input-number
            v-if="row.showTime"
            v-model="row.timeSeconds"
            :min="0"
            :step="1"
            step-strictly
            :controls="false"
            :disabled="!editMode"
            class="time-input"
            :class="{ changed: editMode && row.timeSeconds !== baseline.get(row.id) }"
          />
          <span v-else class="no-time">—</span>
        </template>
      </el-table-column>
      <el-table-column label="开关" width="120" align="center">
        <template #default="{ row }">
          <el-switch
            v-if="row.slotType === 1"
            v-model="row.switchOn"
            @change="onSwitchChange(row)"
          />
          <span v-else>—</span>
        </template>
      </el-table-column>
    </el-table>
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
.title {
  font-size: 15px;
  font-weight: 600;
}
.slot-name {
  font-weight: 600;
}
.time-input {
  width: 180px;
}
.time-input.changed :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #e6a23c inset;
}
/* 编辑态表格整体高亮提示 */
.el-table.editing {
  outline: 1px solid #409eff;
  outline-offset: -1px;
}
.no-time {
  color: #c0c4cc;
}
</style>
