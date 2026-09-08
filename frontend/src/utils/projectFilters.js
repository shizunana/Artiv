import { PROJECT_CATEGORY_OPTIONS } from '../constants/projectCategories.js'
import { projectSubjects } from '../constants/projectSubjects.js'

export function copyProjectFilter(value = {}) {
  const categories = PROJECT_CATEGORY_OPTIONS.filter(category => (value.categories || []).includes(category))
  const source = Array.isArray(value.subjects) ? value.subjects : value.subject ? [value.subject] : []
  const subjects = categories.length === 1
    ? projectSubjects(categories[0]).filter(subject => source.includes(subject))
    : []
  return { categories, subjects }
}

export function toggleProjectCategory(value, category) {
  const next = copyProjectFilter(value)
  next.categories = next.categories.includes(category) ? next.categories.filter(item => item !== category) : [...next.categories, category]
  next.subjects = []
  return copyProjectFilter(next)
}

export function projectFilterParams(value) {
  const filter = copyProjectFilter(value)
  return { ...(filter.categories.length ? { projectCategories: filter.categories } : {}), ...(filter.subjects.length ? { subjects: filter.subjects } : {}) }
}
