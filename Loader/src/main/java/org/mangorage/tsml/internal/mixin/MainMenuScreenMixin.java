package org.mangorage.tsml.internal.mixin;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.github.tommyettinger.textra.TypingLabel;
import com.imjustdoom.triviaspire.TriviaSpire;
import com.imjustdoom.triviaspire.screen.BaseScreen;
import com.imjustdoom.triviaspire.screen.LoadingScreen;
import org.mangorage.tsml.api.Mods;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BaseScreen.class)
public abstract class MainMenuScreenMixin {
    private TypingLabel injectedLabel;
    private Table rootTable; // Store the table so we can clean it up

    @Inject(method = "show", at = @At("TAIL"))
    private void onShow(CallbackInfo ci) {
        BaseScreen self = (BaseScreen)(Object)this;
        Stage stage = self.getUiStage();

        if (!(self instanceof LoadingScreen)) {
            // 1. Cleanup: Remove both the label and its container table
            if (rootTable != null) rootTable.remove();

            // 2. Initialize the Label
            injectedLabel = new TypingLabel("TSML v" + Mods.getMod("tsml").getVersion(), TriviaSpire.get().getTextures().getSkin());

            // 3. Create the Container Table
            rootTable = new Table();
            rootTable.setFillParent(true); // Table now matches the Stage size exactly

            // 4. "Snug" Alignment
            // .top().right() pushes the content to the top-right corner
            // .pad(10f) gives it a consistent 10px breathing room from the edges
            rootTable.top().right();
            rootTable.add(injectedLabel).padTop(10f).padRight(10f);

            // 5. Add to Stage
            stage.addActor(rootTable);
        }
    }
}