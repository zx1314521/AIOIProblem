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
const subscriptPattern = new RegExp(String.raw`${MATH_PREFIX}(${SUBSCRIPTED_SYMBOL})(?![\p{L}\p{N}_$])`, 'gu')
const commandPattern = new RegExp(String.raw`${MATH_PREFIX}\\(ldots|cdots|leq|geq|neq|times|cdot|sum|sqrt)(?![A-Za-z])`, 'gu')

export function normalizeProblemMath(source: string) {
  const converted = source
    .replace(/\\dots/g, '\\ldots')
    .replace(/\\\(([\s\S]+?)\\\)/g, (_, expression: string) => `$${expression}$`)
    .replace(/\\\[([\s\S]+?)\\\]/g, (_, expression: string) => `\n$$\n${expression}\n$$\n`)

  return converted
    .split(/(\$\$[\s\S]*?\$\$|\$[^$\n]+\$)/g)
    .map(part => part.startsWith('$') ? part : wrapLooseMath(part))
    .join('')
}

function wrapLooseMath(source: string) {
  const placeholders: string[] = []
  const protect = (expression: string) => {
    const marker = `@@AIOI_MATH_${placeholders.length}@@`
    placeholders.push(`$${expression.trim()}$`)
    return marker
  }

  const wrapped = source
    .replace(sequencePattern, (_, prefix: string, expression: string) => `${prefix}${protect(expression)}`)
    .replace(commandExpressionPattern, (_, prefix: string, expression: string) => `${prefix}${protect(expression)}`)
    .replace(relationPattern, (_, prefix: string, expression: string) => `${prefix}${protect(expression)}`)
    .replace(subscriptPattern, (_, prefix: string, expression: string) => `${prefix}${protect(expression)}`)
    .replace(commandPattern, (_, prefix: string, command: string) => `${prefix}${protect(`\\${command}`)}`)

  return placeholders.reduce((text, value, index) => text.replace(`@@AIOI_MATH_${index}@@`, value), wrapped)
}
