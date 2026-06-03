import { describe, expect, test } from 'vitest'
import { createProblemMarkdown, normalizeProblemMath, renderProblemMarkdown } from './problemMath'

const markdown = createProblemMarkdown()

describe('normalizeProblemMath', () => {
  test('wraps common OI subscript sequences as one formula', () => {
    expect(renderProblemMarkdown(markdown, 'a_1, a_2, \\dots, a_n')).toContain('<span class="compact-math"')
    expect(renderProblemMarkdown(markdown, 'a_1, a_2, \\dots, a_n')).toContain('₁')
  })

  test('wraps chained bounds as one formula', () => {
    const html = renderProblemMarkdown(markdown, '范围 (1\\leq a_i\\leq n)')
    expect(html).toContain('<span class="compact-math"')
    expect(html).toContain('<span class="compact-var"><var>a</var><span class="compact-sub" aria-hidden="true">ᵢ</span></span>')
  })

  test('wraps single subscripted variables', () => {
    const html = renderProblemMarkdown(markdown, '比较 a_i 和 a_{i+1}')
    expect(html).toContain('<span class="compact-var"><var>a</var><span class="compact-sub" aria-hidden="true">ᵢ</span></span>')
    expect(html).toContain('<span class="compact-var"><var>a</var><span class="compact-sub" aria-hidden="true">ᵢ₊₁</span></span>')
  })

  test('wraps plain ellipsis sequences as one formula', () => {
    const html = renderProblemMarkdown(markdown, '变为 h_l,h_{l+1},...,h_r')
    expect(html).toContain('<span class="compact-math"')
    expect(html).toContain('ₗ₊₁')
  })

  test('does not merge plain capital letters into compact sequences', () => {
    const html = renderProblemMarkdown(
      markdown,
      '我们将选择 X 个两两不同的点 v_1, v_2, ..., v_X。一旦这些点被确定，每个参与者将移动到离他传送距离最近的 v_1, ..., v_X 中的一个点。'
    )

    expect([...html.matchAll(/aria-label="([^"]+)"/g)].map(match => match[1])).toEqual([
      'v_1, v_2, ..., v_X',
      'v_1, ..., v_X'
    ])
    expect(html).toContain('<span class="compact-var"><var>v</var><span class="compact-sub" aria-hidden="true">₁</span></span>')
    expect(html).toContain('<span class="compact-var"><var>v</var><span class="compact-sub" aria-hidden="true">ₓ</span></span>')
  })

  test('does not leak compact markers for plain numeric ellipsis', () => {
    const html = renderProblemMarkdown(markdown, '对于 i=1,2,...,n，求出答案。')

    expect(html).not.toContain('AIOI_COMPACT')
    expect(html).not.toContain('AIOICM')
    expect(html).toContain('class="katex"')
  })

  test('wraps symbolic comparison chains', () => {
    expect(normalizeProblemMath('对于所有 1≤i≤r')).toBe('对于所有 $1≤i≤r$')
  })

  test('wraps command expressions with limits and following atoms', () => {
    expect(normalizeProblemMath('最大化 \\sum_{i=1}^{r-l+1} h_i')).toBe('最大化 $\\sum_{i=1}^{r-l+1} h_i$')
  })

  test('renders explicit simple subscript math compactly', () => {
    const html = renderProblemMarkdown(markdown, '给定数组 $a_1,a_2,\\ldots,a_n$')

    expect(html).toContain('<span class="compact-math"')
    expect(html).toContain('ₙ')
    expect(html).not.toContain('class="katex"')
  })

  test('wraps imported bare latex command expressions', () => {
    const html = renderProblemMarkdown(
      markdown,
      '权值为 \\max_{i=1}^{n-1} (a_{p_i} \\oplus a_{p_{i+1}})，其中 ⊕ 为异或运算。'
    )

    expect(html).toContain('class="katex"')
    expect(html).not.toContain('katex-error')
    expect(html).toContain('⊕')
  })
})
