package com.knime.textflow.api;

import java.util.List;

import com.knime.textflow.core.Processor;
import com.knime.textflow.core.TransformException;

public record TransformRequest(Object input, List<Processor.Step> pipeline) {
    public String inputText() {
        if (!(input instanceof String text)) {
            throw new TransformException("Input must be a string.", null, "input");
        }
        return text;
    }
}
