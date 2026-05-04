import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

import CodeHighlightPanel from './CodeHighlightPanel.vue'

describe('CodeHighlightPanel', () => {
  it('keeps the full source visible and scrolls to the highlighted line', async () => {
    const sourceCode = Array.from({ length: 30 }, (_value, index) => `line ${index + 1}`).join('\n')
    const scrollIntoView = vi.fn()
    window.HTMLElement.prototype.scrollIntoView = scrollIntoView

    const wrapper = mount(CodeHighlightPanel, {
      props: {
        sourceCode,
        highlightedLines: [20],
      },
    })
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('1line 1')
    expect(wrapper.text()).toContain('20line 20')
    expect(wrapper.text()).toContain('30line 30')
    expect(scrollIntoView).toHaveBeenCalledWith({ block: 'center', inline: 'nearest' })
  })
})
