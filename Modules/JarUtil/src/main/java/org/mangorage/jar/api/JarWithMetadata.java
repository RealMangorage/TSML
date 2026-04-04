package org.mangorage.jar.api;

import java.util.List;
import java.util.Map;

public final class JarWithMetadata {
    public static List<JarWithMetadata> empty(List<IJar> foundJars) {
        return foundJars.stream()
                .map(jar -> new JarWithMetadata(jar, null))
                .toList();
    }


    private final IJar jar;
    private final Map<String, Object> metadata;

    public JarWithMetadata(IJar jar, Map<String, Object> metadata) {
        this.jar = jar;
        this.metadata = metadata;
    }

    public IJar getJar() {
        return jar;
    }

    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> metadataClazz) {
        return (T) metadata.get(key);
    }
}
