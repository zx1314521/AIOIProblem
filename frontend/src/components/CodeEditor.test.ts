import { render, waitFor } from '@testing-library/vue'
import CodeEditor from './CodeEditor.vue'

test('sets a visible cursor color for the dark editor theme', async () => {
  render(CodeEditor, {
    props: {
      modelValue: 'int main() { return 0; }'
    }
  })

  await waitFor(() => {
    const themeCss = Array.from(document.querySelectorAll('style'))
      .map(style => style.textContent ?? '')
      .join('\n')

    expect(themeCss).toContain('.cm-cursor')
    expect(themeCss).toContain('#e8f7ff')
  })
})
