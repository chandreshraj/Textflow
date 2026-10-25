package com.knime.textflow.core.transformations;

import java.util.List;
import java.util.Map;

import com.google.auto.service.AutoService;
import static com.knime.textflow.core.Parameters.text;
import com.knime.textflow.core.Transformation;

@AutoService(Transformation.class)
public final class AppendTransformation implements Transformation {
    @Override
    public Descriptor descriptor() {
        return new Descriptor("append", "Append", "Add text to the end.",
                List.of(text("text", "Text to append", "!")));
    }

    @Override
    public String apply(String input, Map<String, Object> parameters) {
        return input + parameters.get("text");
    }
}
