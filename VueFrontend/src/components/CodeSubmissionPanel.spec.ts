import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import CodeSubmissionPanel from './CodeSubmissionPanel.vue'

describe('CodeSubmissionPanel', () => {
  it('shows inline validation for blank input', async () => {
    const wrapper = mount(CodeSubmissionPanel, {
      props: {
        modelValue: '   ',
        busy: false,
        errorMessage: '',
      },
    })

    await wrapper.find('form').trigger('submit')

    expect(wrapper.text()).toContain('Paste Java code before generating a walkthrough.')
  })
})
