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
    deleteProblems: vi.fn(),
    reanalyzeProblems: vi.fn(),
    markPassed: vi.fn(),
    markProblemsPassed: vi.fn(),
    unmarkPassed: vi.fn(),
    getTags: vi.fn(),
    listProblemSets: vi.fn(),
    createProblemSet: vi.fn(),
    createProblemSetWithProblems: vi.fn(),
    addProblemsToSet: vi.fn()
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

const searchProblems: Problem[] = [
  {
    id: 10,
    title: '完全匹配',
    description: '题面',
    difficulty: '简单',
    difficultyCode: 'EASY',
    tags: ['最短路', '网络流'],
    createdAt: '2026-05-20T00:00:00',
    passed: false
  },
  {
    id: 11,
    title: '部分匹配',
    description: '题面',
    difficulty: '简单',
    difficultyCode: 'EASY',
    tags: ['最短路'],
    createdAt: '2026-05-24T00:00:00',
    passed: false
  },
  {
    id: 12,
    title: '同类相关',
    description: '题面',
    difficulty: '简单',
    difficultyCode: 'EASY',
    tags: ['图遍历'],
    createdAt: '2026-05-25T00:00:00',
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
  await userEvent.click(screen.getByText('A 旧题'))
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

test('loads tag catalog and searches by multiple standard tags with relevance first', async () => {
  vi.mocked(api.searchProblems).mockResolvedValue(searchProblems)

  render(ProblemsView)

  await screen.findByText('网络流')
  await userEvent.click(screen.getByRole('button', { name: '选择标签 最短路' }))
  await userEvent.click(screen.getByRole('button', { name: '选择标签 网络流' }))
  expect(screen.getByRole('button', { name: /移除筛选标签 最短路/ })).toBeTruthy()
  expect(screen.getByRole('button', { name: /移除筛选标签 网络流/ })).toBeTruthy()
  await userEvent.click(screen.getByRole('button', { name: /搜索/ }))

  await waitFor(() => {
    expect(api.searchProblems).toHaveBeenLastCalledWith(new URLSearchParams('tags=%E6%9C%80%E7%9F%AD%E8%B7%AF&tags=%E7%BD%91%E7%BB%9C%E6%B5%81'))
    const titles = Array.from(document.querySelectorAll('.problem-row h3')).map(node => node.textContent)
    expect(titles).toEqual(['完全匹配', '部分匹配', '同类相关'])
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

test('bulk selects problems and applies passed/delete/set actions', async () => {
  vi.mocked(api.searchProblems).mockResolvedValue(sampleProblems)
  vi.mocked(api.markProblemsPassed).mockResolvedValue(sampleProblems.map(problem => ({ ...problem, passed: true })))
  vi.mocked(api.deleteProblems).mockResolvedValue(undefined)
  vi.mocked(api.listProblemSets).mockResolvedValue([
    { id: 9, name: '训练题单', description: '', createdAt: '2026-05-24T00:00:00', problems: [] }
  ])
  vi.mocked(api.addProblemsToSet).mockResolvedValue({
    id: 9,
    name: '训练题单',
    description: '',
    createdAt: '2026-05-24T00:00:00',
    problems: sampleProblems
  })
  vi.spyOn(window, 'confirm').mockReturnValue(true)

  render(ProblemsView)

  expect(screen.queryByRole('checkbox', { name: new RegExp(sampleProblems[0].title) })).toBeNull()
  await userEvent.click(await screen.findByRole('button', { name: '选择' }))
  await userEvent.click(await screen.findByRole('checkbox', { name: new RegExp(sampleProblems[0].title) }))
  await userEvent.click(screen.getByRole('checkbox', { name: new RegExp(sampleProblems[1].title) }))

  await userEvent.click(screen.getByRole('button', { name: /批量通过/ }))
  await waitFor(() => {
    expect(api.markProblemsPassed).toHaveBeenCalledWith([1, 2])
  })

  await userEvent.click(screen.getByRole('button', { name: /加入题单/ }))
  await screen.findByRole('dialog')
  await userEvent.click(screen.getByRole('button', { name: /添加到已有题单/ }))
  await waitFor(() => {
    expect(api.addProblemsToSet).toHaveBeenCalledWith(9, [1, 2])
  })

  await userEvent.click(screen.getByRole('button', { name: /批量删除/ }))
  await waitFor(() => {
    expect(api.deleteProblems).toHaveBeenCalledWith([1, 2])
  })
})

test('queues selected problems for reanalysis', async () => {
  vi.mocked(api.searchProblems).mockResolvedValue(sampleProblems)
  vi.mocked(api.reanalyzeProblems).mockResolvedValue({
    job: {
      id: 21,
      name: '重新分析题目',
      status: 'RUNNING',
      totalCount: 2,
      successCount: 0,
      failedCount: 0,
      pendingCount: 2,
      runningCount: 0,
      createdAt: '2026-06-05T00:00:00'
    },
    items: []
  })

  render(ProblemsView)

  await userEvent.click(await screen.findByRole('button', { name: '选择' }))
  await userEvent.click(await screen.findByRole('checkbox', { name: new RegExp(sampleProblems[0].title) }))
  await userEvent.click(screen.getByRole('checkbox', { name: new RegExp(sampleProblems[1].title) }))
  await userEvent.click(screen.getByRole('button', { name: /批量分析/ }))

  await waitFor(() => {
    expect(api.reanalyzeProblems).toHaveBeenCalledWith([1, 2])
  })
  expect(await screen.findByText('已加入重新分析队列，可在批量任务中查看进度。')).toBeTruthy()
})

test('queues a single problem for reanalysis from row actions', async () => {
  vi.mocked(api.searchProblems).mockResolvedValue(sampleProblems)
  vi.mocked(api.reanalyzeProblems).mockResolvedValue({
    job: {
      id: 22,
      name: '重新分析题目',
      status: 'RUNNING',
      totalCount: 1,
      successCount: 0,
      failedCount: 0,
      pendingCount: 1,
      runningCount: 0,
      createdAt: '2026-06-05T00:00:00'
    },
    items: []
  })

  render(ProblemsView)

  const rowButtons = await screen.findAllByRole('button', { name: '分析' })
  await userEvent.click(rowButtons[0])

  await waitFor(() => {
    expect(api.reanalyzeProblems).toHaveBeenCalledWith([2])
  })
  expect(await screen.findByText('《B 新题》已加入重新分析队列，可在批量任务中查看进度。')).toBeTruthy()
})

test('creates a new problem set from selected problems', async () => {
  vi.mocked(api.searchProblems).mockResolvedValue(sampleProblems)
  vi.mocked(api.listProblemSets).mockResolvedValue([])
  vi.mocked(api.createProblemSetWithProblems).mockResolvedValue({
    id: 10,
    name: '新题单',
    description: '题目管理批量创建',
    createdAt: '2026-05-24T00:00:00',
    problems: [sampleProblems[0]]
  })

  render(ProblemsView)

  await userEvent.click(await screen.findByRole('button', { name: '选择' }))
  await userEvent.click(await screen.findByRole('checkbox', { name: new RegExp(sampleProblems[0].title) }))
  await userEvent.click(screen.getByRole('button', { name: /加入题单/ }))
  await userEvent.type(await screen.findByPlaceholderText('输入新题单名称'), '新题单')
  await userEvent.click(screen.getByRole('button', { name: /新建题单并加入/ }))

  await waitFor(() => {
    expect(api.createProblemSetWithProblems).toHaveBeenCalledWith('新题单', '题目管理批量创建', [1])
  })
})
