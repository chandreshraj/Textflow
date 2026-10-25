package com.knime.textflow.core.transformations;

import java.util.List;
import java.util.Map;

import com.google.auto.service.AutoService;
import static com.knime.textflow.core.Parameters.index;
import com.knime.textflow.core.TransformException;
import com.knime.textflow.core.Transformation;

@AutoService(Transformation.class)
public final class SubstringTransformation implements Transformation {
    @Override
    public Descriptor descriptor() {
        return new Descriptor("substring", "Substring", "Keep a range of Unicode characters.",
                List.of(index("start", "Start index", 0), index("end", "End index", 4)));
    }

    @Override
    public String apply(String input, Map<String, Object> parameters) {
        int start = ((Number) parameters.get("start")).intValue();
        int end = ((Number) parameters.get("end")).intValue();
        if (end < start || end > input.codePointCount(0, input.length())) {
            throw new TransformException("End must be at least start and within the text length.", null, "end");
        }
        return input.substring(input.offsetByCodePoints(0, start), input.offsetByCodePoints(0, end));
    }
}
