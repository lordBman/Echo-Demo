package io.bsoft.echo.input;

/**
 * Abstraction over "what the controlled character wants to do this tick".
 *
 * <p>The real player is driven by {@link KeyboardInputSource}; Echoes are driven by a
 * {@code RecordedInputSource}. Because {@code PlayerController} only ever talks to this
 * interface, both share exactly the same movement code, which is what makes Echo
 * playback faithful (spec §5, §14).</p>
 *
 * <p>Movement queries ({@link #left()}, {@link #right()}, {@link #jump()},
 * {@link #interact()}) report <em>held</em> state and are sampled once per fixed step.
 * Meta queries ({@link #record()}, {@link #createEcho()}, {@link #reset()}) report
 * <em>press</em> events and are consumed by the gameplay layer, never by the controller.</p>
 */
public interface InputSource {

    boolean left();

    boolean right();

    boolean jump();

    boolean interact();

    /** Toggle recording (press event). */
    boolean record();

    /** Spawn an Echo from the latest recording (press event). */
    boolean createEcho();

    /** Reset / rewind the attempt (press event). */
    boolean reset();

    /** Pause the game (press event). */
    default boolean pauseJustPressed() {
        return false;
    }

    /**
     * Spawn an Echo from the recording history (press event): 0 = most recent, 1 = the one
     * before, ... or -1 when nothing was pressed this tick.
     */
    default int spawnHistorySlot() {
        return -1;
    }

    /**
     * Called by the simulation after the press events of the current tick have been consumed,
     * so a single tap is never applied to more than one fixed step.
     */
    default void consumePresses() {
    }

    /** When disabled (e.g. paused) the source reports no input at all. */
    default void setEnabled(boolean enabled) {
    }

    /** Convenience: -1, 0 or +1 horizontal axis derived from left/right. */
    default int horizontal() {
        int axis = 0;
        if (left()) {
            axis--;
        }
        if (right()) {
            axis++;
        }
        return axis;
    }
}
