import { mount, flushPromises } from '@vue/test-utils'
import { describe, it, expect, vi, afterEach } from 'vitest'
import App from '../App.vue'
import ParameterField from '../components/ParameterField.vue'
import type { Parameter } from '../types'
const parameter: Parameter = { id: 'text', label: 'Text', type: 'string', required: true, defaultValue: '', min: null, max: null, options: [], help: '' }
const metadata = [{ id: 'new-server-operation', name: 'Server extension', description: 'Added only on the server', parameters: [parameter] }]
afterEach(() => vi.unstubAllGlobals())
describe('Metadata-driven workspace', () => {
 it('renders and executes a previously unknown backend operation', async () => {
  const fetch = vi.fn().mockResolvedValueOnce({ ok: true, json: async () => metadata }).mockResolvedValueOnce({ ok: true, json: async () => ({ output: 'Processed', steps: [] }) })
  vi.stubGlobal('fetch', fetch)
  const wrapper = mount(App); await flushPromises()
  await wrapper.get('[aria-label="Add Server extension"]').trigger('click')
  await wrapper.get('#step-0-text').setValue('custom')
  const run = wrapper.findAll('button').find(b => b.text().includes('Run pipeline'))!
  await run.trigger('click'); await flushPromises()
  expect(wrapper.get('[aria-label="Output text"]').text()).toBe('Processed')
  expect(JSON.parse(fetch.mock.calls[1]![1].body).pipeline).toEqual([{ transformation: 'new-server-operation', parameters: { text: 'custom' } }])
  await wrapper.get('[aria-label="Input text"]').setValue('changed')
  expect(wrapper.find('[aria-label="Output text"]').exists()).toBe(false)
 })
 it('reorders and removes pipeline steps', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => [...metadata, { ...metadata[0], id: 'second', name: 'Second' }] }))
  const wrapper = mount(App); await flushPromises()
  await wrapper.get('[aria-label="Add Server extension"]').trigger('click')
  await wrapper.get('[aria-label="Add Second"]').trigger('click')
  await wrapper.get('[aria-label="Move step 2 up"]').trigger('click')
  expect(wrapper.findAll('h3').map(x => x.text())).toEqual(['Second', 'Server extension'])
  await wrapper.get('[aria-label="Remove step 1"]').trigger('click')
  expect(wrapper.findAll('h3')).toHaveLength(1)
 })
 it('shows a connection failure and retry', async () => {
  vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('Offline')))
  const wrapper = mount(App); await flushPromises()
  expect(wrapper.get('[role="alert"]').text()).toContain('Offline')
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
