package org.mangorage.tsml;

import org.mangorage.tsml.api.logger.ILogger;
import org.mangorage.tsml.internal.core.TSMLDefaultLogger;

public final class TSMLLogger {
    private static ILogger activeLogger = new TSMLDefaultLogger();

    public static void setActiveLogger(ILogger logger) {
        activeLogger = logger;
    }

    public static ILogger get() {
        return activeLogger;
    }

}
