package org.mangorage.tsml.internal.core.mod;

import org.mangorage.tsml.api.TSMLLogger;
import org.mangorage.tsml.api.mod.Mods;

import java.lang.reflect.InvocationTargetException;

public final class TriviaSpireMod {
    public TriviaSpireMod() {
        Mods.getMod("trivia-spire").ifPresent(mod -> {
            TSMLLogger.getInternal().info("Trivia Spire mod found: " + mod.getName());
            TSMLLogger.getInternal().info("Starting TriviaSpire");

            final String mainClass = mod.getProperty("mainClass", String.class).orElseThrow(() -> new IllegalArgumentException("Trivia Spire mod is missing mainClass property"));

            final String[] args = mod.getProperty("args", String[].class).orElse(new String[0]);

            try {
                final var clazz = Thread.currentThread().getContextClassLoader().loadClass(mainClass);
                clazz.getMethod("main", String[].class).invoke(null, (Object) args);
            } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                TSMLLogger.getInternal().error("Something went wrong while starting Trivia Spire mod:");
                TSMLLogger.getInternal().error(e);
                e.printStackTrace();
            }
        });
    }
}
