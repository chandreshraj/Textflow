package com.knime.textflow.core.transformations;

import com.google.auto.service.AutoService;
import com.knime.textflow.core.*;
import java.util.List;
import java.util.Map;
import static com.knime.textflow.core.Parameters.*;

@AutoService(Transformation.class)
public final class TrimTransformation implements Transformation {
    @Override
    public Descriptor descriptor() {
        return new Descriptor("trim", "Trim whitespace", "Remove leading and trailing Unicode whitespace.",
                List.of());
    }

    @Override
    public String apply(String input, Map<String, Object> parameters) {
        return input.strip();
    }
}
