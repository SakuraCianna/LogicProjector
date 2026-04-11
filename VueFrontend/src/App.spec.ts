import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import App from './App.vue'
import * as api from './api/pasApi'

vi.mock('./api/pasApi', () => ({
  createGenerationTask: vi.fn(),
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
})
