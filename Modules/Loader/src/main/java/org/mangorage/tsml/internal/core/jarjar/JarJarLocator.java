package org.mangorage.tsml.internal.core.jarjar;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.mangorage.jar.api.IJar;
import org.mangorage.jar.api.IJarLocator;
import org.mangorage.jar.api.JarWithMetadata;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JarJarLocator implements IJarLocator {

    private static final String METADATA_PATH = "META-INF/jarjar/metadata.json";
    private final Gson gson = new Gson();

    @Override
    public List<JarWithMetadata> locate(List<IJar> jars) {
        List<JarWithMetadata> found = new ArrayList<>();

        for (IJar jar : jars) {
            try {
                if (!jar.exists(METADATA_PATH)) continue;

                byte[] raw = jar.readBytes(METADATA_PATH);
                if (raw == null) continue;

                String json = new String(raw, StandardCharsets.UTF_8);
                JsonObject root = gson.fromJson(json, JsonObject.class);
                if (root == null || !root.has("jars")) continue;

                JsonArray jarsArray = root.getAsJsonArray("jars");

                for (JsonElement elem : jarsArray) {
                    try {
                        JsonObject jarObj = elem.getAsJsonObject();

                        // Deserialize into record (typed access)
                        JarJarEntry entry = gson.fromJson(jarObj, JarJarEntry.class);
                        if (entry == null || entry.path() == null || entry.path().isEmpty()) continue;

                        IJar nested;
                        try {
                            nested = jar.getNestedJar(entry.path());
                        } catch (IOException ioe) {
                            System.err.println("Failed to resolve nested jar '" + entry.path() + "' in " + jar.getName() + ": " + ioe.getMessage());
                            continue;
                        }

                        if (nested == null) continue;

                        // Also keep raw metadata as Map (for your existing system)
                        Map<String, Object> metadataMap = new HashMap<>();
                        metadataMap.put(JarWithMetadata.JAR_JAR_METADATA_KEY, gson.fromJson(jarObj, JarJarEntry.class));

                        found.add(new JarWithMetadata(nested, metadataMap));

                    } catch (Throwable t) {
                        System.err.println("Malformed jar entry in metadata for jar=" + jar.getName() + ": " + t.getMessage());
                    }
                }

            } catch (Throwable t) {
                System.err.println("Failed to process metadata for jar=" + jar.getName() + ": " + t.getMessage());
            }
        }

        return found;
    }
}