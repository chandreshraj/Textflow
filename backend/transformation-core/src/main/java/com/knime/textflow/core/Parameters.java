package com.knime.textflow.core;

import java.util.List;
import com.knime.textflow.core.Transformation.Parameter;

/** Reusable metadata builders; transformation-specific metadata stays with its implementation. */
public final class Parameters {
    private Parameters() {}

    public static Parameter text(String id, String label, String defaultValue) {
        return new Parameter(id, label, "string", true, defaultValue, null, null, List.of(), "");
    }

    public static Parameter index(String id, String label, int defaultValue) {
        return new Parameter(id, label, "number", true, defaultValue, 0, Processor.MAX_TEXT,
                List.of(), "Indices count Unicode code points; end is exclusive.");
    }

    public static Parameter choice(String id, String label, String defaultValue, List<String> options) {
        return new Parameter(id, label, "enum", true, defaultValue, null, null, options, "");
    }
}
