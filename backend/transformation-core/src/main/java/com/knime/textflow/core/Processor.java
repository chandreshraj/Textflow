package com.knime.textflow.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.knime.textflow.core.Transformation.Descriptor;

public final class Processor {
    public static final int MAX_TEXT = 100000, MAX_STEPS = 30;

    public record Step(String transformation, Map<String, Object> parameters) {
    }

    public record StepResult(int index, String transformation, String output) {
    }

    public record Result(String output, List<StepResult> steps) {
    }

    private final Map<String, Transformation> registry = new LinkedHashMap<>();

    public Processor(List<Transformation> transformations) {
        for (var t : transformations)
            if (registry.putIfAbsent(t.descriptor().id(), t) != null)
                throw new IllegalArgumentException("Duplicate transformation ID");
    }

    public List<Descriptor> descriptors() {
        return registry.values().stream().map(Transformation::descriptor).toList();
    }

    public Result execute(String input, List<Step> pipeline) {

        // check on input and pipeline for length and no of steps
        if (input == null || input.length() > MAX_TEXT)
            throw new TransformException("Input is required and must be at most 100000 UTF-16 code units.", null,
                    "input");
        if (pipeline == null || pipeline.size() > MAX_STEPS)
            throw new TransformException("Pipeline is required and may contain at most 30 steps.", null, "pipeline");

        var results = new ArrayList<StepResult>();
        String current = input;

        // Execute each step in a loop and update the results of each Step
        for (int i = 0; i < pipeline.size(); i++) {
            Step step = pipeline.get(i);
            if (step == null || step.transformation() == null || !registry.containsKey(step.transformation()))
                throw new TransformException("Unknown or missing transformation.", i, null);
            var t = registry.get(step.transformation());
            try {
                Map<String, Object> params = ParameterValidator.validate(t.descriptor(), step.parameters());
                current = t.apply(current, params);
                if (current == null || current.length() > MAX_TEXT)
                    throw new TransformException("Step output exceeds 100000 UTF-16 code units.", null, null);
            } catch (TransformException e) {
                throw new TransformException(e.getMessage(), i, e.getParameter());
            }
            results.add(new StepResult(i, step.transformation(), current));
        }
        return new Result(current, List.copyOf(results));
    }
}
