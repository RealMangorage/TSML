package org.mangorage.tsmlmixin.services;

import org.mangorage.tsml.api.classloader.IClassTransformer;
import org.mangorage.tsmlmixin.mixin.SpongeMixinImpl;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import java.util.List;

public final class SpongeMixinClassTransformerImpl implements IClassTransformer {

    private final List<String> blacklisted = List.of(
            "java.",
            "org.spongepowered",
            "org.objectweb"
    );

    public SpongeMixinClassTransformerImpl() {
        SpongeMixinImpl.load();
    }

    @Override
    public byte[] transform(String name, byte[] bytes) {
        for (String s : blacklisted) {
            if (name.startsWith(s)) {
                return null;
            }
        }

        var transformer = SpongeMixinImpl.getTransformer();

        var transformed = transformer.transformClass(
                MixinEnvironment.getCurrentEnvironment(),
                name,
                bytes
        );

        if (!areByteArraysEqual(transformed, bytes)) {
            return transformed;
        } else {
            return null;
        }
    }

    @Override
    public byte[] generateClass(String name) {
        return ((IMixinTransformer)MixinEnvironment.getCurrentEnvironment().getActiveTransformer()).generateClass(MixinEnvironment.getCurrentEnvironment(), name);
    }

    public static boolean areByteArraysEqual(byte[] a, byte[] b) {
        if (a == b) return true; // same reference, duh
        if (a == null || b == null) return false; // null? get out
        if (a.length != b.length) return false; // different size = automatic failure

        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) return false; // mismatch? trash it
        }

        return true; // you got lucky, they're equal
    }
}
