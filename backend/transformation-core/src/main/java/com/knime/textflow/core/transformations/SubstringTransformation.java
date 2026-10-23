package com.knime.textflow.core.transformations;

import com.google.auto.service.AutoService;
import com.knime.textflow.core.*;
import java.util.List;
import java.util.Map;
import static com.knime.textflow.core.Parameters.*;

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
