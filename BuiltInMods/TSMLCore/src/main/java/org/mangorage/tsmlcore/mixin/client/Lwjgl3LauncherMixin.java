package org.mangorage.tsmlcore.mixin.client;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.imjustdoom.triviaspire.lwjgl3.Lwjgl3Launcher;
import org.mangorage.tsml.api.mod.TSMLLoaderAPI;
import org.mangorage.tsmlcore.EarlyWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Lwjgl3Launcher.class)
public final class Lwjgl3LauncherMixin {

    @Inject(
            method = "getDefaultConfiguration",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void onGetDefaultConfiguration(CallbackInfoReturnable<Lwjgl3ApplicationConfiguration> cir) {
        // Get the configuration object created by the original method
        Lwjgl3ApplicationConfiguration config = cir.getReturnValue();

        // Update the title
        config.setTitle("TSML - TriviaSpire Mod Loader v" + TSMLLoaderAPI.getMod("tsml").get().getVersion());
        
        EarlyWindow.close();
        // You can also change other things here, like the window size
        // config.setWindowedMode(1920, 1080);
    }
}