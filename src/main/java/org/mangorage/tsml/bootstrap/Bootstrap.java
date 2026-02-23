package org.mangorage.tsml.bootstrap;

import org.mangorage.tsml.bootstrap.internal.TSMLDefaultLogger;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public final class Bootstrap {

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

    public static void main(String[] args) throws Exception {
        URL bootstrapJar = Bootstrap.class.getProtectionDomain().getCodeSource().getLocation();

        TSMLDefaultLogger.getInstance().info("Bootstrap jar URL: " + bootstrapJar);
        TSMLDefaultLogger.getInstance().info("Extracting nested jars from bootstrap jar...");
        TSMLDefaultLogger.getInstance().info("Looking for entries in JarJarLoader/ that end with .jar");
        TSMLDefaultLogger.getInstance().info("This may take a moment...");

        final var list = getNestedPathsFromStream(bootstrapJar.openStream(), "JarJarLoader");

        TSMLDefaultLogger.getInstance().info("Found " + list.size() + " nested jars:");
        TSMLDefaultLogger.getInstance().info("Creating classloader and starting TSML...");

        try (final var urlClassloader = new BootstrapURLClassloader(new URL[]{bootstrapJar}, list, Bootstrap.class.getClassLoader())) {
            Thread.currentThread().setContextClassLoader(urlClassloader);
            final var mainClass = Class.forName("org.mangorage.tsml.internal.TSML", true, urlClassloader);
            TSMLDefaultLogger.getInstance().info("Found TSML main class: " + mainClass);
            TSMLDefaultLogger.getInstance().info(mainClass.getName());

            for (Method method : mainClass.getMethods()) {
                TSMLDefaultLogger.getInstance().info("Method: " + method);
            }

            TSMLDefaultLogger.getInstance().info("Looking for initPublic method...");
            final var method = mainClass.getMethod("initPublic", String[].class, URL.class);
            TSMLDefaultLogger.getInstance().info("Invoking TSML initPublic method...");
            TSMLDefaultLogger.getInstance().info(method.toString());
            method.invoke(null, (Object) args, bootstrapJar);
        } catch (Throwable e) {
            TSMLDefaultLogger.getInstance().error("Failed to start TSML:");
            TSMLDefaultLogger.getInstance().error(e);
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                TSMLDefaultLogger.getInstance().error("\t" + stackTraceElement);
            }
        }
    }
}