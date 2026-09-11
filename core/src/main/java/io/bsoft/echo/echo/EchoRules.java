package io.bsoft.echo.echo;

/** Per-level Echo constraints (spec §16). Levels override these from data. */
public final class EchoRules {

    public int maxEchoes = 3;
    public float maxRecordingDuration = 10f;
    /**
     * When true, starting a recording teleports the player to the level spawn so every
     * Echo is a replay from the level's initial player state (spec §18 recommended rule).
     * When false, recordings start wherever the player is and the Echo spawns there.
     */
    public boolean recordFromSpawn = true;
    /** Echoes restart their recording when it ends instead of disappearing (advanced). */
    public boolean loopingEchoes = false;
    /** Echoes physically collide with each other and with the player (advanced). */
    public boolean echoesCollide = false;
    /** Recorded CREATE_ECHO actions are replayed by Echoes (Echo inheritance, advanced). */
    public boolean echoInheritance = false;

    public EchoRules copy() {
        EchoRules r = new EchoRules();
        r.maxEchoes = maxEchoes;
        r.maxRecordingDuration = maxRecordingDuration;
        r.recordFromSpawn = recordFromSpawn;
        r.loopingEchoes = loopingEchoes;
        r.echoesCollide = echoesCollide;
        r.echoInheritance = echoInheritance;
        return r;
    }

    public int maxRecordingTicks(float step) {
        return Math.max(1, Math.round(maxRecordingDuration / step));
    }
}
