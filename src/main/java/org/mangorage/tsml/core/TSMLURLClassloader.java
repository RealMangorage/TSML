package org.mangorage.tsml.core;

import org.mangorage.tsml.api.IClassTransformer;
import org.mangorage.tsml.api.ITSMLClassloader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public final class TSMLURLClassloader extends URLClassLoader implements ITSMLClassloader {

    private final List<IClassTransformer> transformers = new ArrayList<>();

    public TSMLURLClassloader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }


    @Override
    protected Class<?> findClass(String name) {
        // Already loaded? Just return it.
        Class<?> loaded = findLoadedClass(name);
        if (loaded != null) return loaded;

        // Transform class bytes before defining
        byte[] classBytes = getClassBytes(name);
        if (!transformers.isEmpty()) {
            for (IClassTransformer transformer : transformers) {
                try {
                    byte[] transformed = transformer.transform(name.replace('/', '.'), classBytes);
                    if (transformed != null) {
                        classBytes = transformed;
                    }
                } catch (Throwable t) {
                    // Don’t just silently ignore; JVM will explode if you return broken bytes
                    throw new RuntimeException(t);
                }
            }
        }

        return defineClass(name, classBytes, 0, classBytes.length);
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
