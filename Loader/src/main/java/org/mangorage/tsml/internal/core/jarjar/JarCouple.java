package org.mangorage.tsml.internal.core.jarjar;

import org.mangorage.tsml.internal.core.nested.api.IJar;

public record JarCouple(
        IJar jar,
        JarMetadata metadata,
        boolean isBuiltIn
) {}
