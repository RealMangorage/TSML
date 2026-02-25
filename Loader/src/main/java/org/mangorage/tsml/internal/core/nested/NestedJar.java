package org.mangorage.tsml.internal.core.nested;


import org.mangorage.tsml.internal.core.nested.api.IJar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public final class NestedJar implements IJar {

    private final byte[] jarBytes;
    private final String name;
    private final String originPath;

    public NestedJar(byte[] jarBytes, String name, String originPath) {
        this.jarBytes = jarBytes;
        this.name = name;
        this.originPath = originPath;
    }

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
                        @Override public void connect() {}
                        @Override public InputStream getInputStream() {
                            return new ByteArrayInputStream(jarBytes);
                        }
                        @Override public int getContentLength() { return jarBytes.length; }
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
        try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(jarBytes))) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().equals(path)) {
                    byte[] bytes = jis.readAllBytes();
                    return new NestedJar(bytes, entry.getName(), originPath + "!/" + name);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public List<IJar> getNestedJars() {
        List<IJar> nested = new ArrayList<>();
        try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(jarBytes))) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".jar")) {
                    byte[] bytes = jis.readAllBytes();
                    nested.add(new NestedJar(bytes, entry.getName(), originPath + "!/" + name));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return nested;
    }

    // ---------------- File Access ----------------
    @Override
    public boolean exists(String path) {
        return getInputStream(path) != null;
    }

    @Override
    public InputStream getInputStream(String path) {
        try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(jarBytes))) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (entry.getName().equals(path)) {
                    byte[] bytes = jis.readAllBytes();
                    return new ByteArrayInputStream(bytes);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public byte[] readBytes(String path) {
        try (InputStream in = getInputStream(path)) {
            if (in == null) return null;
            return in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> listEntries() {
        List<String> entries = new ArrayList<>();
        try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(jarBytes))) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                entries.add(entry.getName());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return entries;
    }

    @Override
    public List<String> listEntries(String directory) {
        if (!directory.endsWith("/")) directory += "/";
        List<String> entries = new ArrayList<>();
        try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(jarBytes))) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (entry.getName().startsWith(directory)) {
                    entries.add(entry.getName());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return entries;
    }

    @Override
    public boolean isDirectory(String path) {
        try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(jarBytes))) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (entry.getName().equals(path)) return entry.isDirectory();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;
    }
}