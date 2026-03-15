package org.mangorage.jar;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class JarClassloader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private final List<IJar> jars;
    private final Set<String> loaded = new HashSet<>();

    public JarClassloader(List<IJar> jars, ClassLoader parent) {
        super(
                jars.stream()
                        .map(IJar::getURL)
                        .toArray(URL[]::new),
                parent
        );
        this.jars = jars;
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

    // Transformer code
    protected byte[] maybeTransform(String name, byte[] original) {
        return original;
    }

    public byte[] getClassBytes(String name) {
        String path = name.replace('.', '/') + ".class";
        return getResourceBytes(path);
    }


    protected Set<String> getLoaded() {
        return loaded;
    }

    public List<IJar> getJars() {
        return Collections.unmodifiableList(jars);
    }

    protected byte[] getResourceBytes(String name) {
        try (InputStream is = getResourceAsStream(name)) {
            if (is == null)
                return null;
            return is.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}