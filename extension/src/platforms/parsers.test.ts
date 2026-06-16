import { describe, expect, test } from 'vitest'
import { parseCurrentPage } from './registry'

function doc(html: string) {
  return new DOMParser().parseFromString(html, 'text/html')
}

describe('OJ page parsers', () => {
  test('parses Codeforces problem pages', () => {
    const result = parseCurrentPage(doc(`
      <div class="problem-statement">
        <div class="header"><div class="title">D. Median Splits</div></div>
        <div><p>You are given an array. Split it.</p></div>
      </div>
    `), 'https://codeforces.com/contest/2229/problem/D')

    expect(result.kind).toBe('problem')
    expect(result.items).toMatchObject([
      {
        platform: 'CODEFORCES',
        sourceId: 'CF2229D',
        title: 'D. Median Splits',
        passed: false
      }
    ])
    expect(result.items[0].statement).toContain('You are given an array')
  })

  test('parses Codeforces problemset problem pages', () => {
    const result = parseCurrentPage(doc(`
      <div class="problem-statement">
        <div class="header"><div class="title">A. Theatre Square</div></div>
        <div><p>Find the number of flagstones.</p></div>
      </div>
    `), 'https://codeforces.com/problemset/problem/1/A')

    expect(result.kind).toBe('problem')
    expect(result.items[0]).toMatchObject({
      platform: 'CODEFORCES',
      sourceId: 'CF1A',
      title: 'A. Theatre Square'
    })
    expect(result.items[0].statement).toContain('Find the number of flagstones')
  })

  test('parses Codeforces gym problem pages', () => {
    const result = parseCurrentPage(doc(`
      <div class="problem-statement">
        <div class="header"><div class="title">B. Binary Search</div></div>
        <div><p>Recover the hidden index.</p></div>
      </div>
    `), 'https://codeforces.com/gym/104573/problem/B')

    expect(result.kind).toBe('problem')
    expect(result.items[0]).toMatchObject({
      platform: 'CODEFORCES',
      sourceId: 'CF104573B',
      title: 'B. Binary Search'
    })
    expect(result.items[0].statement).toContain('Recover the hidden index')
  })

  test('parses AtCoder problem pages', () => {
    const result = parseCurrentPage(doc(`
      <span class="h2">A - 2^N</span>
      <div id="task-statement"><section><h3>Problem Statement</h3><p>Find the answer.</p></section></div>
    `), 'https://atcoder.jp/contests/abc400/tasks/abc400_a')

    expect(result.kind).toBe('problem')
    expect(result.items[0]).toMatchObject({
      platform: 'ATCODER',
      sourceId: 'ATABC400_ABC400_A',
      title: 'A - 2^N'
    })
    expect(result.items[0].statement).toContain('Find the answer')
  })

  test('parses Luogu problem pages', () => {
    const result = parseCurrentPage(doc(`
      <h1>P1001 A+B Problem</h1>
      <article><h2>题目背景</h2><p>输入两个整数。</p></article>
    `), 'https://www.luogu.com.cn/problem/P1001')

    expect(result.kind).toBe('problem')
    expect(result.items[0]).toMatchObject({
      platform: 'LUOGU',
      sourceId: 'LGP1001',
      title: 'P1001 A+B Problem'
    })
    expect(result.items[0].statement).toContain('输入两个整数')
  })

  test('parses Nowcoder problem pages', () => {
    const result = parseCurrentPage(doc(`
      <h1>NC14320 小白月赛</h1>
      <div class="subject-content"><p>输出方案数。</p></div>
    `), 'https://ac.nowcoder.com/acm/problem/14320')

    expect(result.kind).toBe('problem')
    expect(result.items[0]).toMatchObject({
      platform: 'NOWCODER',
      sourceId: 'NC14320',
      title: 'NC14320 小白月赛'
    })
    expect(result.items[0].statement).toContain('输出方案数')
  })

  test('parses VJudge direct problem pages', () => {
    const result = parseCurrentPage(doc(`
      <div id="prob-title">ICP Company <span class="origin">TLX - compfest-7-scpc-final-D</span></div>
      <div id="frame-description-container">
        <h3>Description</h3>
        <p>Anda bekerja di International Computer-based Problem-solving Company.</p>
        <h3>Input</h3>
        <p>Baris pertama terdiri dari T.</p>
      </div>
    `), 'https://vjudge.net/problem/TLX-compfest-7-scpc-final-D')

    expect(result.kind).toBe('problem')
    expect(result.items[0]).toMatchObject({
      platform: 'VJUDGE',
      sourceId: 'VJTLX-compfest-7-scpc-final-D',
      title: 'ICP Company TLX - compfest-7-scpc-final-D',
      passed: false
    })
    expect(result.items[0].statement).toContain('International Computer-based')
  })

  test('parses VJudge contest problem pages', () => {
    const result = parseCurrentPage(doc(`
      <div id="prob-title-contest">D - 股票交易</div>
      <div id="frame-description-container">
        <h3>Description</h3>
        <p>最近 lxhgww 又迷上了投资股票。</p>
      </div>
    `), 'https://vjudge.net/contest/822927#problem/D')

    expect(result.kind).toBe('problem')
    expect(result.items[0]).toMatchObject({
      platform: 'VJUDGE',
      sourceId: 'VJ822927D',
      title: 'D - 股票交易',
      url: 'https://vjudge.net/contest/822927#problem/D'
    })
    expect(result.items[0].statement).toContain('投资股票')
  })

  test('parses VJudge contest overview problem links', () => {
    const result = parseCurrentPage(doc(`
      <table id="contest-problems">
        <tr><th>#</th><th>标题</th></tr>
        <tr><td>A</td><td><a href="#problem/A">笛卡尔树</a></td><td>已解决</td></tr>
        <tr><td>D</td><td><a href="#problem/D">股票交易</a></td><td>1 / 1</td></tr>
      </table>
    `), 'https://vjudge.net/contest/822927#overview')

    expect(result.kind).toBe('submissions')
    expect(result.items).toEqual([
      {
        platform: 'VJUDGE',
        sourceId: 'VJ822927A',
        title: 'A - 笛卡尔树',
        url: 'https://vjudge.net/contest/822927#problem/A',
        passed: true
      },
      {
        platform: 'VJUDGE',
        sourceId: 'VJ822927D',
        title: 'D - 股票交易',
        url: 'https://vjudge.net/contest/822927#problem/D',
        passed: false
      }
    ])
  })

  test('deduplicates submission pages and marks accepted problems as passed', () => {
    const result = parseCurrentPage(doc(`
      <table class="status-frame-datatable">
        <tr><th>Problem</th><th>Verdict</th></tr>
        <tr>
          <td><a href="/contest/2229/problem/D">D - Median Splits</a></td>
          <td><span>Wrong answer</span></td>
        </tr>
        <tr>
          <td><a href="/contest/2229/problem/D">D - Median Splits</a></td>
          <td><span>Accepted</span></td>
        </tr>
      </table>
    `), 'https://codeforces.com/contest/2229/my')

    expect(result.kind).toBe('submissions')
    expect(result.items).toEqual([
      {
        platform: 'CODEFORCES',
        sourceId: 'CF2229D',
        title: 'D - Median Splits',
        url: 'https://codeforces.com/contest/2229/problem/D',
        passed: true
      }
    ])
  })

  test('parses Codeforces problemset links in submission pages', () => {
    const result = parseCurrentPage(doc(`
      <table class="status-frame-datatable">
        <tr>
          <td><a href="/problemset/problem/1/A">A - Theatre Square</a></td>
          <td><span>Accepted</span></td>
        </tr>
      </table>
    `), 'https://codeforces.com/submissions/tourist')

    expect(result.kind).toBe('submissions')
    expect(result.items).toEqual([
      {
        platform: 'CODEFORCES',
        sourceId: 'CF1A',
        title: 'A - Theatre Square',
        url: 'https://codeforces.com/problemset/problem/1/A',
        passed: true
      }
    ])
  })

  test('parses Codeforces gym links in submission pages', () => {
    const result = parseCurrentPage(doc(`
      <table class="status-frame-datatable">
        <tr>
          <td><a href="/gym/104573/problem/B">B - Binary Search</a></td>
          <td><span>Accepted</span></td>
        </tr>
      </table>
    `), 'https://codeforces.com/submissions/team')

    expect(result.kind).toBe('submissions')
    expect(result.items).toEqual([
      {
        platform: 'CODEFORCES',
        sourceId: 'CF104573B',
        title: 'B - Binary Search',
        url: 'https://codeforces.com/gym/104573/problem/B',
        passed: true
      }
    ])
  })

  test('marks Luogu Chinese accepted submissions as passed', () => {
    const result = parseCurrentPage(doc(`
      <div class="record">
        <a href="/problem/P1001">P1001 A+B Problem</a>
        <span>通过</span>
      </div>
    `), 'https://www.luogu.com.cn/record/list?user=1')

    expect(result.kind).toBe('submissions')
    expect(result.items).toEqual([
      {
        platform: 'LUOGU',
        sourceId: 'LGP1001',
        title: 'P1001 A+B Problem',
        url: 'https://www.luogu.com.cn/problem/P1001',
        passed: true
      }
    ])
  })

  test('marks Nowcoder Chinese accepted submissions as passed', () => {
    const result = parseCurrentPage(doc(`
      <div class="status-item">
        <a href="/acm/problem/14320">NC14320 小白月赛</a>
        <span>答案正确</span>
      </div>
    `), 'https://ac.nowcoder.com/acm/contest/status')

    expect(result.kind).toBe('submissions')
    expect(result.items).toEqual([
      {
        platform: 'NOWCODER',
        sourceId: 'NC14320',
        title: 'NC14320 小白月赛',
        url: 'https://ac.nowcoder.com/acm/problem/14320',
        passed: true
      }
    ])
  })
})
