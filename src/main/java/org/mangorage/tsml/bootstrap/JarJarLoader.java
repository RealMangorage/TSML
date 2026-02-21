package org.mangorage.tsml.bootstrap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class JarJarLoader {

    /**
     * For each jar in `fatJars`, reads {folderInJar}/output.txt and extracts the listed jars into `extractToFolder`.
     *
     * @param fatJars       list of URLs pointing to jars that contain the folder with output.txt
     * @param extractToFolder folder to extract jars into
     * @param folderInJar     folder inside the fat jar where output.txt and jars reside
     * @return list of URLs pointing to the extracted jars on disk
     * @throws IOException on IO errors
     */
    public static List<URL> extractJarsFromFatJars(List<URL> fatJars, Path extractToFolder, String folderInJar) throws IOException, URISyntaxException {
        List<URL> extractedUrls = new ArrayList<>();

        if (!Files.exists(extractToFolder)) {
            Files.createDirectories(extractToFolder);
        }

        for (URL fatJarUrl : fatJars) {
            File fatJarFile = Paths.get(fatJarUrl.toURI()).toFile();
            try (JarFile jarFile = new JarFile(fatJarFile)) {

                // Read output.txt from the specified folder
                JarEntry outputEntry = jarFile.getJarEntry(folderInJar + "/output.txt");
                if (outputEntry == null) {
                    System.err.println("No " + folderInJar + "/output.txt found in " + fatJarFile);
                    continue;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(outputEntry)))) {
                    String jarName;
                    while ((jarName = reader.readLine()) != null) {
                        jarName = jarName.trim();
                        if (jarName.isEmpty()) continue;

                        // Extract the jar listed
                        JarEntry jarEntry = jarFile.getJarEntry(folderInJar + "/" + jarName);
                        if (jarEntry == null) {
                            System.err.println("Jar not found inside " + folderInJar + ": " + jarName + " in " + fatJarFile);
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