import { render, screen, waitFor } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import SettingsView from './SettingsView.vue'
import { api } from '../services/api'
import type { AiSettings } from '../types'

vi.mock('../services/api', () => ({
  api: {
    getAiSettings: vi.fn(),
    updateAiSettings: vi.fn()
  }
}))

const settings: AiSettings = {
  provider: 'codex',
  problemAnalysisProvider: 'codex',
  recommendationProvider: 'mock',
  dataGenerationProvider: 'codex',
  deepSeekApiKey: '',
  deepSeekBaseUrl: 'https://api.deepseek.com/chat/completions',
  deepSeekModel: 'deepseek-chat',
  deepSeekTimeoutSeconds: 45,
  codexCommand: 'codex',
  codexModel: 'gpt-5.5',
  codexTimeoutSeconds: 180
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(api.getAiSettings).mockResolvedValue(settings)
  vi.mocked(api.updateAiSettings).mockImplementation(async payload => payload)
})

test('saves different AI providers for different features', async () => {
  render(SettingsView)

  await screen.findByRole('heading', { name: 'AI设置' })
  const selects = await screen.findAllByRole('combobox')
  await userEvent.selectOptions(selects[1], 'deepseek')
  await userEvent.selectOptions(selects[2], 'mock')
  await userEvent.selectOptions(selects[3], 'codex')
  await userEvent.click(screen.getByRole('button', { name: /保存设置/ }))

  await waitFor(() => {
    expect(api.updateAiSettings).toHaveBeenCalledWith(expect.objectContaining({
      provider: 'codex',
      problemAnalysisProvider: 'deepseek',
      recommendationProvider: 'mock',
      dataGenerationProvider: 'codex'
    }))
  })
  expect(await screen.findByText('已保存 AI 设置')).toBeTruthy()
})

test('fills AI data provider when older settings response does not include it', async () => {
  vi.mocked(api.getAiSettings).mockResolvedValue({
    ...settings,
    dataGenerationProvider: undefined
  } as unknown as AiSettings)

  render(SettingsView)

  const selects = await screen.findAllByRole('combobox')
  expect((selects[3] as HTMLSelectElement).value).toBe('codex')
})
