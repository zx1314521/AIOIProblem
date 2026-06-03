import { describe, expect, test } from 'vitest'
import { normalizeProblemMath } from './problemMath'

describe('normalizeProblemMath', () => {
  test('wraps common OI subscript sequences as one formula', () => {
    expect(normalizeProblemMath('a_1, a_2, \\dots, a_n')).toBe('$a_1, a_2, \\ldots, a_n$')
  })

  test('wraps chained bounds as one formula', () => {
    expect(normalizeProblemMath('范围 (1\\leq a_i\\leq n)')).toBe('范围 $(1\\leq a_i\\leq n)$')
  })

  test('wraps single subscripted variables', () => {
    expect(normalizeProblemMath('比较 a_i 和 a_{i+1}')).toBe('比较 $a_i$ 和 $a_{i+1}$')
  })

  test('wraps plain ellipsis sequences as one formula', () => {
    expect(normalizeProblemMath('变为 h_l,h_{l+1},...,h_r')).toBe('变为 $h_l,h_{l+1},...,h_r$')
  })

  test('wraps symbolic comparison chains', () => {
    expect(normalizeProblemMath('对于所有 1≤i≤r')).toBe('对于所有 $1≤i≤r$')
  })

  test('wraps command expressions with limits and following atoms', () => {
    expect(normalizeProblemMath('最大化 \\sum_{i=1}^{r-l+1} h_i')).toBe('最大化 $\\sum_{i=1}^{r-l+1} h_i$')
  })
})
