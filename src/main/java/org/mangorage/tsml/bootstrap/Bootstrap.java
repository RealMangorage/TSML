package org.mangorage.tsml.bootstrap;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public final class Bootstrap {

    public static void deleteDirectoryRecursively(Path dir) throws IOException {
        if (Files.exists(dir)) {
            // Walk through all files and subdirectories in reverse order (files first, then folders)
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to delete " + path, e);
                        }
                    });
        }
    }

    public static void main(String[] args) throws Exception {
        Path rootDir = Path.of("").toAbsolutePath();
        Path extractedLoaderDir = rootDir.resolve("extractedLoader");
        Path defaultMods = rootDir.resolve("defaultMods");

        URL bootstrapJar = Bootstrap.class.getProtectionDomain().getCodeSource().getLocation();

        if (Files.exists(extractedLoaderDir)) {
            deleteDirectoryRecursively(extractedLoaderDir);
        }

        if (Files.exists(defaultMods)) {
            deleteDirectoryRecursively(defaultMods);
        }

        final var loaderJars = JarJarLoader.extractJarsFromFatJars(
                List.of(bootstrapJar),
                extractedLoaderDir,
                "JarJarLoader"
        );

        JarJarLoader.extractJarsFromFatJars(
                List.of(bootstrapJar),
                defaultMods,
                "JarJarMods"
        );

        try (final var urlClassloader = new URLClassLoader(loaderJars.toArray(new URL[0]), Bootstrap.class.getClassLoader())) {
            Thread.currentThread().setContextClassLoader(urlClassloader);
            final var mainClass = Class.forName("org.mangorage.tsml.internal.TSML", true, urlClassloader);
            mainClass.getMethod("main", String[].class).invoke(null, (Object) args);
        }
    }
}