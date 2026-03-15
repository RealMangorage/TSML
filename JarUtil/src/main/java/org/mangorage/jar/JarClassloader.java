package org.mangorage.jar;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.util.*;

public class JarClassloader extends SecureClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private final List<IJar> jars;
    private final Set<String> loaded = new HashSet<>();

    public JarClassloader(List<IJar> jars, ClassLoader parent) {
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

        byte[] classBytes = maybeTransform(name, getClassBytes(name));

        if (classBytes == null) {
            throw new ClassNotFoundException("Class not found: " + name);
        }

        try {
            final var definedClass = defineClass(name, classBytes, 0, classBytes.length);
            loaded.add(name);
            return definedClass;
        } catch (Exception e) {
            throw new ClassNotFoundException("Failed to define class: " + name, e);
        }
    }

    // Transformer code
    protected byte[] maybeTransform(String name, byte[] original) {
        return original;
    }

    public byte[] getClassBytes(String name) {
        String path = name.replace('.', '/') + ".class";
        return getResourceBytes(path);
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

    protected Set<String> getLoaded() {
        return loaded;
    }

    public List<IJar> getJars() {
        return Collections.unmodifiableList(jars);
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

    protected byte[] getResourceBytes(String name) {
        try (InputStream is = getResourceAsStream(name)) {
            if (is == null) return null;
            return is.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}