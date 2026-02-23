package org.mangorage.tsmlmixin.mixin;


import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import org.mangorage.tsml.TSML;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.MixinService;

import java.lang.reflect.Method;

public final class SpongeMixinImpl {
    private static final boolean DEBUG = false;

    private static boolean loaded = false;

    private static IMixinTransformerFactory factory;
    private static IMixinTransformer transformer;

    public static void setTransformerFactory(IMixinTransformerFactory factory) {
        SpongeMixinImpl.factory = factory;
    }

    public static IMixinTransformer getTransformer() {
        return transformer;
    }

    public static void load() {
        if (loaded) return;
        loaded = true;

        // Load

        if (DEBUG) {
            System.setProperty("mixin.debug.verbose", "true");
            System.setProperty("mixin.debug", "true");
            System.setProperty("mixin.env.disableRefMap", "true");
            System.setProperty("mixin.checks", "true");
        }

        final var side = switch (TSML.getEnvironment()) {
            case CLIENT -> MixinEnvironment.Side.CLIENT;
            case SERVER -> MixinEnvironment.Side.SERVER;
            case UNKNOWN -> MixinEnvironment.Side.UNKNOWN;
        };

        MixinBootstrap.init();

        completeMixinBootstrap();

       // MixinExtrasBootstrap.init();
    }

    private static void completeMixinBootstrap() {
        // Move to the default phase.
        try {
            final Method method = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
            method.setAccessible(true);
            method.invoke(null, MixinEnvironment.Phase.INIT);
            method.invoke(null, MixinEnvironment.Phase.DEFAULT);
        } catch(final Exception exception) {
            exception.printStackTrace();
        }
        transformer = factory.createTransformer();
    }
}


