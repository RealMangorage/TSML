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
import java.util.List;

public final class JarJarLocator implements IJarLocator {

    @Override
    public List<JarWithMetadata> locate(List<IJar> jars) {
        List<JarWithMetadata> found = new ArrayList<>();
        Gson gson = new Gson();

        for (IJar jar : jars) {
            try {
                // Check if metadata exists inside this jar
                if (!jar.exists("META-INF/jarjar/metadata.json")) continue;

                byte[] raw = jar.readBytes("META-INF/jarjar/metadata.json");
                if (raw == null) continue;

                String json = new String(raw, StandardCharsets.UTF_8);
                JsonObject root = gson.fromJson(json, JsonObject.class);
                if (root == null || !root.has("jars")) continue;

                JsonArray jarsArray = root.getAsJsonArray("jars");
                for (JsonElement elem : jarsArray) {
                    try {
                        JsonObject jarObj = elem.getAsJsonObject();
                        String path = jarObj.has("path") ? jarObj.get("path").getAsString() : null;
                        if (path == null || path.isEmpty()) continue;

                        // Resolve the nested jar entry (path is relative inside the current jar)
                        IJar nested = null;
                        try {
                            nested = jar.getNestedJar(path);
                        } catch (IOException ioe) {
                            // if nested jar can't be read, skip this entry
                            System.err.println("Failed to resolve nested jar '" + path + "' in " + jar.getName() + ": " + ioe.getMessage());
                            continue;
                        }

                        if (nested == null) continue;

                        // Use the specific metadata for this jar entry (stringified JSON object)
                        String perJarMetadata = jarObj.toString();
                        found.add(new JarWithMetadata(nested, perJarMetadata));
                    } catch (Throwable t) {
                        // Skip a single malformed entry but continue processing other entries
                        System.err.println("Malformed jar entry in metadata for jar=" + jar.getName() + ": " + t.getMessage());
                    }
                }

            } catch (Throwable t) {
                // Skip this jar but continue others
                System.err.println("Failed to process metadata for jar=" + jar.getName() + ": " + t.getMessage());
            }
        }

        return found;
    }
}
