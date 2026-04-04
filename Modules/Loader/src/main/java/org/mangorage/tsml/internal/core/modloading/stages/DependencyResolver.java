package org.mangorage.tsml.internal.core.modloading.stages;

import org.mangorage.jar.api.JarWithMetadata;
import org.mangorage.tsml.internal.core.jarjar.JarJarEntry;

import java.util.*;

public final class DependencyResolver {


    public static List<JarWithMetadata> resolve(List<JarWithMetadata> jars) {

        // Step 1: Extract entries
        List<EntryHolder> holders = new ArrayList<>();

        for (JarWithMetadata jar : jars) {
            JarJarEntry entry = jar.getMetadata(JarWithMetadata.JAR_JAR_METADATA_KEY, JarJarEntry.class);

            if (entry == null) continue;

            holders.add(new EntryHolder(jar, entry));
        }

        // Step 2: Group by identifier (group + artifact)
        Map<JarJarEntry.Identifier, List<EntryHolder>> grouped = new HashMap<>();

        for (EntryHolder holder : holders) {
            grouped.computeIfAbsent(holder.entry.identifier(), k -> new ArrayList<>())
                    .add(holder);
        }

        // Step 3: Resolve each group
        List<JarWithMetadata> result = new ArrayList<>();

        for (var group : grouped.entrySet()) {
            EntryHolder resolved = resolveGroup(group.getKey(), group.getValue());
            result.add(resolved.jar);
        }

        return result;
    }

    private static EntryHolder resolveGroup(
            JarJarEntry.Identifier id,
            List<EntryHolder> candidates
    ) {
        // Sort highest version first
        candidates.sort((a, b) ->
                compareVersions(
                        b.entry.version().artifactVersion(),
                        a.entry.version().artifactVersion()
                )
        );

        for (EntryHolder candidate : candidates) {
            if (satisfiesAll(candidate, candidates)) {
                return candidate;
            }
        }

        throw new IllegalStateException(
                "Version conflict for " + id + ": " +
                        candidates.stream().map(h -> h.entry.version()).toList()
        );
    }

    private static boolean satisfiesAll(
            EntryHolder candidate,
            List<EntryHolder> all
    ) {
        String version = candidate.entry.version().artifactVersion();

        for (EntryHolder other : all) {
            if (!versionInRange(version, other.entry.version().range())) {
                return false;
            }
        }

        return true;
    }

    // ⚠️ Replace this later with real semver
    private static int compareVersions(String v1, String v2) {
        return v1.compareTo(v2);
    }

    private static boolean versionInRange(String version, String range) {
        if (range == null || range.isBlank()) return true;

        boolean minInclusive = range.startsWith("[");
        boolean maxInclusive = range.endsWith("]");

        String inner = range.substring(1, range.length() - 1);
        String[] parts = inner.split(",");

        String min = parts.length > 0 ? parts[0].trim() : "";
        String max = parts.length > 1 ? parts[1].trim() : "";

        if (!min.isEmpty()) {
            int cmp = compareVersions(version, min);
            if (cmp < 0 || (cmp == 0 && !minInclusive)) return false;
        }

        if (!max.isEmpty()) {
            int cmp = compareVersions(version, max);
            if (cmp > 0 || (cmp == 0 && !maxInclusive)) return false;
        }

        return true;
    }

    private record EntryHolder(JarWithMetadata jar, JarJarEntry entry) {}
}