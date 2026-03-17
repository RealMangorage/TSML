package org.mangorage.tsml.api.logger;

public interface ILoaderLogger {
    // Simple message logging
    void debug(String log);
    void info(String log);
    void warn(String log);
    void error(String log);

    // Throwable-aware logging
    void debug(Throwable throwable);
    void info(Throwable throwable);
    void warn(Throwable throwable);
    void error(Throwable throwable);
}
