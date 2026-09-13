package io.bsoft.echo.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import io.bsoft.echo.EchoGame;
import io.bsoft.echo.rendering.Palette;
import io.bsoft.echo.ui.UiCanvas;

/** Title screen with a tiny keyboard menu. */
public final class MainMenuScreen extends ScreenAdapter {

    private static final String[] OPTIONS = {"Play", "Level select", "Quit"};

    private final EchoGame game;
    private final UiCanvas canvas;
    private int selected;
    private float time;

    public MainMenuScreen(EchoGame game) {
        this.game = game;
        this.canvas = new UiCanvas(game.assets());
    }

    @Override
    public void render(float delta) {
        time += delta;
        handleInput();
        ScreenUtils.clear(Palette.BACKGROUND_BOTTOM);
        canvas.begin();
        float cx = UiCanvas.WIDTH / 2f;
        // Title with trailing "echoes".
        for (int i = 3; i >= 1; i--) {
            float a = 0.12f * (4 - i);
            canvas.text(game.assets().fontTitle, "ECHO", cx - i * 14f, UiCanvas.HEIGHT - 160f + i * 4f, Align.center,
                    Palette.echoColor(i), a);
        }
        canvas.text(game.assets().fontTitle, "ECHO", cx, UiCanvas.HEIGHT - 160f, Align.center, Palette.PLAYER, 1f);
        canvas.text(game.assets().font, "cooperate with your past self", cx, UiCanvas.HEIGHT - 235f, Align.center,
                Palette.UI_DIM, 1f);

        float y = UiCanvas.HEIGHT / 2f + 20f;
        for (int i = 0; i < OPTIONS.length; i++) {
            boolean sel = i == selected;
            Color color = sel ? Palette.UI_TEXT : Palette.UI_DIM;
            String label = sel ? "> " + OPTIONS[i] + " <" : OPTIONS[i];
            canvas.text(game.assets().fontLarge, label, cx, y, Align.center, color, 1f);
            y -= 48f;
        }

        boolean isAndroid = Gdx.app.getType() == Application.ApplicationType.Android;
        String prompt = isAndroid ? "tap to select   tap again to confirm" : "UP/DOWN select   ENTER confirm";
        canvas.text(game.assets().font, prompt, cx, 70f, Align.center, Palette.UI_DIM,
                0.7f + 0.3f * (float) Math.sin(time * 2f));
        canvas.end();
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            selected = (selected + 1) % OPTIONS.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            selected = (selected + OPTIONS.length - 1) % OPTIONS.length;
        }

        boolean confirmed = Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE);

        if (Gdx.input.justTouched()) {
            float tx = Gdx.input.getX();
            float ty = Gdx.input.getY();
            canvas.viewport().unproject(temp.set(tx, ty));

            float menuY = UiCanvas.HEIGHT / 2f + 20f;
            for (int i = 0; i < OPTIONS.length; i++) {
                if (Math.abs(temp.x - UiCanvas.WIDTH / 2f) < 200f && Math.abs(temp.y - (menuY - 20f)) < 24f) {
                    if (selected == i) {
                        confirmed = true;
                    } else {
                        selected = i;
                    }
                    break;
                }
                menuY -= 48f;
            }
        }

        if (confirmed) {
            switch (selected) {
                case 0 -> game.startLevel(game.firstUnfinishedLevel());
                case 1 -> game.showLevelSelect();
                default -> Gdx.app.exit();
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }
    }

    private final Vector2 temp = new Vector2();

    @Override
    public void resize(int width, int height) {
        canvas.resize(width, height);
    }
}
