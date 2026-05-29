import { render, screen } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import OjImportHistoryView from './OjImportHistoryView.vue'
import { api } from '../services/api'
import { authState, setAuth } from '../services/auth'
import type { DuplicateHint, OjImportHistoryJob, Problem } from '../types'

vi.mock('../services/api', () => ({
  api: {
    listOjImportHistory: vi.fn(),
    getProblem: vi.fn(),
    listSimilarProblems: vi.fn(),
    createProblemSetWithProblems: vi.fn(),
    updateProblem: vi.fn()
  }
}))

const history: OjImportHistoryJob[] = [
  {
    id: 7,
    name: 'OJ 导入',
    status: 'COMPLETED',
    totalCount: 2,
    successCount: 1,
    failedCount: 1,
    pendingCount: 0,
    runningCount: 0,
    createdAt: '2026-05-26T10:00:00',
    items: [
      {
        id: 71,
        platform: 'CODEFORCES',
        sourceId: 'CF2229D',
        title: 'D. Median Splits',
        status: 'SUCCEEDED',
        problemId: 101,
        sourceUrl: 'https://codeforces.com/contest/2229/problem/D',
        originalStatement: 'You are given arrays. Determine the answer.',
        passedRequested: true,
        createdAt: '2026-05-26T10:00:00',
        finishedAt: '2026-05-26T10:00:08',
        aiProvider: 'DeepSeek API',
        aiModel: 'deepseek-chat',
        aiDurationMs: 8342
      },
      {
        id: 72,
        platform: 'ATCODER',
        sourceId: 'ATABC400_ABC400_A',
        title: 'A - 2^N',
        status: 'FAILED',
        sourceUrl: 'https://atcoder.jp/contests/abc400/tasks/abc400_a',
        originalStatement: 'Find 2^N.',
        passedRequested: false,
        errorMessage: '题面为空',
        createdAt: '2026-05-26T10:01:00',
        finishedAt: '2026-05-26T10:01:02'
      }
    ]
  }
]

const problem: Problem = {
  id: 101,
  title: 'D. Median Splits',
  description: '给定数组，判断答案。',
  difficulty: '简单',
  difficultyCode: 'EASY',
  tags: ['模拟'],
  source: 'Codeforces: CF2229D',
  externalPlatform: 'CODEFORCES',
  externalSourceId: 'CF2229D',
  sourceUrl: 'https://codeforces.com/contest/2229/problem/D',
  createdAt: '2026-05-26T10:00:08',
  passed: true
}

const duplicateHints: DuplicateHint[] = [
  {
    id: 202,
    title: 'Median Splits practice',
    difficulty: 'Easy',
    difficultyCode: 'EASY',
    tags: ['simulation'],
    externalPlatform: 'CODEFORCES',
    externalSourceId: 'CF2229D2',
    sourceUrl: 'https://codeforces.com/problemset/problem/2229/D',
    score: 80,
    reason: 'title: median, splits'
  }
]

beforeEach(() => {
  localStorage.clear()
  setAuth({ token: 'token', user: { id: 1, username: 'teacher' } })
  vi.mocked(api.listOjImportHistory).mockResolvedValue(history)
  vi.mocked(api.getProblem).mockResolvedValue(problem)
  vi.mocked(api.listSimilarProblems).mockResolvedValue(duplicateHints)
  vi.mocked(api.createProblemSetWithProblems).mockResolvedValue({
    id: 301,
    name: 'OJ 导入训练',
    description: '来自 OJ 导入记录',
    createdAt: '2026-05-26T10:02:00',
    problems: [problem]
  })
  vi.mocked(api.updateProblem).mockResolvedValue({ ...problem, description: '修正后的题面' })
})

test('shows similar problem hints when reviewing an imported statement', async () => {
  render(OjImportHistoryView)

  await userEvent.click(await screen.findByRole('button', { name: /预览\/修正 D. Median Splits/ }))

  expect(api.listSimilarProblems).toHaveBeenCalledWith(101)
  expect(await screen.findByText('潜在重复题')).toBeTruthy()
  expect(screen.getByText('Median Splits practice')).toBeTruthy()
  expect(screen.getByText('title: median, splits')).toBeTruthy()
})

test('creates a practice set from imported problems', async () => {
  render(OjImportHistoryView)

  await userEvent.click(await screen.findByRole('button', { name: /生成训练题单/ }))

  expect(api.createProblemSetWithProblems).toHaveBeenCalledWith(
    expect.stringContaining('OJ 导入训练'),
    '来自 OJ 导入记录',
    [101]
  )
  expect(await screen.findByText('训练题单已创建')).toBeTruthy()
})

test('keeps local auth state when OJ import history request fails', async () => {
  vi.mocked(api.listOjImportHistory).mockRejectedValue(new Error('Forbidden'))

  render(OjImportHistoryView)

  expect(await screen.findByText(/前端地址/)).toBeTruthy()
  expect(authState.token).toBe('token')
  expect(authState.user?.username).toBe('teacher')
})

test('renders OJ import batches with item status and source metadata', async () => {
  render(OjImportHistoryView)

  expect(await screen.findByRole('heading', { name: 'OJ 导入记录' })).toBeTruthy()
  expect(await screen.findByText('2 个导入项 · 1 成功 · 1 失败')).toBeTruthy()
  expect(screen.getByText('CF2229D')).toBeTruthy()
  expect(screen.getByText('Codeforces')).toBeTruthy()
  expect(screen.getByText('已请求标记通过')).toBeTruthy()
  expect(screen.getByText('题库 #101')).toBeTruthy()
  expect(screen.getByText('DeepSeek API · deepseek-chat · 8.3 s')).toBeTruthy()
  expect(screen.getByText('ATABC400_ABC400_A')).toBeTruthy()
  expect(screen.getByText('题面为空')).toBeTruthy()
})

test('previews raw and polished statements and saves manual corrections', async () => {
  render(OjImportHistoryView)

  await userEvent.click(await screen.findByRole('button', { name: /预览\/修正 D. Median Splits/ }))

  expect(screen.getByText('原始题面')).toBeTruthy()
  expect(screen.getByText('You are given arrays. Determine the answer.')).toBeTruthy()
  const editor = screen.getByLabelText('整理后题面') as HTMLTextAreaElement
  expect(editor.value).toBe('给定数组，判断答案。')

  await userEvent.clear(editor)
  await userEvent.type(editor, '修正后的题面')
  await userEvent.click(screen.getByRole('button', { name: '保存修正' }))

  expect(api.updateProblem).toHaveBeenCalledWith(101, {
    title: 'D. Median Splits',
    description: '修正后的题面',
    difficulty: 'EASY',
    tags: ['模拟'],
    source: 'Codeforces: CF2229D'
  })
  expect(await screen.findByText('题面修正已保存。')).toBeTruthy()
})
