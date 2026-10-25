<script setup lang="ts">
import { ArrowUp, ArrowDown, X } from "lucide-vue-next";
import ParameterField from "./ParameterField.vue";
import type { ApiError, Descriptor, Step, Value } from "../types";
defineProps<{
  step: Step;
  index: number;
  last: boolean;
  transformation?: Descriptor;
  error: ApiError | null;
}>();
const emit = defineEmits<{
  move: [direction: -1 | 1];
  remove: [];
  updateParameter: [id: string, value: Value];
}>();
</script>

<template>
  <div class="step" :class="{ 'step-invalid': error?.step === index }">
    <div class="step-heading">
      <span class="step-index">{{ String(index + 1).padStart(2, "0") }}</span>
      <h3>{{ transformation?.name }}</h3>
      <div class="step-actions">
        <button
          :disabled="index === 0"
          @click="emit('move', -1)"
          :aria-label="'Move step ' + (index + 1) + ' up'"
        >
          <ArrowUp :size="14" /></button
        ><button
          :disabled="last"
          @click="emit('move', 1)"
          :aria-label="'Move step ' + (index + 1) + ' down'"
        >
          <ArrowDown :size="14" /></button
        ><span></span
        ><button
          @click="emit('remove')"
          :aria-label="'Remove step ' + (index + 1)"
        >
          <X :size="15" />
        </button>
      </div>
    </div>
    <div class="parameters">
      <ParameterField
        v-for="p in transformation?.parameters"
        :key="p.id"
        :parameter="p"
        :model-value="step.parameters[p.id] ?? null"
        @update:model-value="emit('updateParameter', p.id, $event)"
        :field-id="`step-${step.key}-${p.id}`"
        :error="
          error?.step === index && error?.parameter === p.id
            ? error.message
            : undefined
        "
      />
      <p v-if="!transformation?.parameters.length" class="no-parameters">
        Ready to go. No configuration needed.
      </p>
    </div>
    <p
      v-if="
        error?.step === index &&
        !transformation?.parameters.some((p) => p.id === error?.parameter)
      "
      class="field-error"
    >
      {{ error.message }}
    </p>
  </div>
</template>
