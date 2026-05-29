import { render, screen } from '@testing-library/vue'
import HomeView from './HomeView.vue'

test('introduces the project and links the packaged extension download', () => {
  render(HomeView)

  expect(screen.getByRole('heading', { name: 'AIOIProblem' })).toBeTruthy()
  expect(screen.getByText('面向信息学竞赛教学的选题与训练工作台')).toBeTruthy()

  const download = screen.getByRole('link', { name: '下载浏览器插件' }) as HTMLAnchorElement
  expect(download.getAttribute('href')).toBe('/downloads/aioi-oj-import-extension.zip')
  expect(download.hasAttribute('download')).toBe(true)
})
