package org.mangorage.tsml.internal.core.modloading.stages;

import org.mangorage.jar.api.IJar;
import org.mangorage.jar.api.JarWithMetadata;
import org.mangorage.tsml.api.logger.ILogger;
import org.mangorage.tsml.api.mod.Environment;
import org.mangorage.tsml.api.mod.IModPreLaunch;
import org.mangorage.tsml.api.mod.ModLoadingState;

import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ModLoadingManager {

    private static final Object lock = new Object();

    // Get all the jars from mods folder, and their nested jars.
    // Get all the nested jars from TSML itself, and return the TriviaSpire jar.
    private static final InitialDiscoveryStage INITIAL_DISCOVERY_STAGE = new InitialDiscoveryStage();

    // Take in the discovered jars, and set up the classloader.
    private static final ModSetupStage MOD_SETUP_STAGE = new ModSetupStage();

    private static volatile ModLoadingState state = ModLoadingState.NOT_LOADED;

    private static volatile ILogger activeLogger = null;
    private static volatile Environment environment = Environment.UNKNOWN;

    static void setupLogger(ILogger logger) {
        synchronized (lock) {
            activeLogger = logger;
        }
    }

    static void setEnvironment(Environment env) {
        synchronized (lock) {
            environment = env;
        }
    }

    static void setModSetupStage(ModLoadingState modSetupStage) {
        synchronized (lock) {
            state = modSetupStage;
        }
    }

    public synchronized static ModLoadingState getState() {
        synchronized (lock) {
            return state;
        }
    }

    public synchronized static ILogger getActiveLogger() {
        synchronized (lock) {
            return activeLogger;
        }
    }

    public synchronized static Environment getEnvironment() {
        synchronized (lock) {
            return environment;
        }
    }

    static void init(Path loaderJarPath, String[] args) throws Exception {

        final List<JarWithMetadata> discoveredJars = new CopyOnWriteArrayList<>();
        setModSetupStage(ModLoadingState.INITIAL_SETUP);

        final IJar triviaSpireJar = INITIAL_DISCOVERY_STAGE.run(loaderJarPath, discoveredJars, args);
        setModSetupStage(ModLoadingState.MOD_DISCOVERY);

        // TODO: Just stream map it, we dont need to worry about it right away!
        final ModSetupStage.StageResult initialStageResult = MOD_SETUP_STAGE.run(discoveredJars, triviaSpireJar, ModLoadingManager::setupLogger, ModLoadingManager::setEnvironment);

        setModSetupStage(ModLoadingState.MOD_SCANNING);
        ModLoadingStage.scanMods(initialStageResult.foundClass(), args);

        setModSetupStage(ModLoadingState.MOD_PRE_LOAD);
        ServiceLoader.load(IModPreLaunch.class, initialStageResult.classloader()).forEach(IModPreLaunch::onPreLaunch);

        setModSetupStage(ModLoadingState.MOD_LOADING);
        ModLoadingStage.initMods();

        setModSetupStage(ModLoadingState.FINISHED);
    }

    public static void run(Object loaderJarPath, String[] args) throws Exception {
        if (state != ModLoadingState.NOT_LOADED) {
            activeLogger.warn("TSML is already initialized, skipping");
            return;
        }

        init((Path) loaderJarPath, args);
    }
}
