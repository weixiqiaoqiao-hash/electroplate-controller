import { defineStore } from 'pinia'
import { listDicts } from '../api/dict'

// 类别/规格字典缓存：配方表单与筛选下拉共用，字典变更后需强制刷新
export const useDictStore = defineStore('dict', {
  state: () => ({
    categories: [],
    specs: [],
    loaded: false
  }),
  actions: {
    async fetchDicts(force = false) {
      if (this.loaded && !force) return
      const [cat, spec] = await Promise.all([listDicts('CATEGORY'), listDicts('SPEC')])
      this.categories = (cat && cat.data) || []
      this.specs = (spec && spec.data) || []
      this.loaded = true
    }
  }
})
