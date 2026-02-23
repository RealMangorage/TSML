package org.mangorage.tsml.internal.core;


import org.mangorage.tsml.api.logger.ILoaderLogger;

import java.lang.reflect.Method;

public final class TSMLTriviaSpireReflectiveLogger implements ILoaderLogger {
    private final Class<?> delegate;

    public TSMLTriviaSpireReflectiveLogger(Class<?> delegate) {
        this.delegate = delegate;
    }

    private void call(String methodName, Class<?> paramType, Object arg) {
        try {
            Method m = delegate.getMethod(methodName, paramType);
            m.invoke(null, arg); // assuming static methods
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Delegate does not have method: " + methodName, e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke delegate method: " + methodName, e);
        }
    }

    private String getCallingClass() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        return stack[4].getClassName();
    }

    private String formatLog(String log) {
        final var caller = getCallingClass();
        return "[TSML] [" + caller + "] " + log;
    }

    @Override
    public void debug(String log) {
        call("debug", String.class, formatLog(log));
    }

    @Override
    public void info(String log) {
        call("info", String.class, formatLog(log));
    }

    @Override
    public void warn(String log) {
        call("warn", String.class, formatLog(log));
    }

    @Override
    public void error(String log) {
        call("error", String.class, formatLog(log));
    }

    @Override
    public void debug(Throwable throwable) {
        call("debug", Throwable.class, throwable);
    }

    @Override
    public void info(Throwable throwable) {
        call("info", Throwable.class, throwable);
    }

    @Override
    public void warn(Throwable throwable) {
        call("warn", Throwable.class, throwable);
    }

    @Override
    public void error(Throwable throwable) {
        call("error", Throwable.class, throwable);
    }
}