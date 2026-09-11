package io.bsoft.echo.rendering;

import com.badlogic.gdx.graphics.Color;

/** Shared colors so the player and Echoes read instantly as "me" vs "past me" (spec §31). */
public final class Palette {

    public static final Color BACKGROUND_TOP = new Color(0.09f, 0.10f, 0.16f, 1f);
    public static final Color BACKGROUND_BOTTOM = new Color(0.04f, 0.05f, 0.09f, 1f);
    public static final Color PLATFORM = new Color(0.22f, 0.25f, 0.34f, 1f);
    public static final Color PLATFORM_EDGE = new Color(0.40f, 0.46f, 0.60f, 1f);
    public static final Color PLAYER = new Color(1.00f, 0.86f, 0.35f, 1f);
    public static final Color PLAYER_DARK = new Color(0.55f, 0.40f, 0.10f, 1f);
    public static final Color PLATE = new Color(0.55f, 0.60f, 0.75f, 1f);
    public static final Color PLATE_ACTIVE = new Color(0.45f, 1.00f, 0.65f, 1f);
    public static final Color DOOR = new Color(0.85f, 0.30f, 0.35f, 1f);
    public static final Color DOOR_OPEN = new Color(0.35f, 0.85f, 0.55f, 1f);
    public static final Color EXIT = new Color(0.45f, 0.95f, 1.00f, 1f);
    public static final Color BOX = new Color(0.62f, 0.45f, 0.28f, 1f);
    public static final Color HAZARD = new Color(1.00f, 0.25f, 0.30f, 1f);
    public static final Color HAZARD_OFF = new Color(0.45f, 0.15f, 0.18f, 1f);
    public static final Color SWITCH = new Color(0.80f, 0.80f, 0.90f, 1f);
    public static final Color ENEMY = new Color(0.80f, 0.35f, 0.75f, 1f);
    public static final Color RECORDING = new Color(1.00f, 0.30f, 0.30f, 1f);
    public static final Color UI_TEXT = new Color(0.90f, 0.92f, 0.98f, 1f);
    public static final Color UI_DIM = new Color(0.55f, 0.58f, 0.68f, 1f);

    /** Echo colors cycle by echo id. All are cool tones so they never compete with the player. */
    private static final Color[] ECHO_COLORS = {
        new Color(0.40f, 0.75f, 1.00f, 1f),
        new Color(0.75f, 0.50f, 1.00f, 1f),
        new Color(0.40f, 1.00f, 0.85f, 1f),
        new Color(1.00f, 0.55f, 0.85f, 1f),
        new Color(0.60f, 1.00f, 0.45f, 1f),
    };

    private Palette() {
    }

    public static Color echoColor(int echoId) {
        return ECHO_COLORS[Math.abs(echoId - 1) % ECHO_COLORS.length];
    }
}
