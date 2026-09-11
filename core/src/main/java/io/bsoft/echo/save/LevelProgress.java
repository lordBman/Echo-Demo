package io.bsoft.echo.save;

import com.badlogic.gdx.utils.ObjectSet;

/**
 * Saved bests for one level. Negative counts / zero times mean "no record yet".
 * Shaped for future leaderboards (spec §47).
 */
public record LevelProgress(String levelId, boolean completed, float bestTime, int bestEchoes, int bestRewinds,
                            float bestRecording, ObjectSet<String> completedChallenges) {

    public static LevelProgress empty(String levelId) {
        return new LevelProgress(levelId, false, 0f, -1, -1, 0f, new ObjectSet<>());
    }

    public boolean hasChallenge(String key) {
        return completedChallenges.contains(key);
    }
}
