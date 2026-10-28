<template>
  <el-dialog
    :model-value="modelValue"
    :title="recipe ? '编辑配方' : '新增配方'"
    width="940px"
    :close-on-click-modal="false"
    :before-close="handleBeforeClose"
    @update:model-value="v => emit('update:modelValue', v)"
  >
    <!-- 编辑模式只读展示配方编号与创建时间 -->
    <div v-if="recipe" class="edit-header">
      <span>配方编号：<b>{{ recipe.recipeCode }}</b></span>
      <span>创建时间：{{ recipe.createdAt }}</span>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-row :gutter="12">
        <el-col :span="8">
          <el-form-item label="类别" prop="category">
            <el-select v-model="form.category" placeholder="请选择类别" style="width: 100%">
              <el-option v-for="d in store.categories" :key="d.id" :label="d.value" :value="d.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="规格" prop="spec">
            <el-select v-model="form.spec" placeholder="请选择规格" style="width: 100%">
              <el-option v-for="d in store.specs" :key="d.id" :label="d.value" :value="d.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="阻值下限(Ω)" prop="resMin">
            <el-input-number
              v-model="form.resMin"
              :controls="false"
              :min="0"
              :precision="2"
              placeholder="必填"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="阻值上限(Ω)" prop="resMax">
            <el-input-number
              v-model="form.resMax"
              :controls="false"
              :min="0"
              :precision="2"
              placeholder="必填"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="数量下限(pcs)" prop="qtyMin">
            <el-input-number
              v-model="form.qtyMin"
              :controls="false"
              :min="0"
              :precision="0"
              placeholder="必填"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="数量上限(pcs)" prop="qtyMax">
            <el-input-number
              v-model="form.qtyMax"
              :controls="false"
              :min="0"
              :precision="0"
              placeholder="必填"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <el-row :gutter="16">
      <el-col :span="12">
        <ProcessSection v-model="form.nickel" title="镍段参数" />
      </el-col>
      <el-col :span="12">
        <ProcessSection v-model="form.tin" title="锡段参数" />
      </el-col>
    </el-row>

    <div class="tip">
      提示：同类别+规格下，启用配方的阻值与数量区间不可与已有配方同时重叠；保存失败时弹窗保持打开，可调整后重试。
    </div>

    <template #footer>
      <el-button @click="handleBeforeClose(() => emit('update:modelValue', false))">取 消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保 存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createRecipe, updateRecipe } from '../api/recipe'
import { useDictStore } from '../stores/dict'
import { requiredRule, checkRange } from '../utils/validators'
import ProcessSection from './ProcessSection.vue'

const props = defineProps({
  // 弹窗可见性，v-model
  modelValue: { type: Boolean, default: false },
  // 编辑目标配方（RecipeVO）；null 表示新增
  recipe: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue', 'saved'])

const store = useDictStore()
const formRef = ref(null)
const saving = ref(false)
// 脏检查基线：打开时的表单快照，关闭前比对
let snapshot = ''

const rules = {
  category: [requiredRule('请选择类别')],
  spec: [requiredRule('请选择规格')],
  resMin: [requiredRule('请输入阻值下限')],
  resMax: [requiredRule('请输入阻值上限')],
  qtyMin: [requiredRule('请输入数量下限')],
  qtyMax: [requiredRule('请输入数量上限')]
}

// 空白工艺段：二/三段可空，表示该段不使用
function emptySection() {
  return {
    seg1Current: null,
    seg1Time: null,
    seg2Current: null,
    seg2Time: null,
    seg3Current: null,
    seg3Time: null,
    protectCurrent: null,
    alarmVoltage: null
  }
}

const form = reactive({
  category: null,
  spec: null,
  resMin: null,
  resMax: null,
  qtyMin: null,
  qtyMax: null,
  nickel: emptySection(),
  tin: emptySection()
})

function fillFromSection(src) {
  const s = emptySection()
  if (src) {
    s.seg1Current = src.seg1Current ?? null
    s.seg1Time = src.seg1Time ?? null
    s.seg2Current = src.seg2Current ?? null
    s.seg2Time = src.seg2Time ?? null
    s.seg3Current = src.seg3Current ?? null
    s.seg3Time = src.seg3Time ?? null
    s.protectCurrent = src.protectCurrent ?? null
    s.alarmVoltage = src.alarmVoltage ?? null
  }
  return s
}

function resetForm(target) {
  form.category = target ? target.category : null
  form.spec = target ? target.spec : null
  form.resMin = target ? target.resMin : null
  form.resMax = target ? target.resMax : null
  form.qtyMin = target ? target.qtyMin : null
  form.qtyMax = target ? target.qtyMax : null
  form.nickel = fillFromSection(target ? target.nickel : null)
  form.tin = fillFromSection(target ? target.tin : null)
}

function collect() {
  return JSON.stringify({
    category: form.category,
    spec: form.spec,
    resMin: form.resMin,
    resMax: form.resMax,
    qtyMin: form.qtyMin,
    qtyMax: form.qtyMax,
    nickel: { ...form.nickel },
    tin: { ...form.tin }
  })
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      store.fetchDicts()
      resetForm(props.recipe)
      snapshot = collect()
      formRef && formRef.value && formRef.value.clearValidate()
    }
  }
)

// 镍/锡段必填项手动检查（一段电流/一段时间/保护电流/报警电压）
function checkSection(section, label) {
  const requiredFields = [
    ['seg1Current', '一段电流'],
    ['seg1Time', '一段时间'],
    ['protectCurrent', '保护电流'],
    ['alarmVoltage', '报警电压']
  ]
  for (const [key, name] of requiredFields) {
    if (section[key] === null || section[key] === undefined) {
      ElMessage.warning(`请填写${label}的${name}`)
      return false
    }
  }
  return true
}

async function handleSave() {
  if (!store.categories.length) {
    ElMessage.warning('类别字典为空，请先在【字典管理】中维护类别')
    return
  }
  if (!store.specs.length) {
    ElMessage.warning('规格字典为空，请先在【字典管理】中维护规格')
    return
  }
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  if (!checkSection(form.nickel, '镍段')) return
  if (!checkSection(form.tin, '锡段')) return

  const resRangeMsg = checkRange(form.resMin, form.resMax, '阻值')
  if (resRangeMsg) {
    ElMessage.warning(resRangeMsg)
    return
  }
  const qtyRangeMsg = checkRange(form.qtyMin, form.qtyMax, '数量')
  if (qtyRangeMsg) {
    ElMessage.warning(qtyRangeMsg)
    return
  }

  const payload = {
    category: form.category,
    spec: form.spec,
    resMin: form.resMin,
    resMax: form.resMax,
    qtyMin: form.qtyMin,
    qtyMax: form.qtyMax,
    nickel: { ...form.nickel },
    tin: { ...form.tin }
  }

  try {
    saving.value = true
    if (props.recipe) {
      // 乐观锁：携带打开时的版本号，并发修改返回 40902
      payload.version = props.recipe.version
      await updateRecipe(props.recipe.id, payload)
    } else {
      await createRecipe(payload)
    }
    ElMessage.success('保存成功')
    snapshot = collect() // 保存成功后无需再弹脏检查
    emit('saved')
    emit('update:modelValue', false)
  } catch (e) {
    // 拦截器已提示错误（如 40901 区间重叠 / 40902 并发修改），保持弹窗打开便于调整重试
  } finally {
    saving.value = false
  }
}

// 关闭前脏检查：有未保存修改时二次确认
function handleBeforeClose(done) {
  if (collect() === snapshot) {
    done()
    return
  }
  ElMessageBox.confirm('表单存在未保存的修改，确定放弃并关闭吗？', '关闭确认', {
    type: 'warning',
    confirmButtonText: '放弃修改',
    cancelButtonText: '继续编辑'
  })
    .then(() => done())
    .catch(() => {})
}
</script>

<style scoped>
.edit-header {
  display: flex;
  gap: 24px;
  padding: 8px 12px;
  margin-bottom: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 14px;
  color: #606266;
}

.tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
</style>
