import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import App from './App.vue'
import * as api from './api/pasApi'

vi.mock('./api/pasApi', () => ({
  clearStoredToken: vi.fn(),
  createGenerationTask: vi.fn(),
  getGenerationTask: vi.fn(),
  createExportTask: vi.fn(),
  getExportTask: vi.fn(),
  login: vi.fn(),
  me: vi.fn(),
  register: vi.fn(),
  setStoredToken: vi.fn(),
}))

const mockUser = {
  id: 1,
  username: 'teacher',
  creditsBalance: 300,
  frozenCreditsBalance: 0,
  status: 'ACTIVE',
}

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
    vi.mocked(api.me).mockResolvedValue(mockUser)
  })

  async function mountAuthenticatedApp() {
    const wrapper = mount(App)
    await flushPromises()
    return wrapper
  }

  it('restores the logged-in user on mount', async () => {
    const wrapper = await mountAuthenticatedApp()

    expect(api.me).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('teacher')
    expect(wrapper.text()).toContain('Credits: 300')
  })

  it('clears a stale token when auth restore fails with auth expiry', async () => {
    vi.mocked(api.me).mockRejectedValue(Object.assign(
      new Error('Login expired. Please sign in again.'),
      { name: 'AuthExpiredError', status: 403 },
    ))

    const wrapper = mount(App)
    await flushPromises()

    expect(api.clearStoredToken).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('Login')
  })

  it('keeps stored token when auth restore fails for a non-auth reason', async () => {
    vi.mocked(api.me).mockRejectedValue(new Error('Network down'))

    const wrapper = mount(App)
    await flushPromises()

    expect(api.clearStoredToken).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Unable to restore your session. Please try signing in again.')
  })

  it('returns to login when a protected action rejects with auth expiry', async () => {
    vi.mocked(api.createGenerationTask).mockRejectedValue(Object.assign(
      new Error('Login expired. Please sign in again.'),
      { name: 'AuthExpiredError', status: 403 },
    ))

    const wrapper = await mountAuthenticatedApp()
    await wrapper.find('textarea').setValue('class Demo {}')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(api.clearStoredToken).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('Login expired. Please sign in again.')
    expect(wrapper.text()).toContain('Login')
  })

  it('logs in and stores the JWT token', async () => {
    vi.mocked(api.me).mockRejectedValue(new Error('Auth check failed'))
    vi.mocked(api.login).mockResolvedValue({
      token: 'jwt-token',
      user: mockUser,
    })

    const wrapper = mount(App)
    await flushPromises()

    await wrapper.find('input[placeholder="Username"]').setValue('teacher')
    await wrapper.find('input[placeholder="Password"]').setValue('secret-pass')
    await wrapper.find('.auth-form').trigger('submit')
    await flushPromises()

    expect(api.login).toHaveBeenCalledWith('teacher', 'secret-pass')
    expect(api.setStoredToken).toHaveBeenCalledWith('jwt-token')
    expect(wrapper.text()).toContain('teacher')
  })

  it('submits registration from the auth panel', async () => {
    vi.mocked(api.me).mockRejectedValue(new Error('Auth check failed'))
    vi.mocked(api.register).mockResolvedValue(mockUser)

    const wrapper = mount(App)
    await flushPromises()

    await wrapper.find('.auth-toggle').trigger('click')
    await wrapper.find('input[placeholder="Username"]').setValue('new-user')
    await wrapper.find('input[placeholder="Password"]').setValue('secret-pass')
    await wrapper.find('.auth-form').trigger('submit')
    await flushPromises()

    expect(api.register).toHaveBeenCalledWith('new-user', 'secret-pass')
    expect(wrapper.text()).toContain('Registration submitted. Login after it succeeds.')
  })

  it('logs out and clears the stored token', async () => {
    const wrapper = await mountAuthenticatedApp()

    await wrapper.find('.user-bar button').trigger('click')

    expect(api.clearStoredToken).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('Login')
  })

  it('starts over without wiping the editor source', async () => {
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

    const wrapper = await mountAuthenticatedApp()
    await wrapper.find('textarea').setValue('public class MergeSort {}')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    await wrapper.find('[data-start-over-button]').trigger('click')

    expect((wrapper.find('textarea').element as HTMLTextAreaElement).value).toContain('MergeSort')
    expect(wrapper.find('[data-export-button]').exists()).toBe(false)
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

    const wrapper = await mountAuthenticatedApp()

    await wrapper.find('textarea').setValue('public class QuickSort {}')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('QUICK_SORT')
    expect(wrapper.text()).toContain('Compare')
  })

  it('shows a readable error when generation is rejected', async () => {
    vi.mocked(api.createGenerationTask).mockRejectedValue(new Error('Unsupported algorithm or low confidence'))

    const wrapper = await mountAuthenticatedApp()

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

    const wrapper = await mountAuthenticatedApp()
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

    const wrapper = await mountAuthenticatedApp()
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Generation status')
    expect(wrapper.text()).toContain('ANALYZING')
    expect(wrapper.find('.visualization-stage').exists()).toBe(false)
  })

  it('allows starting over while generation is in progress', async () => {
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

    const wrapper = await mountAuthenticatedApp()
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    await wrapper.find('[data-start-over-button]').trigger('click')

    expect(wrapper.find('textarea').exists()).toBe(true)
    expect(wrapper.find('.generation-status-card').exists()).toBe(false)
  })

  it('returns to a recoverable editor state when initial generation refresh fails', async () => {
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
    vi.mocked(api.getGenerationTask).mockRejectedValue(new Error('Generation polling failed'))

    const wrapper = await mountAuthenticatedApp()
    await wrapper.find('textarea').setValue('class Demo {}')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('textarea').exists()).toBe(true)
    expect(wrapper.text()).toContain('Generation polling failed')
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

    const wrapper = await mountAuthenticatedApp()
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    await wrapper.find('[data-export-button]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Export status')
    expect(wrapper.text()).toContain('COMPLETED')
    expect(wrapper.find('[data-download-link]').attributes('href')).toContain('/api/export-tasks/101/download')
  })

  it('disables export while an export task is active', async () => {
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
      status: 'PROCESSING',
      progress: 45,
      videoUrl: null,
      subtitleUrl: null,
      audioUrl: null,
      errorMessage: null,
      creditsFrozen: 18,
      creditsCharged: null,
      createdAt: '2026-04-11T16:00:00Z',
      updatedAt: '2026-04-11T16:00:10Z',
    })

    const wrapper = await mountAuthenticatedApp()
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    await wrapper.find('[data-export-button]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-export-button]').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('Export is running. You can stay on this page while Pas finishes the video.')
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

    const wrapper = await mountAuthenticatedApp()
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    await wrapper.find('[data-export-button]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('VIDEO_COMPOSE_FAILED')
  })

  it('shows retry export action after export failure', async () => {
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
    vi.mocked(api.createExportTask)
      .mockResolvedValueOnce({
        id: 101,
        generationTaskId: 1,
        status: 'PENDING',
        progress: 0,
        creditsFrozen: 18,
      })
      .mockResolvedValueOnce({
        id: 102,
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
      errorMessage: 'Video composition failed.',
      creditsFrozen: 18,
      creditsCharged: null,
      createdAt: '2026-04-11T16:00:00Z',
      updatedAt: '2026-04-11T16:00:10Z',
    })

    const wrapper = await mountAuthenticatedApp()
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    await wrapper.find('[data-export-button]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-retry-export-button]').exists()).toBe(true)
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

    const wrapper = await mountAuthenticatedApp()
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

    const wrapper = await mountAuthenticatedApp()
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
