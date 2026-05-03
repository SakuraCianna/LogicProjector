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

    expect(wrapper.text()).toContain('请先粘贴 Java / C / C++ 代码，再生成讲解')
  })

  it('lets the user choose C++ as the source language', async () => {
    const wrapper = mount(CodeSubmissionPanel, {
      props: {
        modelValue: 'void quickSort(int a[], int low, int high) {}',
        language: 'java',
        busy: false,
        errorMessage: '',
      },
    })

    await wrapper.find('select').setValue('cpp')

    expect(wrapper.emitted('update:language')?.[0]).toEqual(['cpp'])
  })
})
