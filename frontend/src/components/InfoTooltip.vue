<script setup lang="ts">
import { nextTick, onScopeDispose, ref, useId, watch } from "vue";
import { Info } from "lucide-vue-next";

defineProps<{ label: string; description: string }>();
const id = useId();
const trigger = ref<HTMLButtonElement | null>(null);
const tooltip = ref<HTMLElement | null>(null);
const open = ref(false);
const positioned = ref(false);
const pinned = ref(false);
const focused = ref(false);
const position = ref({ left: "0px", top: "0px" });
let scrollFrame: number | undefined;
let hideTimer: ReturnType<typeof setTimeout> | undefined;

function show() { clearTimeout(hideTimer); open.value = true; }
function dismiss() { clearTimeout(hideTimer); open.value = false; pinned.value = false; }
function leave() {
  if (!pinned.value && !focused.value) hideTimer = setTimeout(dismiss, 120);
}
function blur() { focused.value = false; pinned.value = false; leave(); }
function toggle() { pinned.value = !pinned.value; if (pinned.value) show(); else dismiss(); }
function outside(event: PointerEvent) {
  if (event.target instanceof Node && !trigger.value?.contains(event.target) && !tooltip.value?.contains(event.target)) dismiss();
}
function escape(event: KeyboardEvent) { if (event.key === "Escape") dismiss(); }
function cancelPositioning() {
  if (scrollFrame !== undefined) cancelAnimationFrame(scrollFrame);
  scrollFrame = undefined;
}
function scrolled(event: Event) {
  if (event.target instanceof Node && tooltip.value?.contains(event.target)) return;
  if (scrollFrame !== undefined) return;
  scrollFrame = requestAnimationFrame(() => {
    scrollFrame = undefined;
    const anchor = trigger.value?.getBoundingClientRect();
    const boundary = trigger.value?.closest("[data-tooltip-boundary]")?.getBoundingClientRect();
    if (!anchor || anchor.bottom <= 0 || anchor.top >= window.innerHeight
        || (boundary && (anchor.bottom <= boundary.top || anchor.top >= boundary.bottom))) dismiss();
    else positionTooltip(anchor);
  });
}

watch(open, async (visible, _, cleanup) => {
  positioned.value = false;
  if (!visible) return;
  window.addEventListener("resize", dismiss);
  window.addEventListener("scroll", scrolled, { capture: true, passive: true });
  document.addEventListener("pointerdown", outside);
  document.addEventListener("keydown", escape);
  cleanup(() => {
    cancelPositioning();
    window.removeEventListener("resize", dismiss);
    window.removeEventListener("scroll", scrolled, true);
    document.removeEventListener("pointerdown", outside);
    document.removeEventListener("keydown", escape);
  });
  await nextTick();
  positionTooltip();
});
function positionTooltip(anchor = trigger.value?.getBoundingClientRect()) {
  if (!open.value || !anchor || !tooltip.value) return;
  const box = tooltip.value.getBoundingClientRect();
  const margin = 8;
  let x: number, y: number;
  if (anchor.right + box.width + 16 <= window.innerWidth) {
    x = anchor.right + margin; y = anchor.top + (anchor.height - box.height) / 2;
  } else if (anchor.left - box.width - 16 >= 0) {
    x = anchor.left - box.width - margin; y = anchor.top + (anchor.height - box.height) / 2;
  } else {
    x = anchor.left + (anchor.width - box.width) / 2;
    y = anchor.bottom + margin + box.height <= window.innerHeight ? anchor.bottom + margin : anchor.top - box.height - margin;
  }
  position.value = {
    left: `${Math.max(margin, Math.min(x, window.innerWidth - box.width - margin))}px`,
    top: `${Math.max(margin, Math.min(y, window.innerHeight - box.height - margin))}px`,
  };
  positioned.value = true;
}
onScopeDispose(() => { clearTimeout(hideTimer); cancelPositioning(); });
</script>

<template>
  <button ref="trigger" class="transformation-info" :aria-label="label" :aria-expanded="open"
    :aria-describedby="open ? id : undefined" @pointerenter="show" @pointerleave="leave"
    @focus="focused = true; show()" @blur="blur" @click="toggle" @keydown.esc.stop.prevent="dismiss">
    <Info :size="17" aria-hidden="true" />
  </button>
  <Teleport to="body">
    <div v-if="open" ref="tooltip" :id="id" role="tooltip" class="info-tooltip"
      :style="{ ...position, visibility: positioned ? 'visible' : 'hidden' }"
      @pointerenter="show" @pointerleave="leave">
      {{ description }}
    </div>
  </Teleport>
</template>
