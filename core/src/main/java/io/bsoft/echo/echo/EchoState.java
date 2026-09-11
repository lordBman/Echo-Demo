package io.bsoft.echo.echo;

/** Lifecycle of an Echo (spec §17). */
public enum EchoState {
    SPAWNING,
    REPLAYING,
    COMPLETED,
    DEAD,
    LOOPING
}
