<script setup lang="ts">
import { computed, ref } from "vue";
import { Layers, Search } from "lucide-vue-next";
import TransformationCatalogItem from "./TransformationCatalogItem.vue";
import type { Descriptor } from "../types";
const props = defineProps<{ catalog: Descriptor[]; discovering: boolean }>();
const emit = defineEmits<{ add: [transformation: Descriptor] }>();
const query = ref("");
const searchIndex = computed(() => props.catalog.map(transformation => ({
  transformation,
  text: `${transformation.name} ${transformation.description}`.toLowerCase(),
})));
const available = computed(() => {
  const term = query.value.toLowerCase();
  return searchIndex.value.filter(entry => entry.text.includes(term)).map(entry => entry.transformation);
});
</script>

<template>
  <aside class="catalog">
    <div class="catalog-header">
    <div class="panel-heading">
      <span class="small-icon"><Layers :size="17" /></span>
      <h2>Transformations</h2>
      <span class="count">{{ catalog.length }}</span>
    </div>
    <p class="panel-description">Add a building block to your pipeline.</p>
    <div class="search">
      <Search :size="15" /><input
        v-model="query"
        aria-label="Search transformations"
        placeholder="Find a transformation…"
      />
    </div>
    </div>
    <div class="catalog-list" data-tooltip-boundary role="region" aria-label="Available transformations" tabindex="0">
    <p v-if="discovering" class="empty-note" role="status">
      Connecting to the library…
    </p>
    <TransformationCatalogItem
      v-for="t in available"
      :key="t.id"
      :transformation="t"
      @add="emit('add', $event)"
    />
    <p v-if="!discovering && !available.length" class="empty-note">
      No transformations found.
    </p>
    </div>
    <div class="library-note">
      <span class="status-dot"></span>
      <div>
        <strong>Powered by your backend</strong>
        <p>New transformations appear here automatically.</p>
      </div>
    </div>
  </aside>
</template>
