package org.mangorage.tsml.internal.core.classloader;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public final class NestedJarHandler {
    private final List<NestedJar> nestedJars;
    private final ClassLoader classloader;

    NestedJarHandler(List<NestedJar> nestedJars, ClassLoader classLoader) {
        this.nestedJars = nestedJars;
        this.classloader = classLoader;
    }


    InputStream getResourceAsStream(String name) {
        // 2. Try nested
        byte[] data = searchNestedJars(name);
        if (data != null) {
            return new java.io.ByteArrayInputStream(data);
        }
        return null;
    }

    /**
     * Searches both external URLs and nested JARs for class bytes.
     */
    byte[] getNestedClassBytes(String cn) {
        String path = cn.replace('.', '/') + ".class";

        // Try external URLs first (Native URLClassLoader behavior) via super
        try (InputStream is = classloader.getResourceAsStream(path)) {
            if (is != null) return is.readAllBytes();
        } catch (IOException ignored) {}

        // Fallback to searching the NestedJar tree
        return searchNestedJars(path);
    }

    // Update your search entry point
    byte[] searchNestedJars(String resourcePath) {
        for (NestedJar node : nestedJars) {
            byte[] found = searchRecursive(node, resourcePath);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * Recursive search that follows your NestedJar tree structure.
     */
    private byte[] searchRecursive(NestedJar node, String targetPath) {
        // 1. Get the stream for the current node (the JAR itself)
        // If it's a top-level JAR, get from parent. If deeper, this logic needs
        // to be adjusted to pull from the current stream context.
        try (InputStream jarStream = classloader.getParent().getResourceAsStream(node.jarPath())) {
            if (jarStream == null) return null;

            try (JarInputStream jis = new JarInputStream(jarStream)) {
                JarEntry entry;
                while ((entry = jis.getNextJarEntry()) != null) {
                    String name = entry.getName();

                    // Check if this entry is the file we want
                    if (name.equals(targetPath)) {
                        return jis.readAllBytes();
                    }

                    // If this entry is one of the nested jars registered in our node
                    for (NestedJar child : node.nestedJars()) {
                        if (name.equals(child.jarPath())) {
                            // Dive into the nested stream
                            byte[] found = searchInsideNestedStream(jis, child, targetPath);
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

    private byte[] searchInsideNestedStream(InputStream parentJis, NestedJar node, String targetPath) throws IOException {
        // Wrap to prevent closing the parent JarInputStream
        InputStream shield = new FilterInputStream(parentJis) {
            @Override public void close() {}
        };

        try (JarInputStream jis = new JarInputStream(shield)) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (entry.getName().equals(targetPath)) {
                    return jis.readAllBytes();
                }

                // Recurse further if this node has registered children
                for (NestedJar child : node.nestedJars()) {
                    if (entry.getName().equals(child.jarPath())) {
                        byte[] found = searchInsideNestedStream(jis, child, targetPath);
                        if (found != null) return found;
                    }
                }
            }
        }
        return null;
    }

    void findResource(String name, List<URL> urls) throws IOException {
        // 2. Add resources from our nested JARs
        for (var nestedPath : nestedJars) {
            byte[] data = searchNestedJars(name); // Use your existing search logic
            if (data != null) {
                urls.add(createNestedURL(nestedPath.jarPath(), name, data));
            }
        }
    }

    private URL createNestedURL(String jarPath, String resourceName, byte[] data) throws IOException {
        // This is the specific constructor that allows a custom handler without global registration
        return new URL("tsml", null, -1, jarPath + "!/" + resourceName, new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) {
                return new URLConnection(u) {
                    @Override
                    public void connect() {}

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
    }
}
