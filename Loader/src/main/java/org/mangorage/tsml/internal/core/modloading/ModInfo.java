package org.mangorage.tsml.internal.core.modloading;

import org.mangorage.tsml.api.dependency.Dependency;

import java.util.List;
import java.util.Map;

public record ModInfo(
        String id,
        String name,
        String version,
        String description,
        String mainClass,
        List<Dependency> dependencies,
        List<String> authors,
        Map<String, Object> properties
) {}
