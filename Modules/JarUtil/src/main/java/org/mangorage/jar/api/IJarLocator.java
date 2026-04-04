package org.mangorage.jar.api;

import java.util.List;

public interface IJarLocator {
    default List<JarWithMetadata> locate(IJar jar) {
        return locate(List.of(jar));
    }

    List<JarWithMetadata> locate(List<IJar> jars);
}
