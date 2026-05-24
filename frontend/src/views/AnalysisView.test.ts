import { render, screen } from '@testing-library/vue'
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
    name: '单题分析',
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
      title: 'P6 错排问题',
      content: '题面内容 a_i',
      status: 'PENDING',
      sortOrder: 0,
      tags: [],
      createdAt: '2026-05-24T00:00:00'
    }
  ]
}

test('starts single analysis by enqueueing it as a task', async () => {
  vi.mocked(api.listBatchJobs).mockResolvedValue([])
  vi.mocked(api.uploadBatch).mockResolvedValue(queuedJob)

  render(AnalysisView)
  await userEvent.type(screen.getByPlaceholderText('例如：区间最大值'), 'P6 错排问题')
  await userEvent.type(screen.getByPlaceholderText('在这里粘贴题面、输入输出与数据范围'), '题面内容 a_i')
  await userEvent.click(screen.getByText('开始分析'))

  expect(api.uploadBatch).toHaveBeenCalledTimes(1)
  const [, files] = vi.mocked(api.uploadBatch).mock.calls[0]
  expect(files).toHaveLength(1)
  expect(files[0].name).toBe('P6 错排问题.md')

  expect(await screen.findByRole('button', { name: /P6 错排问题/ })).toBeTruthy()
  expect(screen.getByRole('heading', { name: 'P6 错排问题' })).toBeTruthy()
  expect(screen.getAllByText('等待中')).toHaveLength(2)
})
