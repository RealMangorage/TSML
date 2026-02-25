package org.mangorage.tsml.internal.core.classloader;

import org.mangorage.tsml.api.classloader.IClassTransformer;
import org.mangorage.tsml.api.classloader.ITSMLClassloader;
import org.mangorage.tsml.internal.core.nested.api.IJar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.SecureClassLoader;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class IJarClassloader extends SecureClassLoader implements ITSMLClassloader {

    private final List<IJar> jars;
    private final List<IClassTransformer> transformers = new CopyOnWriteArrayList<>();
    private final Set<String> loaded = new HashSet<>();

    public IJarClassloader(List<IJar> jars, ClassLoader parent) {
        super(parent);
        this.jars = jars;
    }

    public void init() {
        // Auto-load transformers
        ServiceLoader.load(IClassTransformer.class, this).forEach(transformers::add);
    }

    // ------------------- Class Loading -------------------

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        final Class<?> loaded = findLoadedClass(name);
        if (loaded != null) return loaded;

        byte[] classBytes = getClassBytes(name);
        if (classBytes == null) {

            for (IClassTransformer transformer : transformers) {
                classBytes = transformer.generateClass(name);
                if (classBytes != null)
                    break;
            }

            // Still null, then we failed.
            if (classBytes == null) {
                throw new ClassNotFoundException();
            }
        }

        if (!transformers.isEmpty()) {
            for (IClassTransformer transformer : transformers) {
                try {
                    byte[] transformed = transformer.transform(name, classBytes);
                    if (transformed != null)
                        classBytes = transformed;
                } catch (Throwable t) {
                    throw new IllegalStateException(t);
                }
            }
        }
        try {
            this.loaded.add(name);
            return defineClass(name, classBytes, 0, classBytes.length);
        } catch (Exception e) {
            throw new ClassNotFoundException("Failed to define class: " + name, e);
        }
    }

    // ------------------- ITSMLClassloader -------------------

    @Override
    public byte[] getClassBytes(String name) {
        String path = name.replace('.', '/') + ".class";
        return getResourceBytes(path);
    }

    @Override
    public boolean hasClass(final String name) {
        if (!loaded.contains("Launcher")) return false;
        return findLoadedClass(name.replace('/', '.')) != null;
    }

    @Override
    public List<IJar> getJars() {
        return Collections.unmodifiableList(jars);
    }

    // ------------------- Resource Handling -------------------

    @Override
    public URL getResource(String name) {
        byte[] data = getResourceBytes(name);
        if (data != null) {
            try {
                return new URL(null, "memory:" + name, new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL u) {
                        return new URLConnection(u) {
                            @Override
                            public void connect() { /* no-op */ }

                            @Override
                            public InputStream getInputStream() {
                                return new ByteArrayInputStream(data);
                            }
                        };
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return getParent().getResource(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        byte[] data = getResourceBytes(name);
        if (data != null) return new ByteArrayInputStream(data);
        return getParent().getResourceAsStream(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> urls = new ArrayList<>();

        // Resources from jars
        for (IJar jar : jars) {
            if (jar.exists(name)) {
                byte[] data = jar.readBytes(name);
                if (data != null) {
                    try {
                        URL url = new URL(null, "memory:" + name, new URLStreamHandler() {
                            @Override
                            protected URLConnection openConnection(URL u) {
                                return new URLConnection(u) {
                                    @Override
                                    public void connect() { }

                                    @Override
                                    public InputStream getInputStream() {
                                        return new ByteArrayInputStream(data);
                                    }
                                };
                            }
                        });
                        urls.add(url);
                    } catch (Exception ignored) { }
                }
            }
        }

        // Parent resources
        Enumeration<URL> parentUrls = getParent().getResources(name);
        while (parentUrls.hasMoreElements()) urls.add(parentUrls.nextElement());

        return Collections.enumeration(urls);
    }

    private byte[] getResourceBytes(String name) {
        // Check all registered IJars
        for (IJar jar : jars) {
            if (jar.exists(name)) {
                byte[] bytes = jar.readBytes(name);
                if (bytes != null) return bytes;
            }
        }

        // Fallback: parent classloader
        try (InputStream is = getParent().getResourceAsStream(name)) {
            if (is == null) return null;
            return is.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}