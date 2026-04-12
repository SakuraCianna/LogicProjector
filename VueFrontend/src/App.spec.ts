import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import App from './App.vue'
import * as api from './api/pasApi'

vi.mock('./api/pasApi', () => ({
  createGenerationTask: vi.fn(),
  createExportTask: vi.fn(),
  getExportTask: vi.fn(),
}))

const mockCompletedTask = {
  id: 1,
  status: 'COMPLETED',
  language: 'java',
  detectedAlgorithm: 'QUICK_SORT',
  summary: 'Quick sort picks a pivot and partitions the array.',
  confidenceScore: 0.93,
  visualizationPayload: {
    algorithm: 'QUICK_SORT',
    steps: [
      {
        title: 'Compare',
        narration: 'Compare values around the pivot.',
        arrayState: [5, 1, 4],
        activeIndices: [0, 1],
        highlightedLines: [3, 4],
      },
    ],
  },
  errorMessage: null,
  creditsCharged: 8,
}

describe('App', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('switches from editor to playback after a successful generation', async () => {
    vi.mocked(api.createGenerationTask).mockResolvedValue(mockCompletedTask)

    const wrapper = mount(App)

    await wrapper.find('textarea').setValue('public class QuickSort {}')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('QUICK_SORT')
    expect(wrapper.text()).toContain('Compare')
  })

  it('shows a readable error when generation is rejected', async () => {
    vi.mocked(api.createGenerationTask).mockRejectedValue(new Error('Unsupported algorithm or low confidence'))

    const wrapper = mount(App)

    await wrapper.find('textarea').setValue('class Knapsack {}')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Unsupported algorithm or low confidence')
  })

  it('creates and renders export progress after clicking export', async () => {
    vi.mocked(api.createGenerationTask).mockResolvedValue(mockCompletedTask)
    vi.mocked(api.createExportTask).mockResolvedValue({
      id: 101,
      generationTaskId: 1,
      status: 'PENDING',
      progress: 0,
      creditsFrozen: 18,
    })
    vi.mocked(api.getExportTask).mockResolvedValue({
      id: 101,
      generationTaskId: 1,
      status: 'COMPLETED',
      progress: 100,
      videoUrl: '/api/export-tasks/101/download',
      subtitleUrl: '/files/101.srt',
      audioUrl: '/files/101.mp3',
      errorMessage: null,
      creditsFrozen: 18,
      creditsCharged: 1231,
      createdAt: '2026-04-11T16:00:00Z',
      updatedAt: '2026-04-11T16:00:30Z',
    })

    const wrapper = mount(App)
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    await wrapper.find('[data-export-button]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Export status')
    expect(wrapper.text()).toContain('COMPLETED')
    expect(wrapper.find('[data-download-link]').attributes('href')).toContain('/api/export-tasks/101/download')
  })

  it('shows export failure message when polling returns failed', async () => {
    vi.mocked(api.createGenerationTask).mockResolvedValue(mockCompletedTask)
    vi.mocked(api.createExportTask).mockResolvedValue({
      id: 101,
      generationTaskId: 1,
      status: 'PENDING',
      progress: 0,
      creditsFrozen: 18,
    })
    vi.mocked(api.getExportTask).mockResolvedValue({
      id: 101,
      generationTaskId: 1,
      status: 'FAILED',
      progress: 100,
      videoUrl: null,
      subtitleUrl: null,
      audioUrl: null,
      errorMessage: 'VIDEO_COMPOSE_FAILED',
      creditsFrozen: 18,
      creditsCharged: null,
      createdAt: '2026-04-11T16:00:00Z',
      updatedAt: '2026-04-11T16:00:10Z',
    })

    const wrapper = mount(App)
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    await wrapper.find('[data-export-button]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('VIDEO_COMPOSE_FAILED')
  })
})
