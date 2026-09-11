package io.bsoft.echo.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;
import io.bsoft.echo.rendering.Assets;
import io.bsoft.echo.rendering.Palette;

/** Pause menu drawn over the frozen game. Keyboard driven. */
public final class PauseOverlay {

    private final Assets assets;
    private final UiCanvas canvas;

    public PauseOverlay(Assets assets, UiCanvas canvas) {
        this.assets = assets;
        this.canvas = canvas;
    }

    public void render() {
        canvas.begin();
        canvas.rect(0f, 0f, UiCanvas.WIDTH, UiCanvas.HEIGHT, Color.BLACK, 0.6f);
        float cx = UiCanvas.WIDTH / 2f;
        float y = UiCanvas.HEIGHT / 2f + 120f;
        canvas.text(assets.fontTitle, "PAUSED", cx, y, Align.center, Palette.UI_TEXT, 1f);
        y -= 110f;
        canvas.text(assets.fontLarge, "[ESC]  Resume", cx, y, Align.center, Palette.UI_TEXT, 1f);
        y -= 40f;
        canvas.text(assets.fontLarge, "[R]  Restart level", cx, y, Align.center, Palette.UI_TEXT, 1f);
        y -= 40f;
        canvas.text(assets.fontLarge, "[L]  Level select", cx, y, Align.center, Palette.UI_TEXT, 1f);
        y -= 40f;
        canvas.text(assets.fontLarge, "[M]  Main menu", cx, y, Align.center, Palette.UI_TEXT, 1f);
        y -= 70f;
        canvas.text(assets.font, "A/D move   SPACE jump   E interact   Q record   F echo   1-5 history   R rewind   H hint", cx, y,
                Align.center, Palette.UI_DIM, 1f);
        canvas.end();
    }
}
