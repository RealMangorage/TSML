package org.mangorage.tsml.api;

import org.mangorage.tsml.internal.core.mod.TSMLModloader;

import java.util.List;

public final class Mods {
    public static List<IModContainer> getAllMods() {
        return TSMLModloader.getAllMods();
    }

    public static IModContainer getMod(String id) {
        return TSMLModloader.getMod(id);
    }
}
