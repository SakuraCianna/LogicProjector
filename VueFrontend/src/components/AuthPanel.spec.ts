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

  it('requires password confirmation before registering', async () => {
    const wrapper = mount(AuthPanel)

    await wrapper.find('.auth-toggle').trigger('click')
    await wrapper.find('input[placeholder="用户名"]').setValue('sakura')
    await wrapper.find('input[placeholder="密码"]').setValue('secret-pass')
    await wrapper.find('input[placeholder="确认密码"]').setValue('different-pass')
    await wrapper.find('.auth-form').trigger('submit')

    expect(wrapper.emitted('register')).toBeUndefined()
    expect(wrapper.text()).toContain('两次输入的密码不一致')
  })

  it('switches back to login mode after successful registration', async () => {
    const wrapper = mount(AuthPanel, {
      props: {
        successMessage: '',
      },
    })

    await wrapper.find('.auth-toggle').trigger('click')
    expect(wrapper.text()).toContain('创建账号')

    await wrapper.setProps({ successMessage: '注册成功，请登录' })

    expect(wrapper.text()).toContain('欢迎回来')
    expect(wrapper.text()).toContain('登录')
    expect(wrapper.find('input[placeholder="确认密码"]').exists()).toBe(false)
  })
})
