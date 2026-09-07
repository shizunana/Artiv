import { PROJECT_CATEGORY_OPTIONS } from '../constants/projectCategories.js'
import { projectSubjects } from '../constants/projectSubjects.js'

export function copyProjectFilter(value = {}) {
  const categories = PROJECT_CATEGORY_OPTIONS.filter(category => (value.categories || []).includes(category))
  return { categories, subject: categories.length === 1 && projectSubjects(categories[0]).includes(value.subject) ? value.subject : '' }
}

export function toggleProjectCategory(value, category) {
  const next = copyProjectFilter(value)
  next.categories = next.categories.includes(category) ? next.categories.filter(item => item !== category) : [...next.categories, category]
  next.subject = ''
  return copyProjectFilter(next)
}

export function projectFilterParams(value) {
  const filter = copyProjectFilter(value)
  return { ...(filter.categories.length ? { projectCategories: filter.categories } : {}), ...(filter.subject ? { subject: filter.subject } : {}) }
}
