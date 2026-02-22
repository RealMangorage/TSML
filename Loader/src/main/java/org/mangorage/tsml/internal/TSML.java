package org.mangorage.tsml.internal;

import org.mangorage.tsml.TSMLLogger;
import org.mangorage.tsml.api.Environment;
import org.mangorage.tsml.api.ILogger;
import org.mangorage.tsml.api.IModPreLaunch;
import org.mangorage.tsml.internal.core.classloader.NestedJar;
import org.mangorage.tsml.internal.core.mod.TSMLModloader;
import org.mangorage.tsml.internal.core.TSMLTriviaSpireReflectiveLogger;
import org.mangorage.tsml.internal.core.classloader.TSMLURLClassloader;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import static org.mangorage.tsml.internal.core.util.NestedUtil.*;
import static org.mangorage.tsml.internal.core.util.Util.*;

/**
 * TriviaSpire ModLoader
 */
public final class TSML {

    private static Environment environment;

    public static Environment getEnvironment() {
        return environment;
    }

    public static void init(String[] args, URL baseResource) throws Exception {

        Path rootPath = Path.of("").toAbsolutePath();

        URL trivialURL = findJarURLs(rootPath).stream()
                .filter(url -> url.getFile().contains("TriviaSpire"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find TriviaSpire.jar in root folder"));

        List<URL> urls = new ArrayList<>();
        final var modJars = findJarURLs(rootPath.resolve("mods"));

        urls.addAll(modJars);
        urls.add(trivialURL);

        final var finalUrls = urls.toArray(new URL[0]);

        final List<NestedJar> nestedJars = new ArrayList<>();
        nestedJars.addAll(buildNestedJarTree(baseResource));

        modJars.forEach(url -> {
            try {
                nestedJars.addAll(buildNestedJarTreeFromMod(url));
            } catch (IOException | URISyntaxException e) {
                System.err.println("Failed to build nested jar tree for: " + url);
                e.printStackTrace();
            }
        });


        ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
        try (var tsmlLoader = new TSMLURLClassloader(finalUrls, nestedJars, currentLoader)) {

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

            final var path = nestedJars
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
}
