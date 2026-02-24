package org.mangorage.tsmlmixin.mixin;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import com.llamalad7.mixinextras.utils.MixinInternals;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IClassGenerator;

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

        final Extensions extensions = (Extensions) transformer.getExtensions();
        extensions.add(new IClassGenerator() {
            @Override
            public String getName() {
                System.out.println("[Mixin] getName() called on IClassGenerator");
                return "";
            }

            @Override
            public boolean generate(String name, ClassNode classNode) {
                System.out.println("[Mixin] Attempting to generate class: " + name);
                return false;
            }
        });

        System.out.println("[Mixin] Registered MixinExtension");

        MixinExtrasBootstrap.init();
    }

}


