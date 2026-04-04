package org.mangorage.tsml.internal.core.modloading.stages;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class TSMLThreads {

    // Main thread reference
    private static volatile Thread MAIN_THREAD;

    // Queue for tasks that MUST run on main thread
    private static final BlockingQueue<Runnable> MAIN_QUEUE = new LinkedBlockingQueue<>();

    // Background executor (now actually useful)
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private TSMLThreads() {
        throw new UnsupportedOperationException("No instances");
    }

    /**
     * Call this ONCE from your main method.
     */
    public static void initMainThread() {
        if (MAIN_THREAD != null) {
            throw new IllegalStateException("Main thread already initialized");
        }
        MAIN_THREAD = Thread.currentThread();
    }

    /**
     * Run something on the main thread.
     */
    public static void runOnMain(Runnable task) {
        if (Thread.currentThread() == MAIN_THREAD) {
            task.run();
        } else {
            MAIN_QUEUE.add(task);
        }
    }

    /**
     * Pump queued main-thread tasks.
     * You MUST call this in your main loop.
     */
    public static void pumpMainQueue() {
        Runnable task;
        while ((task = MAIN_QUEUE.poll()) != null) {
            try {
                task.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    /**
     * Run async (background).
     */
    public static Future<?> runAsync(Runnable runnable) {
        return EXECUTOR.submit(runnable);
    }

    /**
     * Shutdown everything cleanly.
     */
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