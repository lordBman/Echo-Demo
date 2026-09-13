package io.bsoft.echo.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import io.bsoft.echo.input.TouchInputSource;
import io.bsoft.echo.rendering.Assets;
import io.bsoft.echo.rendering.Palette;

/**
 * Android touch controls overlay.
 * Renders a virtual joystick on the left and action buttons on the right.
 */
public final class TouchOverlay {

    private static final float JOYSTICK_X = 180f;
    private static final float JOYSTICK_Y = 160f;
    private static final float JOYSTICK_RADIUS = 100f;
    private static final float KNOB_RADIUS = 40f;

    private static final float BUTTON_RADIUS = 55f;
    private static final float JUMP_X = UiCanvas.WIDTH - 120f;
    private static final float JUMP_Y = 140f;

    private static final float RECORD_X = UiCanvas.WIDTH - 260f;
    private static final float RECORD_Y = 100f;

    private static final float SPAWN_X = UiCanvas.WIDTH - 100f;
    private static final float SPAWN_Y = 280f;

    private static final float RESET_X = UiCanvas.WIDTH / 2f + 75f;
    private static final float RESET_Y = UiCanvas.HEIGHT - 55f;

    private static final float PAUSE_X = UiCanvas.WIDTH / 2f - 75f;
    private static final float PAUSE_Y = UiCanvas.HEIGHT - 55f;

    private final Assets assets;
    private final UiCanvas canvas;
    private final TouchInputSource input;

    private final Vector2 touchPos = new Vector2();
    private int joystickPointer = -1;
    private final Vector2 joystickKnob = new Vector2();
    private float horizontalAxis;

    public TouchOverlay(Assets assets, UiCanvas canvas, TouchInputSource input) {
        this.assets = assets;
        this.canvas = canvas;
        this.input = input;
    }

    public void update() {
        boolean jump = false;
        boolean interact = false; // Not explicitly requested but useful, maybe combine with jump or separate
        boolean record = false;
        boolean spawn = false;
        boolean reset = false;
        boolean pause = false;

        horizontalAxis = 0;
        boolean joystickTouched = false;

        for (int i = 0; i < 10; i++) {
            if (Gdx.input.isTouched(i)) {
                touchPos.set(Gdx.input.getX(i), Gdx.input.getY(i));
                canvas.viewport().unproject(touchPos);

                // Joystick logic
                float dist = touchPos.dst(JOYSTICK_X, JOYSTICK_Y);
                if (dist < JOYSTICK_RADIUS * 1.5f || i == joystickPointer) {
                    if (joystickPointer == -1 || i == joystickPointer) {
                        joystickPointer = i;
                        joystickTouched = true;
                        float dx = touchPos.x - JOYSTICK_X;
                        horizontalAxis = Math.min(1f, Math.max(-1f, dx / JOYSTICK_RADIUS));
                        joystickKnob.set(JOYSTICK_X + horizontalAxis * JOYSTICK_RADIUS, JOYSTICK_Y);
                    }
                }

                // Buttons
                if (touchPos.dst(JUMP_X, JUMP_Y) < BUTTON_RADIUS * 1.2f) jump = true;
                if (touchPos.dst(RECORD_X, RECORD_Y) < BUTTON_RADIUS * 1.2f) record = Gdx.input.justTouched();
                if (touchPos.dst(SPAWN_X, SPAWN_Y) < BUTTON_RADIUS * 1.2f) spawn = Gdx.input.justTouched();
                if (touchPos.dst(RESET_X, RESET_Y) < BUTTON_RADIUS * 1.2f) reset = Gdx.input.justTouched();
                if (touchPos.dst(PAUSE_X, PAUSE_Y) < BUTTON_RADIUS * 1.2f) pause = Gdx.input.justTouched();
            } else {
                if (i == joystickPointer) {
                    joystickPointer = -1;
                }
            }
        }

        if (!joystickTouched) {
            joystickKnob.set(JOYSTICK_X, JOYSTICK_Y);
        }

        input.update(horizontalAxis, jump, interact, record, spawn, reset);
        if (pause) input.setPausePressed();
    }

    public void render() {
        canvas.begin();

        // Joystick
        canvas.circle(JOYSTICK_X, JOYSTICK_Y, JOYSTICK_RADIUS, Palette.UI_DIM, 0.2f);
        canvas.circle(joystickKnob.x, joystickKnob.y, KNOB_RADIUS, Color.WHITE, 0.4f);

        // Action Buttons
        drawButton(JUMP_X, JUMP_Y, "JUMP", Palette.PLAYER);
        drawButton(RECORD_X, RECORD_Y, "RECORD", Palette.RECORDING);
        drawButton(SPAWN_X, SPAWN_Y, "ECHO", Palette.UI_TEXT);

        // Reset Button
        drawButton(RESET_X, RESET_Y, "RESET", Palette.HAZARD);

        // Pause Button
        drawButton(PAUSE_X, PAUSE_Y, "PAUSE", Palette.UI_DIM);

        canvas.end();
    }

    private void drawButton(float x, float y, String label, Color color) {
        canvas.circle(x, y, BUTTON_RADIUS, color, 0.3f);
        canvas.text(assets.font, label, x, y + 8f, Align.center, Color.WHITE, 0.8f);
    }
}
