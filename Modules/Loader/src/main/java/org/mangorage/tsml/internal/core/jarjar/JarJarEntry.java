package org.mangorage.tsml.internal.core.jarjar;

public record JarJarEntry(
        Identifier identifier,
        Version version,
        String path,
        boolean isObfuscated
) {
    public record Identifier(
            String group,
            String artifact
    ) {}

    public record Version(
            String range,
            String artifactVersion
    ) {}
}