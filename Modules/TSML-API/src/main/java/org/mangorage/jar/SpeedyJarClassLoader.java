package org.mangorage.jar;

import org.mangorage.jar.api.IJar;

import java.net.URL;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.security.cert.Certificate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SpeedyJarClassLoader extends SecureClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private final List<IJar> jars;
    private final URL[] urls;
    private final Set<String> loaded = ConcurrentHashMap.newKeySet();

    public SpeedyJarClassLoader(List<IJar> jars, ClassLoader parent) {
        super(parent);
        this.jars = new CopyOnWriteArrayList<>(jars);

        this.urls = jars.stream()
                .map(IJar::getURL)
                .toArray(URL[]::new);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {

            Class<?> c = findLoadedClass(name);
            if (c == null) {

                // Always protect JDK
                if (isSystemClass(name)) {
                    c = getParent().loadClass(name);
                } else {
                    try {
                        c = findClass(name);
                    } catch (ClassNotFoundException e) {
                        c = getParent().loadClass(name);
                    }
                }
            }

            if (resolve) {
                resolveClass(c);
            }

            return c;
        }
    }

    /**
     * 🔥 CLASS DEFINITION
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            byte[] bytes = getClassBytes(name);

            // TSML HOOK (UNCHANGED CONCEPT)
            bytes = maybeTransform(name, bytes);

            if (bytes == null) {
                throw new ClassNotFoundException(name);
            }

            String path = name.replace('.', '/') + ".class";
            CodeSource cs = findCodeSource(path);

            Class<?> defined = (cs != null)
                    ? defineClass(name, bytes, 0, bytes.length, cs)
                    : defineClass(name, bytes, 0, bytes.length);

            loaded.add(name);
            return defined;
        }
    }


    public byte[] getClassBytes(String name) {
        String path = name.replace('.', '/') + ".class";
        return getResourceBytes(path);
    }

    protected byte[] getResourceBytes(String path) {
        for (IJar jar : jars) {
            byte[] data = jar.readBytes(path);
            if (data != null) return data;
        }
        return null;
    }

    protected byte[] maybeTransform(String name, byte[] original) {
        return original;
    }

    @Override
    protected URL findResource(String name) {
        for (IJar jar : jars) {
            URL url = jar.findResource(name);
            if (url != null) return url;
        }
        return null;
    }

    @Override
    protected Enumeration<URL> findResources(String name) {
        List<URL> out = new ArrayList<>();
        for (IJar jar : jars) {
            URL url = jar.findResource(name);
            if (url != null) out.add(url);
        }
        return Collections.enumeration(out);
    }

    private CodeSource findCodeSource(String path) {
        for (IJar jar : jars) {
            if (jar.exists(path)) {
                try {
                    return new CodeSource(jar.getURL(), (Certificate[]) null);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    protected boolean isSystemClass(String name) {
        return name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("sun.")
                || name.startsWith("jdk.");
    }

    public URL[] getUrls() {
        return urls;
    }

    public List<IJar> getJars() {
        return Collections.unmodifiableList(jars);
    }

    protected Set<String> getLoaded() {
        return Collections.unmodifiableSet(loaded);
    }
}