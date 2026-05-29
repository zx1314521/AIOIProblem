import { describe, expect, test } from 'vitest'
import { buildHashAppUrl, normalizeLocalHttpUrl } from './urls'

describe('popup URL helpers', () => {
  test('keeps API and app addresses separate for local development', () => {
    expect(normalizeLocalHttpUrl('http://localhost:8080/', 'http://localhost:8080', 'API')).toBe('http://localhost:8080')
    expect(buildHashAppUrl('http://localhost:5173/', '/oj-imports')).toBe('http://localhost:5173/#/oj-imports')
  })

  test('defaults quick links to the frontend dev server', () => {
    expect(buildHashAppUrl(undefined, 'problems')).toBe('http://localhost:5173/#/problems')
  })

  test('can hand the extension binding to the frontend app', () => {
    const url = buildHashAppUrl('http://localhost:5173', '/oj-imports', {
      token: 'jwt-token',
      userId: 7,
      username: 'Zwzy'
    })

    expect(url).toContain('http://localhost:5173/#/oj-imports?')
    expect(url).toContain('aioiExtensionToken=jwt-token')
    expect(decodeURIComponent(url)).toContain('"username":"Zwzy"')
  })

  test('rejects non-local addresses in the first extension version', () => {
    expect(() => normalizeLocalHttpUrl('https://example.com', 'http://localhost:8080', 'API')).toThrow(/localhost/)
  })
})
