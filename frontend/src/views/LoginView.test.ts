import { render, screen } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import LoginView from './LoginView.vue'
import { api } from '../services/api'

const replace = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ replace })
}))

vi.mock('../services/api', () => ({
  api: {
    login: vi.fn(),
    register: vi.fn()
  }
}))

test('logs in and redirects to analysis workspace', async () => {
  vi.mocked(api.login).mockResolvedValue({ token: 'token', user: { id: 1, username: 'teacher' } })
  render(LoginView)

  await userEvent.click(screen.getByText('登录'))

  expect(api.login).toHaveBeenCalledWith('teacher', 'password123')
  expect(replace).toHaveBeenCalledWith('/analysis')
})

