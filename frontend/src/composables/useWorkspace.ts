import { computed, onMounted, ref, shallowRef } from "vue";
import type { ApiError, Descriptor } from "../types";
import { getTransformations } from "../api/transformations";
import { usePipeline } from "./usePipeline";
import { useLiveExecution } from "./useLiveExecution";

/** Connects backend discovery, pipeline editing, and automatic output previews. */
export function useWorkspace() {
  const catalog = shallowRef<Descriptor[]>([]);
  const editor = usePipeline(catalog);
  const discovering = ref(true);
  const discoveryError = ref<ApiError | null>(null);
  const ready = computed(() => !discovering.value && catalog.value.length > 0);
  const preview = useLiveExecution(editor.input, editor.pipeline, ready);
  const error = computed(() => discoveryError.value ?? preview.error.value);

  async function discover() {
    discovering.value = true;
    discoveryError.value = null;
    try {
      catalog.value = await getTransformations();
      if (!editor.pipeline.value.length) editor.example();
    } catch (cause) {
      discoveryError.value = { message: cause instanceof Error ? cause.message : "Could not connect to the API." };
    } finally {
      discovering.value = false;
    }
  }

  function reportError(message: string) {
    preview.error.value = { message };
  }

  onMounted(discover);
  return { ...editor, catalog, result: preview.result, stale: preview.stale, error, loading: preview.loading, discovering, discover, reportError };
}
