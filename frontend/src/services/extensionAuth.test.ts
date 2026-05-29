import { beforeEach, describe, expect, test } from 'vitest'
import { authState, clearAuth } from './auth'
import { consumeExtensionAuthFromLocation, parseExtensionAuthHash } from './extensionAuth'

describe('extension auth handoff', () => {
  beforeEach(() => {
    clearAuth()
    window.history.replaceState(null, '', '/')
  })

  test('parses plugin token from hash query and removes sensitive params', () => {
    const rawUser = encodeURIComponent(JSON.stringify({ id: 7, username: 'Zwzy' }))
    const handoff = parseExtensionAuthHash(`#/oj-imports?foo=bar&aioiExtensionToken=jwt&aioiExtensionUser=${rawUser}`)

    expect(handoff?.auth.token).toBe('jwt')
    expect(handoff?.auth.user).toEqual({ id: 7, username: 'Zwzy' })
    expect(handoff?.cleanedHash).toBe('#/oj-imports?foo=bar')
  })

  test('stores extension auth in the web app and cleans the URL', () => {
    const rawUser = encodeURIComponent(JSON.stringify({ id: 7, username: 'Zwzy' }))
    window.history.replaceState(null, '', `/#/oj-imports?aioiExtensionToken=jwt&aioiExtensionUser=${rawUser}`)

    expect(consumeExtensionAuthFromLocation()).toBe(true)
    expect(authState.token).toBe('jwt')
    expect(authState.user?.username).toBe('Zwzy')
    expect(window.location.hash).toBe('#/oj-imports')
  })
})
