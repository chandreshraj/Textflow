package com.knime.textflow.core;

import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Uses Java SPI providers generated at compilation by @AutoService. No
 * framework required.
 */
public final class TransformationDiscovery {
    private TransformationDiscovery() {
    }

    public static List<Transformation> load() {
        return load(Transformation.class.getClassLoader());
    }

    public static List<Transformation> load(ClassLoader classLoader) {
        return ServiceLoader.load(Transformation.class, classLoader).stream()
                .map(ServiceLoader.Provider::get)
                .sorted(Comparator.comparing(t -> t.descriptor().id()))
                .toList();
    }
}
