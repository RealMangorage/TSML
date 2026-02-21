package org.mangorage.tsml.internal.core.mod;

import java.util.Map;

public record ModInfo(
        String id,
        String name,
        String version,
        String description,
        String mainClass,
        Map<String, Object> properties
) {}
