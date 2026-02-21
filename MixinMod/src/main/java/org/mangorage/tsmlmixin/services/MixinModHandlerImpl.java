package org.mangorage.tsmlmixin.services;

import org.mangorage.tsml.api.IMod;
import org.mangorage.tsml.api.IModHandler;
import org.mangorage.tsmlmixin.mixin.core.MixinMod;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

public final class MixinModHandlerImpl implements IModHandler {
    @Override
    public void handleMod(IMod mod) {
        if (mod.hasProperty("mixins"))
            Mixins.addConfiguration(
                    "mixins.%s.json".formatted(mod.getId()),
                    new MixinMod(mod)
            );
    }
}
