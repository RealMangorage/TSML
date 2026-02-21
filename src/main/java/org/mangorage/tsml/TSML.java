package org.mangorage.tsml;

import org.mangorage.tsml.api.IMod;
import org.mangorage.tsml.api.IModHandler;
import org.mangorage.tsml.core.JarJarLoader;
import org.mangorage.tsml.core.TSMLURLClassloader;

import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

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

    public static void main(String[] args) throws Exception{
        Path rootPath = Path.of("").toAbsolutePath();

        URL trivialURL = findJarURLs(rootPath).stream()
                .filter(url -> url.getFile().contains("TriviaSpire"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find TriviaSpire.jar in root folder"));

        List<URL> urls = new ArrayList<>();
        final var modJars = findJarURLs(rootPath.resolve("mods"));
        final var jarJarUrls = JarJarLoader.extractJarsFromFatJars(modJars, rootPath.resolve("extracted"));

        urls.addAll(jarJarUrls);
        urls.addAll(modJars);
        urls.add(trivialURL);

        final var finalUrls = urls.toArray(new URL[0]);

        ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
        try (var tsmlLoader = new TSMLURLClassloader(finalUrls, currentLoader)) {

            Thread.currentThread().setContextClassLoader(tsmlLoader);

            tsmlLoader.init();

            final var modHandlers = ServiceLoader.load(IModHandler.class)
                    .stream()
                    .map(ServiceLoader.Provider::get)
                    .collect(Collectors.toSet());

            ServiceLoader.load(IMod.class).forEach(mod -> {
                System.out.println("Loaded mod: " + mod.getName());
                modHandlers.forEach(handler -> handler.handleMod(mod));
                mod.onInitialize();
            });

            Class<?> mainClass = tsmlLoader.loadClass("com.imjustdoom.triviaspire.lwjgl3.Lwjgl3Launcher");
            mainClass.getMethod("main", String[].class).invoke(null, (Object) new String[]{});
        }
    }
}
