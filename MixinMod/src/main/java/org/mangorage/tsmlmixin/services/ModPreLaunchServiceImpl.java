package org.mangorage.tsmlmixin.services;

import org.mangorage.tsml.TSMLLogger;
import org.mangorage.tsml.api.IModPreLaunch;
import org.mangorage.tsml.api.Mods;
import org.spongepowered.asm.mixin.Mixins;

import java.util.List;

public final class ModPreLaunchServiceImpl implements IModPreLaunch {
    @Override
    public void onPreLaunch() {
        Mods.getAllMods().forEach(mod -> {
            try {
                final List<String> mixins = mod.getProperty("mixins", List.class);
                if (mixins != null) {
                    TSMLLogger.get().info("Adding mixins for mod " + mod.getId() + ": " + mixins);
                    mixins.forEach(Mixins::addConfiguration);
                }
            } catch (Throwable throwable) {
                TSMLLogger.get().error("Failed to run pre launch for mod " + mod.getId());
                TSMLLogger.get().error(throwable);
            }
        });
    }
}
