<template>
  <div class="process-section">
    <div class="section-title">{{ title }}</div>
    <div class="field">
      <span class="label required">一段电流(A)</span>
      <el-input-number
        class="input"
        :controls="false"
        :min="0"
        :precision="2"
        :model-value="modelValue.seg1Current"
        placeholder="必填"
        @update:model-value="set('seg1Current', $event)"
      />
    </div>
    <div class="field">
      <span class="label required">一段时间(Sec)</span>
      <el-input-number
        class="input"
        :controls="false"
        :min="0"
        :precision="0"
        :model-value="modelValue.seg1Time"
        placeholder="必填"
        @update:model-value="set('seg1Time', $event)"
      />
    </div>
    <div class="field">
      <span class="label">二段电流(A)</span>
      <el-input-number
        class="input"
        :controls="false"
        :min="0"
        :precision="2"
        :model-value="modelValue.seg2Current"
        placeholder="不使用可留空"
        @update:model-value="set('seg2Current', $event)"
      />
    </div>
    <div class="field">
      <span class="label">二段时间(Sec)</span>
      <el-input-number
        class="input"
        :controls="false"
        :min="0"
        :precision="0"
        :model-value="modelValue.seg2Time"
        placeholder="不使用可留空"
        @update:model-value="set('seg2Time', $event)"
      />
    </div>
    <div class="field">
      <span class="label">三段电流(A)</span>
      <el-input-number
        class="input"
        :controls="false"
        :min="0"
        :precision="2"
        :model-value="modelValue.seg3Current"
        placeholder="不使用可留空"
        @update:model-value="set('seg3Current', $event)"
      />
    </div>
    <div class="field">
      <span class="label">三段时间(Sec)</span>
      <el-input-number
        class="input"
        :controls="false"
        :min="0"
        :precision="0"
        :model-value="modelValue.seg3Time"
        placeholder="不使用可留空"
        @update:model-value="set('seg3Time', $event)"
      />
    </div>
    <div class="field">
      <span class="label">总时间(Sec)</span>
      <el-input class="input readonly-total" :model-value="totalTime" disabled />
    </div>
    <div class="field">
      <span class="label required">保护电流(A)</span>
      <el-input-number
        class="input"
        :controls="false"
        :min="0"
        :precision="2"
        :model-value="modelValue.protectCurrent"
        placeholder="必填"
        @update:model-value="set('protectCurrent', $event)"
      />
    </div>
    <div class="field">
      <span class="label required">报警电压(V)</span>
      <el-input-number
        class="input"
        :controls="false"
        :min="0"
        :precision="2"
        :model-value="modelValue.alarmVoltage"
        placeholder="必填"
        @update:model-value="set('alarmVoltage', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

// 镍段/锡段工艺参数组：镍/锡段复用同一组件，v-model 绑定参数对象
const props = defineProps({
  title: { type: String, required: true },
  modelValue: { type: Object, required: true }
})

const emit = defineEmits(['update:modelValue'])

function set(key, val) {
  emit('update:modelValue', { ...props.modelValue, [key]: val })
}

// 总时间锁定为三段时间之和，只读展示，服务端保存时以同样规则计算
const totalTime = computed(() => {
  const m = props.modelValue || {}
  return (m.seg1Time || 0) + (m.seg2Time || 0) + (m.seg3Time || 0)
})
</script>

<style scoped>
.process-section {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px 16px;
  margin-bottom: 16px;
  background: #fafafa;
}

.section-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
  margin-bottom: 12px;
}

.field {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.field:last-child {
  margin-bottom: 0;
}

.label {
  width: 110px;
  flex-shrink: 0;
  text-align: right;
  padding-right: 10px;
  font-size: 14px;
  color: #606266;
}

.label.required::before {
  content: '*';
  color: #f56c6c;
  margin-right: 2px;
}

.input {
  flex: 1;
  width: 100%;
}

.readonly-total :deep(.el-input__inner) {
  font-weight: 600;
  color: #909399;
}
</style>
