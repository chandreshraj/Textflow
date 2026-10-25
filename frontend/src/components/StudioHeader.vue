<script setup lang="ts">
import { Braces } from "lucide-vue-next";
</script>

<template>
  <header class="topbar">
    <a class="brand" href="/" aria-label="Textflow home"
      ><span class="brand-icon"><Braces :size="21" /></span>textflow<span
        class="brand-dot"
        >.</span
      ></a
    ><span class="top-divider"></span
    ><span class="workspace-name">String transformation studio</span
    ><span class="local-badge"><span></span> Local workspace</span>
  </header>
</template>
