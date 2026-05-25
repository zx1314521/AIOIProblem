import { render, screen, waitFor, within } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import ProblemSetsView from './ProblemSetsView.vue'
import { api } from '../services/api'
import type { Problem, ProblemSet } from '../types'

vi.mock('../services/api', () => ({
  api: {
    listProblemSets: vi.fn(),
    createProblemSet: vi.fn(),
    searchProblems: vi.fn(),
    addProblemsToSet: vi.fn(),
    removeProblemFromSet: vi.fn(),
    reorderProblemSetItems: vi.fn()
  }
}))

const firstProblem: Problem = {
  id: 1,
  title: '基础模拟',
  description: '给定数组 $a_i$，输出最大值。\n\n#### 输入格式\n一行整数。',
  difficulty: '简单',
  difficultyCode: 'EASY',
  tags: ['模拟'],
  createdAt: '2026-05-24T00:00:00',
  passed: false
}

const secondProblem: Problem = {
  id: 2,
  title: '贪心训练',
  description: '题面',
  difficulty: 'CSPJ中等',
  difficultyCode: 'CSPJ_MEDIUM',
  tags: ['贪心'],
  createdAt: '2026-05-25T00:00:00',
  passed: true
}

const sampleSet: ProblemSet = {
  id: 7,
  name: '入门训练',
  description: '第一周练习',
  createdAt: '2026-05-25T00:00:00',
  problems: [firstProblem]
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(api.listProblemSets).mockResolvedValue([sampleSet])
})

test('searches problems and adds selected results to current set', async () => {
  vi.mocked(api.searchProblems).mockResolvedValue([secondProblem])
  vi.mocked(api.addProblemsToSet).mockResolvedValue({ ...sampleSet, problems: [firstProblem, secondProblem] })

  render(ProblemSetsView)

  expect(await screen.findByRole('heading', { name: '入门训练' })).toBeTruthy()
  await userEvent.click(screen.getByRole('button', { name: /添加题目/ }))
  await userEvent.type(await screen.findByPlaceholderText('搜索题目关键词'), '贪心')
  await userEvent.click(screen.getByRole('button', { name: '搜索' }))
  await userEvent.click(await screen.findByRole('checkbox', { name: /选择题目 贪心训练/ }))
  await userEvent.click(screen.getByRole('button', { name: /加入当前题单/ }))

  await waitFor(() => {
    expect(api.addProblemsToSet).toHaveBeenCalledWith(7, [2])
  })
  expect(await screen.findByText('贪心训练')).toBeTruthy()
})

test('reorders current problem set items with move buttons', async () => {
  const twoProblemSet = { ...sampleSet, problems: [firstProblem, secondProblem] }
  vi.mocked(api.listProblemSets).mockResolvedValue([twoProblemSet])
  vi.mocked(api.reorderProblemSetItems).mockResolvedValue({ ...twoProblemSet, problems: [secondProblem, firstProblem] })

  render(ProblemSetsView)

  await screen.findByText('基础模拟')
  const secondRow = screen.getByText('贪心训练').closest('.set-problem-row') as HTMLElement
  await userEvent.click(within(secondRow).getByRole('button', { name: '上移' }))

  await waitFor(() => {
    expect(api.reorderProblemSetItems).toHaveBeenCalledWith(7, [2, 1])
  })
  const titles = Array.from(document.querySelectorAll('.set-problem-title')).map(node => node.textContent)
  expect(titles).toEqual(['贪心训练', '基础模拟'])
})

test('opens a problem statement from the selected problem set', async () => {
  render(ProblemSetsView)

  await userEvent.click(await screen.findByRole('button', { name: '基础模拟' }))

  expect(await screen.findByRole('dialog', { name: /基础模拟/ })).toBeTruthy()
  expect(screen.getByText(/输出最大值/)).toBeTruthy()
  expect(document.querySelector('.problem-statement .katex')).toBeTruthy()
})
