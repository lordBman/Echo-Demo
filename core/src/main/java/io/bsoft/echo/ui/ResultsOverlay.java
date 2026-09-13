package io.bsoft.echo.ui;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import io.bsoft.echo.level.Challenge;
import io.bsoft.echo.level.LevelResult;
import io.bsoft.echo.rendering.Assets;
import io.bsoft.echo.rendering.Palette;
import io.bsoft.echo.save.LevelProgress;

/** Level complete summary (spec §45) with challenge results and best comparisons. */
public final class ResultsOverlay {

    private final Assets assets;
    private final UiCanvas canvas;
    private LevelResult result;
    private Array<Challenge> challenges;
    private LevelProgress progress;
    private boolean hasNext;
    private float appear;

    public ResultsOverlay(Assets assets, UiCanvas canvas) {
        this.assets = assets;
        this.canvas = canvas;
    }

    public void show(LevelResult result, Array<Challenge> challenges, LevelProgress progress, boolean hasNext) {
        this.result = result;
        this.challenges = challenges;
        this.progress = progress;
        this.hasNext = hasNext;
        this.appear = 0f;
    }

    public void update(float delta) {
        appear = Math.min(1f, appear + delta * 2.5f);
    }

    public void render() {
        if (result == null) {
            return;
        }
        canvas.begin();
        canvas.rect(0f, 0f, UiCanvas.WIDTH, UiCanvas.HEIGHT, Color.BLACK, 0.65f * appear);
        float cx = UiCanvas.WIDTH / 2f;
        float y = UiCanvas.HEIGHT - 110f - (1f - appear) * 30f;
        canvas.text(assets.fontTitle, "LEVEL COMPLETE", cx, y, Align.center, Palette.EXIT, appear);
        y -= 90f;

        float left = cx - 200f;
        float right = cx + 200f;
        y = statRow(y, left, right, "Time", String.format("%.1fs", result.completionTime()),
                progress != null && progress.bestTime() > 0f && result.completionTime() <= progress.bestTime());
        y = statRow(y, left, right, "Echoes", Integer.toString(result.echoesUsed()),
                progress != null && progress.bestEchoes() >= 0 && result.echoesUsed() <= progress.bestEchoes());
        y = statRow(y, left, right, "Rewinds", Integer.toString(result.rewindCount()), false);
        y = statRow(y, left, right, "Deaths", Integer.toString(result.deaths()), false);
        y = statRow(y, left, right, "Recording", String.format("%.1fs", result.recordingTime()), false);
        if (result.paradoxes() > 0) {
            y = statRow(y, left, right, "Paradoxes", Integer.toString(result.paradoxes()), false);
        }
        y -= 30f;

        if (challenges != null) {
            for (Challenge c : challenges) {
                boolean done = c.isSatisfied(result);
                String mark = done ? "[x]" : "[ ]";
                Color color = done ? Palette.PLATE_ACTIVE : Palette.UI_DIM;
                canvas.text(assets.fontLarge, mark + "  " + c.description(), left, y, Align.left, color, appear);
                y -= 36f;
            }
        }
        y -= 30f;
        boolean isAndroid = Gdx.app.getType() == Application.ApplicationType.Android;
        String prompt;
        if (isAndroid) {
            prompt = hasNext ? "Next level     Retry     Level select" : "Retry     Level select";
        } else {
            prompt = hasNext ? "[ENTER] Next level     [R] Retry     [L] Level select"
                    : "[R] Retry     [L] Level select";
        }
        canvas.text(assets.font, prompt, cx, Math.max(y, 60f), Align.center, Palette.UI_TEXT, appear);
        canvas.end();
    }

    private float statRow(float y, float left, float right, String label, String value, boolean best) {
        canvas.text(assets.fontLarge, label, left, y, Align.left, Palette.UI_DIM, appear);
        canvas.text(assets.fontLarge, value, right, y, Align.right, Palette.UI_TEXT, appear);
        if (best) {
            canvas.text(assets.font, "BEST", right + 16f, y - 4f, Align.left, Palette.PLATE_ACTIVE, appear);
        }
        return y - 40f;
    }
}
