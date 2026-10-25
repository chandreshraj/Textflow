<script setup lang="ts">
import { Plus } from "lucide-vue-next";
import InfoTooltip from "./InfoTooltip.vue";
import type { Descriptor } from "../types";

defineProps<{ transformation: Descriptor }>();
const emit = defineEmits<{ add: [transformation: Descriptor] }>();
</script>

<template>
  <div class="catalog-row">
    <button class="catalog-item" :aria-label="'Add ' + transformation.name" @click="emit('add', transformation)">
      <span class="transform-symbol" aria-hidden="true">{{ transformation.name.substring(0, 1) }}<span>↗</span></span>
      <span class="catalog-copy"><strong>{{ transformation.name }}</strong></span>
      <Plus class="add-icon" :size="16" aria-hidden="true" />
    </button>
    <InfoTooltip :label="'About ' + transformation.name" :description="transformation.description" />
  </div>
</template>
