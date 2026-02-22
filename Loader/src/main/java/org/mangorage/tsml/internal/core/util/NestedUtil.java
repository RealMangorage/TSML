package org.mangorage.tsml.internal.core.util;

import org.mangorage.tsml.internal.TSML;
import org.mangorage.tsml.internal.core.classloader.NestedJar;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public final class NestedUtil {
    public static List<String> getNestedPathsFromStream(InputStream jarStream, String prefix) throws IOException {
        List<String> internalPaths = new ArrayList<>();
        // Wrap the incoming stream in a JarInputStream to read its entries
        try (JarInputStream jis = new JarInputStream(jarStream)) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (entry.getName().startsWith(prefix + "/") && entry.getName().endsWith(".jar")) {
                    internalPaths.add(entry.getName());
                }
            }
        }
        return internalPaths;
    }

    public static List<NestedJar> buildNestedJarTree(URL baseResource) throws IOException, URISyntaxException {
        // 1. Get the Mod Jars from the Base Resource (Fat Jar)
        // This will be the list we pass to the TSMLURLClassloader
        List<NestedJar> nestedJarTree = new ArrayList<>();

        // 1. Get the Top-Level Mod Jars from the Fat Jar
        List<String> nestedModPaths = JarJarUtil.getNestedJarPaths(List.of(baseResource), "JarJarMods");

        for (String modPath : nestedModPaths) {
            // Create the parent node for the Mod
            NestedJar modNode = new NestedJar(modPath, new ArrayList<>());

            // 2. Look inside THIS specific mod for its own nested libraries
            try (InputStream modStream = TSML.class.getClassLoader().getResourceAsStream(modPath)) {
                if (modStream != null) {
                    // Find libraries inside "JarJar/" within "JarJarMods/some-mod.jar"
                    List<String> modLibs = getNestedPathsFromStream(modStream, "JarJar");

                    for (String libPath : modLibs) {
                        // Add the library as a child of the mod
                        modNode.nestedJars().add(new NestedJar(libPath, new ArrayList<>()));
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to scan libraries inside mod: " + modPath);
            }

            // Add the mod (and its children) to our tree
            nestedJarTree.add(modNode);
        }

        return nestedJarTree;
    }

    public static List<NestedJar> buildNestedJarTreeFromMod(URL modResource) throws IOException, URISyntaxException {

        List<NestedJar> nestedJarTree = new ArrayList<>();

        // Convert the mod URL into something we can actually read
        URI modUri = modResource.toURI();

        try (InputStream modStream = modResource.openStream()) {

            // Find nested jars inside the mod (e.g. under "JarJar/")
            List<String> nestedLibs = getNestedPathsFromStream(modStream, "JarJar");

            // Create a root node representing the mod itself
            NestedJar modNode = new NestedJar(modUri.toString(), new ArrayList<>());

            for (String libPath : nestedLibs) {
                modNode.nestedJars().add(new NestedJar(libPath, new ArrayList<>()));
            }

            nestedJarTree.add(modNode);
        }

        return nestedJarTree;
    }

    public static String getNestedJarPath(URL rootJar, String jarName) {
        return rootJar.toString() + "!/" + jarName;
    }
}
