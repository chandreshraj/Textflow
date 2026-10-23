package com.knime.textflow.core;

import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Map;
import static com.knime.textflow.core.Parameters.text;

/** Deliberately absent from production sources and any explicit registration list. */
@AutoService(Transformation.class)
public final class DiscoveredTestTransformation implements Transformation {
    @Override
    public Descriptor descriptor() {
        return new Descriptor("test-decorate", "Test decoration", "Discovery fixture",
                List.of(text("suffix", "Suffix", "!")));
    }

    @Override
    public String apply(String input, Map<String, Object> parameters) {
        return input + parameters.get("suffix");
    }
}
