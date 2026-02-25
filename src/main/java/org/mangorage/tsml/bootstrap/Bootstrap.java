package org.mangorage.tsml.bootstrap;

import org.mangorage.tsml.bootstrap.internal.TSMLDefaultLogger;
import org.mangorage.tsml.bootstrap.internal.core.nested.WrappedJar;
import org.mangorage.tsml.bootstrap.internal.core.nested.api.IJar;
import java.net.URL;
import java.util.List;

public final class Bootstrap {
    private static final String LAUNCH_CLASS = "org.mangorage.tsml.internal.core.modloading.ModLoadingManager";


    public static void main(String[] args) {
        try {
            URL bootstrapJarURL = Bootstrap.class.getProtectionDomain().getCodeSource().getLocation();
            IJar bootstrapJar = WrappedJar.create(bootstrapJarURL.getFile());


            TSMLDefaultLogger.getInstance().info("Starting TSML Bootstrap...");
            TSMLDefaultLogger.getInstance().info("This may take a moment...");

            TSMLDefaultLogger.getInstance().info("Looking for JarJarLoader nested jar...");
            final List<IJar> loaderJars = bootstrapJar.getNestedJars()
                    .stream()
                    .filter(jar -> jar.getName().startsWith("JarJarLoader/") && jar.getName().endsWith(".jar"))
                    .toList();

            if (loaderJars.isEmpty()) {
                TSMLDefaultLogger.getInstance().info("Could not find JarJarLoader nested jars in TSML bootstrap jar");
                throw new RuntimeException("Could not find JarJarLoader nested jar in TSML bootstrap jar");
            }

            TSMLDefaultLogger.getInstance().info("Creating classloader and starting TSML...");

            final var bootstrapClassloader = new BootstrapClassloader(loaderJars, Bootstrap.class.getClassLoader());

            Thread.currentThread().setContextClassLoader(bootstrapClassloader);
            final var mainClass = Class.forName(LAUNCH_CLASS, true, bootstrapClassloader);
            TSMLDefaultLogger.getInstance().info("Found TSML main class: " + mainClass);
            TSMLDefaultLogger.getInstance().info(mainClass.getName());

            TSMLDefaultLogger.getInstance().info("Looking for run method...");
            final var method = mainClass.getMethod("run", URL.class, String[].class);
            TSMLDefaultLogger.getInstance().info("Invoking TSML initPublic method...");
            TSMLDefaultLogger.getInstance().info(method.toString());
            method.invoke(null, bootstrapJarURL, (Object) args);
        } catch (Throwable e) {
            TSMLDefaultLogger.getInstance().error("Failed to start TSML:");
            TSMLDefaultLogger.getInstance().error(e);

            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                TSMLDefaultLogger.getInstance().error("\t" + stackTraceElement);
            }
        }
    }
}