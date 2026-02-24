package org.mangorage.tsmlcore.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.github.tommyettinger.textra.TextraLabel;
import com.imjustdoom.triviaspire.TriviaSpire;
import com.imjustdoom.triviaspire.register.UiElements;
import com.imjustdoom.triviaspire.screen.BaseScreen;
import org.mangorage.tsml.api.mod.IModContainer;
import org.mangorage.tsml.api.mod.Mods;

public class ModMenuScreen extends BaseScreen {

    public ModMenuScreen() {
        super(0.05f, 0.05f, 0.1f, 1.0f);
    }

    @Override
    public void show() {
        super.show();

        Skin skin = TriviaSpire.get().getTextures().getSkin();

        // --- ROOT LAYOUT ---
        Table root = new Table();
        root.setFillParent(true);
        // Use padding based on percentage or safe-insets for mobile
        root.pad(Value.percentWidth(0.05f, root));

        // --- TITLE ---
        var title = new TextraLabel("MODS", skin);
        title.setWrap(true);
        root.add(title).expandX().fillX().center().padBottom(20).row();

        // --- MOD LIST ---
        Table modList = new Table();
        modList.top().left();
        // This is key: defaults apply to all cells added to this table
        modList.defaults().pad(5).expandX().fillX();

        for (IModContainer mod : Mods.getAllMods()) {
            Table modEntry = new Table(skin);

            // Name (Left) & Version (Right)
            var nameLabel = new TextraLabel(mod.getName().toUpperCase(), skin);
            nameLabel.setColor(Color.CYAN);
            var versionLabel = new TextraLabel("V" + mod.getVersion().toUpperCase(), skin);

            modEntry.add(nameLabel).left().expandX();
            modEntry.add(versionLabel).right().row();

            // Authors
            if (mod.getAuthors() != null && !mod.getAuthors().isEmpty()) {
                String authors = "BY: " + String.join(", ", mod.getAuthors()).toUpperCase();
                var authorsLabel = new TextraLabel(authors, skin);
                authorsLabel.setColor(Color.GOLD);
                authorsLabel.setWrap(true); // Wrap authors if the list is long
                modEntry.add(authorsLabel).colspan(2).left().fillX().padTop(1).row();
            }

            // Description - DYNAMIC WRAPPING
            if (mod.getDescription() != null && !mod.getDescription().isEmpty()) {
                var descLabel = new TextraLabel(mod.getDescription().toUpperCase(), skin);
                descLabel.setWrap(true);

                // Using fillX() and expandX() here ensures the label takes up
                // the available width of the table before wrapping.
                modEntry.add(descLabel).colspan(2).left().expandX().fillX().padTop(4).row();
            }

            modList.add(modEntry).padBottom(15).row();
        }

        // --- SCROLLING ---
        ScrollPane scroll = new ScrollPane(modList);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false); // Disable horizontal scroll, enable vertical

        // This makes the scroll pane occupy all available space between title and button
        root.add(scroll).expand().fill().row();

        // --- NAVIGATION ---
        Button backButton = new Button(UiElements.getButtonStyle(UiElements.LEAVE_GAME_BUTTON));
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                TriviaSpire.get().setScreen(Screens.MENU);
            }
        });

        // Use a Value for the button size to keep it consistent across DPIs
        float btnSize = 48f;
        root.add(backButton).size(btnSize).padTop(10).bottom();

        this.getUiStage().addActor(root);
    }
}