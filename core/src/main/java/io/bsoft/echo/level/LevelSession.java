package io.bsoft.echo.level;

/**
 * Per-attempt statistics for the mastery system (spec §44). All times are simulation time
 * accumulated from fixed steps.
 */
public final class LevelSession {

    private final String levelId;
    private float attemptTime;
    private int rewinds;
    private int deaths;
    private boolean completed;
    private LevelResult result;

    public LevelSession(String levelId) {
        this.levelId = levelId;
    }

    public void tick(float dt) {
        if (!completed) {
            attemptTime += dt;
        }
    }

    /** Called on a rewind: the attempt timer restarts, rewind count grows. */
    public void onRewind() {
        rewinds++;
        attemptTime = 0f;
        deaths = 0;
        completed = false;
        result = null;
    }

    public void onDeath() {
        deaths++;
    }

    public LevelResult complete(int echoesUsed, float recordingTime, int paradoxes) {
        completed = true;
        result = new LevelResult(levelId, true, attemptTime, echoesUsed, rewinds, recordingTime, deaths, paradoxes);
        return result;
    }

    public float attemptTime() {
        return attemptTime;
    }

    public int rewinds() {
        return rewinds;
    }

    public int deaths() {
        return deaths;
    }

    public boolean isCompleted() {
        return completed;
    }

    public LevelResult result() {
        return result;
    }

    /** Full reset (new level load), including the rewind count. */
    public void clear() {
        attemptTime = 0f;
        rewinds = 0;
        deaths = 0;
        completed = false;
        result = null;
    }
}
