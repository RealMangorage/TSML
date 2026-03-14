package org.mangorage.tsml.bootstrap;

import org.mangorage.tsml.bootstrap.internal.core.nested.IJar;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class BootstrapClassloader extends SecureClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private static final boolean DEBUG = false;

    private final List<IJar> jars;
    private final Set<String> loaded = new HashSet<>();

    private long totalTime = 0;

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
                    throw new RuntimeException("Failed to create ProtectionDomain for class " + className, e);
                }
            }
        }

        return null;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        final var time = System.currentTimeMillis();

        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) return loadedClass;

        byte[] classBytes = getClassBytes(name);

        if (classBytes == null) {
            throw new ClassNotFoundException("Class not found: " + name);
        }

        try {
            loaded.add(name);

            final var definedClass = defineClass(name, classBytes, 0, classBytes.length, getClassProtectionDomain(name));

            final var timeTotal = (System.currentTimeMillis() - time);
            totalTime+=timeTotal;

            if (DEBUG)
                System.out.println(name + " Time: " + timeTotal + " Time Total: " + totalTime);

            return definedClass;
        } catch (Exception e) {
            throw new ClassNotFoundException("Failed to define class: " + name, e);
        }
    }

    // ------------------- ITSMLClassloader -------------------

    public byte[] getClassBytes(String name) {
        String path = name.replace('.', '/') + ".class";
        return getResourceBytes(path);
    }

    public List<IJar> getJars() {
        return Collections.unmodifiableList(jars);
    }

    // ------------------- Resource Handling -------------------

    private URL createURL(String name, IJar jar) {
        try {
            return new URL(null, "memory:" + name, new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL u) {
                    return new URLConnection(u) {
                        @Override
                        public void connect() { }

                        @Override
                        public InputStream getInputStream() {
                            return jar.getInputStream(name);
                        }
                    };
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public URL getResource(String name) {
        final var jar = getJarForFirstResource(name);
        return jar == null ? getParent().getResource(name) : createURL(name, jar);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        final var jar = getJarForFirstResource(name);
        final var is = jar != null ? jar.getInputStream(name) : null;
        return is != null ? is : getParent().getResourceAsStream(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> urls = new ArrayList<>();

        for (IJar jar : jars) {
            if (jar.exists(name)) {
                try {
                    URL url = createURL(name, jar);
                    urls.add(url);
                } catch (Exception e) {
                    System.err.println("Failed to create URL for resource " + name + " in jar " + jar.getName() + ": " + e.getMessage());
                }
            }
        }

        Enumeration<URL> parentUrls = getParent().getResources(name);
        while (parentUrls.hasMoreElements()) urls.add(parentUrls.nextElement());

        return Collections.enumeration(urls);
    }

    /*
        USEFUL HELPERS
     */
    private IJar getJarForFirstResource(String name) {
        for (IJar jar : jars) {
            if (jar.exists(name))
                return jar;
        }
        return null;
    }

    private byte[] getResourceBytes(String name) {
        try (InputStream is = getResourceAsStream(name)) {
            if (is == null) return null;
            return is.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}