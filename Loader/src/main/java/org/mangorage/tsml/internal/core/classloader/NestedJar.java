package org.mangorage.tsml.internal.core.classloader;

import java.util.List;

public final class NestedJar {
    private final String resourcePath; // path for getResourceAsStream (no file:/! stuff)
    private final String jarPath;      // full root-aware URL for CodeSource / URLs
    private final List<NestedJar> nestedJars;

    public NestedJar(String resourcePath, String jarPath, List<NestedJar> nestedJars) {
        this.resourcePath = resourcePath;
        this.jarPath = jarPath;
        this.nestedJars = nestedJars;
    }

    public String getFullPath() {
        return jarPath + "!/" + resourcePath;
    }

    public String resourcePath() {
        return resourcePath;
    }

    public List<NestedJar> nestedJars() {
        return nestedJars;
    }
}