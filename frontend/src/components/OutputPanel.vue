<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { Check, Copy, Braces, LoaderCircle, AlertCircle } from "lucide-vue-next";
import type { Result } from "../types";

const props = defineProps<{
  result: Result | null;
  loading: boolean;
  stale: boolean;
  hasError: boolean;
}>();
const emit = defineEmits<{ error: [message: string] }>();
const copied = ref(false);
const canCopy = computed(() => !!props.result && !props.loading && !props.stale);
const status = computed(() => props.loading ? "Updating" : props.hasError ? "Needs attention" : props.stale ? "Previous preview" : props.result ? "Updated" : "Ready");
const notice = computed(() => {
  if (props.result && props.stale) return props.loading
    ? "Showing the previous result while your changes are processed."
    : "Showing the previous result. Fix the current error to update it.";
  if (props.loading) return "Your result will appear here shortly.";
  if (props.hasError) return "Check the highlighted step to update your preview.";
  return props.result ? "Results reflect your current pipeline." : "Output updates automatically as you edit.";
});
watch([() => props.result, () => props.stale], () => { copied.value = false; });

async function copy() {
  if (!canCopy.value || !props.result) return;
  try {
    await navigator.clipboard.writeText(props.result.output);
    copied.value = true;
  } catch {
    emit("error", "Clipboard unavailable. Select and copy the output manually.");
  }
}
</script>

<template>
  <section class="editor-card output-card" :class="{ 'preview-stale': stale }" :aria-busy="loading">
    <div class="editor-heading">
      <h2><span class="section-number">03</span> Output</h2>
      <button class="text-button copy" :disabled="!canCopy" @click="copy">
        <Check v-if="copied" :size="16" /><Copy v-else :size="16" />{{ copied ? "Copied" : "Copy" }}
      </button>
    </div>
    <div class="preview-status-row">
      <span class="preview-status" :class="{ 'is-updating': loading, 'is-warning': !loading && (stale || hasError) }" role="status">
        <LoaderCircle v-if="loading" class="loading-icon" :size="15" />
        <AlertCircle v-else-if="stale || hasError" :size="15" />
        <Check v-else :size="15" />{{ status }}
      </span>
      <span v-if="stale && result" class="previous-label">Previous result</span>
    </div>
    <div class="output-content">
      <pre v-if="result" tabindex="0" aria-label="Output text">{{ result.output || "(empty output)" }}</pre>
      <div v-else class="output-placeholder">
        <span class="output-placeholder-icon"><Braces :size="24" /></span>
        <p>{{ loading ? "Transforming your text…" : hasError ? "Preview unavailable" : "Your next version starts here." }}</p>
      </div>
    </div>
    <p class="preview-notice">{{ notice }}</p>
    <div class="editor-footer">
      <span>{{ loading ? "Updating output…" : hasError ? "Needs attention" : result && !stale ? "Pipeline completed successfully" : "Waiting for a current result" }}</span>
      <span>{{ result ? result.output.length.toLocaleString() + " characters" : "—" }}</span>
    </div>
  </section>
</template>
