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

public final class VirtualJar implements IJar {

    public static IJar create(byte[] jarBytes, String name, String originPath) throws IOException {
        return new VirtualJar(jarBytes, name, originPath);
    }

    private final JarInputStream jarInputStream;
    private final int length;

    private final String name;
    private final String originPath;
    private final String fullPath;

    private final URL URL;

    private final Map<String, byte[]> entries = new HashMap<>();
    private final Set<String> directories = new HashSet<>();
    private final List<IJar> nestedJars = new ArrayList<>();

    private Manifest manifest;

    VirtualJar(byte[] jarBytes, String name, String originPath) throws IOException {
        this.jarInputStream = new JarInputStream(new ByteArrayInputStream(jarBytes));
        this.length = jarBytes.length;
        this.name = name;
        this.originPath = originPath;
        this.fullPath = originPath != null ? originPath + "!/" + name : name;

        this.URL = new URL("file", null, -1, fullPath, new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) {
                return new URLConnection(u) {

                    @Override
                    public void connect() {

                    }

                    @Override
                    public InputStream getInputStream() {
                        return jarInputStream;
                    }

                    @Override
                    public int getContentLength() {
                        return length;
                    }
                };
            }
        });

        indexJar();
    }

    private void indexJar() {
        try (JarInputStream jis = jarInputStream) {

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
                    nestedJars.add(create(data, entryName, fullPath));
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
        return URL;
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