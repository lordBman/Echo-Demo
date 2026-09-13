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
import io.bsoft.echo.level.LevelCatalog;
import io.bsoft.echo.rendering.Palette;
import io.bsoft.echo.save.LevelProgress;
import io.bsoft.echo.ui.UiCanvas;

/** Lists all levels with saved bests. Locked levels are shown dimmed. */
public final class LevelSelectScreen extends ScreenAdapter {

    private final EchoGame game;
    private final UiCanvas canvas;
    private int selected;

    public LevelSelectScreen(EchoGame game) {
        this.game = game;
        this.canvas = new UiCanvas(game.assets());
        this.selected = Math.min(game.firstUnfinishedLevel(), game.levels().size() - 1);
    }

    @Override
    public void render(float delta) {
        handleInput();
        ScreenUtils.clear(Palette.BACKGROUND_BOTTOM);
        canvas.begin();
        canvas.text(game.assets().fontLarge, "LEVEL SELECT", UiCanvas.WIDTH / 2f, UiCanvas.HEIGHT - 50f, Align.center,
                Palette.UI_TEXT, 1f);
        LevelCatalog levels = game.levels();
        float y = UiCanvas.HEIGHT - 120f;
        float left = 160f;
        for (int i = 0; i < levels.size(); i++) {
            LevelCatalog.Entry e = levels.entry(i);
            boolean unlocked = game.isUnlocked(i);
            boolean sel = i == selected;
            LevelProgress progress = game.save().load(e.id());
            Color color = !unlocked ? new Color(Palette.UI_DIM).mul(0.6f) : sel ? Palette.UI_TEXT : Palette.UI_DIM;
            if (sel) {
                canvas.rect(left - 20f, y - 30f, UiCanvas.WIDTH - 2f * left + 40f, 40f, Color.WHITE, 0.06f);
            }
            String label = String.format("%d-%d  %s%s", e.chapter(), i + 1, e.name(), unlocked ? "" : "  (locked)");
            canvas.text(game.assets().fontLarge, label, left, y, Align.left, color, 1f);
            if (progress.completed()) {
                String best = String.format("best %.1fs   echoes %d   rewinds %d   challenges %d",
                        progress.bestTime(), progress.bestEchoes(), progress.bestRewinds(),
                        progress.completedChallenges().size);
                canvas.text(game.assets().font, best, UiCanvas.WIDTH - left, y - 4f, Align.right, Palette.PLATE_ACTIVE,
                        0.9f);
            } else if (unlocked) {
                canvas.text(game.assets().font, "not completed", UiCanvas.WIDTH - left, y - 4f, Align.right,
                        Palette.UI_DIM, 0.9f);
            }
            y -= 46f;
        }

        boolean isAndroid = Gdx.app.getType() == Application.ApplicationType.Android;
        String prompt = isAndroid ? "tap to select   tap again to play   ESC back" : "UP/DOWN select   ENTER play   ESC back   [U] unlock all (dev)";
        canvas.text(game.assets().font, prompt,
                UiCanvas.WIDTH / 2f, 50f, Align.center, Palette.UI_DIM, 1f);
        canvas.end();
    }

    private void handleInput() {
        int n = game.levels().size();
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            selected = (selected + 1) % n;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            selected = (selected + n - 1) % n;
        }

        boolean confirmed = Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE);

        if (Gdx.input.justTouched()) {
            float tx = Gdx.input.getX();
            float ty = Gdx.input.getY();
            canvas.viewport().unproject(temp.set(tx, ty));

            float entryY = UiCanvas.HEIGHT - 120f;
            float left = 160f;
            for (int i = 0; i < n; i++) {
                if (temp.x > left - 20f && temp.x < UiCanvas.WIDTH - left + 20f && Math.abs(temp.y - (entryY - 10f)) < 20f) {
                    if (selected == i) {
                        confirmed = true;
                    } else {
                        selected = i;
                    }
                    break;
                }
                entryY -= 46f;
            }
        }

        if (confirmed) {
            if (game.isUnlocked(selected) || devUnlockAll) {
                game.startLevel(selected);
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.U)) {
            devUnlockAll = !devUnlockAll;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.showMainMenu();
        }
    }

    private final Vector2 temp = new Vector2();
    private boolean devUnlockAll;

    @Override
    public void resize(int width, int height) {
        canvas.resize(width, height);
    }
}
