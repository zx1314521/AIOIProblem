import { describe, expect, test } from 'vitest'
import { shouldClearStoredAuth } from './authState'

describe('popup auth state handling', () => {
  test('only clears stored binding for unauthorized responses', () => {
    expect(shouldClearStoredAuth(401)).toBe(true)
    expect(shouldClearStoredAuth(403)).toBe(false)
    expect(shouldClearStoredAuth(500)).toBe(false)
  })
})
