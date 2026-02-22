package org.mangorage.tsml.api.mod;

import org.mangorage.tsml.internal.core.mod.TSMLModloader;

import java.util.List;
import java.util.Optional;

public final class Mods {
    public static List<IModContainer> getAllMods() {
        return TSMLModloader.getAllMods();
    }

    public static Optional<IModContainer> getMod(String id) {
        return Optional.ofNullable(TSMLModloader.getMod(id));
    }
}
