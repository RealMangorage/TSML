package org.mangorage.tsml.api.mod;

import org.mangorage.tsml.api.logger.ILoaderLogger;
import org.mangorage.tsml.internal.core.modloading.stages.ModLoadingStage;
import org.mangorage.tsml.internal.core.modloading.stages.ModLoadingManager;

import java.util.List;
import java.util.Optional;

public final class TSMLLoaderAPI {

    public ILoaderLogger getLogger() {
        return ModLoadingManager.getActiveLogger();
    }

    public static ModLoadingState getLoadingState() {
        return ModLoadingManager.getState();
    }

    public static Environment getEnvironment() {
        return ModLoadingManager.getEnvironment();
    }

    public static List<IModContainer> getAllMods() {
        return ModLoadingStage.getAllMods();
    }

    public static Optional<IModContainer> getMod(String id) {
        return Optional.ofNullable(ModLoadingStage.getMod(id));
    }
}
