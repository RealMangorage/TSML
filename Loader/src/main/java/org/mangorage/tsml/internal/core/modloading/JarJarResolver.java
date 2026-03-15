package org.mangorage.tsml.internal.core.modloading;

import com.google.gson.Gson;
import org.mangorage.jar.IJar;
import org.mangorage.tsml.internal.core.jarjar.JarJarMetadata;
import org.mangorage.tsml.internal.core.jarjar.JarMetadata;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public final class JarJarResolver {
    private static final Gson gson = new Gson();

    public static List<IJar> resolveAll(List<IJar> baseJars) {
        // Maps "group:artifact" to the best version found so far
        Map<String, ArtifactEntry> artifacts = new LinkedHashMap<>();

        // The queue for discovery
        Deque<IJar> discoveryQueue = new ArrayDeque<>(baseJars);

        while (!discoveryQueue.isEmpty()) {
            IJar current = discoveryQueue.poll();
            JarJarMetadata metadata = readMetadata(current);

            if (metadata == null || metadata.jars() == null) continue;

            for (JarMetadata meta : metadata.jars()) {
                String id = meta.identifier().group() + ":" + meta.identifier().artifact();
                String version = meta.version().artifactVersion();

                ArtifactEntry existing = artifacts.get(id);

                // If we haven't seen this, or this version is better than the one we have
                if (existing == null || compare(version, existing.version) > 0) {
                    try {
                        IJar nested = current.getNestedJar(meta.path());
                        if (nested != null) {
                            artifacts.put(id, new ArtifactEntry(version, nested));
                            // RECURSION: Add the nested jar to the queue to find ITS nested jars
                            discoveryQueue.add(nested);
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to extract nested jar " + meta.path() + " from " + current.getName());
                    }
                }
            }
        }

        // Final result: The original mods + the unique winners from JarJar
        List<IJar> result = new ArrayList<>(baseJars);
        for (ArtifactEntry entry : artifacts.values()) {
            result.add(entry.jar);
        }

        return result;
    }

    private static JarJarMetadata readMetadata(IJar jar) {
        try (InputStream in = jar.getInputStream("META-INF/jarjar/metadata.json")) {
            if (in == null) return null;
            return gson.fromJson(new InputStreamReader(in), JarJarMetadata.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static int compare(String a, String b) {
        if (a.equals(b)) return 0;
        String[] as = a.split("[.-]");
        String[] bs = b.split("[.-]");

        int length = Math.max(as.length, bs.length);
        for (int i = 0; i < length; i++) {
            int av = i < as.length ? parseSegment(as[i]) : 0;
            int bv = i < bs.length ? parseSegment(bs[i]) : 0;
            if (av != bv) return Integer.compare(av, bv);
        }
        return 0;
    }

    private static int parseSegment(String s) {
        String digits = s.replaceAll("\\D", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
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