<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { Check, ChevronDown } from "lucide-vue-next";
import type { Descriptor, Result } from "../types";
const props = defineProps<{ result: Result; catalog: Descriptor[]; stale: boolean }>();
const expanded = ref<number | null>(null);
const descriptors = computed(() => new Map(props.catalog.map(t => [t.id, t])));
watch(
  () => props.result,
  () => {
    expanded.value = null;
  },
);
</script>

<template>
  <section class="trace">
    <h2>Behind the flow <span class="muted">{{ stale ? "Previous results" : "Step-by-step results" }}</span></h2>
    <div v-for="s in result.steps" :key="s.index">
      <button
        @click="expanded = expanded === s.index ? null : s.index"
        :aria-expanded="expanded === s.index"
      >
        <Check :size="14" /><span
          >{{ s.index + 1 }}. {{ descriptors.get(s.transformation)?.name }}</span
        ><small>{{ s.output.length }} chars</small><ChevronDown :size="15" />
      </button>
      <pre v-if="expanded === s.index">{{ s.output || "(empty output)" }}</pre>
    </div>
  </section>
</template>
