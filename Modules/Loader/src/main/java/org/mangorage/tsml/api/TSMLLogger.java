package org.mangorage.tsml.api;

import org.mangorage.tsml.api.logger.ILogger;
import org.mangorage.tsml.api.mod.TSMLLoaderAPI;
import org.mangorage.tsml.bootstrap.internal.TSMLDefaultLogger;
import org.mangorage.tsml.internal.core.modloading.stages.ModLoadingManager;

public final class TSMLLogger {
    /**
     * Gets the logger for the bootstrap phase.
     * This will return a default logger if the loader logger is not set yet.
     */
    public static ILogger getLogger() {
        final var activeLogger = ModLoadingManager.getActiveLogger();
        if (activeLogger == null)
            return TSMLDefaultLogger.getInstance();
        return activeLogger;
    }
}
