package org.mangorage.tsmlmixin.services;

import org.mangorage.tsml.api.TSMLLogger;
import org.mangorage.tsml.api.mod.IModPreLaunch;
import org.mangorage.tsml.api.mod.Mods;
import org.spongepowered.asm.mixin.Mixins;

import java.util.List;
import java.util.Optional;

public final class ModPreLaunchServiceImpl implements IModPreLaunch {
    @Override
    public void onPreLaunch() {
//        Mods.getAllMods().forEach(mod -> {
//            try {
//                final Optional<List<String>> mixins = mod.getPropertyList("mixins", String.class);
//                if (mixins.isPresent()) {
//                    TSMLLogger.getLogger().info("Adding mixins for mod " + mod.getId() + ": " + mixins);
//                    mixins.get().forEach(Mixins::addConfiguration);
//                }
//            } catch (Throwable throwable) {
//                TSMLLogger.getLogger().error("Failed to run pre launch for mod " + mod.getId());
//                TSMLLogger.getLogger().error(throwable);
//            }
//        });
        Mixins.addConfiguration("tsmlcore.mixins.json"); // Testing
    }
}
