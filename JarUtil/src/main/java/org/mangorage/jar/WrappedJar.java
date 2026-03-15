package org.mangorage.jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public final class WrappedJar implements IJar {

    // -------------------- Factory --------------------
    public static IJar create(Path path) {
        try {
            return new WrappedJar(
                    new JarFile(
                            path.toFile(), true
                    )
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static IJar create(File file) {
        return create(file.toPath());
    }

    public static IJar create(URI uri) {
        return create(Path.of(uri));
    }

    public static IJar create(URL url) {
        try {
            return create(url.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private final JarFile jarFile;

    WrappedJar(JarFile jarFile) {
        this.jarFile = jarFile;
    }


    public static IJar create(JarFile jarFile) {
        return new WrappedJar(jarFile);
    }

    @Override
    public String getName() {
        return jarFile.getName();
    }

    // -------------------- IJar --------------------
    @Override
    public URL getURL() {
        try {
            return new File(jarFile.getName()).toURI().toURL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IJar getNestedJar(String path) {
        try {
            JarEntry entry = jarFile.getJarEntry(path);
            if (entry == null || entry.isDirectory()) return null;

            try (InputStream in = jarFile.getInputStream(entry)) {
                byte[] bytes = in.readAllBytes();
                return VirtualJar.create(bytes, entry.getName(), jarFile.getName());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean exists(String path) {
        return jarFile.getJarEntry(path) != null;
    }

    @Override
    public InputStream getInputStream(String path) {
        try {
            JarEntry entry = jarFile.getJarEntry(path);
            if (entry == null) return null;
            return jarFile.getInputStream(entry);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] readBytes(String path) {
        try (InputStream in = getInputStream(path)) {
            return in != null ? in.readAllBytes() : null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> listEntries() {
        return jarFile.stream().map(JarEntry::getName).collect(Collectors.toList());
    }

    @Override
    public List<String> listEntries(String directory) {
        if (!directory.endsWith("/")) directory += "/";
        String finalDirectory = directory;
        return jarFile.stream()
                .map(JarEntry::getName)
                .filter(n -> n.startsWith(finalDirectory))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isDirectory(String path) {
        JarEntry entry = jarFile.getJarEntry(path);
        return entry != null && entry.isDirectory();
    }

    @Override
    public Manifest getManifest() throws IOException {
        return jarFile.getManifest();
    }

    @Override
    public List<IJar> getNestedJars() {
        return jarFile.stream()
                .filter(e -> !e.isDirectory())
                .filter(e -> e.getName().endsWith(".jar"))
                .map(e -> {
                    try (InputStream in = jarFile.getInputStream(e)) {
                        byte[] bytes = in.readAllBytes();
                        return VirtualJar.create(bytes, e.getName(), jarFile.getName());
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .collect(Collectors.toList());
    }


}