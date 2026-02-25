package org.mangorage.tsmlcore;

import org.mangorage.tsml.api.mod.IEarlyMod;
import org.mangorage.tsml.api.mod.ModLoadingState;
import org.mangorage.tsml.internal.core.modloading.ModLoadingManager;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public final class EarlyWindow implements IEarlyMod {

    private static boolean loaded = false;
    private static JFrame frame;
    private static JLabel label;
    private static volatile boolean running = false;

    // Track the last observed state and when it started
    private static ModLoadingState lastState = ModLoadingState.NOT_LOADED;
    private static long stateStartTime;

    public EarlyWindow() {
        init();
        loaded = true;
        stateStartTime = System.currentTimeMillis();
    }

    public static void init() {
        if (loaded) return;

        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Loading TriviaSpire Mod Loader");
            label = new JLabel("", SwingConstants.CENTER);

            frame.add(label);
            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.setVisible(true);

            startLoop();
        });
    }

    private static void startLoop() {
        running = true;
        Thread loopThread = new Thread(() -> {
            long lastTime = System.nanoTime();
            final double nsPerFrame = 1_000_000_000.0 / 30.0; // 30 FPS

            while (running) {
                long now = System.nanoTime();
                if (now - lastTime >= nsPerFrame) {
                    updateFrame();
                    lastTime += nsPerFrame;
                } else {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ignored) {}
                }
            }
        }, "EarlyWindow-FPS-Loop");
        loopThread.setDaemon(true);
        loopThread.start();
    }

    private static void updateFrame() {
        if (label == null) return;

        // Get the current state from ModLoadingManager
        ModLoadingState currentState = ModLoadingManager.getState();

        // If the state changed, reset the timer
        if (currentState != lastState) {
            lastState = currentState;
            stateStartTime = System.currentTimeMillis();
        }

        long elapsed = (System.currentTimeMillis() - stateStartTime) / 1000; // seconds

        String text = "<html>State: " + currentState
                + "<br>Time in state: " + elapsed + "s</html>";

        SwingUtilities.invokeLater(() -> label.setText(text));
    }

    public static void close() {
        running = false;
        if (frame == null) return;
        SwingUtilities.invokeLater(() -> frame.dispose());
    }
}