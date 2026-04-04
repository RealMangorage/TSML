package org.mangorage.tsml.internal.core.modloading;

import com.google.gson.Gson;
import org.mangorage.jar.api.IJarLocator;
import org.mangorage.jar.api.JarWithMetadata;

import java.util.*;

public final class JarJarResolver {
    private static final Gson gson = new Gson();

    public static List<JarWithMetadata> resolveAll(List<IJarLocator> jarLocators, List<JarWithMetadata> jars) {
        Set<String> visited = new HashSet<>();
        List<JarWithMetadata> result = new ArrayList<>();

        resolveRecursive(jarLocators, jars, visited, result);

        return result;
    }

    private static void resolveRecursive(
            List<IJarLocator> jarLocators,
            List<JarWithMetadata> jars,
            Set<String> visited,
            List<JarWithMetadata> result
    ) {
        for (JarWithMetadata jar : jars) {
            // Use something stable as identity (path, name, etc.)
            String id = jar.getJar().toString();

            if (!visited.add(id)) {
                continue;
            }

            result.add(jar);

            List<JarWithMetadata> discovered = new ArrayList<>();

            for (IJarLocator jarLocator : jarLocators) {
                List<JarWithMetadata> located = jarLocator.locate(jar.getJar());
                if (located != null && !located.isEmpty()) {
                    discovered.addAll(located);
                }
            }

            if (!discovered.isEmpty()) {
                resolveRecursive(jarLocators, discovered, visited, result);
            }
        }
    }
}