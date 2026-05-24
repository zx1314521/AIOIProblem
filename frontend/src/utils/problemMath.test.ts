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
})
