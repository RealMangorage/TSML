package org.mangorage.tsml.internal.core.modloading;

import com.google.gson.Gson;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.mangorage.tsml.api.TSMLLogger;
import org.mangorage.tsml.api.classloader.ITSMLClassloader;
import org.mangorage.tsml.api.dependency.Dependency;
import org.mangorage.tsml.api.mod.IModContainer;
import org.mangorage.tsml.api.mod.Mod;
import org.mangorage.tsml.internal.core.nested.api.IJar;
import org.mangorage.tsml.internal.mod.BuiltInMod;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TODO: Integrate into the {@link ModLoadingManager} system
 */
@Deprecated
public final class TSMLModloader {

    private static final Map<String, IModContainer> modContainerMap = new HashMap<>();

    public static ModInfo getModInfo(String id) {
        // 1. Construct the expected filename
        String fileName = id + ".mods.json";

        // 2. Locate the file on the classpath
        // Using the ClassLoader ensures it looks inside all loaded mod JARs
        InputStream is = id.equals("tsml") ? Thread.currentThread().getContextClassLoader().getParent().getResourceAsStream(fileName) : Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);

        if (is == null) {
            System.err.println("Could not find metadata file: " + fileName);
            return null;
        }

        try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            // 3. Parse with GSON
           final Gson gson = new Gson();
            return gson.fromJson(reader, ModInfo.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static ModInfo createTriviaSpireModInfo(String mainClass, String args[]) {
        final Map<String, Object> map = new HashMap<>();
        map.put("mainClass", mainClass);
        map.put("args", args);

        return new ModInfo(
                "trivia-spire",
                "TriviaSpire Mod",
                "1.0.0", // TODO: Figure out how to make this fetch the correct version later...
                "Trivia Spire Game",
                TriviaSpireMod.class.getName(),
                List.of(),
                List.of("JustDoom"),
                map
        );
    }

    public static List<String> getClasspathAsStrings() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        if (cl instanceof ITSMLClassloader iJarCl) {
            // Map the IJar list to string representations (e.g., jar paths or names)
            return iJarCl.getJars().stream()
                    .map(IJar::getURL) // assuming IJar has a getName() method
                    .map(URL::toString)
                    .collect(Collectors.toList());
        } else if (cl instanceof URLClassLoader urlCl) {
            return Arrays.stream(urlCl.getURLs())
                    .map(URL::toString)
                    .collect(Collectors.toList());
        } else {
            // Fallback: maybe just return empty list or throw
            throw new IllegalStateException("Current ClassLoader is neither URLClassLoader nor TSMLClassloader");
        }
    }

    public static void scanMods(String mainClass, String[] args) {
        TSMLLogger.getInternal().info("Scanning for mods...");

        modContainerMap.put(
                "trivia-spire",
                new ModContainerImpl(
                        createTriviaSpireModInfo(mainClass, args),
                        TriviaSpireMod.class
                )
        );

        modContainerMap.put("tsml",
                new ModContainerImpl(
                        getModInfo("tsml"),
                        BuiltInMod.class
                )
        );

        try (ScanResult scanResult = new ClassGraph()
                .enableAnnotationInfo()         // Required to find @Annotation
                .enableClassInfo()
                .enableRemoteJarScanning()
                .addClassLoader(Thread.currentThread().getContextClassLoader())
                .overrideClasspath(getClasspathAsStrings())
                .scan()
        ) {

            // Find classes that have the @Mod annotation
            scanResult
                    .getClassesWithAnnotation(Mod.class)
                    .forEach(classInfo -> {
                        TSMLLogger.getInternal().info("Discovered: " + classInfo.getName());
                        final var id = classInfo.getAnnotationInfo(Mod.class.getName()).getParameterValues().getValue("id").toString();
                        ModInfo modInfo = getModInfo(id);
                        if (modInfo == null) {
                            TSMLLogger.getInternal().error("Failed to load mod metadata for " + id);
                        } else {
                            try {
                                Class<?> modClass = classInfo.loadClass();
                                IModContainer container = new ModContainerImpl(modInfo, modClass);
                                modContainerMap.put(id, container);
                                TSMLLogger.getInternal().info("Loaded mod: " + modInfo.name());
                            } catch (Throwable e) {
                                TSMLLogger.getInternal().warn("Failed to load mod class for " + id);
                                TSMLLogger.getInternal().error(e);
                            }
                        }
                    });
        }
    }

    public static void printDependencyGraph() {
        TSMLLogger.getInternal().info("=== Mod Dependency Graph ===");
        modContainerMap.forEach((id, mod) -> {
            List<Dependency> deps = mod.getDependencies();
            if (deps == null || deps.isEmpty()) {
                TSMLLogger.getInternal().info(id + " -> [none]");
            } else {
                String depList = deps.stream()
                        .map(d -> d.id() + (d.optional() ? " (optional)" : ""))
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                TSMLLogger.getInternal().info(id + " -> [" + depList + "]");
            }
        });
        TSMLLogger.getInternal().info("============================");
    }

    public static void initMods() {
        printDependencyGraph();
        // Build dependency graph
        Map<String, List<String>> graph = new HashMap<>();
        modContainerMap.forEach((id, mod) -> {
            graph.putIfAbsent(id, new ArrayList<>());

            // Add all dependencies (required + optional)
            List<Dependency> deps = mod.getDependencies();
            if (deps != null) {
                for (Dependency dep : deps) {
                    graph.get(id).add(dep.id());
                }
            }
        });

        // Topological sort to determine load order
        List<String> sorted = topologicalSort(graph);
        if (sorted == null) {
            TSMLLogger.getInternal().error("Failed to sort mods by dependencies (circular dependency detected).");
            return;
        }

        // Initialize mods in sorted order
        for (String id : sorted) {
            IModContainer mod = modContainerMap.get(id);
            List<Dependency> deps = mod.getDependencies();

            // Check that all required dependencies are present
            boolean canLoad = true;
            if (deps != null) {
                for (Dependency dep : deps) {
                    if (!dep.optional() && !modContainerMap.containsKey(dep.id())) {
                        TSMLLogger.getInternal().warn("Skipping mod " + id + " because required dependency is missing: " + dep.id());
                        canLoad = false;
                        break;
                    }
                }
            }

            if (!canLoad) continue;

            // Initialize
            TSMLLogger.getInternal().info("Initializing mod: " + id);
            try {
                ((ModContainerImpl) mod).init();
                TSMLLogger.getInternal().info("Initialized mod: " + id);
            } catch (Throwable e) {
                TSMLLogger.getInternal().warn("Failed to initialize mod: " + id);
                TSMLLogger.getInternal().error(e);
            }
        }
    }

    private static List<String> topologicalSort(Map<String, List<String>> graph) {
        List<String> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> tempMark = new HashSet<>();

        for (String node : graph.keySet()) {
            if (!visited.contains(node)) {
                if (!visit(node, graph, visited, tempMark, result)) {
                    return null; // Circular dependency detected
                }
            }
        }
        return result;
    }

    private static boolean visit(String node, Map<String, List<String>> graph, Set<String> visited, Set<String> tempMark, List<String> result) {
        if (tempMark.contains(node)) return false; // cycle detected
        if (visited.contains(node)) return true;

        tempMark.add(node);
        for (String dep : graph.getOrDefault(node, Collections.emptyList())) {
            if (!modContainerMap.containsKey(dep)) {
                TSMLLogger.getInternal().warn("Mod " + node + " depends on missing mod: " + dep);
                continue;
            }
            if (!visit(dep, graph, visited, tempMark, result)) return false;
        }
        tempMark.remove(node);
        visited.add(node);
        result.add(node);
        return true;
    }

    public static List<IModContainer> getAllMods() {
        return List.copyOf(modContainerMap.values());
    }

    public static IModContainer getMod(String id) {
        return modContainerMap.get(id);
    }
}
