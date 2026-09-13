package io.bsoft.echo.ui;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;
import io.bsoft.echo.echo.Echo;
import io.bsoft.echo.echo.EchoController;
import io.bsoft.echo.echo.EchoManager;
import io.bsoft.echo.echo.EchoRecording;
import io.bsoft.echo.rendering.Assets;
import io.bsoft.echo.rendering.Palette;
import io.bsoft.echo.world.GameEventListener;
import io.bsoft.echo.world.GameWorld;

/**
 * Minimal in-game HUD (spec §33): echo slots, recording bar, timer, level hint, toasts.
 */
public final class Hud implements GameEventListener {

    private static final float MARGIN = 28f;
    /** The hint stays at least this long, and until the first recording starts. */
    private static final float HINT_DURATION = 9f;
    private static final float TOAST_DURATION = 2.2f;

    private final Assets assets;
    private final UiCanvas canvas;
    private GameWorld world;
    private float hintTimer;
    private boolean hintPinned;
    private String toast = "";
    private float toastTimer;
    private final Color toastColor = new Color(Color.WHITE);
    private final StringBuilder sb = new StringBuilder(32);

    public Hud(Assets assets, UiCanvas canvas) {
        this.assets = assets;
        this.canvas = canvas;
    }

    public void attach(GameWorld world) {
        if (this.world != null) {
            this.world.events().remove(this);
        }
        this.world = world;
        world.events().add(this);
        hintTimer = HINT_DURATION;
        hintPinned = true;
        toastTimer = 0f;
    }

    /** Re-shows the level hint (H key). */
    public void showHint() {
        hintTimer = HINT_DURATION;
    }

    public void update(float delta) {
        if (!hintPinned) {
            hintTimer = Math.max(0f, hintTimer - delta);
        }
        toastTimer = Math.max(0f, toastTimer - delta);
    }

    public void showToast(String message, Color color) {
        toast = message;
        toastColor.set(color);
        toastTimer = TOAST_DURATION;
    }

    public void render() {
        canvas.begin();
        drawEchoSlots();
        drawRecording();
        drawTimer();
        drawHint();
        drawToast();
        canvas.end();
    }

    private void drawEchoSlots() {
        EchoManager echoes = world.echoManager();
        float y = UiCanvas.HEIGHT - MARGIN;
        canvas.text(assets.font, "ECHOES", MARGIN, y, Align.left, Palette.UI_DIM, 1f);
        float cy = y - 34f;
        int max = echoes.maxEchoes();
        for (int i = 0; i < max; i++) {
            float cx = MARGIN + 10f + i * 26f;
            if (i < echoes.count()) {
                Echo echo = echoes.echoes().get(i);
                canvas.circle(cx, cy, 9f, Palette.echoColor(echo.id()), 1f);
                // Remaining playback arc drawn as a shrinking inner dot.
                float remaining = 1f - echo.progress();
                canvas.circle(cx, cy, 6f * remaining, Color.WHITE, 0.7f);
            } else {
                canvas.circle(cx, cy, 9f, Palette.UI_DIM, 0.5f);
                canvas.circle(cx, cy, 6.5f, Palette.BACKGROUND_TOP, 1f);
            }
        }
        if (max == 0) {
            canvas.text(assets.font, "none", MARGIN, cy + 8f, Align.left, Palette.UI_DIM, 1f);
        }
    }

    private void drawRecording() {
        boolean isAndroid = Gdx.app.getType() == Application.ApplicationType.Android;
        EchoController controller = world.echoController();
        float y = UiCanvas.HEIGHT - MARGIN - 78f;
        float barW = 180f;
        if (controller.isRecording()) {
            float blink = 0.6f + 0.4f * (float) Math.sin(System.nanoTime() / 1.5e8);
            canvas.circle(MARGIN + 7f, y - 8f, 7f, Palette.RECORDING, blink);
            canvas.text(assets.font, "RECORDING", MARGIN + 22f, y, Align.left, Palette.RECORDING, 1f);
            float progress = controller.recorder().progress();
            canvas.rect(MARGIN, y - 30f, barW, 8f, Palette.UI_DIM, 0.35f);
            canvas.rect(MARGIN, y - 30f, barW * progress, 8f, Palette.RECORDING, 1f);
            sb.setLength(0);
            sb.append(String.format("%.1f / %.0fs", controller.recorder().elapsed(),
                    world.echoRules().maxRecordingDuration));
            canvas.text(assets.font, sb, MARGIN + barW + 10f, y - 20f, Align.left, Palette.UI_TEXT, 1f);
        } else if (controller.hasRecording()) {
            EchoRecording rec = controller.lastRecording();
            sb.setLength(0);
            String spawnPrompt = isAndroid ? "spawn echo" : "[F] spawn echo";
            sb.append(String.format("RECORDING READY  %.1fs   %s", rec.duration(), spawnPrompt));
            canvas.text(assets.font, sb, MARGIN, y, Align.left, Palette.UI_TEXT, 0.9f);
            String againPrompt = isAndroid ? "record again" : "[Q] record again";
            canvas.text(assets.font, againPrompt, MARGIN, y - 22f, Align.left, Palette.UI_DIM, 1f);
            if (controller.history().size > 1) {
                sb.setLength(0);
                sb.append("HISTORY ");
                for (int i = 0; i < controller.history().size; i++) {
                    String slot = isAndroid ? String.valueOf(i + 1) : "[" + (i + 1) + "]";
                    sb.append(" ").append(slot).append(" ")
                            .append(String.format("%.1fs", controller.history().get(i).duration()));
                }
                canvas.text(assets.font, sb, MARGIN, y - 44f, Align.left, Palette.UI_DIM, 1f);
            }
        } else {
            String startPrompt = isAndroid ? "start recording" : "[Q] start recording";
            canvas.text(assets.font, startPrompt, MARGIN, y, Align.left, Palette.UI_DIM, 1f);
        }
    }

    private void drawTimer() {
        float y = UiCanvas.HEIGHT - MARGIN;
        float x = UiCanvas.WIDTH - MARGIN;
        canvas.text(assets.font, world.levelData().name.toUpperCase(), x, y, Align.right, Palette.UI_DIM, 1f);
        sb.setLength(0);
        sb.append(String.format("%05.1f", world.session().attemptTime()));
        canvas.text(assets.fontLarge, sb, x, y - 24f, Align.right, Palette.UI_TEXT, 1f);
        sb.setLength(0);
        sb.append("REWINDS ").append(world.session().rewinds());
        if (world.session().deaths() > 0) {
            sb.append("   DEATHS ").append(world.session().deaths());
        }
        canvas.text(assets.font, sb, x, y - 62f, Align.right, Palette.UI_DIM, 1f);
    }

    private void drawHint() {
        String hint = world.levelData().hint;
        if (hint == null || hint.isEmpty() || hintTimer <= 0f) {
            return;
        }
        float alpha = hintPinned ? 1f : Math.min(1f, hintTimer / 1.5f);
        float maxWidth = Math.min(720f, Math.max(360f, canvas.textWidth(assets.font, hint)));
        float textHeight = canvas.wrappedHeight(assets.font, hint, maxWidth);
        float pad = 14f;
        float boxH = textHeight + pad * 2f + 22f;
        float top = 200f + boxH; // sits above the floor of every level
        canvas.rect(UiCanvas.WIDTH / 2f - maxWidth / 2f - pad, top - boxH, maxWidth + pad * 2f, boxH, Color.BLACK,
                0.55f * alpha);
        canvas.textWrapped(assets.font, hint, UiCanvas.WIDTH / 2f, top - pad, maxWidth, Palette.UI_TEXT, alpha);

        boolean isAndroid = Gdx.app.getType() == Application.ApplicationType.Android;
        String hintPrompt = isAndroid ? "hint" : "[H] hint";
        canvas.text(assets.font, hintPrompt, UiCanvas.WIDTH / 2f, top - pad - textHeight - 6f, Align.center,
                Palette.UI_DIM, alpha * 0.8f);
    }

    private void drawToast() {
        if (toastTimer <= 0f) {
            return;
        }
        float alpha = Math.min(1f, toastTimer / 0.6f);
        canvas.text(assets.fontLarge, toast, UiCanvas.WIDTH / 2f, UiCanvas.HEIGHT / 2f + 120f, Align.center,
                toastColor, alpha);
    }

    // ---------------------------------------------------------------- events

    @Override
    public void onEchoRejected(EchoRecording recording, String reason) {
        showToast(reason.toUpperCase(), Palette.RECORDING);
    }

    @Override
    public void onRecordingStarted() {
        hintPinned = false;
        hintTimer = Math.min(hintTimer, 2.5f);
    }

    @Override
    public void onRecordingStopped(EchoRecording recording, boolean autoStopped) {
        if (autoStopped) {
            showToast("RECORDING LIMIT REACHED", Palette.RECORDING);
        }
    }

    @Override
    public void onEchoParadox(Echo echo) {
        showToast("PARADOX  -  the past no longer replays the same", Palette.HAZARD);
    }

    @Override
    public void onPlayerDied(io.bsoft.echo.player.Player player) {
        showToast("LOST IN TIME", Palette.HAZARD);
    }

    @Override
    public void onLevelReset(boolean countedAsRewind) {
        if (countedAsRewind) {
            showToast("REWIND", Palette.UI_TEXT);
        }
    }
}
