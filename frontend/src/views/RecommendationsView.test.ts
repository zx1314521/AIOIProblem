import { render, screen } from '@testing-library/vue'
import RecommendationsView from './RecommendationsView.vue'
import { api } from '../services/api'
import type { RecommendationResponse } from '../types'

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to', 'target', 'rel'],
    template: '<a :href="to" :target="target" :rel="rel"><slot /></a>'
  }
}))

vi.mock('../services/api', () => ({
  api: {
    recommendations: vi.fn(),
    markPassed: vi.fn()
  }
}))

const recommendation: RecommendationResponse = {
  weakTags: ['图论'],
  items: [
    {
      practiceOrder: 1,
      reason: '补齐最短路基础。',
      problem: {
        id: 12,
        title: '最短路入门',
        description: 'statement',
        difficulty: 'CSPJ中等',
        difficultyCode: 'CSPJ_MEDIUM',
        tags: ['最短路'],
        createdAt: '2026-06-05T00:00:00',
        passed: false
      }
    }
  ]
}

test('links recommended problem titles to detail pages', async () => {
  vi.mocked(api.recommendations).mockResolvedValue(recommendation)

  render(RecommendationsView)

  const link = await screen.findByRole('link', { name: '最短路入门' })
  expect(link.getAttribute('href')).toBe('/problems/12')
  expect(link.getAttribute('target')).toBe('_blank')
  expect(link.getAttribute('rel')).toBe('noopener noreferrer')
})
