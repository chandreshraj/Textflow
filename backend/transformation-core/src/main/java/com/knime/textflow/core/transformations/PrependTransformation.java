package com.knime.textflow.core.transformations;

import com.google.auto.service.AutoService;
import com.knime.textflow.core.*;
import java.util.List;
import java.util.Map;
import static com.knime.textflow.core.Parameters.*;

@AutoService(Transformation.class)
public final class PrependTransformation implements Transformation {
    @Override
    public Descriptor descriptor() {
        return new Descriptor("prepend", "Prepend", "Add text to the beginning.",
                List.of(text("text", "Text to prepend", "Hello, ")));
    }

    @Override
    public String apply(String input, Map<String, Object> parameters) {
        return parameters.get("text") + input;
    }
}
