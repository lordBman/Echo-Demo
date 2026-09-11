package io.bsoft.echo.level;

/** One optional objective and how to evaluate it against a {@link LevelResult}. */
public record Challenge(ChallengeType type, float value) {

    public String description() {
        return type.describe(value);
    }

    /** Stable key for saving completion state. */
    public String key() {
        return type.jsonName() + (value != 0f ? "_" + Math.round(value * 100f) : "");
    }

    public boolean isSatisfied(LevelResult r) {
        return switch (type) {
            case COMPLETE -> r.completed();
            case MAX_ECHOES -> r.completed() && r.echoesUsed() <= Math.round(value);
            case MAX_TIME -> r.completed() && r.completionTime() <= value;
            case MAX_RECORDING -> r.completed() && r.recordingTime() <= value;
            case MAX_REWINDS -> r.completed() && r.rewindCount() <= Math.round(value);
            case NO_REWIND -> r.completed() && r.rewindCount() == 0;
            case NO_DEATHS -> r.completed() && r.deaths() == 0;
            case NO_PARADOX -> r.completed() && r.paradoxes() == 0;
        };
    }
}
