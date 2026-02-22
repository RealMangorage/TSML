package org.mangorage.tsml.internal.core.classloader;

import org.mangorage.tsml.api.IClassTransformer;
import org.mangorage.tsml.api.ITSMLClassloader;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.ServiceLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public final class TSMLURLClassloader extends URLClassLoader implements ITSMLClassloader {

    private final List<IClassTransformer> transformers = new ArrayList<>();
    private final List<NestedJar> nestedJars = new ArrayList<>();
    private final NestedJarHandler nestedJarHandler = new NestedJarHandler(nestedJars, this);
    private final URL[] jarUrls;

    public TSMLURLClassloader(URL[] urls, List<NestedJar> nestedJars, ClassLoader parent) {
        super(urls, parent);
        this.jarUrls = urls;
        if (nestedJars != null) {
            this.nestedJars.addAll(nestedJars);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // Already loaded? Just return it

        Class<?> loaded = findLoadedClass(name);
        if (loaded != null) return loaded;


        // Load raw bytes
        byte[] classBytes = getClassBytes(name);

        // Apply transformers
        if (!transformers.isEmpty()) {
            for (IClassTransformer transformer : transformers) {
                try {
                    byte[] transformed = transformer.transform(name.replace('/', '.'), classBytes);
                    if (transformed != null) {
                        classBytes = transformed;
                    }
                } catch (Throwable t) {
                    throw new RuntimeException("Transformation failed for " + name, t);
                }
            }
        }

        // Proper CodeSource for Mixin / Tinylog
        URL codeSourceURL = jarUrls.length > 0 ? jarUrls[0] : null;
        CodeSource codeSource = codeSourceURL != null ? new CodeSource(codeSourceURL, (Certificate[]) null) : null;
        ProtectionDomain pd = new ProtectionDomain(codeSource, null, this, null);

        return defineClass(name, classBytes, 0, classBytes.length, pd);
    }

    @Override
    public byte[] getClassBytes(String cn) {
        String path = cn.replace('.', '/') + ".class";
        try (var resource = getResourceAsStream(path)) {
            if (resource == null) {
                return nestedJarHandler.getNestedClassBytes(cn);
            }
            return resource.readAllBytes(); // Java 9+
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public URL findResource(String name) {
        // Check external JARs
        URL url = super.findResource(name);
        if (url != null) return url;

        // NOTE: If you need to return a URL for a nested resource,
        // you would need a custom URLStreamHandler.
        // For simple stream reading, use getResourceAsStream().
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        // 1. Try external
        InputStream is = super.getResourceAsStream(name);
        if (is != null) {
            return is;
        }

        is = nestedJarHandler.getResourceAsStream(name);
        if (is != null) {
            return is;
        }

        return getParent().getResourceAsStream(name);
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        List<URL> urls = new ArrayList<>();

        // 1. Get resources from the standard URLClassLoader (the external JARs)
        Enumeration<URL> superResources = super.findResources(name);
        while (superResources.hasMoreElements()) {
            urls.add(superResources.nextElement());
        }

        nestedJarHandler.findResource(name, urls);

        return Collections.enumeration(urls);
    }

    @Override
    public boolean hasClass(final String name) {
        return findLoadedClass(name.replace('/', '.')) != null;
    }

    public void init() {
        // Load transformers via SPI
        ServiceLoader.load(IClassTransformer.class, this).forEach(transformers::add);
    }
}