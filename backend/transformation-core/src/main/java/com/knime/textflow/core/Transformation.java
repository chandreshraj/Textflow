package com.knime.textflow.core;
import java.util.List;
import java.util.Map;

/** Implement and register this interface to add a transformation without changing the UI. */
public interface Transformation {
    record Parameter(String id, String label, String type, boolean required, Object defaultValue,
                     Integer min, Integer max, List<String> options, String help) {}
    record Descriptor(String id, String name, String description, List<Parameter> parameters) {}
    Descriptor descriptor();
    String apply(String input, Map<String, Object> parameters);
}
