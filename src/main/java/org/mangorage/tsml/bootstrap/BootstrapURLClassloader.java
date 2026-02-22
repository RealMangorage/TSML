package org.mangorage.tsml.bootstrap;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class BootstrapURLClassloader extends URLClassLoader {
    private final List<String> nestedJarPaths = new ArrayList<>();
    private final URL[] jarUrls;

    public BootstrapURLClassloader(URL[] urls, List<String> nestedJars, ClassLoader parent) {
        super(urls, parent);
        this.jarUrls = urls;
        if (nestedJars != null) {
            this.nestedJarPaths.addAll(nestedJars);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // 1. Check if we already loaded it
        Class<?> loaded = findLoadedClass(name);
        if (loaded != null) return loaded;

        // 2. Try to get bytes from our system
        byte[] classBytes = getClassBytes(name);

        if (classBytes == null) {
            // Fallback to parent if we can't find it in nested jars
            return super.findClass(name);
        }

        // 3. Define the package so the JVM doesn't complain
        String packageName = name.lastIndexOf('.') != -1 ? name.substring(0, name.lastIndexOf('.')) : "";
        if (getDefinedPackage(packageName) == null) {
            try {
                definePackage(packageName, null, null, null, null, null, null, null);
            } catch (IllegalArgumentException e) {
                // Package might have been defined by a parallel thread
            }
        }

        // 4. Define the class
        URL codeSourceURL = jarUrls.length > 0 ? jarUrls[0] : null;
        CodeSource codeSource = new CodeSource(codeSourceURL, (Certificate[]) null);
        ProtectionDomain pd = new ProtectionDomain(codeSource, null, this, null);

        return defineClass(name, classBytes, 0, classBytes.length, pd);
    }

    public byte[] getClassBytes(String cn) {
        String path = cn.replace('.', '/') + ".class";
        // getResourceAsStream handles both external and nested paths now
        try (InputStream is = getResourceAsStream(path)) {
            if (is != null) {
                return is.readAllBytes();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        // 1. Try external (standard URLClassLoader behavior)
        InputStream is = super.getResourceAsStream(name);
        if (is != null) return is;

        // 2. Try nested single-layer search
        byte[] data = searchNestedJars(name);
        if (data != null) {
            return new ByteArrayInputStream(data);
        }

        return null;
    }

    /**
     * Searches across all nested jars for a specific resource.
     * Returns the first match it finds.
     */
    private byte[] searchNestedJars(String resourcePath) {
        for (String nestedPath : nestedJarPaths) {
            byte[] found = searchSpecificNestedJar(nestedPath, resourcePath);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * Opens a specific nested jar and looks for the target path (One layer deep).
     */
    private byte[] searchSpecificNestedJar(String jarPath, String targetPath) {
        try (InputStream jarStream = getParent().getResourceAsStream(jarPath)) {
            if (jarStream == null) return null;

            try (JarInputStream jis = new JarInputStream(jarStream)) {
                JarEntry entry;
                while ((entry = jis.getNextJarEntry()) != null) {
                    if (entry.getName().equals(targetPath)) {
                        return jis.readAllBytes();
                    }
                }
            }
        } catch (IOException ignored) {}
        return null;
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        List<URL> urls = new ArrayList<>();

        // 1. Get resources from the standard URLClassLoader (the external JARs)
        Enumeration<URL> superResources = super.findResources(name);
        while (superResources.hasMoreElements()) {
            urls.add(superResources.nextElement());
        }

        // 2. Add resources from our nested JARs (Crucial for ServiceLoader)
        for (String nestedPath : nestedJarPaths) {
            // Search THIS specific jar, otherwise ServiceLoader will load the same file multiple times!
            byte[] data = searchSpecificNestedJar(nestedPath, name);
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
}