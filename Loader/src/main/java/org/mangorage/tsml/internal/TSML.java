package org.mangorage.tsml.internal;

import org.mangorage.tsml.api.TSMLLogger;
import org.mangorage.tsml.api.logger.ILoaderLogger;
import org.mangorage.tsml.api.misc.Environment;
import org.mangorage.tsml.api.mod.IModPreLaunch;
import org.mangorage.tsml.internal.core.classloader.IJarClassloader;
import org.mangorage.tsml.internal.core.mod.TSMLModloader;
import org.mangorage.tsml.internal.core.TSMLTriviaSpireReflectiveLogger;
import org.mangorage.tsml.internal.core.nested.JarWrapper;
import org.mangorage.tsml.internal.core.nested.api.IJar;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

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

        final var baseJar = JarWrapper.create(baseResource.getFile());

        final List<IJar> nestedJars = new ArrayList<>();
        nestedJars.addAll(
                baseJar.getNestedJars()
                        .stream()
                        .filter(jar -> !jar.getName().contains("JarJarLoader"))
                        .toList()
        );


        List<IJar> builtInMods = List.copyOf(nestedJars);
        builtInMods.forEach(jar -> {
            try {
                nestedJars.addAll(jar.getNestedJars());
                TSMLLogger.getInternal().info("Built nested jar tree for built-in mod: " + jar.getURL());
            } catch (Exception e) {
                TSMLLogger.getInternal().error("Failed to build nested jar tree for built-in mod: " + jar.getURL());
                TSMLLogger.getInternal().error(e);
            }
        });


        modJars.forEach(url -> {
//            try {
//                nestedJars.addAll(buildNestedJarTreeFromMod(url));
//                TSMLLogger.getInternal().info("Built nested jar tree for mod: " + url);
//            } catch (IOException | URISyntaxException e) {
//                TSMLLogger.getInternal().error("Failed to build nested jar tree for: " + url);
//                TSMLLogger.getInternal().error(e);
//            }
        });



        final List<IJar> allIJars = new ArrayList<>();
        allIJars.add(JarWrapper.create(trivialURL.getFile()));
        allIJars.addAll(nestedJars);


        ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
        IJarClassloader tsmlLoader = new IJarClassloader(allIJars, currentLoader);

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

        ServiceLoader.load(IModPreLaunch.class, tsmlLoader).forEach(IModPreLaunch::onPreLaunch);

        TSMLModloader.add(foundClass, args);

        TSMLModloader.initMods();

    }
}
