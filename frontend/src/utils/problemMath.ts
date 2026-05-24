const SUBSCRIPTED_SYMBOL = String.raw`[A-Za-z][A-Za-z0-9]*_(?:\{[^}\n]+\}|[A-Za-z0-9]+)`
const MATH_ATOM = String.raw`(?:${SUBSCRIPTED_SYMBOL}|[A-Za-z]|\d+)`
const ELLIPSIS_COMMAND = String.raw`\\(?:ldots|cdots)`
const RELATION_COMMAND = String.raw`\\(?:leq|geq|neq|lt|gt)`

const sequencePattern = new RegExp(
  String.raw`(^|[^\w$\\])((?:${SUBSCRIPTED_SYMBOL}|${ELLIPSIS_COMMAND})(?:\s*[,，、]\s*(?:${SUBSCRIPTED_SYMBOL}|${ELLIPSIS_COMMAND}))+)(?![\w$])`,
  'g'
)

const relationPattern = new RegExp(
  String.raw`(^|[^\w$\\])([（(]?\s*${MATH_ATOM}\s*${RELATION_COMMAND}\s*${MATH_ATOM}(?:\s*${RELATION_COMMAND}\s*${MATH_ATOM})?\s*[）)]?)`,
  'g'
)

const subscriptPattern = new RegExp(String.raw`(^|[^\w$\\])(${SUBSCRIPTED_SYMBOL})(?![\w$])`, 'g')
const commandPattern = new RegExp(String.raw`(^|[^\w$\\])\\(ldots|cdots|leq|geq|neq|times|cdot|sum|sqrt)(?![A-Za-z])`, 'g')

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
    .replace(relationPattern, (_, prefix: string, expression: string) => `${prefix}${protect(expression)}`)
    .replace(subscriptPattern, (_, prefix: string, expression: string) => `${prefix}${protect(expression)}`)
    .replace(commandPattern, (_, prefix: string, command: string) => `${prefix}${protect(`\\${command}`)}`)

  return placeholders.reduce((text, value, index) => text.replace(`@@AIOI_MATH_${index}@@`, value), wrapped)
}
