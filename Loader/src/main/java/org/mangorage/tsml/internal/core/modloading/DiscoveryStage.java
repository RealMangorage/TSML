package org.mangorage.tsml.internal.core.modloading;

import org.mangorage.tsml.internal.core.jarjar.JarCouple;
import org.mangorage.tsml.internal.core.jarjar.JarJarHelper;
import org.mangorage.tsml.internal.core.nested.WrappedJar;
import org.mangorage.tsml.internal.core.nested.api.IJar;

import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
     * @param finalClasspathJars → Add any jars you want exposed.
     * @return The main TriviaSpire.jar, used for the next stage to find the main class and logger class.
     */
    public IJar run(URL baseResource, List<IJar> finalClasspathJars) throws IOException {
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


        finalClasspathJars.add(triviaSpireJar);
        finalClasspathJars.addAll(modJars);

        // Jar Coupling

        final List<JarCouple> jarInJarsBuiltInMods = new ArrayList<>();
        JarJarHelper.scanJar(tsmlJar, jarInJarsBuiltInMods, true);

        final List<JarCouple> jarInJarBuiltInModsLibraries = new ArrayList<>();
        jarInJarsBuiltInMods.forEach(couple -> {
            try {
                final var builtInJar = couple.jar().getNestedJar(couple.metadata().path());
                JarJarHelper.scanJar(builtInJar, jarInJarBuiltInModsLibraries, false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });

        final List<JarCouple> jarInJars = new ArrayList<>();
        modJars.forEach(jar -> JarJarHelper.scanJar(jar, jarInJars, true));


        final List<JarCouple> finalJarCouples = new ArrayList<>();
        finalJarCouples.addAll(jarInJarsBuiltInMods);
        finalJarCouples.addAll(jarInJarBuiltInModsLibraries);
        finalJarCouples.addAll(jarInJars);

        final List<IJar> finalJars = new ArrayList<>();

        finalJarCouples.forEach(couple -> {
            try {
                final var nestedJar = couple.jar().getNestedJar(couple.metadata().path());
                finalClasspathJars.add(nestedJar);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });


        return triviaSpireJar;
    }
}
