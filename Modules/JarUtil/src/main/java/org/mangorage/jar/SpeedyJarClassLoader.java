package org.mangorage.jar;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLClassLoader;
import java.security.SecureClassLoader;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.*;
import java.net.URL;

public class SpeedyJarClassLoader extends SecureClassLoader {

    private final List<IJar> jars;
    private final URL[] urls;
    private final Set<String> loaded = Collections.synchronizedSet(new HashSet<>());

    public SpeedyJarClassLoader(List<IJar> jars, ClassLoader parent) {
        super(parent);
        this.jars = new ArrayList<>(jars);
        this.urls = jars.stream()
                .map(IJar::getURL)
                .toArray(URL[]::new);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String path = name.replace('.', '/').concat(".class");

        try (InputStream is = getResourceAsStream(path)) {
            byte[] bytes = is == null ? null : is.readAllBytes();

            // Transform the class bytes
            bytes = maybeTransform(name, bytes);

            if (bytes == null)
                return super.findClass(name);


            return defineClass(name, bytes, 0, bytes.length);
        } catch (IOException e) {
            return super.findClass(name);
        }
    }


    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        List<URL> urls = new ArrayList<>();
        for (IJar jar : jars) {
            URL resource = jar.findResource(name);
            if (resource != null) {
                urls.add(resource);
            }
        }
        return Collections.enumeration(urls);
    }

    @Override
    protected URL findResource(String name) {
        for (IJar jar : jars) {
            final var resource = jar.findResource(name);
            if (resource != null)
                return resource;
        }
        return null;
    }

    /** Override this to apply class transformations */
    protected byte[] maybeTransform(String name, byte[] original) {
        return original;
    }

    /** Load class bytes directly from the IJars */
    public byte[] getClassBytes(String name) {
        String path = name.replace('.', '/') + ".class";
        return getResourceBytes(path);
    }

    /** Load resource bytes from the IJars */
    protected byte[] getResourceBytes(String path) {
        for (IJar jar : jars) {
            byte[] data = jar.readBytes(path);
            if (data != null) return data;
        }
        return null;
    }

    /** Find a CodeSource URL from the first JAR that contains this class */
    private CodeSource findCodeSourceForClass(String path) {
        for (IJar jar : jars) {
            if (jar.exists(path)) {
                try {
                    URL url = jar.getURL();
                    return new CodeSource(url, (Certificate[]) null);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /** List of loaded classes */
    protected Set<String> getLoaded() {
        return Collections.unmodifiableSet(loaded);
    }

    /** Access to the IJars */
    public List<IJar> getJars() {
        return Collections.unmodifiableList(jars);
    }

    public URL[] getUrls() {
        return urls;
    }
}