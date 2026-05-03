import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import AuthPanel from './AuthPanel.vue'

describe('AuthPanel', () => {
  it('disables submit and shows loading label while busy', () => {
    const wrapper = mount(AuthPanel, {
      props: {
        busy: true,
        errorMessage: '',
        successMessage: '',
      },
    })

    expect(wrapper.find('button[type="submit"]').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('处理中')
  })
})
