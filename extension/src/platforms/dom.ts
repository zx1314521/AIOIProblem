export function textOf(element: Element | null | undefined) {
  return (element?.textContent ?? '').replace(/\s+/g, ' ').trim()
}

export function blockText(element: Element | null | undefined) {
  return (element?.textContent ?? '')
    .replace(/\r/g, '')
    .replace(/[ \t]+\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .replace(/[ \t]{2,}/g, ' ')
    .trim()
}

export function absoluteUrl(href: string, currentUrl: string) {
  return new URL(href, currentUrl).toString()
}
