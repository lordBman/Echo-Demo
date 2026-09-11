package io.bsoft.echo.physics;

/**
 * Central definition of Box2D collision categories (spec §10).
 * Combine with bitwise OR to build mask bits. Never use raw bit values elsewhere.
 */
public final class CollisionBits {

    public static final short PLAYER = 1 << 0;
    public static final short ECHO = 1 << 1;
    public static final short WORLD = 1 << 2;
    public static final short INTERACTIVE = 1 << 3;
    public static final short HAZARD = 1 << 4;
    public static final short ENEMY = 1 << 5;
    public static final short SENSOR = 1 << 6;

    public static final short ALL = (short) 0xFFFF;

    /** Everything a character (player or echo) stands on or bumps into by default. */
    public static final short CHARACTER_SOLIDS = WORLD | INTERACTIVE | ENEMY;

    /** Bits that identify a controllable character avatar. */
    public static final short CHARACTERS = PLAYER | ECHO;

    private CollisionBits() {
    }

    public static boolean has(short bits, short flag) {
        return (bits & flag) != 0;
    }
}
