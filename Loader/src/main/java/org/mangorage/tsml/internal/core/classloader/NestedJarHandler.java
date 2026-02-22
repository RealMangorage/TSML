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
            ClassBytesWithCodeSource found = searchRecursive(node, path, node.jarPath());
            if (found != null) return found;
        }

        return null;
    }

    private ClassBytesWithCodeSource searchRecursive(NestedJar node, String targetPath, String parentPath) {
        try (InputStream jarStream = classloader.getParent().getResourceAsStream(node.jarPath())) {
            if (jarStream == null) return null;

            try (JarInputStream jis = new JarInputStream(jarStream)) {
                JarEntry entry;
                while ((entry = jis.getNextJarEntry()) != null) {

                    if (entry.getName().equals(targetPath)) {
                        byte[] bytes = jis.readAllBytes();
                        URL originUrl = createNestedJarURL(parentPath, bytes);
                        return new ClassBytesWithCodeSource(bytes, originUrl);
                    }

                    for (NestedJar child : node.nestedJars()) {
                        if (entry.getName().equals(child.jarPath())) {
                            ClassBytesWithCodeSource found =
                                    searchInsideNestedStream(jis, child, targetPath, parentPath + "!/" + child.jarPath());
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

    private ClassBytesWithCodeSource searchInsideNestedStream(InputStream parentJis, NestedJar node, String targetPath, String fullPath) throws IOException {
        InputStream shield = new FilterInputStream(parentJis) { @Override public void close() {} };

        try (JarInputStream jis = new JarInputStream(shield)) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {

                if (entry.getName().equals(targetPath)) {
                    byte[] bytes = jis.readAllBytes();
                    URL originUrl = createNestedJarURL(fullPath, bytes);
                    return new ClassBytesWithCodeSource(bytes, originUrl);
                }

                for (NestedJar child : node.nestedJars()) {
                    if (entry.getName().equals(child.jarPath())) {
                        ClassBytesWithCodeSource found =
                                searchInsideNestedStream(jis, child, targetPath, fullPath + "!/" + child.jarPath());
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
            ResourceWithOrigin res = searchResourceRecursive(node, name, node.jarPath());
            if (res != null) return res;
        }
        return null;
    }

    private ResourceWithOrigin searchResourceRecursive(NestedJar node, String targetPath, String parentPath) {
        try (InputStream jarStream = classloader.getParent().getResourceAsStream(node.jarPath())) {
            if (jarStream == null) return null;

            try (JarInputStream jis = new JarInputStream(jarStream)) {
                JarEntry entry;
                while ((entry = jis.getNextJarEntry()) != null) {
                    if (entry.getName().equals(targetPath)) {
                        byte[] bytes = jis.readAllBytes();
                        URL origin = createNestedJarURL(parentPath, bytes);
                        return new ResourceWithOrigin(bytes, origin);
                    }

                    for (NestedJar child : node.nestedJars()) {
                        if (entry.getName().equals(child.jarPath())) {
                            ResourceWithOrigin found =
                                    searchResourceInsideNestedStream(jis, child, targetPath, parentPath + "!/" + child.jarPath());
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

    private ResourceWithOrigin searchResourceInsideNestedStream(InputStream parentJis, NestedJar node, String targetPath, String fullPath) throws IOException {
        InputStream shield = new FilterInputStream(parentJis) { @Override public void close() {} };

        try (JarInputStream jis = new JarInputStream(shield)) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (entry.getName().equals(targetPath)) {
                    byte[] bytes = jis.readAllBytes();
                    URL originUrl = createNestedJarURL(fullPath, bytes);
                    return new ResourceWithOrigin(bytes, originUrl);
                }

                for (NestedJar child : node.nestedJars()) {
                    if (entry.getName().equals(child.jarPath())) {
                        ResourceWithOrigin found =
                                searchResourceInsideNestedStream(jis, child, targetPath, fullPath + "!/" + child.jarPath());
                        if (found != null) return found;
                    }
                }
            }
        }

        return null;
    }

    // Add this to NestedJarHandler
    public void findResources(String name, List<URL> urls) throws IOException {
        for (NestedJar node : nestedJars) {
            findResourcesRecursive(node, name, node.jarPath(), urls);
        }
    }

    private void findResourcesRecursive(NestedJar node, String name, String parentPath, List<URL> urls) throws IOException {
        try (InputStream jarStream = classloader.getParent().getResourceAsStream(node.jarPath())) {
            if (jarStream == null) return;

            try (JarInputStream jis = new JarInputStream(jarStream)) {
                JarEntry entry;
                while ((entry = jis.getNextJarEntry()) != null) {
                    if (entry.getName().equals(name)) {
                        byte[] bytes = jis.readAllBytes();
                        urls.add(createNestedJarURL(parentPath, bytes));
                    }

                    // Recurse nested children
                    for (NestedJar child : node.nestedJars()) {
                        if (entry.getName().equals(child.jarPath())) {
                            findResourcesInsideNestedStream(jis, child, name, parentPath + "!/" + child.jarPath(), urls);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void findResourcesInsideNestedStream(InputStream parentJis, NestedJar node, String name, String fullPath, List<URL> urls) throws IOException {
        InputStream shield = new FilterInputStream(parentJis) { @Override public void close() {} };

        try (JarInputStream jis = new JarInputStream(shield)) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (entry.getName().equals(name)) {
                    byte[] bytes = jis.readAllBytes();
                    urls.add(createNestedJarURL(fullPath, bytes));
                }

                for (NestedJar child : node.nestedJars()) {
                    if (entry.getName().equals(child.jarPath())) {
                        findResourcesInsideNestedStream(jis, child, name, fullPath + "!/" + child.jarPath(), urls);
                    }
                }
            }
        }
    }

    // -------------------- Nested URL Creation --------------------

    private URL createNestedJarURL(String jarPath, byte[] data) {
        try {
            return new URL("file", null, -1, jarPath, new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL u) {
                    return new URLConnection(u) {
                        @Override public void connect() {}

                        @Override
                        public InputStream getInputStream() {
                            return new ByteArrayInputStream(data);
                        }

                        @Override
                        public int getContentLength() {
                            return data.length;
                        }
                    };
                }
            });
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}