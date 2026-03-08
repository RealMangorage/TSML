package org.mangorage.tsml.internal.core.modloading;

import com.google.gson.Gson;
import org.mangorage.tsml.api.jar.IJar;
import org.mangorage.tsml.internal.core.jarjar.JarJarMetadata;
import org.mangorage.tsml.internal.core.jarjar.JarMetadata;
import org.mangorage.tsml.internal.core.jarjar.VersionMetadata;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public final class JarJarResolver {

    private static final Gson gson = new Gson();

    public static List<IJar> resolveAll(List<IJar> baseJars) {
        List<IJar> resolved = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Map<String, ArtifactEntry> artifacts = new HashMap<>();

        for (IJar jar : baseJars) {
            resolveJar(jar, resolved, visited, artifacts);
        }

        return resolved;
    }

    private static void resolveJar(
            IJar jar,
            List<IJar> resolved,
            Set<String> visited,
            Map<String, ArtifactEntry> artifacts
    ) {
        if (!visited.add(jar.getName())) {
            return;
        }

        resolved.add(jar);

        JarJarMetadata metadata = readMetadata(jar);
        if (metadata == null) {
            return;
        }

        for (JarMetadata meta : metadata.jars()) {
            try {
                String key = meta.identifier().group() + ":" + meta.identifier().artifact();

                IJar nested = jar.getNestedJar(meta.path());
                if (nested == null) {
                    continue;
                }

                if (!versionMatches(meta.version())) {
                    throw new RuntimeException(
                            "Jar version mismatch for "
                                    + key
                                    + " expected " + meta.version().range()
                                    + " got " + meta.version().artifactVersion()
                    );
                }

                ArtifactEntry existing = artifacts.get(key);

                if (existing == null) {
                    artifacts.put(key, new ArtifactEntry(meta.version().artifactVersion(), nested));
                    resolveJar(nested, resolved, visited, artifacts);
                } else {
                    String newVersion = meta.version().artifactVersion();

                    if (compare(newVersion, existing.version) > 0) {
                        artifacts.put(key, new ArtifactEntry(newVersion, nested));
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException("Failed resolving nested jar " + meta.path(), e);
            }
        }
    }

    private static JarJarMetadata readMetadata(IJar jar) {
        try (InputStream in = jar.getInputStream("META-INF/jarjar/metadata.json")) {
            if (in == null) {
                return null;
            }
            return gson.fromJson(new InputStreamReader(in), JarJarMetadata.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed reading jarjar metadata from " + jar.getName(), e);
        }
    }

    private static boolean versionMatches(VersionMetadata expected) {
        String range = expected.range();
        if (range == null || range.isEmpty()) {
            return true;
        }

        String version = expected.artifactVersion();

        if (range.startsWith("[")) {
            String min = range.substring(1, range.indexOf(','));
            return compare(version, min) >= 0;
        }

        return true;
    }

    private static int compare(String a, String b) {
        String[] as = a.split("\\.");
        String[] bs = b.split("\\.");

        int len = Math.max(as.length, bs.length);

        for (int i = 0; i < len; i++) {
            int ai = i < as.length ? parseSegment(as[i]) : 0;
            int bi = i < bs.length ? parseSegment(bs[i]) : 0;

            if (ai != bi) {
                return Integer.compare(ai, bi);
            }
        }

        return 0;
    }

    private static int parseSegment(String s) {
        int end = 0;

        while (end < s.length() && Character.isDigit(s.charAt(end))) {
            end++;
        }

        if (end == 0) {
            return 0;
        }

        return Integer.parseInt(s.substring(0, end));
    }

    private static class ArtifactEntry {
        final String version;
        final IJar jar;

        ArtifactEntry(String version, IJar jar) {
            this.version = version;
            this.jar = jar;
        }
    }
}