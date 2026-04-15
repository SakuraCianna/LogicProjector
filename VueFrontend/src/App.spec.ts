import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import App from './App.vue'
import * as api from './api/pasApi'

vi.mock('./api/pasApi', () => ({
  createGenerationTask: vi.fn(),
  getGenerationTask: vi.fn(),
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
    vi.useRealTimers()
  })

  it('switches from editor to playback after a successful generation', async () => {
    vi.mocked(api.createGenerationTask).mockResolvedValue({
      id: 1,
      status: 'PENDING',
      language: 'java',
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: null,
      creditsCharged: 0,
    })
    vi.mocked(api.getGenerationTask).mockResolvedValue(mockCompletedTask)

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

  it('shows generation failure message when polling returns failed', async () => {
    vi.mocked(api.createGenerationTask).mockResolvedValue({
      id: 1,
      status: 'PENDING',
      language: 'java',
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: null,
      creditsCharged: 0,
    })
    vi.mocked(api.getGenerationTask).mockResolvedValue({
      id: 1,
      status: 'FAILED',
      language: 'java',
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: 'Unsupported algorithm or low confidence',
      creditsCharged: 0,
    })

    const wrapper = mount(App)
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Unsupported algorithm or low confidence')
  })

  it('shows generation status while queued or processing', async () => {
    vi.mocked(api.createGenerationTask).mockResolvedValue({
      id: 1,
      status: 'PENDING',
      language: 'java',
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: null,
      creditsCharged: 0,
    })
    vi.mocked(api.getGenerationTask).mockResolvedValue({
      id: 1,
      status: 'ANALYZING',
      language: 'java',
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: null,
      creditsCharged: 0,
    })

    const wrapper = mount(App)
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Generation status')
    expect(wrapper.text()).toContain('ANALYZING')
    expect(wrapper.find('.visualization-stage').exists()).toBe(false)
  })

  it('creates and renders export progress after clicking export', async () => {
    vi.mocked(api.createGenerationTask).mockResolvedValue({
      id: 1,
      status: 'PENDING',
      language: 'java',
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: null,
      creditsCharged: 0,
    })
    vi.mocked(api.getGenerationTask).mockResolvedValue(mockCompletedTask)
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
    vi.mocked(api.createGenerationTask).mockResolvedValue({
      id: 1,
      status: 'PENDING',
      language: 'java',
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: null,
      creditsCharged: 0,
    })
    vi.mocked(api.getGenerationTask).mockResolvedValue(mockCompletedTask)
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

  it('auto advances steps while playing and stops at the end', async () => {
    vi.useFakeTimers()
    vi.mocked(api.createGenerationTask).mockResolvedValue({
      id: 1,
      status: 'PENDING',
      language: 'java',
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: null,
      creditsCharged: 0,
    })
    vi.mocked(api.getGenerationTask).mockResolvedValue({
      ...mockCompletedTask,
      visualizationPayload: {
        algorithm: 'QUICK_SORT',
        steps: [
          {
            title: 'Step 1',
            narration: 'First',
            arrayState: [5, 1, 4],
            activeIndices: [0, 1],
            highlightedLines: [3, 4],
          },
          {
            title: 'Step 2',
            narration: 'Second',
            arrayState: [1, 5, 4],
            activeIndices: [1, 2],
            highlightedLines: [5],
          },
        ],
      },
    })

    const wrapper = mount(App)
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Step 1 / 2')
    await wrapper.find('[data-play-toggle]').trigger('click')
    await vi.advanceTimersByTimeAsync(1800)

    expect(wrapper.text()).toContain('Step 2 / 2')
    expect(wrapper.find('[data-play-toggle]').text()).toContain('Play')
  })

  it('allows scrubbing timeline and changing playback speed', async () => {
    vi.useFakeTimers()
    vi.mocked(api.createGenerationTask).mockResolvedValue({
      id: 1,
      status: 'PENDING',
      language: 'java',
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: null,
      creditsCharged: 0,
    })
    vi.mocked(api.getGenerationTask).mockResolvedValue({
      ...mockCompletedTask,
      visualizationPayload: {
        algorithm: 'QUICK_SORT',
        steps: [
          {
            title: 'Step 1',
            narration: 'First',
            arrayState: [5, 1, 4],
            activeIndices: [0, 1],
            highlightedLines: [3, 4],
          },
          {
            title: 'Step 2',
            narration: 'Second',
            arrayState: [1, 5, 4],
            activeIndices: [1, 2],
            highlightedLines: [5],
          },
          {
            title: 'Step 3',
            narration: 'Third',
            arrayState: [1, 4, 5],
            activeIndices: [2],
            highlightedLines: [6],
          },
        ],
      },
    })

    const wrapper = mount(App)
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    await wrapper.find('[data-speed-select]').setValue('2')
    await wrapper.find('[data-step-slider]').setValue('1')
    expect(wrapper.text()).toContain('Step 2')

    await wrapper.find('[data-play-toggle]').trigger('click')
    await vi.advanceTimersByTimeAsync(800)

    expect(wrapper.text()).toContain('Step 3 / 3')
  })
})
