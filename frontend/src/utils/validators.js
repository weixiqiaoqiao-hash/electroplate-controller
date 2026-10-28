// 必填规则工厂
export function requiredRule(message) {
  return { required: true, message, trigger: ['blur', 'change'] }
}

// 区间检查：下限不得大于上限；返回错误文案，合法返回空串
// 前端仅做辅助校验，服务端仍会全量校验
export function checkRange(min, max, label) {
  if (min === null || min === undefined || max === null || max === undefined) return ''
  if (Number(min) > Number(max)) return `${label}下限不能大于上限`
  return ''
}
