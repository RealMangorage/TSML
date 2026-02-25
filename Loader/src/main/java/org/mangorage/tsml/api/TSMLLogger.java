package org.mangorage.tsml.api;

import org.mangorage.tsml.api.logger.ILoaderLogger;
import org.mangorage.tsml.bootstrap.api.logger.ILogger;
import org.mangorage.tsml.bootstrap.internal.TSMLDefaultLogger;
import org.mangorage.tsml.internal.core.modloading.ModLoadingManager;

public final class TSMLLogger {
    /**
     * Gets the logger for the bootstrap phase.
     * This will return a default logger if the loader logger is not set yet.
     */
    public static ILogger getInternal() {
        return TSMLDefaultLogger.getInstance();
    }

    /**
     * Gets the logger for the loader phase. This will return null if the loader logger is not set yet.
     */
    public static ILoaderLogger getLogger() {
        return ModLoadingManager.getActiveLogger();
    }
}
