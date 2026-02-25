package org.mangorage.tsml.internal.core.modloading;

import org.mangorage.tsml.api.logger.ILoaderLogger;
import org.mangorage.tsml.api.mod.Environment;
import org.mangorage.tsml.api.mod.IModPreLaunch;
import org.mangorage.tsml.api.mod.ModLoadingState;
import org.mangorage.tsml.internal.core.nested.api.IJar;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public final class ModLoadingManager {

    private static final DiscoveryStage DISCOVERY_STAGE = new DiscoveryStage();
    private static final InitialSetupStage INITIAL_SETUP_STAGE = new InitialSetupStage();

    private static volatile ModLoadingState state = ModLoadingState.NOT_LOADED;

    private static volatile ILoaderLogger activeLogger = null;
    private static volatile Environment environment = Environment.UNKNOWN;

    static void setupLogger(ILoaderLogger logger) {
        activeLogger = logger;
    }

    static void setEnvironment(Environment env) {
        environment = env;
    }

    public static ModLoadingState getState() {
        return state;
    }

    public static ILoaderLogger getActiveLogger() {
        return activeLogger;
    }

    public static Environment getEnvironment() {
        return environment;
    }

    static void init(URL baseResource, String[] args) throws Exception {
        final List<IJar> discoveredJars = new ArrayList<>();
        state = ModLoadingState.SETUP;
        final IJar triviaSpireJar = DISCOVERY_STAGE.run(baseResource, discoveredJars);

        state = ModLoadingState.MOD_DISCOVERY;
        final InitialSetupStage.StageResult initialStageResult = INITIAL_SETUP_STAGE.run(discoveredJars, triviaSpireJar, ModLoadingManager::setupLogger, ModLoadingManager::setEnvironment);

        state = ModLoadingState.MOD_SCANNING_STATE;
        TSMLModloader.scanMods(initialStageResult.foundClass(), args);

        state = ModLoadingState.MOD_PRE_LOAD_STATE;
        ServiceLoader.load(IModPreLaunch.class, initialStageResult.classloader()).forEach(IModPreLaunch::onPreLaunch);

        state = ModLoadingState.MOD_LOADING_STATE;
        TSMLModloader.initMods();

        state = ModLoadingState.LOADED;
    }

    public static void run(URL baseResource, String[] args) throws Exception {
        if (state != ModLoadingState.NOT_LOADED) {
            activeLogger.warn("TSML is already initialized, skipping");
            return;
        }

        init(baseResource, args);
    }
}
