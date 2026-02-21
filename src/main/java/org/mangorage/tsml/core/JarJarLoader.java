package org.mangorage.tsml.core;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class JarJarLoader {

    /**
     * For each jar in `fatJars`, reads JarJar/output.txt and extracts the listed jars into `extractToFolder`.
     *
     * @param fatJars         list of URLs pointing to jars that contain JarJar/output.txt
     * @param extractToFolder folder to extract jars into
     * @return list of URLs pointing to the extracted jars on disk
     * @throws IOException on IO errors
     */
    public static List<URL> extractJarsFromFatJars(List<URL> fatJars, Path extractToFolder) throws IOException, URISyntaxException {
        List<URL> extractedUrls = new ArrayList<>();

        if (!Files.exists(extractToFolder)) {
            Files.createDirectories(extractToFolder);
        }

        for (URL fatJarUrl : fatJars) {
            File fatJarFile = Paths.get(fatJarUrl.toURI()).toFile();
            try (JarFile jarFile = new JarFile(fatJarFile)) {

                // Read JarJar/output.txt
                JarEntry outputEntry = jarFile.getJarEntry("JarJar/output.txt");
                if (outputEntry == null) {
                    System.err.println("No JarJar/output.txt found in " + fatJarFile);
                    continue;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(outputEntry)))) {
                    String jarName;
                    while ((jarName = reader.readLine()) != null) {
                        jarName = jarName.trim();
                        if (jarName.isEmpty()) continue;

                        // Extract the jar listed
                        JarEntry jarEntry = jarFile.getJarEntry("JarJar/" + jarName);
                        if (jarEntry == null) {
                            System.err.println("Jar not found inside JarJar: " + jarName + " in " + fatJarFile);
                            continue;
                        }

                        Path outJar = extractToFolder.resolve(jarName);
                        try (InputStream in = jarFile.getInputStream(jarEntry);
                             OutputStream out = Files.newOutputStream(outJar, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                            in.transferTo(out);
                        }

                        extractedUrls.add(outJar.toUri().toURL());
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to process jar: " + fatJarFile);
                e.printStackTrace();
            }
        }

        return extractedUrls;
    }
}
