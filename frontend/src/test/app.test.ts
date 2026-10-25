import { mount, flushPromises, enableAutoUnmount } from '@vue/test-utils'
import { describe, it, expect, vi, afterEach, beforeEach } from 'vitest'
import App from '../App.vue'
import ParameterField from '../components/ParameterField.vue'
import { PREVIEW_DELAY_MS } from '../composables/useLiveExecution'
import type { Parameter } from '../types'

const parameter: Parameter = { id: 'text', label: 'Text', type: 'string', required: true, defaultValue: '', min: null, max: null, options: [], help: '' }
const metadata = [{ id: 'new-server-operation', name: 'Server extension', description: 'Added only on the server', parameters: [parameter] }]
const success = (output: string) => ({ ok: true, json: async () => ({ output, steps: [] }) })
const preview = () => vi.advanceTimersByTimeAsync(PREVIEW_DELAY_MS)
enableAutoUnmount(afterEach)
beforeEach(() => vi.useFakeTimers())
afterEach(() => { vi.useRealTimers(); vi.unstubAllGlobals() })

describe('Automatic metadata-driven workspace', () => {
 it('executes an unknown operation automatically after adding and configuring it', async () => {
  const fetch = vi.fn().mockResolvedValueOnce({ ok: true, json: async () => metadata }).mockResolvedValue(success('Processed'))
  vi.stubGlobal('fetch', fetch)
  const wrapper = mount(App); await flushPromises()
  await wrapper.get('[aria-label="Add Server extension"]').trigger('click')
  await wrapper.get('#step-0-text').setValue('custom')
  expect(wrapper.text()).not.toContain('Run pipeline')
  await preview()
  expect(wrapper.get('[aria-label="Output text"]').text()).toBe('Processed')
  expect(JSON.parse(fetch.mock.calls[1]![1].body).pipeline).toEqual([{ transformation: 'new-server-operation', parameters: { text: 'custom' } }])
  await wrapper.get('[aria-label="Input text"]').setValue('changed')
  expect(wrapper.get('[aria-label="Output text"]').text()).toBe('Processed')
  expect(wrapper.text()).toContain('Previous result')
  expect(wrapper.get('button.copy').attributes('disabled')).toBeDefined()
 })

 it('recomputes after reordering, removing, and clearing steps', async () => {
  const fetch = vi.fn().mockResolvedValueOnce({ ok: true, json: async () => [...metadata, { ...metadata[0], id: 'second', name: 'Second' }] }).mockResolvedValue(success('Preview'))
  vi.stubGlobal('fetch', fetch)
  const wrapper = mount(App); await flushPromises()
  await wrapper.get('[aria-label="Add Server extension"]').trigger('click')
  await wrapper.get('[aria-label="Add Second"]').trigger('click'); await preview()
  await wrapper.get('[aria-label="Move step 2 up"]').trigger('click'); await preview()
  expect(JSON.parse(fetch.mock.calls[2]![1].body).pipeline.map((s: { transformation: string }) => s.transformation)).toEqual(['second', 'new-server-operation'])
  await wrapper.get('[aria-label="Remove step 1"]').trigger('click'); await preview()
  expect(JSON.parse(fetch.mock.calls[3]![1].body).pipeline).toHaveLength(1)
  await wrapper.findAll('button').find(b => b.text() === 'Clear all')!.trigger('click'); await preview()
  expect(fetch).toHaveBeenCalledTimes(4)
  expect(wrapper.get('[aria-label="Output text"]').text()).toBe('Fake Python')
 })

 it('debounces rapid input edits into one request containing the latest values', async () => {
  const fetch = vi.fn().mockResolvedValueOnce({ ok: true, json: async () => metadata }).mockResolvedValue(success('latest'))
  vi.stubGlobal('fetch', fetch)
  const wrapper = mount(App); await flushPromises()
  await wrapper.get('[aria-label="Add Server extension"]').trigger('click')
  await wrapper.get('[aria-label="Input text"]').setValue('a')
  await wrapper.get('[aria-label="Input text"]').setValue('ab')
  await wrapper.get('[aria-label="Input text"]').setValue('abc')
  await vi.advanceTimersByTimeAsync(PREVIEW_DELAY_MS - 1)
  expect(fetch).toHaveBeenCalledTimes(1)
  await vi.advanceTimersByTimeAsync(1)
  expect(fetch).toHaveBeenCalledTimes(2)
  expect(JSON.parse(fetch.mock.calls[1]![1].body).input).toBe('abc')
 })

 it('cancels obsolete requests and ignores late results without blocking newer previews', async () => {
  let finish!: (value: unknown) => void
  const fetch = vi.fn().mockResolvedValueOnce({ ok: true, json: async () => metadata })
   .mockImplementationOnce(() => new Promise(resolve => { finish = resolve }))
   .mockResolvedValue(success('fresh result'))
  vi.stubGlobal('fetch', fetch)
  const wrapper = mount(App); await flushPromises()
  await wrapper.get('[aria-label="Add Server extension"]').trigger('click'); await preview()
  await wrapper.get('[aria-label="Input text"]').setValue('new input')
  expect(fetch.mock.calls[1]![1].signal.aborted).toBe(true)
  await preview()
  expect(wrapper.get('[aria-label="Output text"]').text()).toBe('fresh result')
  finish(success('stale result')); await flushPromises()
  expect(wrapper.get('[aria-label="Output text"]').text()).toBe('fresh result')
 })

 it('keeps field errors and automatically recovers after a correction', async () => {
  const fetch = vi.fn().mockResolvedValueOnce({ ok: true, json: async () => metadata })
   .mockResolvedValueOnce({ ok: false, json: async () => ({ message: 'Invalid suffix', step: 0, parameter: 'text' }) })
   .mockResolvedValue(success('Fixed'))
  vi.stubGlobal('fetch', fetch)
  const wrapper = mount(App); await flushPromises()
  await wrapper.get('[aria-label="Add Server extension"]').trigger('click')
  await wrapper.get('#step-0-text').setValue('keep this'); await preview()
  expect(wrapper.get('[role="alert"]').text()).toContain('Step 1: Invalid suffix')
  expect(wrapper.get('#step-0-text').attributes('aria-invalid')).toBe('true')
  expect((wrapper.get('#step-0-text').element as HTMLInputElement).value).toBe('keep this')
  await wrapper.get('#step-0-text').setValue('fixed'); await preview()
  expect(wrapper.find('[role="alert"]').exists()).toBe(false)
  expect(wrapper.get('[aria-label="Output text"]').text()).toBe('Fixed')
 })

 it('cancels pending previews when the workspace unmounts', async () => {
  const fetch = vi.fn().mockResolvedValue({ ok: true, json: async () => metadata })
  vi.stubGlobal('fetch', fetch)
  const wrapper = mount(App); await flushPromises()
  await wrapper.get('[aria-label="Add Server extension"]').trigger('click'); wrapper.unmount()
  await preview()
  expect(fetch).toHaveBeenCalledTimes(1)
 })

 it('aborts active previews on unmount', async () => {
  const fetch = vi.fn().mockResolvedValueOnce({ ok: true, json: async () => metadata }).mockImplementationOnce(() => new Promise(() => {}))
  vi.stubGlobal('fetch', fetch)
  const wrapper = mount(App); await flushPromises()
  await wrapper.get('[aria-label="Add Server extension"]').trigger('click'); await preview(); wrapper.unmount()
  expect(fetch.mock.calls[1]![1].signal.aborted).toBe(true)
 })

 it('shows a discovery failure without attempting execution', async () => {
  const fetch = vi.fn().mockRejectedValue(new Error('Offline'))
  vi.stubGlobal('fetch', fetch)
  const wrapper = mount(App); await flushPromises(); await preview()
  expect(wrapper.get('[role="alert"]').text()).toContain('Offline')
  expect(fetch).toHaveBeenCalledTimes(1)
 })

 it('preserves false, zero and empty string in generic fields', async () => {
  const numeric = mount(ParameterField, { props: { parameter: { ...parameter, type: 'number' }, modelValue: 1, fieldId: 'n' } })
  await numeric.get('input').setValue('0'); expect(numeric.emitted('update:modelValue')?.[0]).toEqual([0])
  const text = mount(ParameterField, { props: { parameter, modelValue: 'x', fieldId: 't' } })
  await text.get('input').setValue(''); expect(text.emitted('update:modelValue')?.[0]).toEqual([''])
  const boolean = mount(ParameterField, { props: { parameter: { ...parameter, type: 'boolean' }, modelValue: true, fieldId: 'b' } })
  await boolean.get('input').setValue(false); expect(boolean.emitted('update:modelValue')?.[0]).toEqual([false])
 })
})

it('keeps the previous preview and trace stable while updating or invalid, with copying disabled', async () => {
 const fetch = vi.fn().mockResolvedValueOnce({ ok: true, json: async () => metadata })
  .mockResolvedValueOnce({ ok: true, json: async () => ({ output: 'Last good result', steps: [{ index: 0, transformation: 'new-server-operation', output: 'Last good result' }] }) })
  .mockResolvedValueOnce({ ok: false, json: async () => ({ message: 'Invalid value', step: 0, parameter: 'text' }) })
  .mockResolvedValue(success('Current result'))
 vi.stubGlobal('fetch', fetch)
 const wrapper = mount(App); await flushPromises()
 await wrapper.get('[aria-label="Add Server extension"]').trigger('click'); await preview()
 await wrapper.get('.trace button').trigger('click')
 expect(wrapper.get('.trace pre').text()).toBe('Last good result')
 expect(wrapper.get('button.copy').attributes('disabled')).toBeUndefined()
 await wrapper.get('#step-0-text').setValue('invalid')
 expect(wrapper.get('[aria-label="Output text"]').text()).toBe('Last good result')
 expect(wrapper.get('.trace pre').text()).toBe('Last good result')
 expect(wrapper.get('.preview-status').text()).toBe('Updating')
 expect(wrapper.get('button.copy').attributes('disabled')).toBeDefined()
 await preview()
 expect(wrapper.get('.preview-status').text()).toBe('Needs attention')
 expect(wrapper.get('[aria-label="Output text"]').text()).toBe('Last good result')
 expect(wrapper.get('button.copy').attributes('disabled')).toBeDefined()
 await wrapper.get('#step-0-text').setValue('valid'); await preview()
 expect(wrapper.get('[aria-label="Output text"]').text()).toBe('Current result')
 expect(wrapper.get('.preview-status').text()).toBe('Updated')
 expect(wrapper.get('button.copy').attributes('disabled')).toBeUndefined()
 expect(wrapper.find('.previous-label').exists()).toBe(false)
})
