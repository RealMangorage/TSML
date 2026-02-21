package org.mangorage.tsml.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(com.imjustdoom.triviaspire.lwjgl3.Lwjgl3Launcher.class)
public final class LWJGL3LauncherMixin {
    @Overwrite
    public static void main(String[] args) {
        System.out.println("Hello from the mixin!");
    }
}
