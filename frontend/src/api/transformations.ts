import type { ApiError, Descriptor, Result, Step } from "../types";

export class TransformationApiError extends Error {
  constructor(readonly details: ApiError) {
    super(details.message);
  }
}

export async function getTransformations(): Promise<Descriptor[]> {
  const response = await fetch("/api/transformations");
  if (!response.ok) {
    throw new Error("Cannot load transformations. Check that the backend is running.");
  }
  return response.json();
}

export async function executePipeline(input: string, pipeline: Step[], signal?: AbortSignal): Promise<Result> {
  const response = await fetch("/api/transform", {
    method: "POST",
    signal,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      input,
      pipeline: pipeline.map(({ transformation, parameters }) => ({ transformation, parameters })),
    }),
  });
  if (!response.ok) {
    const body: ApiError = await response.json();
    throw new TransformationApiError({
      message: body.message || "Transformation failed.",
      step: body.step,
      parameter: body.parameter,
    });
  }
  return response.json();
}
