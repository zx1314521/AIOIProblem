import { render, screen } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import AnalysisView from './AnalysisView.vue'
import { api } from '../services/api'

vi.mock('../services/api', () => ({
  api: {
    analyzeText: vi.fn(),
    analyzeFile: vi.fn(),
    createProblem: vi.fn()
  }
}))

test('shows progressive hints only after clicking each hint', async () => {
  vi.mocked(api.analyzeText).mockResolvedValue({
    difficulty: 'CSPJ中等',
    difficultyCode: 'CSPJ_MEDIUM',
    confidence: 0.81,
    tags: ['动态规划'],
    hints: ['先定义状态', '写出转移', '滚动数组优化'],
    reasoningSummary: '需要状态设计。',
    similarProblems: []
  })

  render(AnalysisView)
  await userEvent.type(screen.getByPlaceholderText('在这里粘贴题面、输入输出与数据范围'), 'n <= 1000 动态规划')
  await userEvent.click(screen.getByText('分析文本'))

  expect(await screen.findByText('CSPJ中等')).toBeTruthy()
  expect(screen.queryByText('先定义状态')).toBeNull()

  await userEvent.click(screen.getByText('提示1'))
  expect(screen.getByText('先定义状态')).toBeTruthy()
})

