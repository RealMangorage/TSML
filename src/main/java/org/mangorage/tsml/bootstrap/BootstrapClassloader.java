package org.mangorage.tsml.bootstrap;


import org.mangorage.tsml.bootstrap.internal.core.nested.api.IJar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.util.*;

public final class BootstrapClassloader extends SecureClassLoader {

    private final List<IJar> jars;

    BootstrapClassloader(List<IJar> jars, ClassLoader parent) {
        super(parent);
        this.jars = jars;
    }

    // ------------------- Class Loading -------------------

    private ProtectionDomain getClassProtectionDomain(String className) {
        String resourcePath = className.replace('.', '/') + ".class";

        for (IJar jar : jars) {
            if (jar.exists(resourcePath)) {
                try {
                    URL jarUrl = jar.getURL();
                    CodeSource source = new CodeSource(jarUrl, (java.security.cert.Certificate[]) null);
                    return new ProtectionDomain(source, getPermissions(source));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create ProtectionDomain for " + className, e);
                }
            }
        }

        return null;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> loaded = findLoadedClass(name);
        if (loaded != null) return loaded;

        byte[] classBytes = getClassBytes(name);
        if (classBytes == null)
            throw new ClassNotFoundException(name);

        return defineClass(name, classBytes, 0, classBytes.length, getClassProtectionDomain(name));
    }

    // ------------------- ITSMLClassloader -------------------
    byte[] getClassBytes(String name) {
        String path = name.replace('.', '/') + ".class";
        return getResourceBytes(path);
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
                            @Override public void connect() { }

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

        for (IJar jar : jars) {
            if (jar.exists(name)) {
                byte[] data = jar.readBytes(name);
                if (data != null) {
                    try {
                        URL url = new URL(null, "memory:" + name, new URLStreamHandler() {
                            @Override
                            protected URLConnection openConnection(URL u) {
                                return new URLConnection(u) {
                                    @Override public void connect() { }

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

        Enumeration<URL> parentUrls = getParent().getResources(name);
        while (parentUrls.hasMoreElements())
            urls.add(parentUrls.nextElement());

        return Collections.enumeration(urls);
    }

    private byte[] getResourceBytes(String name) {
        for (IJar jar : jars) {
            if (jar.exists(name)) {
                byte[] bytes = jar.readBytes(name);
                if (bytes != null) return bytes;
            }
        }

        try (InputStream is = getParent().getResourceAsStream(name)) {
            if (is == null) return null;
            return is.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}