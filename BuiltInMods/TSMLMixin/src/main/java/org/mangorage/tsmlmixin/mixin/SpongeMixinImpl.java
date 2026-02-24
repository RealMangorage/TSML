package org.mangorage.tsmlmixin.mixin;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;

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

        MixinBootstrap.init();

        transformer = factory.createTransformer();

        MixinExtrasBootstrap.init();
    }

}


