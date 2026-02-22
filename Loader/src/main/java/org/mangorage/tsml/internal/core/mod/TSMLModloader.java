package org.mangorage.tsml.internal.core.mod;

import com.google.gson.Gson;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.mangorage.tsml.TSMLLogger;
import org.mangorage.tsml.api.IModContainer;
import org.mangorage.tsml.api.Mod;
import org.mangorage.tsml.internal.core.BuiltInMod;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static void scanMods(List<String> nestedJars) {
        TSMLLogger.get().info("Scanning for mods...");

        modContainerMap.put("tsml", new ModContainerImpl(
                getModInfo("tsml"),
                BuiltInMod.class
        ));

        try (ScanResult scanResult = new ClassGraph()
                .enableAnnotationInfo()         // Required to find @Annotation
                .enableClassInfo()
                .enableRemoteJarScanning()
                .addClassLoader(Thread.currentThread().getContextClassLoader())
                .overrideClasspath(nestedJars)
                .scan()
        ) {

            // Find classes that have the @Mod annotation
            scanResult
                    .getClassesWithAnnotation(Mod.class)
                    .forEach(classInfo -> {
                        TSMLLogger.get().info("Discovered: " + classInfo.getName());
                        final var id = classInfo.getAnnotationInfo(Mod.class.getName()).getParameterValues().getValue("id").toString();
                        ModInfo modInfo = getModInfo(id);
                        if (modInfo == null) {
                            TSMLLogger.get().error("Failed to load mod metadata for " + id);
                        } else {
                            try {
                                Class<?> modClass = classInfo.loadClass();
                                IModContainer container = new ModContainerImpl(modInfo, modClass);
                                modContainerMap.put(id, container);
                                TSMLLogger.get().info("Loaded mod: " + modInfo.name());
                            } catch (Throwable e) {
                                TSMLLogger.get().warn("Failed to load mod class for " + id);
                                TSMLLogger.get().error(e);
                            }
                        }
                    });
        }
    }

    public static void initMods() {
        modContainerMap.forEach((id, container) -> {
            TSMLLogger.get().info("Initializing mod: " + id);
            try {
                ((ModContainerImpl) container).init();
                TSMLLogger.get().info("Initialized mod: " + id);
            } catch (Throwable e) {
                TSMLLogger.get().warn("Failed to initialize mod: " + id);
                TSMLLogger.get().error(e);
            }
        });
    }

    public static List<IModContainer> getAllMods() {
        return List.copyOf(modContainerMap.values());
    }

    public static IModContainer getMod(String id) {
        return modContainerMap.get(id);
    }
}
