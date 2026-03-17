package org.mangorage.tsml.internal.core.jarjar;

public record JarMetadata(IdentifierMetadata identifier, VersionMetadata version, String path, boolean isObfuscated) { }
