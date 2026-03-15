package org.mangorage.jar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public final class JarInJar implements IJar {

    private final byte[] jarBytes;
    private final String name;
    private final String originPath;

    private final Map<String, byte[]> entries = new HashMap<>();
    private final Set<String> directories = new HashSet<>();
    private final List<IJar> nestedJars = new ArrayList<>();

    private Manifest manifest;

    public JarInJar(byte[] jarBytes, String name, String originPath) {
        this.jarBytes = jarBytes;
        this.name = name;
        this.originPath = originPath;

        indexJar();
    }

    private void indexJar() {
        try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(jarBytes))) {

            manifest = jis.getManifest();

            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {

                String entryName = entry.getName();

                if (entry.isDirectory()) {
                    directories.add(entryName);
                    continue;
                }

                byte[] data = jis.readAllBytes();
                entries.put(entryName, data);

                // Track directories
                String dir = entryName;
                int slash;
                while ((slash = dir.lastIndexOf('/')) != -1) {
                    dir = dir.substring(0, slash + 1);
                    directories.add(dir);
                    dir = dir.substring(0, dir.length() - 1);
                }

                // Nested jar detection
                if (entryName.endsWith(".jar")) {
                    nestedJars.add(new JarInJar(data, entryName, originPath + "!/" + name));
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------- Basic Info ----------------

    @Override
    public String getName() {
        return name;
    }

    @Override
    public URL getURL() {
        try {
            return new URL("file", null, -1, originPath + "!/" + name, new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL u) {
                    return new URLConnection(u) {

                        @Override
                        public void connect() {}

                        @Override
                        public InputStream getInputStream() {
                            return new ByteArrayInputStream(jarBytes);
                        }

                        @Override
                        public int getContentLength() {
                            return jarBytes.length;
                        }
                    };
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------- Nested Jar Support ----------------

    @Override
    public IJar getNestedJar(String path) {
        for (IJar jar : nestedJars) {
            if (jar.getName().equals(path)) {
                return jar;
            }
        }
        return null;
    }

    @Override
    public List<IJar> getNestedJars() {
        return new ArrayList<>(nestedJars);
    }

    // ---------------- File Access ----------------

    @Override
    public boolean exists(String path) {
        return entries.containsKey(path) || directories.contains(path);
    }

    @Override
    public InputStream getInputStream(String path) {
        byte[] data = entries.get(path);
        return data == null ? null : new ByteArrayInputStream(data);
    }

    @Override
    public byte[] readBytes(String path) {
        return entries.get(path);
    }

    // ---------------- Listing ----------------

    @Override
    public List<String> listEntries() {
        return new ArrayList<>(entries.keySet());
    }

    @Override
    public List<String> listEntries(String directory) {

        if (!directory.endsWith("/")) {
            directory += "/";
        }

        List<String> result = new ArrayList<>();

        for (String entry : entries.keySet()) {
            if (entry.startsWith(directory)) {
                result.add(entry);
            }
        }

        for (String dir : directories) {
            if (dir.startsWith(directory)) {
                result.add(dir);
            }
        }

        return result;
    }

    // ---------------- Directory Check ----------------

    @Override
    public boolean isDirectory(String path) {
        return directories.contains(path);
    }

    // ---------------- Manifest ----------------

    @Override
    public Manifest getManifest() {
        return manifest;
    }
}