import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

import ExportStatusCard from './ExportStatusCard.vue'

vi.mock('../api/pasApi', () => ({
  downloadExportVideo: vi.fn(),
}))

describe('ExportStatusCard', () => {
  it('shows a readable warning when TTS falls back to silent export', () => {
    const wrapper = mount(ExportStatusCard, {
      props: {
        exportTask: {
          id: 101,
          generationTaskId: 42,
          status: 'COMPLETED',
          progress: 100,
          videoUrl: '/api/export-tasks/101/download',
          subtitleUrl: '101/101.srt',
          audioUrl: null,
          errorMessage: null,
          warningMessage: 'TTS_FAILED_FALLBACK_TO_SILENT',
          creditsFrozen: 18,
          creditsCharged: 10,
          createdAt: '2026-04-22T10:00:00Z',
          updatedAt: '2026-04-22T10:02:00Z',
        },
      },
    })

    expect(wrapper.text()).toContain('语音生成失败，已导出静音视频')
  })
})
