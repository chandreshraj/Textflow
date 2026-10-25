import { effectScope, ref, shallowRef } from 'vue'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { PREVIEW_DELAY_MS, useLiveExecution } from '../composables/useLiveExecution'
import type { Result, Step } from '../types'

const scopes: ReturnType<typeof effectScope>[] = []
const step: Step = { key: 0, transformation: 'append', parameters: { text: 'Hello' } }
const response = (result: Result) => ({ ok: true, json: async () => result })
const advance = () => vi.advanceTimersByTimeAsync(PREVIEW_DELAY_MS)

function workspace(text = '', steps: Step[] = [], discovered = true) {
 const scope = effectScope()
 scopes.push(scope)
 const input = ref(text)
 const pipeline = shallowRef(steps)
 const ready = ref(discovered)
 const preview = scope.run(() => useLiveExecution(input, pipeline, ready))!
 return { input, pipeline, ready, ...preview }
}

beforeEach(() => vi.useFakeTimers())
afterEach(() => {
 scopes.splice(0).forEach(scope => scope.stop())
 vi.useRealTimers()
 vi.unstubAllGlobals()
})

it.each(['', 'Original text', ' \t\n'])('skips execution for an empty pipeline with input %j, including after discovery becomes ready', async text => {
 const fetch = vi.fn()
 vi.stubGlobal('fetch', fetch)
 const editor = workspace(text, [], false)
 editor.ready.value = true
 await advance()
 expect(fetch).not.toHaveBeenCalled()
 expect(editor.result.value).toEqual(text === '' ? null : { output: text, steps: [] })
 expect(editor.loading.value).toBe(false)
 expect(editor.stale.value).toBe(false)
 expect(editor.error.value).toBeNull()
})

it('cancels a pending debounce and restores the original input when the pipeline is cleared', async () => {
 const fetch = vi.fn()
 vi.stubGlobal('fetch', fetch)
 const editor = workspace('pending input', [step])
 editor.pipeline.value = []
 await advance()
 expect(fetch).not.toHaveBeenCalled()
 expect(editor.result.value).toEqual({ output: 'pending input', steps: [] })
 expect(editor.loading.value).toBe(false)
})

it('clears previous output, trace and execution errors when the last step is removed from empty input', async () => {
 const fetch = vi.fn()
  .mockResolvedValueOnce(response({ output: 'Hello', steps: [{ index: 0, transformation: 'append', output: 'Hello' }] }))
  .mockResolvedValueOnce({ ok: false, json: async () => ({ message: 'Invalid value', step: 0, parameter: 'text' }) })
 vi.stubGlobal('fetch', fetch)
 const editor = workspace('', [step])
 await advance()
 expect(editor.result.value?.steps).toHaveLength(1)
 expect(JSON.parse(fetch.mock.calls[0]![1].body)).toEqual({ input: '', pipeline: [{ transformation: 'append', parameters: { text: 'Hello' } }] })
 editor.pipeline.value = [{ ...step, parameters: { text: null } }]
 await advance()
 expect(editor.error.value?.message).toBe('Invalid value')
 editor.pipeline.value = []
 expect(editor.result.value).toBeNull()
 expect(editor.error.value).toBeNull()
 expect(editor.stale.value).toBe(false)
 expect(editor.loading.value).toBe(false)
 await advance()
 expect(fetch).toHaveBeenCalledTimes(2)
})

it('aborts an active request and ignores its late result after the pipeline is cleared', async () => {
 let finish!: (value: ReturnType<typeof response>) => void
 const fetch = vi.fn().mockImplementation(() => new Promise(resolve => { finish = resolve }))
 vi.stubGlobal('fetch', fetch)
 const editor = workspace('pending input', [step])
 await advance()
 editor.pipeline.value = []
 expect(fetch.mock.calls[0]![1].signal.aborted).toBe(true)
 finish(response({ output: 'obsolete output', steps: [] }))
 await advance()
 expect(editor.result.value).toEqual({ output: 'pending input', steps: [] })
 expect(editor.error.value).toBeNull()
 expect(editor.loading.value).toBe(false)
 expect(editor.stale.value).toBe(false)
 expect(fetch).toHaveBeenCalledTimes(1)
})

it('updates input locally without steps and resumes API execution for empty and whitespace input with steps', async () => {
 const fetch = vi.fn().mockResolvedValue(response({ output: 'Preview', steps: [] }))
 vi.stubGlobal('fetch', fetch)
 const editor = workspace()
 editor.input.value = ' \t\n'
 await advance()
 expect(fetch).not.toHaveBeenCalled()
 expect(editor.result.value).toEqual({ output: ' \t\n', steps: [] })
 editor.input.value = ''
 editor.pipeline.value = [step]
 await advance()
 expect(JSON.parse(fetch.mock.calls[0]![1].body)).toEqual({ input: '', pipeline: [{ transformation: 'append', parameters: { text: 'Hello' } }] })
 editor.input.value = ' \t\n'
 await advance()
 expect(JSON.parse(fetch.mock.calls[1]![1].body)).toEqual({ input: ' \t\n', pipeline: [{ transformation: 'append', parameters: { text: 'Hello' } }] })
 expect(fetch).toHaveBeenCalledTimes(2)
})
