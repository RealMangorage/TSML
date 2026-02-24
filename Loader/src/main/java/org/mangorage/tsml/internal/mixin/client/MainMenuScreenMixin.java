package org.mangorage.tsml.internal.mixin.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
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
public abstract class MainMenuScreenMixin extends BaseScreen {

    @Final
    @Shadow
    private Button singleplayerButton;

    @Final
    @Shadow
    private Button multiplayerButton;

    @Unique
    private Button customButton;

    // Dummy constructor to satisfy extending BaseScreen so we can access getMiddleground()
    protected MainMenuScreenMixin() {
        super(0, 0, 0, 0);
    }

    /**
     * Inject at the end of the constructor to initialize and register our custom button.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void initCustomButton(CallbackInfo ci) {
        System.out.println("[Mixin] Starting custom button initialization...");

        try {
            System.out.println("[Mixin] Fetching button style...");
            Button.ButtonStyle style = UiElements.getButtonStyle(UiElements.QUIT_GAME_BUTTON);

            System.out.println("[Mixin] Creating customButton instance...");
            this.customButton = new Button(style);

            System.out.println("[Mixin] Setting customButton visibility...");
            this.customButton.setVisible(false);

            System.out.println("[Mixin] Adding ClickListener to customButton...");
            this.customButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    System.out.println("Custom button clicked!");
                }
            });

            System.out.println("[Mixin] Fetching middleground and adding customButton actor...");
            this.getMiddleground().addActor(this.customButton);

            System.out.println("[Mixin] Custom button initialization complete!");

        } catch (Exception e) {
            System.err.println("[Mixin] CRASH during initCustomButton! Printing stack trace:");
            e.printStackTrace();
        }
    }

    /**
     * Inject into the first lambda inside playIntro() where positions and animations are set.
     * Note: Depending on your compiler (Javac vs ECJ), the lambda name might slightly vary,
     * but 'lambda$playIntro$0' is standard for JavaC.
     */
    @Inject(method = "lambda$playIntro$0", at = @At("TAIL"), remap = false)
    private void injectIntoPlayIntroAnimation(CallbackInfo ci) {
        float from = this.getStage().getHeight();

        // 1. Shift the upper buttons up by 33 pixels (the standard vertical gap in the original code)
        // Original singleplayer Y was from + 96.0F; multiplayer was from + 129.0F
        this.singleplayerButton.setY(from + 129.0F);
        this.multiplayerButton.setY(from + 162.0F);

        // 2. Set our custom button's visibility and starting position
        this.customButton.setVisible(true);
        // Place it right where singleplayer used to be (above the quit button which is at 63.0F)
        this.customButton.setPosition(50.0F, from + 96.0F);

        // 3. Add the sliding drop animation.
        // The delays in the original are 0.4F (quit) and 0.5F (singleplayer), so we use 0.45F.
        this.customButton.addAction(Actions.delay(0.45F, Actions.sequence(
                Actions.moveBy(0.0F, -from, 0.5F),
                Actions.run(() -> {
                    Gdx.input.vibrate(69);
                    this.addButtonHover(this.customButton);
                })
        )));
    }
}