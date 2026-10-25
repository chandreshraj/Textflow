<script setup lang="ts">
const input = defineModel<string>({ required: true });
</script>

<template>
  <div class="editor-card input-card">
    <div class="editor-heading">
      <h2><span class="section-number">01</span> Input text</h2>
      <span class="muted">Start with something</span>
    </div>
    <textarea
      v-model="input"
      aria-label="Input text"
      placeholder="Type or paste your text here…"
      maxlength="100000"
      spellcheck="false"
    ></textarea>
    <div class="editor-footer">
      <span>Plain text</span
      ><span>{{ input.length.toLocaleString() }} characters</span>
    </div>
  </div>
</template>
