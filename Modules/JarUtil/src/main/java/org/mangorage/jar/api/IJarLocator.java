package org.mangorage.jar.api;

import java.util.List;

public interface IJarLocator {

    // Locate jar within jar
    default List<JarWithMetadata> locate(IJar jar) {
        return locate(List.of(jar));
    }

    // Locate jars within jars
    default List<JarWithMetadata> locate(List<IJar> jars) {
        return List.of();
    }
}
