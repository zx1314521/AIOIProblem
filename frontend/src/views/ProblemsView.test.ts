import { render, screen, waitFor, within } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import ProblemsView from './ProblemsView.vue'
import { api } from '../services/api'
import type { Problem, TagCatalog } from '../types'

vi.mock('../services/api', () => ({
  api: {
    searchProblems: vi.fn(),
    getProblem: vi.fn(),
    createProblem: vi.fn(),
    updateProblem: vi.fn(),
    deleteProblem: vi.fn(),
    markPassed: vi.fn(),
    unmarkPassed: vi.fn(),
    getTags: vi.fn()
  }
}))

const catalog: TagCatalog = {
  categories: [
    { name: '图论', tags: ['最短路', '网络流'] },
    { name: '基础算法', tags: ['模拟', '贪心'] }
  ]
}

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
    tags: ['最短路'],
    createdAt: '2026-05-24T00:00:00',
    passed: false
  }
]

beforeEach(() => {
  vi.mocked(api.getTags).mockResolvedValue(catalog)
})

test('manages problems with sorting and create dialog', async () => {
  vi.mocked(api.searchProblems).mockResolvedValue(sampleProblems)

  render(ProblemsView)

  expect(await screen.findByRole('heading', { name: '题目管理' })).toBeTruthy()
  await waitFor(() => {
    const titles = Array.from(document.querySelectorAll('.problem-row h3')).map(node => node.textContent)
    expect(titles).toEqual(['B 新题', 'A 旧题'])
  })
  expect(screen.queryByText('旧题面')).toBeNull()

  await userEvent.click(screen.getByText('倒序'))
  await waitFor(() => {
    const titles = Array.from(document.querySelectorAll('.problem-row h3')).map(node => node.textContent)
    expect(titles).toEqual(['A 旧题', 'B 新题'])
  })

  vi.mocked(api.getProblem).mockResolvedValue(sampleProblems[0])
  await userEvent.click(screen.getAllByText('查看')[0])
  expect(await screen.findByRole('dialog')).toBeTruthy()
  expect(await screen.findByText('旧题面')).toBeTruthy()

  await userEvent.click(screen.getByTitle('关闭'))
  await userEvent.click(screen.getByText('新建题目'))
  expect(screen.getByRole('dialog')).toBeTruthy()
  expect(screen.getByPlaceholderText('题面描述')).toBeTruthy()
})

test('toggles passed state from problem list', async () => {
  vi.mocked(api.searchProblems).mockResolvedValue([{ ...sampleProblems[0], passed: true }])
  vi.mocked(api.unmarkPassed).mockResolvedValue({ ...sampleProblems[0], passed: false })

  render(ProblemsView)

  const passedButton = await screen.findByRole('button', { name: '已通过' })
  await userEvent.click(passedButton)

  expect(api.unmarkPassed).toHaveBeenCalledWith(1)
  expect(await screen.findByRole('button', { name: '通过' })).toBeTruthy()
})

test('loads tag catalog and searches by selected standard tag', async () => {
  vi.mocked(api.searchProblems).mockResolvedValue(sampleProblems)

  render(ProblemsView)

  await screen.findByText('网络流')
  await userEvent.click(screen.getByRole('button', { name: '选择标签 网络流' }))
  await userEvent.click(screen.getByRole('button', { name: /搜索/ }))

  await waitFor(() => {
    expect(api.searchProblems).toHaveBeenLastCalledWith(new URLSearchParams('tag=%E7%BD%91%E7%BB%9C%E6%B5%81'))
  })
})

test('selects standard tags in problem form instead of typing them', async () => {
  vi.mocked(api.searchProblems).mockResolvedValue([])
  vi.mocked(api.createProblem).mockResolvedValue({
    id: 3,
    title: '新题',
    description: '题面',
    difficulty: '简单',
    difficultyCode: 'EASY',
    tags: ['最短路'],
    createdAt: '2026-05-24T00:00:00',
    passed: false
  })

  render(ProblemsView)

  await userEvent.click(await screen.findByText('新建题目'))
  await userEvent.type(screen.getByPlaceholderText('标题'), '新题')
  await userEvent.type(screen.getByPlaceholderText('题面描述'), '题面')
  const dialog = screen.getByRole('dialog')
  await userEvent.click(within(dialog).getByRole('button', { name: '选择标签 最短路' }))
  await userEvent.click(within(dialog).getByRole('button', { name: /添加题目/ }))

  await waitFor(() => {
    expect(api.createProblem).toHaveBeenCalledWith(expect.objectContaining({ tags: ['最短路'] }))
  })
})
