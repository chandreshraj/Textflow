package com.knime.textflow.core;

import java.util.HashMap;
import java.util.Map;

import com.knime.textflow.core.Transformation.Descriptor;

/**
 * Applies descriptor defaults and validates parameter values before execution.
 */
final class ParameterValidator {
    private ParameterValidator() {
    }

    static Map<String, Object> validate(Descriptor d, Map<String, Object> supplied) {
        if (supplied == null)
            throw new TransformException("Parameters must be an object.", null, "parameters");
        var validated = new HashMap<String, Object>();
        for (String key : supplied.keySet())
            if (d.parameters().stream().noneMatch(p -> p.id().equals(key)))
                throw new TransformException("Unknown parameter: " + key, null, key);
        for (var p : d.parameters()) {
            Object v = supplied.containsKey(p.id()) ? supplied.get(p.id()) : p.defaultValue();
            if (v == null) {
                if (p.required())
                    throw new TransformException("A value is required.", null, p.id());
                else
                    continue;
            }
            boolean valid = switch (p.type()) {
                case "string", "multiline" -> v instanceof String;
                case "boolean" -> v instanceof Boolean;
                case "enum" -> v instanceof String && p.options().contains(v);
                case "number" -> v instanceof Number n && Double.isFinite(n.doubleValue())
                        && n.doubleValue() == Math.rint(n.doubleValue())
                        && (p.min() == null || n.doubleValue() >= p.min())
                        && (p.max() == null || n.doubleValue() <= p.max());
                default -> false;
            };
            if (!valid)
                throw new TransformException("Invalid value for " + p.label() + ".", null, p.id());
            validated.put(p.id(), v);
        }
        return validated;
    }
}
