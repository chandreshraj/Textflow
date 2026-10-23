package com.knime.textflow.core.transformations;

import com.google.auto.service.AutoService;
import com.knime.textflow.core.*;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import static com.knime.textflow.core.Parameters.*;

@AutoService(Transformation.class)
public final class ChangeCaseTransformation implements Transformation {
    @Override
    public Descriptor descriptor() {
        return new Descriptor("case", "Change case", "Convert text using locale-independent casing.",
                List.of(choice("mode", "Letter case", "UPPERCASE", List.of("UPPERCASE", "lowercase"))));
    }

    @Override
    public String apply(String input, Map<String, Object> parameters) {
        return "UPPERCASE".equals(parameters.get("mode"))
                ? input.toUpperCase(Locale.ROOT)
                : input.toLowerCase(Locale.ROOT);
    }
}
