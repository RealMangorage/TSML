package org.mangorage.tsml.internal;

import org.mangorage.tsml.api.TSMLLogger;
import org.mangorage.tsml.api.logger.ILoaderLogger;
import org.mangorage.tsml.api.misc.Environment;
import org.mangorage.tsml.api.mod.IModPreLaunch;
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

    private static boolean loaded = false;

    private static ILoaderLogger activeLogger = null;

    static void setActiveLogger(ILoaderLogger logger) {
        activeLogger = logger;
    }

    public static ILoaderLogger getActiveLogger() {
        return activeLogger;
    }

    private static Environment environment;

    public static Environment getEnvironment() {
        return environment;
    }

    public static void initPublic(String[] args, URL baseResource) throws Exception {
        if (loaded) {
            TSMLLogger.getInternal().warn("TSML is already initialized, skipping");
            return;
        }

        loaded = true;

        try {
            init(args, baseResource);
        } catch (Exception e) {
            TSMLLogger.getInternal().error("Failed to initialize TSML");
            TSMLLogger.getInternal().error(e);
            throw e;
        }
    }

    static void init(String[] args, URL baseResource) throws Exception {
        Path rootPath = Path.of("").toAbsolutePath();

        URL trivialURL = findJarURLs(rootPath).stream()
                .filter(url -> url.getFile().endsWith(".jar"))
                .filter(url -> url.getFile().contains("TriviaSpire"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find TriviaSpire.jar in root folder"));

        final var modJars = findJarURLs(rootPath.resolve("mods"));

        List<URL> urls = new ArrayList<>(modJars);
        urls.add(trivialURL);

        final var finalUrls = urls.toArray(new URL[0]);

        final List<NestedJar> nestedJars = new ArrayList<>(buildNestedJarTree(baseResource));

        modJars.forEach(url -> {
            try {
                nestedJars.addAll(buildNestedJarTreeFromMod(url));
                TSMLLogger.getInternal().info("Built nested jar tree for mod: " + url);
            } catch (IOException | URISyntaxException e) {
                TSMLLogger.getInternal().error("Failed to build nested jar tree for: " + url);
                TSMLLogger.getInternal().error(e);
            }
        });


        ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
        try (var tsmlLoader = new TSMLURLClassloader(finalUrls, nestedJars, currentLoader)) {

            Thread.currentThread().setContextClassLoader(tsmlLoader);

            /**
             * No idea why,
             * but we need
             * to do this so we don't get a {@link NoClassDefFoundError} Exception when exiting game
              */
            Class.forName("org.tinylog.converters.GzipEncoder", false, tsmlLoader);

            final var clientClass = "com.imjustdoom.triviaspire.lwjgl3.Lwjgl3Launcher";
            final var serverClass = "com.imjustdoom.triviaspire.server.ServerLauncher";
            final var foundClass = getMainClass(new File(trivialURL.toURI()));

            final var triviaLogger = tsmlLoader.loadClass("com.imjustdoom.triviaspire.shared.TriviaLogger");
            final var triviaSpireReflectiveLogger = new TSMLTriviaSpireReflectiveLogger(triviaLogger);

            /**
             * Can now use {@link TSMLLogger#getLogger()}
             */
            setActiveLogger(triviaSpireReflectiveLogger);

            TSMLLogger.getLogger().info("Started TriviaSpire ModLoader");

            if (foundClass.equals(clientClass)) {
                TSMLLogger.getLogger().info("Detected client environment");
                environment = Environment.CLIENT;
            } else if (foundClass.equals(serverClass)) {
                TSMLLogger.getLogger().info("Detected server environment");
                environment = Environment.SERVER;
            } else {
                TSMLLogger.getLogger().warn("Could not detect environment, found main class: " + foundClass);
                environment = Environment.UNKNOWN;
            }

            ServiceLoader.load(ILoaderLogger.class, tsmlLoader).stream()
                    .limit(1)
                    .findAny()
                    .ifPresentOrElse(provider -> {
                        setActiveLogger(provider.get());
                        TSMLLogger.getLogger().info("Found custom logger provider: " + provider.type());
                        TSMLLogger.getLogger().info("Using custom logger");
                    }, () -> {
                        TSMLLogger.getLogger().warn("No custom logger provider found, using default logger");
                    });

            tsmlLoader.init();

            final var path = nestedJars
                    .stream()
                    .filter(nestedJar -> nestedJar.resourcePath().contains("Mixin"))
                    .findAny()
                    .get();

            TSMLModloader.scanMods(
                    List.of(
                            getNestedJarPath(
                                    baseResource,
                                    path.resourcePath()
                            )
                    ),
                    foundClass,
                    args
            );

            ServiceLoader.load(IModPreLaunch.class, tsmlLoader).forEach(IModPreLaunch::onPreLaunch);

            TSMLModloader.initMods();
        }
    }
}
