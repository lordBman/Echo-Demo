package io.bsoft.echo.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import io.bsoft.echo.level.Challenge;
import io.bsoft.echo.level.LevelResult;

/**
 * Lightweight progression persistence on top of LibGDX {@link Preferences} (spec §51).
 * Keys follow {@code echo_<levelId>_<field>}.
 */
public final class SaveManager {

    private static final String PREFS_NAME = "echo-save";
    private final Preferences prefs;

    public SaveManager() {
        this(Gdx.app.getPreferences(PREFS_NAME));
    }

    public SaveManager(Preferences prefs) {
        this.prefs = prefs;
    }

    private static String key(String levelId, String field) {
        return "echo_" + levelId + "_" + field;
    }

    public LevelProgress load(String levelId) {
        boolean completed = prefs.getBoolean(key(levelId, "complete"), false);
        float bestTime = prefs.getFloat(key(levelId, "best_time"), 0f);
        int bestEchoes = prefs.getInteger(key(levelId, "best_echoes"), -1);
        int bestRewinds = prefs.getInteger(key(levelId, "best_rewinds"), -1);
        float bestRecording = prefs.getFloat(key(levelId, "best_recording"), 0f);
        ObjectSet<String> challenges = new ObjectSet<>();
        String list = prefs.getString(key(levelId, "challenges"), "");
        for (String c : list.split(",")) {
            if (!c.isBlank()) {
                challenges.add(c.trim());
            }
        }
        return new LevelProgress(levelId, completed, bestTime, bestEchoes, bestRewinds, bestRecording, challenges);
    }

    /**
     * Merges a result into the saved bests and returns the updated progress.
     * Only improvements are written; challenge completion is cumulative.
     */
    public LevelProgress record(LevelResult result, Array<Challenge> challenges) {
        LevelProgress previous = load(result.levelId());
        if (!result.completed()) {
            return previous;
        }
        String id = result.levelId();
        float bestTime = LevelResult.betterTime(previous.bestTime(), result.completionTime());
        int bestEchoes = LevelResult.betterCount(previous.bestEchoes(), result.echoesUsed());
        int bestRewinds = LevelResult.betterCount(previous.bestRewinds(), result.rewindCount());
        float bestRecording = previous.completed()
                ? Math.min(previous.bestRecording(), result.recordingTime()) : result.recordingTime();
        ObjectSet<String> done = new ObjectSet<>(previous.completedChallenges());
        for (Challenge c : challenges) {
            if (c.isSatisfied(result)) {
                done.add(c.key());
            }
        }
        prefs.putBoolean(key(id, "complete"), true);
        prefs.putFloat(key(id, "best_time"), bestTime);
        prefs.putInteger(key(id, "best_echoes"), bestEchoes);
        prefs.putInteger(key(id, "best_rewinds"), bestRewinds);
        prefs.putFloat(key(id, "best_recording"), bestRecording);
        StringBuilder sb = new StringBuilder();
        for (String c : done) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(c);
        }
        prefs.putString(key(id, "challenges"), sb.toString());
        prefs.flush();
        return new LevelProgress(id, true, bestTime, bestEchoes, bestRewinds, bestRecording, done);
    }

    public boolean isCompleted(String levelId) {
        return prefs.getBoolean(key(levelId, "complete"), false);
    }

    public void clearAll() {
        prefs.clear();
        prefs.flush();
    }
}
