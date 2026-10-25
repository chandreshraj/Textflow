package com.knime.textflow.core.transformations;

import java.util.List;
import java.util.Map;

import com.google.auto.service.AutoService;
import com.knime.textflow.core.Transformation;

@AutoService(Transformation.class)
public final class NormalizeWhitespaceTransformation implements Transformation {
    @Override
    public Descriptor descriptor() {
        return new Descriptor("normalize-whitespace", "Normalize Whitespace",
                "Collapse repeated spaces and tabs, with optional trimming and line breaks.",
                List.of(
                        new Parameter("trimEdges", "Trim edges", "boolean", true, true,
                                null, null, List.of(),
                                "Trim each line when preserving line breaks; otherwise trim the whole text."),
                        new Parameter("preserveLineBreaks", "Preserve line breaks", "boolean", true, true,
                                null, null, List.of(),
                                "Keep blank lines and normalize line endings to LF. Disable to join lines with spaces.")));
    }

    @Override
    public String apply(String input, Map<String, Object> parameters) {
        boolean trim = (Boolean) parameters.get("trimEdges");
        boolean preserveLines = (Boolean) parameters.get("preserveLineBreaks");
        var output = new StringBuilder(input.length());
        boolean pendingSpace = false;

        // One pass over code points; output never grows beyond the input length.
        for (int offset = 0; offset < input.length();) {
            int codePoint = input.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (preserveLines && isLineBreak(codePoint)) {
                if (pendingSpace && !trim)
                    output.append(' ');
                pendingSpace = false;
                output.append('\n');
                if (codePoint == '\r' && offset < input.length() && input.charAt(offset) == '\n')
                    offset++;
            } else if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)
                    || isLineBreak(codePoint)) {
                pendingSpace = true;
            } else {
                if (pendingSpace && (!trim || (!output.isEmpty() && output.charAt(output.length() - 1) != '\n'))) {
                    output.append(' ');
                }
                pendingSpace = false;
                output.appendCodePoint(codePoint);
            }
        }
        if (pendingSpace && !trim)
            output.append(' ');
        return output.toString();
    }

    private static boolean isLineBreak(int codePoint) {
        return codePoint == '\n' || codePoint == '\r' || codePoint == 0x0B || codePoint == 0x0C
                || codePoint == 0x85 || codePoint == 0x2028 || codePoint == 0x2029;
    }
}
