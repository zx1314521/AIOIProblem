import { render, screen, waitFor } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import ProblemDataView from './ProblemDataView.vue'
import { api } from '../services/api'
import type { Problem, ProblemDataSet } from '../types'

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '1' } }),
  RouterLink: { props: ['to'], template: '<a><slot /></a>' }
}))

vi.mock('../services/api', () => ({
  api: {
    getProblem: vi.fn(),
    getProblemData: vi.fn(),
    generateProblemData: vi.fn(),
    addProblemDataCase: vi.fn(),
    updateProblemDataCase: vi.fn(),
    deleteProblemDataCase: vi.fn(),
    downloadProblemData: vi.fn()
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

const dataSet: ProblemDataSet = {
  id: 1,
  problemId: 1,
  status: 'READY',
  stdCpp: 'int main(){}',
  configYaml: 'type: default',
  cases: [{ id: 8, index: 1, input: '2 3\n', output: '5\n' }]
}

beforeEach(() => {
  vi.mocked(api.getProblem).mockResolvedValue(problem)
  vi.mocked(api.getProblemData).mockResolvedValue(dataSet)
})

test('loads and updates a selected test case', async () => {
  vi.mocked(api.updateProblemDataCase).mockResolvedValue({
    ...dataSet,
    cases: [{ id: 8, index: 1, input: '4 7\n', output: '11\n' }]
  })

  render(ProblemDataView)

  expect(await screen.findByRole('heading', { name: 'A+B' })).toBeTruthy()
  const input = screen.getByLabelText('输入 .in')
  const output = screen.getByLabelText('输出 .out')
  await userEvent.clear(input)
  await userEvent.type(input, '4 7\n')
  await userEvent.clear(output)
  await userEvent.type(output, '11\n')
  await userEvent.click(screen.getByRole('button', { name: /保存/ }))

  await waitFor(() => {
    expect(api.updateProblemDataCase).toHaveBeenCalledWith(1, 8, { index: 1, input: '4 7\n', output: '11\n' })
  })
  expect(await screen.findByText('测试点已保存')).toBeTruthy()
})
