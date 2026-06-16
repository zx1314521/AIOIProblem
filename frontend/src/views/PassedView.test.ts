import { render, screen } from '@testing-library/vue'
import PassedView from './PassedView.vue'
import { api } from '../services/api'
import type { Problem } from '../types'

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to', 'target', 'rel'],
    template: '<a :href="to" :target="target" :rel="rel"><slot /></a>'
  }
}))

vi.mock('../services/api', () => ({
  api: {
    searchProblems: vi.fn()
  }
}))

const problems: Problem[] = [
  {
    id: 8,
    title: '区间 DP',
    description: 'statement',
    difficulty: 'CSPS提高',
    difficultyCode: 'CSPS_ADVANCED',
    tags: ['动态规划'],
    createdAt: '2026-06-05T00:00:00',
    passed: true
  },
  {
    id: 9,
    title: '未通过题',
    description: 'statement',
    difficulty: '简单',
    difficultyCode: 'EASY',
    tags: ['模拟'],
    createdAt: '2026-06-05T00:00:00',
    passed: false
  }
]

test('links passed problem titles to detail pages', async () => {
  vi.mocked(api.searchProblems).mockResolvedValue(problems)

  render(PassedView)

  const link = await screen.findByRole('link', { name: '区间 DP' })
  expect(link.getAttribute('href')).toBe('/problems/8')
  expect(link.getAttribute('target')).toBe('_blank')
  expect(link.getAttribute('rel')).toBe('noopener noreferrer')
  expect(screen.queryByText('未通过题')).toBeNull()
})
