package io.bsoft.echo.level;

/**
 * Anything that can snapshot its initial state and later return to it (spec §21, §50).
 * The {@link ResetManager} calls {@link #captureInitialState()} once after the level is
 * built and {@link #reset()} on every rewind.
 */
public interface Resettable {

    default void captureInitialState() {
    }

    void reset();
}
