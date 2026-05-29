import { describe, expect, test } from 'vitest'
import { mergeImportResultsWithHistory } from './importStatus'
import type { ImportResult, OjImportHistoryJob } from './importStatus'

describe('mergeImportResultsWithHistory', () => {
  test('uses backend history status and problem ids when available', () => {
    const results: ImportResult[] = [
      { sourceId: 'CF2229D', title: 'Median', status: 'QUEUED' },
      { sourceId: 'ATABC400_A', title: '2^N', status: 'QUEUED' }
    ]
    const history: OjImportHistoryJob[] = [
      {
        id: 1,
        items: [
          {
            sourceId: 'CF2229D',
            title: 'Median',
            status: 'SUCCEEDED',
            problemId: 101,
            errorMessage: ''
          },
          {
            sourceId: 'ATABC400_A',
            title: '2^N',
            status: 'FAILED',
            errorMessage: '题面为空'
          }
        ]
      }
    ]

    expect(mergeImportResultsWithHistory(results, history)).toEqual([
      {
        sourceId: 'CF2229D',
        title: 'Median',
        status: '已入库',
        detail: '题库 #101',
        problemId: 101
      },
      {
        sourceId: 'ATABC400_A',
        title: '2^N',
        status: '失败',
        detail: '题面为空',
        problemId: undefined
      }
    ])
  })
})
