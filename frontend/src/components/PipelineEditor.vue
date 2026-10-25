<script setup lang="ts">
import { computed } from "vue";
import { Layers, Sparkles } from "lucide-vue-next";
import PipelineStep from "./PipelineStep.vue";
import type { ApiError, Descriptor, Step, Value } from "../types";
const props = defineProps<{
  pipeline: Step[];
  catalog: Descriptor[];
  error: ApiError | null;
  loading: boolean;
  discovering: boolean;
}>();
const descriptors = computed(() => new Map(props.catalog.map(t => [t.id, t])));
const emit = defineEmits<{
  clear: [];
  move: [index: number, direction: -1 | 1];
  remove: [index: number];
  updateParameter: [index: number, id: string, value: Value];
}>();
</script>

<template>
  <section class="pipeline-card">
    <div class="pipeline-heading">
      <h2>
        <span class="section-number">02</span> Transformation pipeline
        <span class="count">{{ pipeline.length }}</span>
      </h2>
      <button
        class="text-button"
        @click="emit('clear')"
        :disabled="!pipeline.length"
      >
        Clear all
      </button>
    </div>
    <p class="pipeline-hint">
      Runs from top to bottom. Each step picks up where the last left off.
    </p>
    <div v-if="!pipeline.length" class="empty-pipeline">
      <Layers :size="26" /><strong
        >A little possibility, a blank pipeline.</strong
      ><span>Choose a transformation from the library to get started.</span>
    </div>
    <PipelineStep
      v-for="(step, i) in pipeline"
      :key="step.key"
      :step="step"
      :index="i"
      :last="i === pipeline.length - 1"
      :transformation="descriptors.get(step.transformation)"
      :error="error?.step === i ? error : null"
      @move="emit('move', i, $event)"
      @remove="emit('remove', i)"
      @update-parameter="(id, value) => emit('updateParameter', i, id, value)"
    />
    <div class="pipeline-bottom">
      <span
        ><Sparkles :size="14" />
        {{
          pipeline.length
            ? `${pipeline.length} steps, one smooth flow`
            : "Make it your own"
        }}</span
      >
      <span class="live-status" role="status">{{
        discovering
          ? "Connecting…"
          : loading
            ? "Updating output…"
            : "Output updates automatically"
      }}</span>
    </div>
  </section>
</template>
