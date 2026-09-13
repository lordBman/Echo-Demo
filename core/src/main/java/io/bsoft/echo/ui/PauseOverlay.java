package io.bsoft.echo.ui;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;
import io.bsoft.echo.rendering.Assets;
import io.bsoft.echo.rendering.Palette;

/** Pause menu drawn over the frozen game. Keyboard driven (Desktop) or Touch driven (Android). */
public final class PauseOverlay {

    private final Assets assets;
    private final UiCanvas canvas;

    public PauseOverlay(Assets assets, UiCanvas canvas) {
        this.assets = assets;
        this.canvas = canvas;
    }

    public void render() {
        boolean isAndroid = Gdx.app.getType() == Application.ApplicationType.Android;
        canvas.begin();
        canvas.rect(0f, 0f, UiCanvas.WIDTH, UiCanvas.HEIGHT, Color.BLACK, 0.6f);
        float cx = UiCanvas.WIDTH / 2f;
        float y = UiCanvas.HEIGHT / 2f + 120f;
        canvas.text(assets.fontTitle, "PAUSED", cx, y, Align.center, Palette.UI_TEXT, 1f);
        y -= 110f;

        String resume = isAndroid ? "Resume" : "[ESC]  Resume";
        String restart = isAndroid ? "Restart level" : "[R]  Restart level";
        String select = isAndroid ? "Level select" : "[L]  Level select";
        String menu = isAndroid ? "Main menu" : "[M]  Main menu";

        canvas.text(assets.fontLarge, resume, cx, y, Align.center, Palette.UI_TEXT, 1f);
        y -= 40f;
        canvas.text(assets.fontLarge, restart, cx, y, Align.center, Palette.UI_TEXT, 1f);
        y -= 40f;
        canvas.text(assets.fontLarge, select, cx, y, Align.center, Palette.UI_TEXT, 1f);
        y -= 40f;
        canvas.text(assets.fontLarge, menu, cx, y, Align.center, Palette.UI_TEXT, 1f);
        y -= 70f;

        if (!isAndroid) {
            canvas.text(assets.font, "A/D move   SPACE jump   E interact   Q record   F echo   1-5 history   R rewind   H hint", cx, y,
                    Align.center, Palette.UI_DIM, 1f);
        }
        canvas.end();
    }
}
