package io.bsoft.echo.level;

/** Optional mastery objectives (spec §44). */
public enum ChallengeType {
    COMPLETE("complete", "Complete the level"),
    MAX_ECHOES("maxEchoes", "Use at most %d Echoes"),
    MAX_TIME("maxTime", "Finish under %.0f seconds"),
    MAX_RECORDING("maxRecording", "Record at most %.0f seconds"),
    MAX_REWINDS("maxRewinds", "Rewind at most %d times"),
    NO_REWIND("noRewind", "No rewinds"),
    NO_DEATHS("noDeaths", "No deaths"),
    NO_PARADOX("noParadox", "No paradoxes");

    private final String jsonName;
    private final String descriptionFormat;

    ChallengeType(String jsonName, String descriptionFormat) {
        this.jsonName = jsonName;
        this.descriptionFormat = descriptionFormat;
    }

    public String jsonName() {
        return jsonName;
    }

    public String describe(float value) {
        return switch (this) {
            case MAX_ECHOES, MAX_REWINDS -> String.format(descriptionFormat, Math.round(value));
            case MAX_TIME, MAX_RECORDING -> String.format(descriptionFormat, value);
            default -> descriptionFormat;
        };
    }

    public static ChallengeType fromJsonName(String name) {
        if (name == null) {
            return null;
        }
        for (ChallengeType t : values()) {
            if (t.jsonName.equalsIgnoreCase(name) || t.name().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }
}
