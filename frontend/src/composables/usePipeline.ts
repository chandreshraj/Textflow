import { shallowRef, type Ref } from "vue";
import type { Descriptor, Step, Value } from "../types";
import { exampleInput, exampleTransformations } from "../presets/example";

/** Immutable edits publish one change and preserve unaffected step identities. */
export function usePipeline(catalog: Ref<Descriptor[]>) {
  const input = shallowRef(exampleInput);
  const pipeline = shallowRef<Step[]>([]);
  let nextKey = 0;

  function createStep(transformation: Descriptor): Step {
    return {
      key: nextKey++,
      transformation: transformation.id,
      parameters: Object.fromEntries(transformation.parameters.map(p => [p.id, p.defaultValue])),
    };
  }

  function add(transformation: Descriptor) {
    pipeline.value = [...pipeline.value, createStep(transformation)];
  }

  function move(index: number, direction: -1 | 1) {
    const destination = index + direction;
    if (index < 0 || index >= pipeline.value.length || destination < 0 || destination >= pipeline.value.length) return;
    const next = [...pipeline.value];
    const [step] = next.splice(index, 1);
    if (step) next.splice(destination, 0, step);
    pipeline.value = next;
  }

  function remove(index: number) {
    if (index >= 0 && index < pipeline.value.length) pipeline.value = pipeline.value.filter((_, i) => i !== index);
  }

  function clear() {
    if (pipeline.value.length) pipeline.value = [];
  }

  function updateParameter(index: number, id: string, value: Value) {
    const step = pipeline.value[index];
    if (!step || Object.is(step.parameters[id], value)) return;
    pipeline.value = pipeline.value.map((current, i) => i === index
      ? { ...current, parameters: { ...current.parameters, [id]: value } }
      : current);
  }

  function example() {
    const transformations = exampleTransformations.map(id => catalog.value.find(t => t.id === id));
    pipeline.value = transformations.every(t => t !== undefined) ? transformations.map(createStep) : [];
    input.value = exampleInput;
  }

  return { input, pipeline, add, move, remove, clear, updateParameter, example };
}
