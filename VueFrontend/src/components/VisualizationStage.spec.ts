import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'

import VisualizationStage from './VisualizationStage.vue'

describe('VisualizationStage', () => {
  it('renders array bars for the active step', () => {
    const wrapper = mount(VisualizationStage, {
      props: {
        step: {
          title: 'Compare 0 and 1',
          narration: 'Comparing adjacent values',
          arrayState: [5, 1, 4],
          activeIndices: [0, 1],
          highlightedLines: [3, 4],
        },
      },
    })

    expect(wrapper.findAll('[data-array-value]')).toHaveLength(3)
  })
})
