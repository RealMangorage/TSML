package org.mangorage.tsml.internal.core.modloading.stages;

import org.mangorage.jar.api.IJar;
import org.mangorage.jar.VFSJar;
import org.mangorage.jar.api.JarWithMetadata;
import org.mangorage.tsml.internal.core.jarjar.JarJarLocator;
import org.mangorage.tsml.internal.core.modloading.JarJarResolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
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

    public static IJar findTriviaSpireJar(Path rootPath, List<String> args) throws IOException {
        // 1️⃣ Check if the user supplied --TriviaSpireJar <path>
        for (int i = 0; i < args.size() - 1; i++) {
            if ("--TriviaSpireJar".equals(args.get(i))) {
                Path jarPath = Path.of(args.get(i + 1));
                if (!jarPath.toFile().exists()) {
                    throw new RuntimeException("Specified TriviaSpireJar does not exist: " + jarPath);
                }
                return VFSJar.create(jarPath);
            }
        }

        // 2️⃣ Fallback: search the rootPath for a TriviaSpire jar
        Optional<IJar> foundJar = findJarPaths(rootPath)
                .stream()
                .map(Path::toFile)
                .filter(file -> file.getName().endsWith(".jar"))
                .filter(file -> file.getName().contains("TriviaSpire"))
                .map(File::toPath)
                .map(VFSJar::create)
                .findFirst();

        return foundJar.orElseThrow(() -> new RuntimeException(
                "Could not find TriviaSpire.jar in root folder: " + rootPath
        ));
    }


    /**
     * @param loaderJarPath → Will be the TSML jar itself
     * @param finalJars → Add any jars you want exposed.
     * @return The main TriviaSpire.jar, used for the next stage to find the main class and logger class.
     */
    public IJar run(Path loaderJarPath, List<JarWithMetadata> finalJars, String[] args) throws IOException {
        Path rootPath = Path.of("");
        Path modsPath = rootPath.resolve("mods").toAbsolutePath();

        List<IJar> foundJars = new CopyOnWriteArrayList<>();

        IJar triviaSpireJar =  findTriviaSpireJar(rootPath, List.of(args));

        IJar tsmlJar = VFSJar.create(loaderJarPath);

        foundJars.add(tsmlJar);

        if (Files.exists(modsPath)) {
            try (Stream<Path> stream = Files.list(modsPath)) {
                stream
                        .filter(Files::isRegularFile)
                        .map(VFSJar::create)
                        .forEach(foundJars::add);
            }
        }

        // TODO: Update to use service loader for getting locators perhaps? Dunno yet!
        final List<JarWithMetadata> resolvedJars = JarJarResolver.resolveAll(List.of(new JarJarLocator()), JarWithMetadata.empty(foundJars));

        finalJars.addAll(resolvedJars);

        return triviaSpireJar;
    }

}
