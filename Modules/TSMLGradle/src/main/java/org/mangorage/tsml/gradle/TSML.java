package org.mangorage.tsml.gradle;

public final class TSML {
    private static boolean enabled = true;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void disable() {
        enabled = false;
    }
}
