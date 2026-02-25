package org.mangorage.tsml.internal.core.modloading;

import org.mangorage.tsml.api.TSMLLogger;
import org.mangorage.tsml.internal.core.nested.WrappedJar;
import org.mangorage.tsml.internal.core.nested.api.IJar;

import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DiscoveryStage {

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
     * @param jars → Add any jars you want exposed.
     * @return The main TriviaSpire.jar, used for the next stage to find the main class and logger class.
     */
    public IJar run(URL baseResource, List<IJar> jars) throws IOException {
        Path rootPath = Path.of("");
        Path modsPath = rootPath.resolve("mods");

        IJar triviaSpireJar = findJarPaths(rootPath)
                .stream()
                .map(Path::toFile)
                .filter(file -> file.getName().endsWith(".jar"))
                .filter(file -> file.getName().contains("TriviaSpire"))
                .map(WrappedJar::create)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find TriviaSpire.jar in root folder"));

        IJar tsmlJar = WrappedJar.create(baseResource.getFile());

        // External TSMLLoaderAPI in mods folder
        final List<IJar> modJars = findJarPaths(modsPath)
                .stream()
                .filter(path -> path.endsWith(".jar"))
                .map(WrappedJar::create)
                .toList();

        // Internal TSMLLoaderAPI, included by default
        final List<IJar> builtInMods = new ArrayList<>(tsmlJar.getNestedJars()
                .stream()
                .filter(jar -> !jar.getName().contains("JarJarLoader"))
                .toList()
        );

        // Get all JarJared Libraries for mods
        final List<IJar> libraryJars = new ArrayList<>();

        // Get All Libraries for BuiltIn TSMLLoaderAPI
        builtInMods.forEach(jar -> {
            try {
                libraryJars.addAll(jar.getNestedJars());
                TSMLLogger.getInternal().info("Built nested jar tree for built-in mod: " + jar.getURL());
            } catch (Exception e) {
                TSMLLogger.getInternal().error("Failed to build nested jar tree for built-in mod: " + jar.getURL());
                TSMLLogger.getInternal().error(e);
            }
        });

        // Get All Libraries for External TSMLLoaderAPI
        modJars.forEach(modJar -> {
            try {
                libraryJars.addAll(modJar.getNestedJars());
                TSMLLogger.getInternal().info("Built nested jar tree for mod: " + modJar.getURL());
            } catch (IOException e) {
                TSMLLogger.getInternal().error("Failed to build nested jar tree for: " + modJar.getURL());
                TSMLLogger.getInternal().error(e);
            }
        });

        jars.add(triviaSpireJar);
        jars.addAll(modJars);
        jars.addAll(builtInMods);
        // TODO: Handle Better library conflict resolutions.
        jars.addAll(libraryJars);

        return triviaSpireJar;
    }
}
