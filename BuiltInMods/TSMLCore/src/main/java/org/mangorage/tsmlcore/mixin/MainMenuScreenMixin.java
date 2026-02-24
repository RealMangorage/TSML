package org.mangorage.tsmlcore.mixin;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.imjustdoom.triviaspire.register.UiElements;
import com.imjustdoom.triviaspire.screen.BaseScreen;
import com.imjustdoom.triviaspire.screen.MainMenuScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MainMenuScreen.class)
public abstract class MainMenuScreenMixin {

    @Shadow @Final private Button settingsButton;
    @Unique private Button myCustomIconButton;


    @Inject(method = "<init>", at = @At("TAIL"))
    private void initCustomIconButton(CallbackInfo ci) {
        BaseScreen screen = (BaseScreen) (Object) this;

        try {
            // 1. Copy the style from the Settings button
            Button.ButtonStyle style = UiElements.getButtonStyle(UiElements.SETTINGS_BUTTON);
            this.myCustomIconButton = new Button(style);

            // 2. Set position: Settings is at (10, 10).
            // We'll put ours at (10, 45) to stack it neatly above.
            this.myCustomIconButton.setPosition(10.0F, 45.0F);

            this.myCustomIconButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    System.out.println("Custom icon button clicked!");
                }
            });

            // 3. Add to the stage and register hover effect
            screen.getStage().addActor(this.myCustomIconButton);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}