<script setup>
// 新增/编辑槽位弹窗（槽位管理页使用）
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createSlot, updateSlot } from '../api/slot'

const props = defineProps({
  modelValue: Boolean,
  slot: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'saved'])

const visible = computed(() => props.modelValue)
const formRef = ref(null)
const saving = ref(false)

const emptyForm = () => ({
  slotName: '',
  slotType: 0,
  timeSeconds: 0,
  showTime: true,
  sortNo: 1,
  switchOn: true
})
const form = reactive(emptyForm())

const rules = {
  slotName: [
    { required: true, message: '请输入槽位名称', trigger: 'blur' },
    { max: 32, message: '长度不能超过 32 字符', trigger: 'blur' }
  ],
  timeSeconds: [{ required: true, message: '请输入时间设定值', trigger: 'blur' }],
  sortNo: [{ required: true, message: '请输入工序序号', trigger: 'blur' }]
}

// 打开时：编辑回填，新增重置
watch(visible, val => {
  if (!val) return
  if (props.slot) {
    Object.assign(form, {
      slotName: props.slot.slotName,
      slotType: props.slot.slotType,
      timeSeconds: props.slot.timeSeconds,
      showTime: props.slot.showTime,
      sortNo: props.slot.sortNo,
      switchOn: props.slot.switchOn !== false
    })
  } else {
    Object.assign(form, emptyForm())
  }
  formRef.value && formRef.value.clearValidate()
})

function close() {
  emit('update:modelValue', false)
}

async function handleSave() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  if (form.timeSeconds == null || form.timeSeconds < 0) {
    ElMessage.warning('时间设定值必须大于等于 0')
    return
  }
  if (form.sortNo == null || form.sortNo < 1) {
    ElMessage.warning('工序序号必须大于等于 1')
    return
  }
  const payload = {
    slotName: form.slotName.trim(),
    slotType: form.slotType,
    timeSeconds: form.timeSeconds,
    showTime: form.showTime,
    sortNo: form.sortNo,
    // 仅镀槽的开关有意义；普通槽由服务端强制关
    switchOn: form.slotType === 1 ? form.switchOn : false
  }
  saving.value = true
  try {
    if (props.slot) {
      await updateSlot(props.slot.id, { ...payload, version: props.slot.version })
      ElMessage.success('修改成功')
    } else {
      await createSlot(payload)
      ElMessage.success('新增成功')
    }
    close()
    emit('saved')
  } catch (e) {
    // 拦截器已弹错误提示，保持弹窗打开供修改
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="slot ? '编辑槽位' : '新增槽位'"
    width="460px"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
      <el-form-item label="槽位名称" prop="slotName">
        <el-input v-model="form.slotName" maxlength="32" placeholder="如：酸洗 / 镀镍3 / 交换" />
      </el-form-item>
      <el-form-item label="槽位类型">
        <el-radio-group v-model="form.slotType">
          <el-radio :label="0">普通槽</el-radio>
          <el-radio :label="1">镀槽</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="时间设定值(Sec)" prop="timeSeconds">
        <el-input-number v-model="form.timeSeconds" :min="0" :step="1" step-strictly :controls="false" style="width: 100%" />
      </el-form-item>
      <el-form-item label="是否展示时间">
        <el-switch v-model="form.showTime" />
        <span class="tip">关闭后生产参数设置页不显示该槽位的时间输入框</span>
      </el-form-item>
      <el-form-item label="工序序号" prop="sortNo">
        <el-input-number v-model="form.sortNo" :min="1" :step="1" step-strictly :controls="false" style="width: 100%" />
        <span class="tip">全局唯一，不可与其他槽位重复</span>
      </el-form-item>
      <el-form-item v-if="form.slotType === 1" label="开关状态">
        <el-switch v-model="form.switchOn" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}
</style>
