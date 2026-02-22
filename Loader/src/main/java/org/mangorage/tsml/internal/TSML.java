package org.mangorage.tsml.internal;

import org.mangorage.tsml.TSMLLogger;
import org.mangorage.tsml.api.Environment;
import org.mangorage.tsml.api.ILogger;
import org.mangorage.tsml.api.IModPreLaunch;
import org.mangorage.tsml.internal.core.JarJarLoader;
import org.mangorage.tsml.internal.core.NestedJar;
import org.mangorage.tsml.internal.core.mod.TSMLModloader;
import org.mangorage.tsml.internal.core.TSMLTriviaSpireReflectiveLogger;
import org.mangorage.tsml.internal.core.TSMLURLClassloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * TriviaSpire ModLoader
 */
public final class TSML {

    /**
     * Finds all .jar files in a folder (non-recursive) and returns them as URLs
     *
     * @param folderPath folder to search
     * @return list of URLs pointing to jars
     * @throws IOException if folder access fails
     */
    public static List<URL> findJarURLs(Path folderPath) throws IOException {
        List<URL> jarUrls = new ArrayList<>();

        if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
            System.err.println("Invalid folder: " + folderPath);
            return jarUrls;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folderPath, "*.jar")) {
            for (Path jarPath : stream) {
                jarUrls.add(jarPath.toUri().toURL());
            }
        }

        return jarUrls;
    }

    public static String getMainClass(File jarFile) throws IOException {
        try (JarFile jar = new JarFile(jarFile)) {
            Manifest manifest = jar.getManifest();
            if (manifest == null) return null;

            return manifest.getMainAttributes().getValue("Main-Class");
        }
    }

    private static Environment environment;

    public static Environment getEnvironment() {
        return environment;
    }

    public static List<String> getNestedPathsFromStream(InputStream jarStream, String prefix) throws IOException {
        List<String> internalPaths = new ArrayList<>();
        // Wrap the incoming stream in a JarInputStream to read its entries
        try (JarInputStream jis = new JarInputStream(jarStream)) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (entry.getName().startsWith(prefix + "/") && entry.getName().endsWith(".jar")) {
                    internalPaths.add(entry.getName());
                }
            }
        }
        return internalPaths;
    }

    public static void main(String[] args, URL baseResource) throws Exception {

        Path rootPath = Path.of("").toAbsolutePath();

        URL trivialURL = findJarURLs(rootPath).stream()
                .filter(url -> url.getFile().contains("TriviaSpire"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find TriviaSpire.jar in root folder"));

        List<URL> urls = new ArrayList<>();
        final var modJars = findJarURLs(rootPath.resolve("mods"));

        List<String> allNestedJars = new ArrayList<>();

        // 1. Get the Mod Jars from the Base Resource (Fat Jar)
    // This will be the list we pass to the TSMLURLClassloader
        List<NestedJar> nestedJarTree = new ArrayList<>();

        // 1. Get the Top-Level Mod Jars from the Fat Jar
        List<String> nestedModPaths = JarJarLoader.getNestedJarPaths(List.of(baseResource), "JarJarMods");

        for (String modPath : nestedModPaths) {
            // Create the parent node for the Mod
            NestedJar modNode = new NestedJar(modPath, new ArrayList<>());

            // 2. Look inside THIS specific mod for its own nested libraries
            try (InputStream modStream = TSML.class.getClassLoader().getResourceAsStream(modPath)) {
                if (modStream != null) {
                    // Find libraries inside "JarJar/" within "JarJarMods/some-mod.jar"
                    List<String> modLibs = TSML.getNestedPathsFromStream(modStream, "JarJar");

                    for (String libPath : modLibs) {
                        // Add the library as a child of the mod
                        modNode.nestedJars().add(new NestedJar(libPath, new ArrayList<>()));
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to scan libraries inside mod: " + modPath);
            }

            // Add the mod (and its children) to our tree
            nestedJarTree.add(modNode);
        }

        urls.addAll(modJars);
        urls.add(trivialURL);

        final var finalUrls = urls.toArray(new URL[0]);

        ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
        try (var tsmlLoader = new TSMLURLClassloader(finalUrls, nestedJarTree, currentLoader)) {

            Thread.currentThread().setContextClassLoader(tsmlLoader);

            final var clientClass = "com.imjustdoom.triviaspire.lwjgl3.Lwjgl3Launcher";
            final var serverClass = "com.imjustdoom.triviaspire.server.ServerLauncher";
            final var foundClass = getMainClass(new File(trivialURL.toURI()));

            final var triviaLogger = tsmlLoader.loadClass("com.imjustdoom.triviaspire.shared.TriviaLogger");
            final var triviaSpireReflectiveLogger = new TSMLTriviaSpireReflectiveLogger(triviaLogger);

            TSMLLogger.setActiveLogger(triviaSpireReflectiveLogger);

            TSMLLogger.get().info("Started TriviaSpire ModLoader");

            if (foundClass.equals(clientClass)) {
                TSMLLogger.get().info("Detected client environment");
                environment = Environment.CLIENT;
            } else if (foundClass.equals(serverClass)) {
                TSMLLogger.get().info("Detected server environment");
                environment = Environment.SERVER;
            } else {
                TSMLLogger.get().warn("Could not detect environment, found main class: " + foundClass);
                environment = Environment.UNKNOWN;
            }

            ServiceLoader.load(ILogger.class, tsmlLoader).stream()
                    .limit(1)
                    .findAny()
                    .ifPresentOrElse(provider -> {
                        TSMLLogger.setActiveLogger(provider.get());
                        TSMLLogger.get().info("Found custom logger provider: " + provider.type());
                        TSMLLogger.get().info("Using custom logger");
                    }, () -> {
                        TSMLLogger.get().warn("No custom logger provider found, using default logger");
                    });

            tsmlLoader.init();

            final var path = nestedJarTree
                    .stream()
                    .filter(nestedJar -> nestedJar.jarPath().contains("Mixin"))
                    .findAny()
                    .get();

            TSMLModloader.scanMods(
                    List.of(
                            getNestedJarPath(
                                    baseResource,
                                    path.jarPath()
                            )
                    )
            );

            ServiceLoader.load(IModPreLaunch.class, tsmlLoader).forEach(IModPreLaunch::onPreLaunch);

            TSMLModloader.initMods();

            Class<?> mainClass = tsmlLoader.loadClass("com.imjustdoom.triviaspire.lwjgl3.Lwjgl3Launcher");
            mainClass.getMethod("main", String[].class).invoke(null, (Object) args);
        }
    }

    private static String getNestedJarPath(URL rootJar, String jarName) {
        return rootJar.toString() + "!/" + jarName;
    }
}
