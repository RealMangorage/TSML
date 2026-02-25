package org.mangorage.tsmlmixin.mixin;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import com.llamalad7.mixinextras.utils.MixinInternals;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IClassGenerator;

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

    public static void prepare() {
        if (SpongeMixinImpl.transformer != null) return;
        SpongeMixinImpl.transformer = factory.createTransformer();
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
        prepare();
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

        completeMixinBootstrap();

        MixinExtrasBootstrap.init();

        Mixins.addConfiguration("tsmlcore.mixins.json"); // Testing
    }

}


