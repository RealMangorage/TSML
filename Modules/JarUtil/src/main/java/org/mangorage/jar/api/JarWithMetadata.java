package org.mangorage.jar.api;

import java.util.List;

public final class JarWithMetadata {
    public static List<JarWithMetadata> empty(List<IJar> foundJars) {
        return foundJars.stream()
                .map(jar -> new JarWithMetadata(jar, null))
                .toList();
    }


    private final IJar jar;
    private final String metadata;

    public JarWithMetadata(IJar jar, String metadata) {
        this.jar = jar;
        this.metadata = metadata;
    }

    public IJar getJar() {
        return jar;
    }

    public String getMetadata() {
        return metadata;
    }
}
