package org.mangorage.tsml.internal.core.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class JarJarUtil {

    /**
     * Scans the fat jars for JarJar/output.txt and returns the full internal resource paths.
     * Example return: ["JarJar/library-1.0.jar", "JarJar/other-lib.jar"]
     */
    public static List<String> getNestedJarPaths(List<URL> fatJars, String location) throws IOException, URISyntaxException {
        List<String> internalPaths = new ArrayList<>();

        for (URL fatJarUrl : fatJars) {
            File fatJarFile = Paths.get(fatJarUrl.toURI()).toFile();
            try (JarFile jarFile = new JarFile(fatJarFile)) {
                JarEntry outputEntry = jarFile.getJarEntry(location + "/output.txt");
                if (outputEntry == null) continue;

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(outputEntry)))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            // Prepend the directory where these jars live
                            internalPaths.add(location + "/" + line);
                        }
                    }
                }
            }
        }
        return internalPaths;
    }
}
