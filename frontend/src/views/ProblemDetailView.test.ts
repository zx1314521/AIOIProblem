import { render, screen, waitFor } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import ProblemDetailView from './ProblemDetailView.vue'
import { api } from '../services/api'
import type { Problem, ProblemDataStatus } from '../types'

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '1' } }),
  RouterLink: { props: ['to'], template: '<a><slot /></a>' }
}))

vi.mock('../components/CodeEditor.vue', () => ({
  default: {
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template: `<textarea aria-label="code" :value="modelValue" @input="$emit('update:modelValue', $event.target.value)" />`
  }
}))

vi.mock('../services/api', () => ({
  api: {
    getProblem: vi.fn(),
    getProblemDataStatus: vi.fn(),
    generateProblemData: vi.fn(),
    runProblemDebug: vi.fn(),
    runProblemCases: vi.fn()
  }
}))

const problem: Problem = {
  id: 1,
  title: 'A+B',
  description: 'Read two integers.',
  difficulty: '简单',
  difficultyCode: 'EASY',
  tags: ['模拟'],
  createdAt: '2026-06-05T00:00:00',
  passed: false,
  dataStatus: 'READY'
}

const status: ProblemDataStatus = {
  id: 1,
  problemId: 1,
  status: 'READY',
  caseCount: 25
}

beforeEach(() => {
  vi.mocked(api.getProblem).mockResolvedValue(problem)
  vi.mocked(api.getProblemDataStatus).mockResolvedValue(status)
})

test('loads problem statement and runs debug input', async () => {
  vi.mocked(api.runProblemDebug).mockResolvedValue({
    status: 'OK',
    stdout: '5\n',
    stderr: '',
    exitCode: 0,
    durationMs: 12,
    cases: []
  })

  render(ProblemDetailView)

  expect(await screen.findByRole('heading', { name: 'A+B' })).toBeTruthy()
  await userEvent.type(screen.getByPlaceholderText('stdin 输入'), '2 3')
  await userEvent.click(screen.getByRole('button', { name: /开始/ }))

  await waitFor(() => {
    expect(api.runProblemDebug).toHaveBeenCalledWith(1, expect.objectContaining({ input: '2 3' }))
  })
  expect(await screen.findByText('OK')).toBeTruthy()
  expect(await screen.findByText('5')).toBeTruthy()
})

test('shows a clear notice when the problem has no data', async () => {
  vi.mocked(api.getProblem).mockResolvedValue({ ...problem, dataStatus: 'NONE' })

  render(ProblemDetailView)

  expect(await screen.findByText('本题没数据')).toBeTruthy()
  expect(screen.getByText('可以点击右上角 AI数据 生成测试点，或进入数据管理手动添加。')).toBeTruthy()
})
