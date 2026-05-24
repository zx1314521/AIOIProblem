import { render, screen, waitFor } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import ProblemsView from './ProblemsView.vue'
import { api } from '../services/api'
import type { Problem } from '../types'

vi.mock('../services/api', () => ({
  api: {
    searchProblems: vi.fn(),
    createProblem: vi.fn(),
    markPassed: vi.fn()
  }
}))

const sampleProblems: Problem[] = [
  {
    id: 1,
    title: 'A 旧题',
    description: '旧题面',
    difficulty: '简单',
    difficultyCode: 'EASY',
    tags: ['模拟'],
    createdAt: '2026-05-23T00:00:00',
    passed: false
  },
  {
    id: 2,
    title: 'B 新题',
    description: '新题面',
    difficulty: 'NOIP困难',
    difficultyCode: 'NOIP_HARD',
    tags: ['图论'],
    createdAt: '2026-05-24T00:00:00',
    passed: false
  }
]

test('manages problems with sorting and create dialog', async () => {
  vi.mocked(api.searchProblems).mockResolvedValue(sampleProblems)

  render(ProblemsView)

  expect(await screen.findByRole('heading', { name: '题目管理' })).toBeTruthy()
  await waitFor(() => {
    const titles = Array.from(document.querySelectorAll('.problem-row h3')).map(node => node.textContent)
    expect(titles).toEqual(['B 新题', 'A 旧题'])
  })

  await userEvent.click(screen.getByText('倒序'))
  await waitFor(() => {
    const titles = Array.from(document.querySelectorAll('.problem-row h3')).map(node => node.textContent)
    expect(titles).toEqual(['A 旧题', 'B 新题'])
  })

  await userEvent.click(screen.getByText('新建题目'))
  expect(screen.getByRole('dialog')).toBeTruthy()
  expect(screen.getByPlaceholderText('题面描述')).toBeTruthy()
})
