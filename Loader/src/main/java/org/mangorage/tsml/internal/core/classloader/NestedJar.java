package org.mangorage.tsml.internal.core.classloader;

import java.util.ArrayList;
import java.util.List;

public record NestedJar(String jarPath, List<NestedJar> nestedJars) {
    public NestedJar(String jarPath) {
        this(jarPath, new ArrayList<>());
    }

    public boolean hasNested() {
        return !nestedJars.isEmpty();
    }
}
