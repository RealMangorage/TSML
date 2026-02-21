package org.mangorage.tsml.internal.mixin;

import com.imjustdoom.triviaspire.shared.TriviaLogger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(com.imjustdoom.triviaspire.lwjgl3.Lwjgl3Launcher.class)
public final class LWJGL3LauncherMixin {
    @Shadow
    public static void main(String[] args) {
        TriviaLogger.info("Hello from the mixin!");
    }
}
