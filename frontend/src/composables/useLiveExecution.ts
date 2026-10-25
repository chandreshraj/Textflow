import { onScopeDispose, ref, shallowRef, watch, type Ref } from "vue";
import type { ApiError, Result, Step } from "../types";
import { executePipeline, TransformationApiError } from "../api/transformations";

export const PREVIEW_DELAY_MS = 250;

/** Debounces edits, cancels obsolete requests, and accepts only the latest revision. */
export function useLiveExecution(input: Ref<string>, pipeline: Ref<Step[]>, ready: Ref<boolean>) {
  const result = shallowRef<Result | null>(null);
  const error = ref<ApiError | null>(null);
  const stale = ref(false);
  const loading = ref(false);
  let revision = 0;
  let timer: ReturnType<typeof setTimeout> | undefined;
  let activeRequest: AbortController | undefined;
  let disposed = false;

  function cancel() {
    clearTimeout(timer);
    activeRequest?.abort();
    activeRequest = undefined;
  }

  async function execute(current: number) {
    const controller = new AbortController();
    activeRequest = controller;
    try {
      const output = await executePipeline(input.value, pipeline.value, controller.signal);
      if (!disposed && current === revision) {
        result.value = output;
        stale.value = false;
      }
    } catch (cause) {
      if (!disposed && current === revision) {
        error.value = cause instanceof TransformationApiError ? cause.details : {
          message: "Unable to reach the server. Check your connection and try again.",
        };
      }
    } finally {
      if (!disposed && current === revision) {
        loading.value = false;
        activeRequest = undefined;
      }
    }
  }

  watch([input, pipeline, ready], () => {
    revision++;
    cancel();
    error.value = null;
    if (pipeline.value.length === 0) {
      result.value = input.value === "" ? null : { output: input.value, steps: [] };
      stale.value = false;
      loading.value = false;
      return;
    }
    stale.value = result.value !== null;
    loading.value = ready.value;
    if (ready.value) {
      const current = revision;
      timer = setTimeout(() => { void execute(current); }, PREVIEW_DELAY_MS);
    }
  }, { flush: "sync", immediate: true });

  onScopeDispose(() => {
    disposed = true;
    revision++;
    cancel();
  });

  return { result, error, loading, stale };
}
