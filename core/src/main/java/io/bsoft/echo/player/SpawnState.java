package io.bsoft.echo.player;

/**
 * The fully-defined initial state of an avatar (spec §18). Position is the center of the
 * body. Velocity is included so a recording that starts mid-air can be replayed faithfully
 * when {@code EchoRules.recordFromSpawn} is disabled.
 */
public record SpawnState(float x, float y, float vx, float vy, boolean facingRight) {

    public static SpawnState atRest(float x, float y) {
        return new SpawnState(x, y, 0f, 0f, true);
    }

    /** Feet-on-ground convenience: converts a feet position to a body-center position. */
    public static SpawnState standing(float feetX, float feetY, float bodyHeight) {
        return new SpawnState(feetX, feetY + bodyHeight / 2f, 0f, 0f, true);
    }
}
