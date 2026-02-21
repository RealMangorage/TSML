package org.mangorage.tsml.internal.core;

import org.mangorage.tsml.api.IClassTransformer;
import org.mangorage.tsml.api.ITSMLClassloader;

import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public final class TSMLURLClassloader extends java.net.URLClassLoader implements ITSMLClassloader {

    private final List<IClassTransformer> transformers = new ArrayList<>();
    private final URL[] jarUrls;

    public TSMLURLClassloader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.jarUrls = urls;
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
                throw new ClassNotFoundException("Class resource not found: " + cn);
            }
            return resource.readAllBytes(); // Java 9+
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasClass(final String name) {
        return findLoadedClass(name.replace('/', '.')) != null;
    }

    public void init() {
        ServiceLoader.load(IClassTransformer.class, this).forEach(transformers::add);
    }
}