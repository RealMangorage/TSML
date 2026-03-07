package org.mangorage.tsml.internal.core.jarjar;

import org.mangorage.tsml.api.jar.IJar;

public record JarCouple(
        IJar jar,
        JarMetadata metadata,
        boolean isBuiltIn
) {}
