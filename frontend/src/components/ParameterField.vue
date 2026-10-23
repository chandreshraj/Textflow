<script setup lang="ts">
import type { Parameter, Value } from '../types'
const props = defineProps<{ parameter: Parameter; modelValue: Value; fieldId: string; error?: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: Value] }>()
function change(event: Event) {
 const target = event.target as HTMLInputElement
 emit('update:modelValue', props.parameter.type === 'boolean' ? target.checked : props.parameter.type === 'number' ? (target.value === '' ? null : Number(target.value)) : target.value)
}
</script>
<template>
 <div class="field"><label :for="fieldId">{{ parameter.label }}</label>
 <select v-if="parameter.type === 'enum'" :id="fieldId" :value="modelValue" @change="change"><option v-for="option in parameter.options" :key="option">{{ option }}</option></select>
 <textarea v-else-if="parameter.type === 'multiline'" :id="fieldId" :value="String(modelValue ?? '')" @input="change" />
 <input v-else :id="fieldId" :type="parameter.type === 'number' ? 'number' : parameter.type === 'boolean' ? 'checkbox' : 'text'" :value="modelValue ?? ''" :checked="modelValue === true" :min="parameter.min ?? undefined" :max="parameter.max ?? undefined" :required="parameter.required" :aria-invalid="!!error" :aria-describedby="error ? fieldId + '-error' : undefined" @input="change">
 <small v-if="parameter.help" class="help">{{ parameter.help }}</small><small v-if="error" :id="fieldId + '-error'" class="field-error">{{ error }}</small>
 </div>
</template>
