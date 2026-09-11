package io.bsoft.echo;

/**
 * Global, immutable configuration constants for the game.
 *
 * <p>The game world is measured in meters (1 tile = 1 meter). The reference
 * resolution is 1280x720 and the camera shows {@link #VIEW_WIDTH} x {@link #VIEW_HEIGHT}
 * meters at zoom 1, which gives exactly {@link #PIXELS_PER_METER} pixels per meter.</p>
 */
public final class GameConfig {

    public static final String TITLE = "ECHO";

    /** Reference resolution used for the UI and default window size. */
    public static final int REFERENCE_WIDTH = 1280;
    public static final int REFERENCE_HEIGHT = 720;

    /** Logical world view in meters at zoom 1. */
    public static final float VIEW_WIDTH = 40f;
    public static final float VIEW_HEIGHT = 22.5f;
    public static final float PIXELS_PER_METER = REFERENCE_WIDTH / VIEW_WIDTH;

    /** Fixed simulation step. All gameplay and physics advance in these increments. */
    public static final float FIXED_STEP = 1f / 60f;
    /** Largest render delta we are willing to simulate in a single frame (avoids spiral of death). */
    public static final float MAX_FRAME_DELTA = 0.25f;
    /** Maximum number of fixed steps executed per render frame. */
    public static final int MAX_STEPS_PER_FRAME = 8;

    private GameConfig() {
    }
}
