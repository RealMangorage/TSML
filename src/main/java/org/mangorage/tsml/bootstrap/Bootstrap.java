package org.mangorage.tsml.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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
        Path rootDir = Path.of("").toAbsolutePath();

        URL bootstrapJar = Bootstrap.class.getProtectionDomain().getCodeSource().getLocation();

        final var list = getNestedPathsFromStream(bootstrapJar.openStream(), "JarJarLoader");

        try (final var urlClassloader = new BootstrapURLClassloader(new URL[]{bootstrapJar}, list, Bootstrap.class.getClassLoader())) {
            Thread.currentThread().setContextClassLoader(urlClassloader);
            final var mainClass = Class.forName("org.mangorage.tsml.internal.TSML", true, urlClassloader);
            mainClass.getMethod("main", String[].class, URL.class).invoke(null, (Object) args, bootstrapJar);
        }
    }
}