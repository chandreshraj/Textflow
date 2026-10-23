<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ArrowRight, ArrowUp, ArrowDown, Plus, X, Play, Copy, Check, Search, Layers, Braces, RotateCcw, ChevronDown, Sparkles } from 'lucide-vue-next'
import ParameterField from './components/ParameterField.vue'
import type { Descriptor, Step, Result, ApiError } from './types'
const catalog = ref<Descriptor[]>([]), pipeline = ref<Step[]>([]), input = ref('Fake Python')
const query = ref(''), result = ref<Result | null>(null), error = ref<ApiError | null>(null)
const loading = ref(false), discovering = ref(true), copied = ref(false), expanded = ref<number | null>(null)
let nextKey = 0, revision = 0
const available = computed(() => catalog.value.filter(t => `${t.name} ${t.description}`.toLowerCase().includes(query.value.toLowerCase())))
const descriptor = (id: string) => catalog.value.find(t => t.id === id)
function add(t: Descriptor) { pipeline.value.push({ key: nextKey++, transformation: t.id, parameters: Object.fromEntries(t.parameters.map(p => [p.id, p.defaultValue])) }) }
function move(index: number, direction: number) { const [step] = pipeline.value.splice(index, 1); if (step) pipeline.value.splice(index + direction, 0, step) }
function example() {
 pipeline.value = []
 const replace = catalog.value.find(t => t.id === 'replace'), append = catalog.value.find(t => t.id === 'append')
 if (replace && append) { add(replace); add(append) }
 input.value = 'Fake Python'
}
watch([input, pipeline], () => { revision++; result.value = null; error.value = null; copied.value = false }, { deep: true, flush: 'sync' })
async function discover() {
 discovering.value = true; error.value = null
 try { const response = await fetch('/api/transformations'); if (!response.ok) throw new Error('Cannot load transformations. Check that the backend is running.'); catalog.value = await response.json(); if (!pipeline.value.length) example() }
 catch (e) { error.value = { message: e instanceof Error ? e.message : 'Could not connect to the API.' } }
 finally { discovering.value = false }
}
async function run() {
 const current = revision; loading.value = true; error.value = null; result.value = null
 try {
  const response = await fetch('/api/transform', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ input: input.value, pipeline: pipeline.value.map(({ transformation, parameters }) => ({ transformation, parameters })) }) })
  const body = await response.json()
  if (current !== revision) return
  if (!response.ok) error.value = { message: body.message || 'Transformation failed.', step: body.step, parameter: body.parameter }
  else result.value = body
 } catch { if (current === revision) error.value = { message: 'Unable to reach the server. Check your connection and try again.' } }
 finally { loading.value = false }
}
async function copy() { if (!result.value) return; try { await navigator.clipboard.writeText(result.value.output); copied.value = true } catch { error.value = { message: 'Clipboard unavailable. Select and copy the output manually.' } } }
onMounted(discover)
</script>

<template>
 <div class="studio">
  <header class="topbar"><a class="brand" href="/" aria-label="Textflow home"><span class="brand-icon"><Braces :size="21" /></span>textflow<span class="brand-dot">.</span></a><span class="top-divider"></span><span class="workspace-name">String transformation studio</span><span class="local-badge"><span></span> Local workspace</span></header>
  <main>
   <div class="page-heading"><div><div class="eyebrow">LESS REPETITION. MORE FLOW.</div><h1>Your text, transformed.</h1><p>Build a sequence. Shape your text. No code needed.</p></div><button class="button secondary" @click="example" :disabled="discovering"><RotateCcw :size="15" /> Load example</button></div>
   <div class="workspace">
    <aside class="catalog"><div class="panel-heading"><span class="small-icon"><Layers :size="17" /></span><h2>Transformations</h2><span class="count">{{ catalog.length }}</span></div><p class="panel-description">Add a building block to your pipeline.</p><div class="search"><Search :size="15" /><input v-model="query" aria-label="Search transformations" placeholder="Find a transformation…"></div>
     <p v-if="discovering" class="empty-note" role="status">Connecting to the library…</p>
     <button v-for="t in available" :key="t.id" class="catalog-item" @click="add(t)" :aria-label="'Add ' + t.name"><span class="transform-symbol">{{ t.name.substring(0, 1) }}<span>↗</span></span><span class="catalog-copy"><strong>{{ t.name }}</strong><small>{{ t.description }}</small></span><Plus class="add-icon" :size="17" /></button>
     <p v-if="!discovering && !available.length" class="empty-note">No transformations found.</p>
     <div class="library-note"><span class="status-dot"></span><div><strong>Powered by your backend</strong><p>New transformations appear here automatically.</p></div></div>
    </aside>
    <section class="flow-area" aria-label="Transformation workspace">
     <div class="editor-card input-card"><div class="editor-heading"><h2><span class="section-number">01</span> Input text</h2><span class="muted">Start with something</span></div><textarea v-model="input" aria-label="Input text" placeholder="Type or paste your text here…" maxlength="100000" spellcheck="false"></textarea><div class="editor-footer"><span>Plain text</span><span>{{ input.length.toLocaleString() }} characters</span></div></div>
     <div class="flow-connector"><span></span><ArrowDown :size="15" /><span></span></div>
     <section class="pipeline-card"><div class="pipeline-heading"><h2><span class="section-number">02</span> Transformation pipeline <span class="count">{{ pipeline.length }}</span></h2><button class="text-button" @click="pipeline = []" :disabled="!pipeline.length">Clear all</button></div>
      <p class="pipeline-hint">Runs from top to bottom. Each step picks up where the last left off.</p>
      <div v-if="!pipeline.length" class="empty-pipeline"><Layers :size="26" /><strong>A little possibility, a blank pipeline.</strong><span>Choose a transformation from the library to get started.</span></div>
      <div v-for="(step, i) in pipeline" :key="step.key" class="step" :class="{ 'step-invalid': error?.step === i }">
       <div class="step-heading"><span class="step-index">{{ String(i + 1).padStart(2, '0') }}</span><h3>{{ descriptor(step.transformation)?.name }}</h3><div class="step-actions"><button :disabled="i === 0" @click="move(i, -1)" :aria-label="'Move step ' + (i + 1) + ' up'"><ArrowUp :size="14" /></button><button :disabled="i === pipeline.length - 1" @click="move(i, 1)" :aria-label="'Move step ' + (i + 1) + ' down'"><ArrowDown :size="14" /></button><span></span><button @click="pipeline.splice(i, 1)" :aria-label="'Remove step ' + (i + 1)"><X :size="15" /></button></div></div>
       <div class="parameters"><ParameterField v-for="p in descriptor(step.transformation)?.parameters" :key="p.id" :parameter="p" v-model="step.parameters[p.id]!" :field-id="`step-${step.key}-${p.id}`" :error="error?.step === i && error?.parameter === p.id ? error.message : undefined" /><p v-if="!descriptor(step.transformation)?.parameters.length" class="no-parameters">Ready to go. No configuration needed.</p></div>
       <p v-if="error?.step === i && !descriptor(step.transformation)?.parameters.some(p => p.id === error?.parameter)" class="field-error">{{ error.message }}</p>
      </div>
      <div class="pipeline-bottom"><span><Sparkles :size="14" /> {{ pipeline.length ? `${pipeline.length} steps, one smooth flow` : 'Make it your own' }}</span><button class="button primary" @click="run" :disabled="loading || discovering || !catalog.length"><Play :size="14" :fill="loading ? 'none' : 'currentColor'" />{{ loading ? 'Transforming…' : 'Run pipeline' }}<ArrowRight :size="16" /></button></div>
     </section>
     <div class="flow-connector"><span></span><ArrowDown :size="15" /><span></span></div>
     <section class="editor-card output-card"><div class="editor-heading"><h2><span class="section-number">03</span> Output <span v-if="result" class="success-tag"><Check :size="12" /> Complete</span></h2><button class="text-button copy" :disabled="!result" @click="copy"><Check v-if="copied" :size="14" /><Copy v-else :size="14" />{{ copied ? 'Copied' : 'Copy' }}</button></div><pre v-if="result" tabindex="0" aria-label="Output text">{{ result.output || '(empty output)' }}</pre><div v-else class="output-placeholder"><span class="output-placeholder-icon"><Braces :size="22" /></span><p>Your next version starts here.</p><small>Run your pipeline to see the result.</small></div><div class="editor-footer"><span v-if="result" role="status">Pipeline completed successfully</span><span v-else>Ready when you are</span><span>{{ result ? result.output.length.toLocaleString() + ' characters' : '—' }}</span></div></section>
     <section v-if="result?.steps.length" class="trace"><h2>Behind the flow <span class="muted">Step-by-step results</span></h2><div v-for="s in result.steps" :key="s.index"><button @click="expanded = expanded === s.index ? null : s.index" :aria-expanded="expanded === s.index"><Check :size="14" /><span>{{ s.index + 1 }}. {{ descriptor(s.transformation)?.name }}</span><small>{{ s.output.length }} chars</small><ChevronDown :size="15" /></button><pre v-if="expanded === s.index">{{ s.output || '(empty output)' }}</pre></div></section>
     <div v-if="error" class="error-banner" role="alert">{{ error.step != null ? `Step ${error.step + 1}: ` : '' }}{{ error.message }} <button v-if="!catalog.length" @click="discover">Retry connection</button></div>
    </section>
   </div>
   <footer><span><Braces :size="14" /> Built for the way you think.</span><span>Vue + TypeScript <span class="footer-dot">·</span> Java transformation engine</span></footer>
  </main>
 </div>
</template>
