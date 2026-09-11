package io.bsoft.echo.level;

/**
 * Outcome of a level attempt (spec §44). Shaped so it can later feed leaderboards
 * (spec §47): every field is a plain number.
 */
public record LevelResult(String levelId, boolean completed, float completionTime, int echoesUsed,
                          int rewindCount, float recordingTime, int deaths, int paradoxes) {

    /** Returns whichever result is "better" per category, used to maintain best scores. */
    public static float betterTime(float current, float candidate) {
        return current <= 0f ? candidate : Math.min(current, candidate);
    }

    public static int betterCount(int current, int candidate) {
        return current < 0 ? candidate : Math.min(current, candidate);
    }
}
