package org.mangorage.tsml.internal.core.modloading.stages;

import org.mangorage.tsml.api.TSMLLogger;
import org.mangorage.tsml.api.mod.TSMLLoaderAPI;

import java.lang.reflect.InvocationTargetException;

public final class TriviaSpireMod {
    public TriviaSpireMod() {
        TSMLLoaderAPI.getMod("trivia-spire").ifPresent(mod -> {
            TSMLLogger.getLogger().info("Trivia Spire mod found: " + mod.getName());
            TSMLLogger.getLogger().info("Starting TriviaSpire");

            final String mainClass = mod.getProperty("mainClass", String.class).orElseThrow(() -> new IllegalArgumentException("Trivia Spire mod is missing mainClass property"));

            final String[] args = mod.getProperty("args", String[].class).orElse(new String[0]);

            TSMLLogger.getLogger().info("Initiating TriviaSpire Itself");

            init(mainClass, args);

            TSMLLogger.getLogger().info("Shutting everything down!");
            TSMLThreads.shutdown();
        });
    }

    public void init(String mainClass, String[] args) {
        try {
            args = new String[]{};
            final var clazz = Class.forName(mainClass, false, Thread.currentThread().getContextClassLoader());
            clazz.getMethod("main", String[].class).invoke(null, (Object) args);
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            TSMLLogger.getLogger().error("Something went wrong while starting Trivia Spire mod:");
            TSMLLogger.getLogger().error(e);
        }
    }
}
