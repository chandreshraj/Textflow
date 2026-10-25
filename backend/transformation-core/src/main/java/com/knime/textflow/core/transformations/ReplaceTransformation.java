package com.knime.textflow.core.transformations;

import java.util.List;
import java.util.Map;

import com.google.auto.service.AutoService;
import static com.knime.textflow.core.Parameters.text;
import com.knime.textflow.core.Processor;
import com.knime.textflow.core.TransformException;
import com.knime.textflow.core.Transformation;

@AutoService(Transformation.class)
public final class ReplaceTransformation implements Transformation {
    @Override
    public Descriptor descriptor() {
        return new Descriptor("replace", "Replace", "Find literal text and replace every occurrence.",
                List.of(text("old", "Find", "Fake"), text("new", "Replace with", "Real")));
    }

    @Override
    public String apply(String input, Map<String, Object> parameters) {
        String old = (String) parameters.get("old");
        String replacement = (String) parameters.get("new");
        if (old.isEmpty()) {
            throw new TransformException("Find text cannot be empty.", null, "old");
        }
        long length = input.length();
        int position = 0;
        while ((position = input.indexOf(old, position)) >= 0) {
            length += replacement.length() - old.length();
            position += old.length();
            if (length > Processor.MAX_TEXT) {
                throw new TransformException("Step output exceeds 100000 UTF-16 code units.", null, "new");
            }
        }
        return input.replace(old, replacement);
    }
}
