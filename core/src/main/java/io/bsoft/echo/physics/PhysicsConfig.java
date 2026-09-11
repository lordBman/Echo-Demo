package io.bsoft.echo.physics;

/** Tunable physics world settings. */
public final class PhysicsConfig {

    public float gravityY = -30f;
    public int velocityIterations = 8;
    public int positionIterations = 3;
    public boolean allowSleep = true;
}
