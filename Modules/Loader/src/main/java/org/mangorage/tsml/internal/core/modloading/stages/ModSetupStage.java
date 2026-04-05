package org.mangorage.tsml.internal.core.modloading.stages;

import org.mangorage.jar.api.IJar;
import org.mangorage.jar.SpeedyJarClassLoader;
import org.mangorage.jar.api.JarWithMetadata;
import org.mangorage.tsml.api.TSMLLogger;
import org.mangorage.tsml.api.logger.ILogger;
import org.mangorage.tsml.api.mod.Environment;
import org.mangorage.tsml.api.mod.IEarlyMod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;

public final class ModSetupStage {
    record StageResult(TSMLClassloader classloader, String foundClass) {}

    static String getMainClass(IJar jar) throws IOException {
        final var manifest = jar.getManifest();
        if (manifest == null)
            throw new IllegalStateException("Unable to find manifest for TriviaSpire");
        return manifest.getMainAttributes().getValue("Main-Class");
    }

    enum TSMLType {
        MOD,
        LIBRARY
    }

    static TSMLType getType(IJar jar) {
        try {
            final var attribute = jar.getManifestAttribute("TSMLType");
            if (attribute == null) return TSMLType.LIBRARY;
            if (attribute.equalsIgnoreCase("mod")) return TSMLType.MOD;
        } catch (IOException e) {
            TSMLLogger.getLogger().warn("Failed to read manifest of jar: " + jar.getName());
        }
        return TSMLType.LIBRARY;
    }

    StageResult run(List<JarWithMetadata> classpathJars, IJar triviaJar, Consumer<ILogger> loaderLoggerConsumer, Consumer<Environment> environmentConsumer) throws ClassNotFoundException, IOException {

        classpathJars = DependencyResolver.resolve(classpathJars);

        List<IJar> libraryJars = classpathJars.stream()
                .map(JarWithMetadata::getJar)
                .filter(jar -> getType(jar) == TSMLType.LIBRARY)
                .toList();

        List<IJar> modJars = new ArrayList<>(
                classpathJars.stream()
                        .map(JarWithMetadata::getJar)
                        .filter(jar -> getType(jar) == TSMLType.MOD)
                        .toList()
        );

        modJars.add(triviaJar); // Make it transformable...

        final ClassLoader current = Thread.currentThread().getContextClassLoader();
        final TSMLClassloader tsmlClassloader = new TSMLClassloader(modJars, new SpeedyJarClassLoader(libraryJars, current));
        Thread.currentThread().setContextClassLoader(tsmlClassloader);


        final var clientClass = "com.imjustdoom.triviaspire.lwjgl3.Lwjgl3Launcher";
        final var serverClass = "com.imjustdoom.triviaspire.server.ServerLauncher";
        final var foundClass = getMainClass(triviaJar);

        final var triviaLogger = tsmlClassloader.loadClass("com.imjustdoom.triviaspire.shared.TriviaLogger");
        final var triviaSpireReflectiveLogger = new TSMLTriviaSpireReflectiveLogger(triviaLogger);

        loaderLoggerConsumer.accept(triviaSpireReflectiveLogger);

        TSMLLogger.getLogger().info("Started TriviaSpire ModLoader");

        if (foundClass.equals(clientClass)) {
            TSMLLogger.getLogger().info("Detected client environment");
            environmentConsumer.accept(Environment.CLIENT);
        } else if (foundClass.equals(serverClass)) {
            TSMLLogger.getLogger().info("Detected server environment");
            environmentConsumer.accept(Environment.SERVER);
        } else {
            TSMLLogger.getLogger().warn("Could not detect environment, found main class: " + foundClass);
        }

        ServiceLoader.load(IEarlyMod.class, tsmlClassloader)
                .stream()
                .forEach(provider -> {
                        try {
                            var mod = provider.get();
                            TSMLLogger.getLogger().info("Found early mod: " + mod.getClass().getName());
                        } catch (Throwable t) {
                            TSMLLogger.getLogger().warn("Failed to load early mod provider: " + provider.type());
                            TSMLLogger.getLogger().error(t);
                        }
                    });

        ServiceLoader.load(ILogger.class, tsmlClassloader).stream()
                .limit(1)
                .findAny()
                .ifPresentOrElse(provider -> {
                    loaderLoggerConsumer.accept(provider.get());
                    TSMLLogger.getLogger().info("Found custom logger provider: " + provider.type());
                    TSMLLogger.getLogger().info("Using custom logger");
                }, () -> {
                    TSMLLogger.getLogger().warn("No custom logger provider found, using default logger");
                });

        tsmlClassloader.init();

        return new StageResult(tsmlClassloader, foundClass);
    }
}
