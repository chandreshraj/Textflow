package com.knime.textflow.core;

import java.util.List;

/**
 * Compatibility entry point; implementations and discovery live in their own
 * modules.
 */
public final class Builtins {
    private Builtins() {
    }

    public static List<Transformation> all() {
        return TransformationDiscovery.load();
    }
}
