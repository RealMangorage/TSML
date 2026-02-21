package org.mangorage.tsmlmixin.mixin.services;

import org.mangorage.tsml.TSMLLogger;
import org.spongepowered.asm.service.IMixinServiceBootstrap;

public final class MixinBootstrapServiceImpl implements IMixinServiceBootstrap {
    @Override
    public String getName() {
        return "TSMLMixinBootstrap";
    }

    @Override
    public String getServiceClassName() {
        return "org.mangorage.tsmlmixin.mixin.services.TSMLMixinServiceImpl";
    }

    @Override
    public void bootstrap() {
        TSMLLogger.get().info("MixinBootstrapServiceImpl.bootstrap() called");
    }
}
