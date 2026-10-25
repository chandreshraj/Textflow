<script setup lang="ts">
import { ref } from "vue";
import { ArrowDown, Braces, RotateCcw, Eye } from "lucide-vue-next";
import StudioHeader from "./components/StudioHeader.vue";
import TransformationCatalog from "./components/TransformationCatalog.vue";
import TextInput from "./components/TextInput.vue";
import PipelineEditor from "./components/PipelineEditor.vue";
import OutputPanel from "./components/OutputPanel.vue";
import ExecutionTrace from "./components/ExecutionTrace.vue";
import { useWorkspace } from "./composables/useWorkspace";

const {
  catalog, pipeline, input, result, stale, error, loading, discovering,
  add, move, remove, clear, updateParameter, example, discover, reportError,
} = useWorkspace();
const outputRegion = ref<HTMLElement | null>(null);
function viewOutput() {
  outputRegion.value?.focus({ preventScroll: true });
  outputRegion.value?.scrollIntoView({ block: "start" });
}
</script>

<template>
  <div class="studio">
    <StudioHeader />
    <main>
      <div v-if="error" class="error-banner" role="alert">
        {{ error.step != null ? `Step ${error.step + 1}: ` : "" }}{{ error.message }}
        <button v-if="!catalog.length" @click="discover">Retry connection</button>
      </div>
      <div class="page-heading">
        <div>
          <div class="eyebrow">LESS REPETITION. MORE FLOW.</div>
          <h1>Your text, <span class="heading-accent">transformed.</span></h1>
          <p>Build a sequence. Shape your text. No code needed.</p>
        </div>
        <button class="button secondary" @click="example" :disabled="discovering">
          <RotateCcw :size="15" /> Load example
        </button>
      </div>
      <div class="workspace">
        <TransformationCatalog :catalog="catalog" :discovering="discovering" @add="add" />
        <section class="flow-area" aria-label="Transformation workspace">
          <div class="editing-column">
          <TextInput v-model="input" />
          <div class="flow-connector"><span></span><ArrowDown :size="15" /><span></span></div>
          <PipelineEditor
            :pipeline="pipeline" :catalog="catalog" :error="error"
            :loading="loading" :discovering="discovering"
            @clear="clear" @move="move" @remove="remove"
            @update-parameter="updateParameter"
          />
          </div>
          <aside ref="outputRegion" id="live-output" class="preview-column" aria-label="Live preview" tabindex="-1">
            <OutputPanel :result="result" :loading="loading" :stale="stale" :has-error="!!error" @error="reportError" />
            <ExecutionTrace v-if="result?.steps.length" :result="result" :catalog="catalog" :stale="stale" />
          </aside>
        </section>
      </div>
      <button class="mobile-output-shortcut button primary" aria-label="View output" @click="viewOutput">
        <Eye :size="18" /> View output
      </button>
      <footer>
        <span><Braces :size="14" /> Built for the way you think.</span>
        <span>Vue + TypeScript <span class="footer-dot">·</span> Java transformation engine</span>
      </footer>
    </main>
  </div>
</template>
