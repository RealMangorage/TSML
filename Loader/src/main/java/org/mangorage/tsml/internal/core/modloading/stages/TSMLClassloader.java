package org.mangorage.tsml.internal.core.modloading.stages;

import org.mangorage.tsml.api.classloader.IClassTransformer;
import org.mangorage.tsml.api.jar.IJar;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TSMLClassloader extends JarClassloader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private final List<IClassTransformer> transformers = new CopyOnWriteArrayList<>();

    TSMLClassloader(List<IJar> jars, ClassLoader parent) {
        super(jars, parent);
    }

    void init() {
        // Auto-load transformers
        TSMLThreads.run(() -> {
            ServiceLoader.load(IClassTransformer.class, this)
                    .stream()
                    .forEach(provider -> {
                        TSMLThreads.run(() -> {
                            try {
                                transformers.add(provider.get());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    });
        });
    }

    /**
     * @param name -> Name of the class we want to transform
     * @param original -> The Class bytes associated with it, can be null, which means we need to generate the class fresh.
     * @return The transformed class bytes, or the original if no transformations were applied.
     */
    @Override
    protected byte[] maybeTransform(String name, final byte[] original) {
        byte[] finishedClassbytes = original;

        if (transformers.isEmpty()) { // No transformers, skip the loop and just return the original to save time.
            return super.maybeTransform(name, original);
        } else {
            if (finishedClassbytes == null) {
                for (IClassTransformer transformer : transformers) {
                    finishedClassbytes = transformer.generateClass(name);
                    if (finishedClassbytes != null) {
                        break; // We only want to generate once, if multiple transformers generate the same class, the first one wins.
                    }
                }
            }
        }

        // Then apply transformations to the generated or loaded class bytes.
        for (IClassTransformer transformer : transformers) {
            final byte[] transformed = transformer.transform(name, finishedClassbytes);
            if (transformed != null)
                finishedClassbytes = transformed; // If a transformer returns null, it means it doesn't want to transform this class, so we keep the current bytes.
        }

        return finishedClassbytes;
    }
}