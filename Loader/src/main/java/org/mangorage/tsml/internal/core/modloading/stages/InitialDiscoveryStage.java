package org.mangorage.tsml.internal.core.modloading.stages;

import org.mangorage.tsml.internal.core.jarjar.WrappedJar;
import org.mangorage.tsml.api.jar.IJar;

import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class InitialDiscoveryStage {

    /**
     * Finds all .jar files in a folder (non-recursive) and returns them as Paths
     *
     * @param folderPath folder to search
     * @return list of Paths pointing to jars
     * @throws IOException if folder access fails
     */
    static List<Path> findJarPaths(Path folderPath) throws IOException {
        List<Path> jarPaths = new ArrayList<>();

        if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
            System.err.println("Invalid folder: " + folderPath);
            return jarPaths;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folderPath, "*.jar")) {
            for (Path jarPath : stream) {
                jarPaths.add(jarPath);
            }
        }

        return jarPaths;
    }

    /**
     * @param baseResource → Will be the TSML jar itself
     * @param foundJars → Add any jars you want exposed.
     * @return The main TriviaSpire.jar, used for the next stage to find the main class and logger class.
     */
    public IJar run(URL baseResource, List<IJar> foundJars) throws IOException {
        Path rootPath = Path.of("");
        Path modsPath = rootPath.resolve("mods").toAbsolutePath();

        IJar triviaSpireJar = findJarPaths(rootPath)
                .stream()
                .map(Path::toFile)
                .filter(file -> file.getName().endsWith(".jar"))
                .filter(file -> file.getName().contains("TriviaSpire"))
                .map(WrappedJar::create)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find TriviaSpire.jar in root folder"));

        IJar tsmlJar = WrappedJar.create(baseResource.getFile());

        foundJars.addAll(
                tsmlJar.getNestedJars()
                        .stream()
                        .filter(jar -> !jar.getName().contains("JarJarLoader/") && jar.getName().contains("jarjar") && jar.getName().endsWith(".jar"))
                        .toList()
        );

        if (Files.exists(modsPath)) {
            try (Stream<Path> stream = Files.list(modsPath)) {
                stream
                        .filter(Files::isRegularFile)
                        .map(path -> WrappedJar.create(path.toFile()))
                        .forEach(foundJar -> {
                            foundJars.add(foundJar);
                            try {
                                foundJars.addAll(foundJar.getNestedJars());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
        }

        foundJars.forEach(jar -> {
            try {
                scan(jar, foundJars);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return triviaSpireJar;
    }

    void scan(IJar jar, List<IJar> jars) throws IOException {
        final var nested = jar.getNestedJars();
        jars.addAll(nested);
        nested.forEach(nJar -> {
            try {
                scan(nJar, jars);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
