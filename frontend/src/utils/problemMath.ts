import MarkdownIt from 'markdown-it'
import { katex } from '@mdit/plugin-katex'

const SUBSCRIPTED_SYMBOL = String.raw`[A-Za-z][A-Za-z0-9]*_(?:\{[^}\n]+\}|[A-Za-z0-9]+)`
const MATH_COMMAND = String.raw`\\[A-Za-z]+(?:_\{[^}\n]+\}|_[A-Za-z0-9]+)?(?:\^\{[^}\n]+\}|\^[A-Za-z0-9]+)?`
const MATH_ATOM = String.raw`(?:${MATH_COMMAND}|${SUBSCRIPTED_SYMBOL}|[A-Za-z]|\d+)`
const ELLIPSIS = String.raw`(?:\\(?:ldots|cdots)|\.\.\.|…)`
const RELATION = String.raw`(?:\\(?:leq|geq|neq|lt|gt)|<=|>=|≤|≥|≠|<|>|=)`
const MATH_PREFIX = String.raw`(^|[^\p{L}\p{N}_$\\])`

const sequencePattern = new RegExp(
  String.raw`${MATH_PREFIX}((?:${MATH_ATOM}|${ELLIPSIS})(?:\s*[,，、]\s*(?:${MATH_ATOM}|${ELLIPSIS})){2,})(?![\p{L}\p{N}_$])`,
  'gu'
)

const relationPattern = new RegExp(
  String.raw`${MATH_PREFIX}([（(]?\s*${MATH_ATOM}\s*${RELATION}\s*${MATH_ATOM}(?:\s*${RELATION}\s*${MATH_ATOM})?\s*[）)]?)`,
  'gu'
)

const commandExpressionPattern = new RegExp(
  String.raw`${MATH_PREFIX}(${MATH_COMMAND}(?:\s+${MATH_ATOM})?)(?![\p{L}\p{N}_$])`,
  'gu'
)
const bareLatexExpressionPattern = new RegExp(
  String.raw`${MATH_PREFIX}(\\(?:max|min|sum|prod|frac|sqrt|oplus|otimes|times|cdot|leq|geq|neq)(?:[^，。；：、\n]|\\[A-Za-z]+)+)`,
  'gu'
)
const subscriptPattern = new RegExp(String.raw`${MATH_PREFIX}(${SUBSCRIPTED_SYMBOL})(?![\p{L}\p{N}_$])`, 'gu')
const commandPattern = new RegExp(String.raw`${MATH_PREFIX}\\(ldots|cdots|leq|geq|neq|times|cdot|sum|sqrt)(?![A-Za-z])`, 'gu')
const SUBSCRIPT_CHARACTERS: Record<string, string> = {
  '0': '₀',
  '1': '₁',
  '2': '₂',
  '3': '₃',
  '4': '₄',
  '5': '₅',
  '6': '₆',
  '7': '₇',
  '8': '₈',
  '9': '₉',
  '+': '₊',
  '-': '₋',
  '=': '₌',
  '(': '₍',
  ')': '₎',
  a: 'ₐ',
  e: 'ₑ',
  h: 'ₕ',
  i: 'ᵢ',
  j: 'ⱼ',
  k: 'ₖ',
  l: 'ₗ',
  m: 'ₘ',
  n: 'ₙ',
  o: 'ₒ',
  p: 'ₚ',
  r: 'ᵣ',
  s: 'ₛ',
  t: 'ₜ',
  u: 'ᵤ',
  v: 'ᵥ',
  x: 'ₓ'
}

export function createProblemMarkdown() {
  return new MarkdownIt({ breaks: true, linkify: true }).use(katex, {
    delimiters: 'all',
    throwOnError: false
  })
}

export function normalizeProblemMath(source: string) {
  const converted = source
    .replace(/\\dots/g, '\\ldots')
    .replace(/\\\(([\s\S]+?)\\\)/g, (_, expression: string) => `$${expression}$`)
    .replace(/\\\[([\s\S]+?)\\\]/g, (_, expression: string) => `\n$$\n${expression}\n$$\n`)

  return converted
    .split(/(\$\$[\s\S]*?\$\$|\$[^$\n]+\$)/g)
    .map(part => normalizeMathSegment(part))
    .join('')
}

export function renderProblemMarkdown(markdown: MarkdownIt, source: string) {
  return renderCompactMath(markdown.render(normalizeProblemMath(source)))
}

function normalizeMathSegment(part: string) {
  if (!part.startsWith('$')) {
    return wrapLooseMath(part)
  }
  if (part.startsWith('$$')) {
    return part
  }
  const expression = part.slice(1, -1).trim()
  return shouldRenderCompact(expression) ? compactMathMarker(expression) : part
}

function wrapLooseMath(source: string) {
  const placeholders: string[] = []
  const protect = (expression: string) => {
    const marker = `@@AIOI_MATH_${placeholders.length}@@`
    placeholders.push(`$${expression.trim()}$`)
    return marker
  }
  const protectCompact = (expression: string) => compactMathMarker(expression.trim())

  const wrapped = source
    .replace(sequencePattern, (_, prefix: string, expression: string) => `${prefix}${expression.includes('_') ? protectCompact(expression) : protect(expression)}`)
    .replace(bareLatexExpressionPattern, (_, prefix: string, expression: string) => `${prefix}${protect(expression)}`)
    .replace(commandExpressionPattern, (_, prefix: string, expression: string) => `${prefix}${protect(expression)}`)
    .replace(relationPattern, (_, prefix: string, expression: string) => `${prefix}${expression.includes('_') ? protectCompact(expression) : protect(expression)}`)
    .replace(subscriptPattern, (_, prefix: string, expression: string) => `${prefix}${protectCompact(expression)}`)
    .replace(commandPattern, (_, prefix: string, command: string) => `${prefix}${protect(`\\${command}`)}`)

  return placeholders.reduce((text, value, index) => text.replace(`@@AIOI_MATH_${index}@@`, value), wrapped)
}

function shouldRenderCompact(expression: string) {
  if (!expression.includes('_')) {
    return false
  }
  const withoutTextCommands = expression.replace(/\\(?:ldots|cdots)/g, '')
  if (/\\[A-Za-z]+/.test(withoutTextCommands)) {
    return false
  }
  return /^[A-Za-z0-9_{}\s,，、.+\-<>=()…]+$/.test(withoutTextCommands)
}

function compactMathMarker(expression: string) {
  const bytes = new TextEncoder().encode(expression)
  const encoded = [...bytes].map(byte => byte.toString(16).padStart(2, '0')).join('')
  return `@@AIOI_COMPACT_${encoded}@@`
}

function renderCompactMath(html: string) {
  return html.replace(/@@AIOI_COMPACT_([0-9a-f]+)@@/g, (_, encoded: string) => compactMathHtml(decodeHex(encoded)))
}

function decodeHex(encoded: string) {
  const bytes = encoded.match(/.{1,2}/g)?.map(value => Number.parseInt(value, 16)) ?? []
  return new TextDecoder().decode(new Uint8Array(bytes))
}

function compactMathHtml(expression: string) {
  const normalized = expression
    .replace(/\\ldots|\\cdots|\.\.\.|…/g, '...')
    .replace(/\\leq/g, '≤')
    .replace(/\\geq/g, '≥')
    .replace(/\\neq/g, '≠')
    .replace(/\\lt/g, '<')
    .replace(/\\gt/g, '>')
  const content = escapeHtml(normalized).replace(
    /([A-Za-z][A-Za-z0-9]*)_(?:\{([^}<]+)\}|([A-Za-z0-9]+))/g,
    (_, base: string, bracedSubscript: string | undefined, plainSubscript: string | undefined) =>
      `<span class="compact-var"><var>${base}</var>${compactSubscriptHtml(bracedSubscript ?? plainSubscript ?? '')}</span>`
  )
  return `<span class="compact-math" aria-label="${escapeAttribute(normalized)}">${content}</span>`
}

function compactSubscriptHtml(value: string) {
  const compact = [...value].map(character => SUBSCRIPT_CHARACTERS[character] ?? SUBSCRIPT_CHARACTERS[character.toLowerCase()]).join('')
  if (compact.length === value.length) {
    return `<span class="compact-sub" aria-hidden="true">${compact}</span>`
  }
  return `<sub>${escapeHtml(value)}</sub>`
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

function escapeAttribute(value: string) {
  return escapeHtml(value).replace(/"/g, '&quot;')
}
