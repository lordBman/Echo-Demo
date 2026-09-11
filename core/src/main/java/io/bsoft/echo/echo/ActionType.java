package io.bsoft.echo.echo;

/**
 * Gameplay intents that can be recorded (spec §12). Recording stores intent transitions
 * (edges), not per-tick state, so a 10 second recording of standing still is one action.
 */
public enum ActionType {
    MOVE_LEFT,
    MOVE_RIGHT,
    MOVE_STOP,
    JUMP_PRESSED,
    JUMP_RELEASED,
    INTERACT_PRESSED,
    INTERACT_RELEASED,
    /** Spawns another recording (Echo inheritance). {@code value} = recording id. */
    CREATE_ECHO
}
