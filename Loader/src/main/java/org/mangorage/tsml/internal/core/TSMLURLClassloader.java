package org.mangorage.tsml.internal.core;

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
    private final List<String> nestedJarPaths = new ArrayList<>();
    private final URL[] jarUrls;

    public TSMLURLClassloader(URL[] urls, List<String> nestedJars, ClassLoader parent) {
        super(urls, parent);
        this.jarUrls = urls;
        if (nestedJars != null) {
            this.nestedJarPaths.addAll(nestedJars);
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
                return getNestedClassBytes(cn);
            }
            return resource.readAllBytes(); // Java 9+
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Searches both external URLs and nested JARs for class bytes.
     */
    public byte[] getNestedClassBytes(String cn) {
        String path = cn.replace('.', '/') + ".class";

        // Try external URLs first (Native URLClassLoader behavior)
        try (InputStream is = super.getResourceAsStream(path)) {
            if (is != null) return is.readAllBytes();
        } catch (IOException ignored) {}

        // Fallback to searching nested JARs
        return searchNestedJars(path);
    }

    private byte[] searchNestedJars(String resourcePath) {
        for (String nestedPath : nestedJarPaths) {
            try (InputStream jarStream = getParent().getResourceAsStream(nestedPath)) {
                if (jarStream == null) continue;

                // Start the recursive search
                byte[] found = searchInStream(jarStream, resourcePath);
                if (found != null) return found;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Recursively searches a stream. If it finds a .jar, it dives inside it.
     */
    private byte[] searchInStream(InputStream in, String targetPath) throws IOException {
        JarInputStream jis = new JarInputStream(in);
        JarEntry entry;

        while ((entry = jis.getNextJarEntry()) != null) {
            String name = entry.getName();

            // 1. Did we find the actual file we want?
            if (name.equals(targetPath)) {
                return jis.readAllBytes();
            }

            // 2. Did we find a nested JAR? Dive deeper!
            if (name.endsWith(".jar")) {
                // Wrap the stream so the inner JarInputStream doesn't close our outer stream when it finishes
                InputStream uncloseableStream = new FilterInputStream(jis) {
                    @Override
                    public void close() {}
                };

                byte[] found = searchInStream(uncloseableStream, targetPath);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
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

        // 2. Try nested
        byte[] data = searchNestedJars(name);
        if (data != null) {
            return new java.io.ByteArrayInputStream(data);
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

        // 2. Add resources from our nested JARs
        for (String nestedPath : nestedJarPaths) {
            byte[] data = searchNestedJars(name); // Use your existing search logic
            if (data != null) {
                urls.add(createNestedURL(nestedPath, name, data));
            }
        }

        return Collections.enumeration(urls);
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

    @Override
    public boolean hasClass(final String name) {
        return findLoadedClass(name.replace('/', '.')) != null;
    }

    public void init() {
        // Load transformers via SPI
        ServiceLoader.load(IClassTransformer.class, this).forEach(transformers::add);
    }
}