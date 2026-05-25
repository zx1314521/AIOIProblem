import { render, screen } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import AnalysisHistoryView from './AnalysisHistoryView.vue'
import { api } from '../services/api'
import type { BatchJobDetail } from '../types'

vi.mock('../services/api', () => ({
  api: {
    listBatchJobs: vi.fn(),
    getBatchJob: vi.fn()
  }
}))

const historyDetail: BatchJobDetail = {
  job: {
    id: 1,
    name: '批量题面分析',
    status: 'COMPLETED',
    totalCount: 2,
    successCount: 1,
    failedCount: 1,
    pendingCount: 0,
    runningCount: 0,
    createdAt: '2026-05-24T00:00:00'
  },
  items: [
    {
      id: 11,
      title: 'P1 最短路',
      content: '给定图，求最短路。',
      status: 'SUCCEEDED',
      sortOrder: 0,
      problemId: 101,
      difficulty: 'CSPJ中等',
      difficultyCode: 'CSPJ_MEDIUM',
      tags: ['最短路'],
      createdAt: '2026-05-24T08:00:00',
      startedAt: '2026-05-24T08:00:01',
      finishedAt: '2026-05-24T08:00:03',
      aiProvider: 'DeepSeek API',
      aiModel: 'deepseek-chat',
      aiConfidence: 0.86,
      aiReasoningSummary: '根据图模型和数据范围判断为最短路。',
      aiHints: ['先建图。', '考虑 Dijkstra。'],
      aiDurationMs: 2345
    },
    {
      id: 12,
      title: 'P2 失败题',
      content: '坏输入',
      status: 'FAILED',
      sortOrder: 1,
      tags: [],
      errorMessage: 'Codex CLI 调用超时',
      createdAt: '2026-05-24T09:00:00',
      startedAt: '2026-05-24T09:00:01',
      finishedAt: '2026-05-24T09:03:01',
      aiProvider: 'Codex CLI',
      aiModel: 'codex',
      aiDurationMs: 180000
    },
    {
      id: 13,
      title: 'P3 兜底题',
      content: '模拟题面',
      status: 'SUCCEEDED',
      sortOrder: 2,
      problemId: 103,
      difficulty: '入门',
      difficultyCode: 'ENTRY',
      tags: [],
      createdAt: '2026-05-24T10:00:00',
      startedAt: '2026-05-24T10:00:01',
      finishedAt: '2026-05-24T10:03:02',
      aiProvider: 'Codex CLI',
      aiModel: 'codex',
      aiConfidence: 0.62,
      aiReasoningSummary: 'Codex CLI 调用失败，已使用本地规则模型兜底：规则模型根据关键词、数据范围和算法标签给出初判。',
      aiHints: ['先模拟。'],
      aiDurationMs: 181000
    }
  ]
}

beforeEach(() => {
  vi.mocked(api.listBatchJobs).mockResolvedValue([historyDetail.job])
  vi.mocked(api.getBatchJob).mockResolvedValue(historyDetail)
})

test('renders analysis logs with AI metadata and process details', async () => {
  render(AnalysisHistoryView)

  expect(await screen.findByRole('heading', { name: '历史记录' })).toBeTruthy()
  expect(await screen.findByText(/3 条记录 · 1 成功 · 1 规则兜底 · 1 失败/)).toBeTruthy()

  await userEvent.click(screen.getByRole('button', { name: /P1 最短路/ }))

  expect(screen.getByText('DeepSeek API')).toBeTruthy()
  expect(screen.getByText('deepseek-chat')).toBeTruthy()
  expect(screen.getByText('2.3 s')).toBeTruthy()
  expect(screen.getByText('86%')).toBeTruthy()
  expect(screen.getByText('根据图模型和数据范围判断为最短路。')).toBeTruthy()
  expect(screen.getByText('先建图。')).toBeTruthy()
  expect(screen.getByText('题库 #101')).toBeTruthy()

  await userEvent.click(screen.getByRole('button', { name: /P2 失败题/ }))
  expect(screen.getByText('Codex CLI 调用超时')).toBeTruthy()
  expect(screen.getByText('180 s')).toBeTruthy()

  await userEvent.click(screen.getByRole('button', { name: /P3 兜底题/ }))
  expect(screen.getAllByText('规则兜底').length).toBeGreaterThan(0)
  expect(screen.getByText('本地规则模型（Codex CLI失败后兜底）')).toBeTruthy()
  expect(screen.getByText('规则模型')).toBeTruthy()
  expect(screen.getByText(/Codex CLI 调用失败；本次难度和提示由本地规则模型兜底生成/)).toBeTruthy()
})
