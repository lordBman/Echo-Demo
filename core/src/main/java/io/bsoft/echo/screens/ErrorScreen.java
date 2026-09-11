package io.bsoft.echo.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import io.bsoft.echo.EchoGame;
import io.bsoft.echo.rendering.Palette;
import io.bsoft.echo.ui.UiCanvas;

/** Shown when something essential failed to load (spec §52): a clear message, no crash. */
public final class ErrorScreen extends ScreenAdapter {

    private final EchoGame game;
    private final UiCanvas canvas;
    private final String title;
    private final String detail;

    public ErrorScreen(EchoGame game, String title, String detail) {
        this.game = game;
        this.canvas = new UiCanvas(game.assets());
        this.title = title;
        this.detail = detail == null ? "" : detail;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.15f, 0.03f, 0.05f, 1f);
        canvas.begin();
        canvas.text(game.assets().fontTitle, title, UiCanvas.WIDTH / 2f, UiCanvas.HEIGHT / 2f + 100f, Align.center,
                Palette.HAZARD, 1f);
        canvas.text(game.assets().font, detail, UiCanvas.WIDTH / 2f, UiCanvas.HEIGHT / 2f, Align.center,
                Palette.UI_TEXT, 1f);
        canvas.text(game.assets().font, "[ESC] quit    [M] main menu (if levels loaded)", UiCanvas.WIDTH / 2f,
                UiCanvas.HEIGHT / 2f - 80f, Align.center, Palette.UI_DIM, 1f);
        canvas.end();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.M) && game.levels() != null) {
            game.showMainMenu();
        }
    }

    @Override
    public void resize(int width, int height) {
        canvas.resize(width, height);
    }
}
