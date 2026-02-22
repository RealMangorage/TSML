package org.mangorage.tsml.internal.core.classloader;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public final class NestedJarHandler {

    private final List<NestedJar> nestedJars;
    private final ClassLoader classloader;

    public NestedJarHandler(List<NestedJar> nestedJars, ClassLoader classLoader) {
        this.nestedJars = nestedJars;
        this.classloader = classLoader;
    }

    // -------------------- Carrier objects --------------------

    public record ResourceWithOrigin(byte[] bytes, URL originJar) {}

    // -------------------- Class Bytes --------------------

    public ClassBytesWithCodeSource getNestedClassBytes(String className) {
        String path = className.replace('.', '/') + ".class";

        // 1. Try standard classloader first
        try (InputStream is = classloader.getResourceAsStream(path)) {
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                URL origin = classloader.getResource(path);
                return new ClassBytesWithCodeSource(bytes, origin);
            }
        } catch (IOException ignored) {}

        // 2. Recurse nested jars
        for (NestedJar node : nestedJars) {
            ClassBytesWithCodeSource found = searchRecursive(node, path);
            if (found != null) return found;
        }

        return null;
    }

    private ClassBytesWithCodeSource searchRecursive(NestedJar node, String targetPath) {
        try (InputStream jarStream = classloader.getParent().getResourceAsStream(node.resourcePath())) {
            if (jarStream == null) return null;

            try (JarInputStream jis = new JarInputStream(jarStream)) {
                JarEntry entry;
                while ((entry = jis.getNextJarEntry()) != null) {

                    if (entry.getName().equals(targetPath)) {
                        byte[] bytes = jis.readAllBytes();
                        return new ClassBytesWithCodeSource(bytes, createOriginURL(node, bytes));
                    }

                    for (NestedJar child : node.nestedJars()) {
                        if (entry.getName().equals(child.resourcePath())) {
                            ClassBytesWithCodeSource found =
                                    searchInsideNestedStream(jis, child, targetPath);
                            if (found != null) return found;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ClassBytesWithCodeSource searchInsideNestedStream(InputStream parentJis, NestedJar node, String targetPath) throws IOException {
        InputStream shield = new FilterInputStream(parentJis) { @Override public void close() {} };

        try (JarInputStream jis = new JarInputStream(shield)) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {

                if (entry.getName().equals(targetPath)) {
                    byte[] bytes = jis.readAllBytes();
                    return new ClassBytesWithCodeSource(bytes, createOriginURL(node, bytes));
                }

                for (NestedJar child : node.nestedJars()) {
                    if (entry.getName().equals(child.resourcePath())) {
                        ClassBytesWithCodeSource found =
                                searchInsideNestedStream(jis, child, targetPath);
                        if (found != null) return found;
                    }
                }
            }
        }

        return null;
    }

    // -------------------- Resource Access --------------------

    public InputStream getResourceAsStream(String name) {
        ResourceWithOrigin res = searchResource(name);
        return res != null ? new ByteArrayInputStream(res.bytes()) : null;
    }

    public ResourceWithOrigin searchResource(String name) {
        for (NestedJar node : nestedJars) {
            ResourceWithOrigin res = searchResourceRecursive(node, name);
            if (res != null) return res;
        }
        return null;
    }

    private ResourceWithOrigin searchResourceRecursive(NestedJar node, String targetPath) {
        try (InputStream jarStream = classloader.getParent().getResourceAsStream(node.resourcePath())) {
            if (jarStream == null) return null;

            try (JarInputStream jis = new JarInputStream(jarStream)) {
                JarEntry entry;
                while ((entry = jis.getNextJarEntry()) != null) {
                    if (entry.getName().equals(targetPath)) {
                        byte[] bytes = jis.readAllBytes();
                        return new ResourceWithOrigin(bytes, createOriginURL(node, bytes));
                    }

                    for (NestedJar child : node.nestedJars()) {
                        if (entry.getName().equals(child.resourcePath())) {
                            ResourceWithOrigin found = searchResourceInsideNestedStream(jis, child, targetPath);
                            if (found != null) return found;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ResourceWithOrigin searchResourceInsideNestedStream(InputStream parentJis, NestedJar node, String targetPath) throws IOException {
        InputStream shield = new FilterInputStream(parentJis) { @Override public void close() {} };

        try (JarInputStream jis = new JarInputStream(shield)) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (entry.getName().equals(targetPath)) {
                    byte[] bytes = jis.readAllBytes();
                    return new ResourceWithOrigin(bytes, createOriginURL(node, bytes));
                }

                for (NestedJar child : node.nestedJars()) {
                    if (entry.getName().equals(child.resourcePath())) {
                        ResourceWithOrigin found =
                                searchResourceInsideNestedStream(jis, child, targetPath);
                        if (found != null) return found;
                    }
                }
            }
        }

        return null;
    }

    // -------------------- Resource Enumeration --------------------

    public void findResources(String name, List<URL> urls) throws IOException {
        for (NestedJar node : nestedJars) {
            findResourcesRecursive(node, name, urls);
        }
    }

    private void findResourcesRecursive(NestedJar node, String name, List<URL> urls) throws IOException {
        try (InputStream jarStream = classloader.getParent().getResourceAsStream(node.resourcePath())) {
            if (jarStream == null) return;

            try (JarInputStream jis = new JarInputStream(jarStream)) {
                JarEntry entry;
                while ((entry = jis.getNextJarEntry()) != null) {
                    if (entry.getName().equals(name)) {
                        byte[] bytes = jis.readAllBytes();
                        urls.add(createOriginURL(node, bytes));
                    }

                    for (NestedJar child : node.nestedJars()) {
                        if (entry.getName().equals(child.resourcePath())) {
                            findResourcesInsideNestedStream(jis, child, name, urls);
                        }
                    }
                }
            }
        }
    }

    private void findResourcesInsideNestedStream(InputStream parentJis, NestedJar node, String name, List<URL> urls) throws IOException {
        InputStream shield = new FilterInputStream(parentJis) { @Override public void close() {} };

        try (JarInputStream jis = new JarInputStream(shield)) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (entry.getName().equals(name)) {
                    byte[] bytes = jis.readAllBytes();
                    urls.add(createOriginURL(node, bytes));
                }

                for (NestedJar child : node.nestedJars()) {
                    if (entry.getName().equals(child.resourcePath())) {
                        findResourcesInsideNestedStream(jis, child, name, urls);
                    }
                }
            }
        }
    }

    // -------------------- Nested URL / CodeSource Creation --------------------

    private URL createOriginURL(NestedJar node, byte[] data) {
        try {
            return new URL("file", null, -1, node.getFullPath(), new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL u) {
                    return new URLConnection(u) {
                        @Override public void connect() {}
                        @Override public InputStream getInputStream() { return new ByteArrayInputStream(data); }
                        @Override public int getContentLength() { return data.length; }
                    };
                }
            });
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}