import { effectScope, ref, watch, nextTick } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { it, vi, expect, afterEach } from 'vitest'
import { usePipeline } from '../composables/usePipeline'
import App from '../App.vue'
import type { Descriptor } from '../types'
const append: Descriptor = { id: 'append', name: 'Append', description: 'Add text', parameters: [{ id: 'text', label: 'Text to append', type: 'string', defaultValue: '!', required: true, options: [], min: null, max: null, help: '' }] }

afterEach(() => { vi.restoreAllMocks(); vi.unstubAllGlobals(); vi.useRealTimers() })

it('publishes one pipeline edit and renders only the affected step', async () => {
 const scope = effectScope()
 const editor = scope.run(() => usePipeline(ref([append])))!
 for (let i = 0; i < 30; i++) editor.add(append)
 let notifications = 0
 scope.run(() => watch(editor.pipeline, () => { notifications++ }, { deep: true, flush: 'sync' }))
 editor.move(0, 1)
 const moveNotifications = notifications
 notifications = 0
 editor.updateParameter(0, 'text', '!')
 const unchangedParameterNotifications = notifications
 scope.stop()
 vi.useFakeTimers()
 vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => [append] }))
 const updates: Record<string, number> = {}
 const wrapper = mount(App, { global: { mixins: [{ updated() { const name = (this.$options as { __name?: string }).__name ?? 'unknown'; updates[name] = (updates[name] ?? 0) + 1 } }] } })
 await flushPromises()
 for (let i = 0; i < 30; i++) await wrapper.get('[aria-label="Add Append"]').trigger('click')
 await nextTick()
 for (const name of Object.keys(updates)) updates[name] = 0
 await wrapper.get('#step-0-text').setValue('changed')
 await nextTick()
 expect(moveNotifications).toBe(1)
 expect(unchangedParameterNotifications).toBe(0)
 expect(updates.PipelineStep).toBe(1)
 expect(updates.TransformationCatalog).toBe(0)
 wrapper.unmount(); vi.unstubAllGlobals(); vi.useRealTimers()
})

it('coalesces tooltip layout reads to one frame and cancels pending work on unmount', async () => {
 let frame: FrameRequestCallback | undefined
 const raf = vi.fn((callback: FrameRequestCallback) => { frame = callback; return 7 })
 const cancel = vi.fn()
 vi.stubGlobal('requestAnimationFrame', raf)
 vi.stubGlobal('cancelAnimationFrame', cancel)
 const { default: InfoTooltip } = await import('../components/InfoTooltip.vue')
 const rect = vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(function (this: HTMLElement) {
  const info = this.classList.contains('transformation-info')
  return { x: 200, y: 300, left: 200, top: 300, right: info ? 236 : 400, bottom: info ? 336 : 360, width: info ? 36 : 200, height: info ? 36 : 60, toJSON() {} }
 })
 const wrapper = mount(InfoTooltip, { props: { label: 'Info', description: 'Description' } })
 await wrapper.get('button').trigger('pointerenter'); await nextTick()
 rect.mockClear()
 for (let i = 0; i < 20; i++) window.dispatchEvent(new Event('scroll'))
 expect(raf).toHaveBeenCalledTimes(1)
 expect(rect).not.toHaveBeenCalled()
 frame!(0)
 expect(rect.mock.calls.length).toBeLessThanOrEqual(2)
 for (let i = 0; i < 20; i++) window.dispatchEvent(new Event('scroll'))
 expect(raf).toHaveBeenCalledTimes(2)
 wrapper.unmount()
 expect(cancel).toHaveBeenCalledWith(7)
 rect.mockRestore()
})
