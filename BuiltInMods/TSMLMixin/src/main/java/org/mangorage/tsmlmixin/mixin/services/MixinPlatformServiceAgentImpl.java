package org.mangorage.tsmlmixin.mixin.services;

import org.mangorage.tsml.TSML;
import org.spongepowered.asm.launch.platform.IMixinPlatformServiceAgent;
import org.spongepowered.asm.launch.platform.MixinPlatformAgentAbstract;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.util.Constants;

import java.util.Collection;
import java.util.List;

public final class MixinPlatformServiceAgentImpl extends MixinPlatformAgentAbstract implements IMixinPlatformServiceAgent {

    public MixinPlatformServiceAgentImpl() {
    }


    @Override
    public void init() {
    }

    @Override
    public String getSideName() {
        return switch (TSML.getEnvironment()) {
            case CLIENT -> Constants.SIDE_CLIENT;
            case SERVER -> Constants.SIDE_DEDICATEDSERVER;
            case UNKNOWN -> Constants.SIDE_UNKNOWN;
        };
    }

    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        return List.of();
    }
}
