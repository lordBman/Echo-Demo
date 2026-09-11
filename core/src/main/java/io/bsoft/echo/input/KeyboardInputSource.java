package io.bsoft.echo.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

/**
 * Reads the real player's keyboard.
 *
 * <p>Held actions are queried live from {@link Input#isKeyPressed(int)} so they are
 * correct at any fixed-step rate. Press events (record / createEcho / reset) are latched
 * during {@link #poll()} which runs once per render frame; this guarantees that a tap is
 * never lost when a frame executes zero fixed steps and never duplicated when a frame
 * executes several.</p>
 */
public final class KeyboardInputSource implements InputSource {

    private final KeyBindings bindings;

    private boolean recordLatched;
    private boolean createEchoLatched;
    private boolean resetLatched;
    private int historySlotLatched = -1;
    private boolean enabled = true;

    public KeyboardInputSource(KeyBindings bindings) {
        this.bindings = bindings;
    }

    /** Call once per render frame, before the fixed-step loop. */
    public void poll() {
        if (!enabled) {
            return;
        }
        recordLatched |= justPressed(bindings.record);
        createEchoLatched |= justPressed(bindings.createEcho);
        resetLatched |= justPressed(bindings.reset);
        for (int i = 0; i < bindings.historySlots.length; i++) {
            if (Gdx.input.isKeyJustPressed(bindings.historySlots[i])) {
                historySlotLatched = i;
            }
        }
    }

    @Override
    public void consumePresses() {
        clearPresses();
    }

    /** Clears latched press events. Call after they have been consumed. */
    public void clearPresses() {
        recordLatched = false;
        createEchoLatched = false;
        resetLatched = false;
        historySlotLatched = -1;
    }

    /** When disabled (e.g. paused) the source reports no input at all. */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            clearPresses();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean left() {
        return enabled && pressed(bindings.left);
    }

    @Override
    public boolean right() {
        return enabled && pressed(bindings.right);
    }

    @Override
    public boolean jump() {
        return enabled && pressed(bindings.jump);
    }

    @Override
    public boolean interact() {
        return enabled && pressed(bindings.interact);
    }

    @Override
    public boolean record() {
        return recordLatched;
    }

    @Override
    public boolean createEcho() {
        return createEchoLatched;
    }

    @Override
    public boolean reset() {
        return resetLatched;
    }

    @Override
    public int spawnHistorySlot() {
        return historySlotLatched;
    }

    public boolean pauseJustPressed() {
        return justPressed(bindings.pause);
    }

    public boolean debugOverlayJustPressed() {
        return justPressed(bindings.debugOverlay);
    }

    public boolean debugPhysicsJustPressed() {
        return justPressed(bindings.debugPhysics);
    }

    private static boolean pressed(int[] keys) {
        for (int key : keys) {
            if (key != Input.Keys.UNKNOWN && Gdx.input.isKeyPressed(key)) {
                return true;
            }
        }
        return false;
    }

    private static boolean justPressed(int[] keys) {
        for (int key : keys) {
            if (key != Input.Keys.UNKNOWN && Gdx.input.isKeyJustPressed(key)) {
                return true;
            }
        }
        return false;
    }
}
