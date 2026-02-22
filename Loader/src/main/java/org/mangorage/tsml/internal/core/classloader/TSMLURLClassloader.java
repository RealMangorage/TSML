package org.mangorage.tsml.internal.core.classloader;

import org.mangorage.tsml.api.classloader.IClassTransformer;
import org.mangorage.tsml.api.classloader.ITSMLClassloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.ServiceLoader;

public final class TSMLURLClassloader extends java.net.URLClassLoader implements ITSMLClassloader {

    private final List<IClassTransformer> transformers = new ArrayList<>();
    private final List<NestedJar> nestedJars = new ArrayList<>();
    private final NestedJarHandler nestedJarHandler = new NestedJarHandler(nestedJars, this);
    private final URL[] jarUrls;

    public TSMLURLClassloader(URL[] urls, List<NestedJar> nestedJars, ClassLoader parent) {
        super(urls, parent);
        this.jarUrls = urls;
        if (nestedJars != null) this.nestedJars.addAll(nestedJars);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // Already loaded?
        Class<?> loaded = findLoadedClass(name);
        if (loaded != null) return loaded;

        // Get class bytes + code source
        ClassBytesWithCodeSource cs = nestedJarHandler.getNestedClassBytes(name);
        byte[] classBytes;
        URL originUrl;

        if (cs != null) {
            classBytes = cs.bytes();
            originUrl = cs.originJar();
        } else {
            // fallback: try standard classloader
            try (InputStream is = getResourceAsStream(name.replace('.', '/') + ".class")) {
                if (is == null) throw new ClassNotFoundException(name);
                classBytes = is.readAllBytes();
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
            originUrl = findResource(name.replace('.', '/') + ".class"); // may be null
        }

        // Apply transformers
        if (!transformers.isEmpty()) {
            for (IClassTransformer transformer : transformers) {
                try {
                    byte[] transformed = transformer.transform(name.replace('/', '.'), classBytes);
                    if (transformed != null) classBytes = transformed;
                } catch (Throwable t) {
                    throw new RuntimeException("Transformation failed for " + name, t);
                }
            }
        }

        // Proper CodeSource + ProtectionDomain
        CodeSource codeSource = originUrl != null ? new CodeSource(originUrl, (Certificate[]) null) : null;
        ProtectionDomain pd = new ProtectionDomain(codeSource, null, this, null);

        return defineClass(name, classBytes, 0, classBytes.length, pd);
    }

    @Override
    public byte[] getClassBytes(String cn) {
        String path = cn.replace('.', '/') + ".class";
        try (InputStream resource = getResourceAsStream(path)) {
            if (resource == null) {
                ClassBytesWithCodeSource cs = nestedJarHandler.getNestedClassBytes(cn);
                if (cs == null) throw new RuntimeException("Class not found: " + cn);
                return cs.bytes();
            }
            return resource.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public URL findResource(String name) {
        // First try top-level JARs
        URL url = super.findResource(name);
        if (url != null) return url;

        // Fallback: nested jars
        NestedJarHandler.ResourceWithOrigin r = nestedJarHandler.searchResource(name);
        if (r != null) return r.originJar();

        // Nothing found
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream is = super.getResourceAsStream(name);
        if (is != null) return is;

        is = nestedJarHandler.getResourceAsStream(name);
        if (is != null) return is;

        return getParent().getResourceAsStream(name);
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        List<URL> urls = new ArrayList<>();

        Enumeration<URL> superResources = super.findResources(name);
        while (superResources.hasMoreElements()) urls.add(superResources.nextElement());

        nestedJarHandler.findResources(name, urls);


        return Collections.enumeration(urls);
    }

    @Override
    public boolean hasClass(final String name) {
        return findLoadedClass(name.replace('/', '.')) != null;
    }

    public void init() {
        ServiceLoader.load(IClassTransformer.class, this).forEach(transformers::add);
    }
}