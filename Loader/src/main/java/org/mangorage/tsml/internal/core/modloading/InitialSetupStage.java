package org.mangorage.tsml.internal.core.modloading;

import org.mangorage.tsml.api.TSMLLogger;
import org.mangorage.tsml.api.logger.ILoaderLogger;
import org.mangorage.tsml.api.mod.Environment;
import org.mangorage.tsml.internal.core.nested.api.IJar;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.jar.Manifest;

public final class InitialSetupStage {
    record StageResult(TSMLClassloader classloader, String foundClass) {}

    static String getMainClass(IJar jar) throws IOException {
        if (!jar.exists("META-INF/MANIFEST.MF")) {
            return null;
        }

        try (InputStream in = jar.getInputStream("META-INF/MANIFEST.MF")) {
            Manifest manifest = new Manifest(in);
            return manifest.getMainAttributes().getValue("Main-Class");
        }
    }

    StageResult run(List<IJar> jars, IJar triviaJar, Consumer<ILoaderLogger> loaderLoggerConsumer, Consumer<Environment> environmentConsumer) throws ClassNotFoundException, IOException {
        final ClassLoader current = Thread.currentThread().getContextClassLoader();
        final TSMLClassloader tsmlClassloader = new TSMLClassloader(jars, current);
        Thread.currentThread().setContextClassLoader(tsmlClassloader);

        Class.forName("org.tinylog.converters.GzipEncoder", false, tsmlClassloader);

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

        ServiceLoader.load(ILoaderLogger.class, tsmlClassloader).stream()
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
