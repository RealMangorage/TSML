package org.mangorage.tsmlmixin.mixin.core;

import org.mangorage.tsml.api.IMod;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource;

public record MixinMod(IMod mod) implements IMixinConfigSource {
    @Override
    public String getId() {
        return mod.getId();
    }

    @Override
    public String getDescription() {
        return mod.getDescription();
    }
}
