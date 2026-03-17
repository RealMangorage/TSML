package org.mangorage.tsml.internal.core.modloading.stages;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class TSMLThreads {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(
            8, // Consider Runtime.getRuntime().availableProcessors() instead of a hardcoded 8
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "TSML-Worker-" + counter.getAndIncrement());
                    t.setDaemon(false);
                    return t;
                }
            }
    );

    private TSMLThreads() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static Future<?> run(Runnable runnable) {
        return EXECUTOR.submit(runnable);
    }

    public static void shutdown() {
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}