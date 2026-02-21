package org.mangorage.tsml.internal;

import org.mangorage.tsml.TSMLLogger;
import org.mangorage.tsml.api.Environment;
import org.mangorage.tsml.api.ILogger;
import org.mangorage.tsml.api.IModPreLaunch;
import org.mangorage.tsml.internal.core.JarJarLoader;
import org.mangorage.tsml.internal.core.mod.TSMLModloader;
import org.mangorage.tsml.internal.core.TSMLTriviaSpireReflectiveLogger;
import org.mangorage.tsml.internal.core.TSMLURLClassloader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.jar.JarFile;
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


    public static void main(String[] args) throws Exception {

        Path rootPath = Path.of("").toAbsolutePath();

        URL trivialURL = findJarURLs(rootPath).stream()
                .filter(url -> url.getFile().contains("TriviaSpire"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find TriviaSpire.jar in root folder"));

        List<URL> urls = new ArrayList<>();
        final var modJars = findJarURLs(rootPath.resolve("mods"));
        final var defaultModJars = findJarURLs(rootPath.resolve("defaultMods"));

        final var jarJarUrls = JarJarLoader.extractJarsFromFatJars(modJars, rootPath.resolve("extracted"));
        final var jarJarDefaultUrls = JarJarLoader.extractJarsFromFatJars(defaultModJars, rootPath.resolve("extracted"));

        urls.addAll(modJars);
        urls.addAll(jarJarUrls);

        urls.addAll(defaultModJars);
        urls.addAll(jarJarDefaultUrls);

        urls.add(trivialURL);

        final var finalUrls = urls.toArray(new URL[0]);

        ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
        try (var tsmlLoader = new TSMLURLClassloader(finalUrls, currentLoader)) {

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

            TSMLModloader.scanMods();
            ServiceLoader.load(IModPreLaunch.class).forEach(IModPreLaunch::onPreLaunch);
            TSMLModloader.initMods();

            Class<?> mainClass = tsmlLoader.loadClass("com.imjustdoom.triviaspire.lwjgl3.Lwjgl3Launcher");
            mainClass.getMethod("main", String[].class).invoke(null, (Object) args);
        }
    }
}
