package io.bsoft.echo.input;

import com.badlogic.gdx.Input;

/**
 * Configurable key bindings. Each action may have up to two physical keys.
 * Defaults follow spec §5.
 */
public final class KeyBindings {

    public int[] left = {Input.Keys.A, Input.Keys.LEFT};
    public int[] right = {Input.Keys.D, Input.Keys.RIGHT};
    public int[] jump = {Input.Keys.SPACE, Input.Keys.W};
    public int[] interact = {Input.Keys.E, Input.Keys.UNKNOWN};
    public int[] record = {Input.Keys.Q, Input.Keys.UNKNOWN};
    public int[] createEcho = {Input.Keys.F, Input.Keys.UNKNOWN};
    public int[] reset = {Input.Keys.R, Input.Keys.UNKNOWN};
    public int[] pause = {Input.Keys.ESCAPE, Input.Keys.UNKNOWN};
    /** Number keys spawn recording history slot 0..4 (1 = most recent). */
    public int[] historySlots = {Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3, Input.Keys.NUM_4,
        Input.Keys.NUM_5};
    public int[] debugOverlay = {Input.Keys.F1, Input.Keys.UNKNOWN};
    public int[] debugPhysics = {Input.Keys.F2, Input.Keys.UNKNOWN};

    public static KeyBindings defaults() {
        return new KeyBindings();
    }
}
