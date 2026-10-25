package com.knime.textflow.core.transformations;

import java.util.List;
import java.util.Map;

import com.google.auto.service.AutoService;
import static com.knime.textflow.core.Parameters.text;
import com.knime.textflow.core.Processor;
import com.knime.textflow.core.TransformException;
import com.knime.textflow.core.Transformation;

@AutoService(Transformation.class)
public final class SplitJoinTransformation implements Transformation {
    @Override
    public Descriptor descriptor() {
        return new Descriptor("split-join", "Split and join",
                "Split on literal text and join with a new separator. Empty items and whitespace are preserved.",
                List.of(text("separator", "Split separator", ","), text("joinSeparator", "Join separator", " · ")));
    }

    @Override
    public String apply(String input, Map<String, Object> parameters) {
        String separator = (String) parameters.get("separator");
        String joinSeparator = (String) parameters.get("joinSeparator");
        if (separator.isEmpty()) {
            throw new TransformException("Split separator cannot be empty.", null, "separator");
        }
        long length = input.length();
        int position = 0;
        while ((position = input.indexOf(separator, position)) >= 0) {
            length += joinSeparator.length() - separator.length();
            position += separator.length();
            if (length > Processor.MAX_TEXT) {
                throw new TransformException("Joined output exceeds 100000 UTF-16 code units.", null, "joinSeparator");
            }
        }
        // Literal replacement is equivalent to split-with-empty-items followed by join,
        // without allocating a potentially large intermediate list of strings.
        return input.replace(separator, joinSeparator);
    }
}
