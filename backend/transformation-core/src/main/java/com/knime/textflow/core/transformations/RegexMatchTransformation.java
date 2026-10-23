package com.knime.textflow.core.transformations;

import com.google.auto.service.AutoService;
import com.knime.textflow.core.*;
import java.util.List;
import java.util.Map;
import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;
import static com.knime.textflow.core.Parameters.*;

@AutoService(Transformation.class)
public final class RegexMatchTransformation implements Transformation {
    @Override
    public Descriptor descriptor() {
        return new Descriptor("regex", "Regex match", "Extract all matches, separated by a line break.",
                List.of(text("pattern", "Pattern", "[A-Za-z]+")));
    }

    @Override
    public String apply(String input, Map<String, Object> parameters) {
        String regex = (String) parameters.get("pattern");
        if (regex.length() > 500) {
            throw new TransformException("Pattern must be at most 500 characters.", null, "pattern");
        }
        try {
            var matcher = Pattern.compile(regex).matcher(input);
            var result = new StringBuilder();
            boolean first = true;
            while (matcher.find()) {
                if (!first) result.append('\n');
                result.append(matcher.group());
                first = false;
            }
            return result.toString();
        } catch (PatternSyntaxException e) {
            throw new TransformException("Invalid or unsupported RE2 pattern: " + e.getDescription(), null, "pattern");
        }
    }
}
