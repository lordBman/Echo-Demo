package io.bsoft.echo.player;

/**
 * Movement tuning (spec §7). All numbers that define "feel" live here, never inline.
 * Units: meters, seconds.
 */
public final class PlayerConfig {

    public float width = 0.8f;
    public float height = 1.6f;

    public float moveSpeed = 9f;
    /** Horizontal acceleration on the ground, m/s². */
    public float acceleration = 90f;
    /** Horizontal deceleration on the ground when no input, m/s². */
    public float deceleration = 110f;
    /** Multiplier applied to acceleration/deceleration while airborne. */
    public float airControl = 0.65f;

    public float jumpVelocity = 13.5f;
    /** Velocity multiplier applied once when jump is released early (variable height). */
    public float jumpCutMultiplier = 0.45f;
    public float coyoteTime = 0.08f;
    public float jumpBufferTime = 0.1f;
    public float maxFallSpeed = 30f;

    /** Mass in kg, used to derive density. Affects how hard boxes are pushed. */
    public float mass = 1.2f;

    public static PlayerConfig defaults() {
        return new PlayerConfig();
    }

    public PlayerConfig copy() {
        PlayerConfig c = new PlayerConfig();
        c.width = width;
        c.height = height;
        c.moveSpeed = moveSpeed;
        c.acceleration = acceleration;
        c.deceleration = deceleration;
        c.airControl = airControl;
        c.jumpVelocity = jumpVelocity;
        c.jumpCutMultiplier = jumpCutMultiplier;
        c.coyoteTime = coyoteTime;
        c.jumpBufferTime = jumpBufferTime;
        c.maxFallSpeed = maxFallSpeed;
        c.mass = mass;
        return c;
    }
}
