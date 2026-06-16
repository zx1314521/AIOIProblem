import { render, screen, waitFor, within } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import AnalysisView from './AnalysisView.vue'
import { api } from '../services/api'
import type { BatchJobDetail } from '../types'

vi.mock('../services/api', () => ({
  api: {
    uploadBatch: vi.fn(),
    listBatchJobs: vi.fn(),
    getBatchJob: vi.fn(),
    updateBatchItem: vi.fn(),
    deleteBatchItem: vi.fn(),
    reorderBatchItems: vi.fn()
  }
}))

const queuedJob: BatchJobDetail = {
  job: {
    id: 1,
    name: 'Single analysis',
    status: 'RUNNING',
    totalCount: 1,
    successCount: 0,
    failedCount: 0,
    pendingCount: 1,
    runningCount: 0,
    createdAt: '2026-05-24T00:00:00'
  },
  items: [
    {
      id: 10,
      title: 'P6 wrong order',
      content: 'Problem content a_i',
      status: 'PENDING',
      sortOrder: 0,
      tags: [],
      createdAt: '2026-05-24T00:00:00'
    }
  ]
}

const mixedJob: BatchJobDetail = {
  job: {
    id: 2,
    name: 'Large batch',
    status: 'RUNNING',
    totalCount: 5,
    successCount: 1,
    failedCount: 1,
    pendingCount: 2,
    runningCount: 1,
    createdAt: '2026-05-24T00:00:00'
  },
  items: [
    {
      id: 21,
      title: 'Pending problem',
      content: '# Pending problem\n\n$a_i$',
      status: 'PENDING',
      sortOrder: 0,
      tags: [],
      createdAt: '2026-05-24T00:00:00'
    },
    {
      id: 22,
      title: 'Running problem',
      content: '# Running problem',
      status: 'RUNNING',
      sortOrder: 1,
      tags: [],
      createdAt: '2026-05-24T00:00:00'
    },
    {
      id: 23,
      title: 'Succeeded problem',
      content: '# Succeeded problem',
      status: 'SUCCEEDED',
      sortOrder: 2,
      tags: [],
      createdAt: '2026-05-24T00:00:00'
    },
    {
      id: 24,
      title: 'Failed problem',
      content: '# Failed problem\n\nInterval DP $f_i$',
      status: 'FAILED',
      sortOrder: 3,
      tags: [],
      errorMessage: 'Codex CLI exited with code 124 after 120 seconds. stderr: request timed out while analyzing long markdown input with many constraints.',
      createdAt: '2026-05-24T00:00:00'
    },
    {
      id: 25,
      title: 'Second pending problem',
      content: '# Second pending problem',
      status: 'PENDING',
      sortOrder: 4,
      tags: [],
      createdAt: '2026-05-24T00:00:00'
    }
  ]
}

const completedArchiveJob: BatchJobDetail = {
  job: {
    id: 3,
    name: 'Previous import',
    status: 'COMPLETED',
    totalCount: 1,
    successCount: 1,
    failedCount: 0,
    pendingCount: 0,
    runningCount: 0,
    createdAt: '2026-05-23T00:00:00'
  },
  items: [
    {
      id: 31,
      title: 'Old visible problem',
      content: '# Old visible problem',
      status: 'SUCCEEDED',
      sortOrder: 0,
      tags: ['dp'],
      createdAt: '2026-05-23T00:00:00'
    }
  ]
}

const dataGenerationJob: BatchJobDetail = {
  job: {
    id: 4,
    name: 'AI数据生成',
    status: 'RUNNING',
    totalCount: 1,
    successCount: 0,
    failedCount: 0,
    pendingCount: 1,
    runningCount: 0,
    createdAt: '2026-05-25T00:00:00'
  },
  items: [
    {
      id: 41,
      title: 'A+B',
      content: 'Read two integers.',
      taskType: 'DATA_GENERATION',
      status: 'PENDING',
      sortOrder: 0,
      problemId: 1,
      tags: ['模拟'],
      createdAt: '2026-05-25T00:00:00'
    }
  ]
}

beforeEach(() => {
  vi.useRealTimers()
  vi.clearAllMocks()
})

afterEach(() => {
  vi.useRealTimers()
})

test('starts single analysis by enqueueing it as a task', async () => {
  vi.mocked(api.listBatchJobs).mockResolvedValue([])
  vi.mocked(api.uploadBatch).mockResolvedValue(queuedJob)

  const { container } = render(AnalysisView)
  const [titleInput, contentInput] = screen.getAllByRole('textbox')
  await userEvent.type(titleInput, 'P6 wrong order')
  await userEvent.type(contentInput, 'Problem content a_i')
  await userEvent.click(container.querySelector('button[type="submit"]') as HTMLButtonElement)

  expect(api.uploadBatch).toHaveBeenCalledTimes(1)
  const [, files] = vi.mocked(api.uploadBatch).mock.calls[0]
  expect(files).toHaveLength(1)
  expect(files[0].name).toBe('P6 wrong order.md')

  expect(await screen.findByRole('button', { name: /P6 wrong order/ })).toBeTruthy()
  expect(screen.getByRole('heading', { name: 'P6 wrong order' })).toBeTruthy()
})

test('filters batch items by status and keeps the selected filter after polling', async () => {
  vi.useFakeTimers()
  const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
  vi.mocked(api.listBatchJobs).mockResolvedValue([mixedJob.job])
  vi.mocked(api.getBatchJob).mockResolvedValue(mixedJob)

  render(AnalysisView)

  expect(await screen.findByRole('button', { name: /全部 5/ })).toBeTruthy()
  expect(screen.getByRole('button', { name: /等待 2/ })).toBeTruthy()
  expect(screen.getByRole('button', { name: /运行中 1/ })).toBeTruthy()
  expect(screen.getByRole('button', { name: /成功 1/ })).toBeTruthy()
  expect(screen.getByRole('button', { name: /失败 1/ })).toBeTruthy()

  await user.click(screen.getByRole('button', { name: /失败 1/ }))

  const queue = screen.getByLabelText('任务题目列表')
  expect(within(queue).getByRole('button', { name: /Failed problem/ })).toBeTruthy()
  expect(within(queue).queryByRole('button', { name: /Pending problem/ })).toBeNull()
  expect(screen.getAllByText(/Codex CLI exited with code 124/).length).toBeGreaterThan(0)
  expect(screen.queryByText(/many constraints\./)).toBeNull()

  await user.click(screen.getByRole('button', { name: '查看完整失败原因' }))
  expect(screen.getAllByText(/many constraints\./).length).toBeGreaterThan(0)

  await vi.advanceTimersByTimeAsync(3000)
  await waitFor(() => expect(api.getBatchJob).toHaveBeenCalledTimes(2))

  expect(screen.getByRole('button', { name: /失败 1/ }).getAttribute('aria-pressed')).toBe('true')
  expect(within(queue).getByRole('button', { name: /Failed problem/ })).toBeTruthy()
  vi.useRealTimers()
})

test('shows items from older import batches in the same task rail', async () => {
  vi.mocked(api.listBatchJobs).mockResolvedValue([mixedJob.job, completedArchiveJob.job])
  vi.mocked(api.getBatchJob).mockImplementation(async (id: number) => {
    if (id === mixedJob.job.id) return mixedJob
    if (id === completedArchiveJob.job.id) return completedArchiveJob
    throw new Error('missing job')
  })

  render(AnalysisView)

  await screen.findByRole('button', { name: /Pending problem/ })
  const queue = screen.getByLabelText('任务题目列表')
  expect(within(queue).getByRole('button', { name: /Pending problem/ })).toBeTruthy()
  expect(within(queue).getByRole('button', { name: /Old visible problem/ })).toBeTruthy()
  expect(screen.getByRole('button', { name: /全部 6/ })).toBeTruthy()
})

test('distinguishes analysis and data generation tasks', async () => {
  vi.mocked(api.listBatchJobs).mockResolvedValue([dataGenerationJob.job, mixedJob.job])
  vi.mocked(api.getBatchJob).mockImplementation(async (id: number) => {
    if (id === dataGenerationJob.job.id) return dataGenerationJob
    if (id === mixedJob.job.id) return mixedJob
    throw new Error('missing job')
  })

  render(AnalysisView)

  await screen.findByRole('button', { name: /A\+B/ })
  expect(screen.getAllByText('造数据').length).toBeGreaterThan(0)
  expect(screen.getAllByText('题目分析').length).toBeGreaterThan(0)
})

test('keeps the selected completed job visible after polling', async () => {
  vi.useFakeTimers()
  const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
  const completedJob: BatchJobDetail = {
    ...mixedJob,
    job: {
      ...mixedJob.job,
      status: 'COMPLETED',
      pendingCount: 0,
      runningCount: 0
    }
  }
  vi.mocked(api.listBatchJobs)
    .mockResolvedValueOnce([mixedJob.job])
    .mockResolvedValueOnce([completedJob.job])
    .mockResolvedValueOnce([completedJob.job])
  vi.mocked(api.getBatchJob)
    .mockResolvedValueOnce(mixedJob)
    .mockResolvedValueOnce(completedJob)
    .mockResolvedValueOnce(completedJob)

  render(AnalysisView)
  await user.click(await screen.findByRole('button', { name: /失败 1/ }))

  await vi.advanceTimersByTimeAsync(3000)
  await waitFor(() => expect(api.getBatchJob).toHaveBeenCalledTimes(2))
  await vi.advanceTimersByTimeAsync(3000)
  await waitFor(() => expect(api.getBatchJob).toHaveBeenCalledTimes(3))

  expect(screen.getByRole('button', { name: /失败 1/ }).getAttribute('aria-pressed')).toBe('true')
  expect(screen.getByRole('button', { name: /Failed problem/ })).toBeTruthy()
  expect(screen.getAllByText(/Codex CLI exited with code 124/).length).toBeGreaterThan(0)
  vi.useRealTimers()
})

test('shows an empty state when the selected status has no items', async () => {
  const noFailedJob: BatchJobDetail = {
    ...mixedJob,
    job: { ...mixedJob.job, totalCount: 4, failedCount: 0 },
    items: mixedJob.items.filter(item => item.status !== 'FAILED')
  }
  vi.mocked(api.listBatchJobs).mockResolvedValue([noFailedJob.job])
  vi.mocked(api.getBatchJob).mockResolvedValue(noFailedJob)

  render(AnalysisView)

  await userEvent.click(await screen.findByRole('button', { name: /失败 0/ }))

  expect(screen.getByText('当前没有失败题')).toBeTruthy()
})
