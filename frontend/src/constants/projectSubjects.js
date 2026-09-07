// 企划专用题材；不得替换橱窗、作品与画师共用的 SUBJECT_OPTIONS。
export const PROJECT_SUBJECT_OPTIONS_BY_CATEGORY = Object.freeze({
  '头像': Object.freeze(["Q版","日系","古风","厚涂","男性","女性","双人","有背景","黑白","像素","小动物","安头"]),
  '立绘': Object.freeze(["正比","Q版","日系","古风","厚涂","男性","女性","有背景","黑白","定制"]),
  '角色设计': Object.freeze(["正比","Q版","日系","古风","男性","女性","小动物"]),
  '插画': Object.freeze(["日系","古风","厚涂","场景","氛围","男性","女性","双人","有背景","横插","竖插","黑白","像素","小动物"]),
  '平面设计': Object.freeze(["日系","古风","黑白","像素","Q版"]),
  '自设/OC': Object.freeze(["正比","Q版","日系","古风","男性","女性","双人","有背景","小动物","定制","安头"]),
})
export const PROJECT_SUBJECT_OPTIONS = Object.freeze([...new Set(Object.values(PROJECT_SUBJECT_OPTIONS_BY_CATEGORY).flat())])
export const projectSubjects = category => PROJECT_SUBJECT_OPTIONS_BY_CATEGORY[category] || []
export const retainProjectSubjects = (category, selected) => selected.filter(value => projectSubjects(category).includes(value))
