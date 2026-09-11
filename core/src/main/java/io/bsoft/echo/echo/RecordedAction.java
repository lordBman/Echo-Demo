package io.bsoft.echo.echo;

/**
 * One recorded intent (spec §12).
 *
 * @param tick      simulation tick relative to the start of the recording; the authoritative key
 *                  used for playback so no float comparisons are needed
 * @param timestamp {@code tick * fixedStep}, in seconds, for display and tooling
 * @param action    what the player attempted
 * @param value     optional payload (e.g. recording id for CREATE_ECHO), 0 otherwise
 */
public record RecordedAction(int tick, float timestamp, ActionType action, float value) {

    public RecordedAction {
        if (tick < 0) {
            throw new IllegalArgumentException("tick must be >= 0, was " + tick);
        }
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
    }

    public static RecordedAction of(int tick, float step, ActionType action) {
        return new RecordedAction(tick, tick * step, action, 0f);
    }

    public static RecordedAction of(int tick, float step, ActionType action, float value) {
        return new RecordedAction(tick, tick * step, action, value);
    }

    @Override
    public String toString() {
        return String.format("%.2f %s%s", timestamp, action, value != 0f ? " " + value : "");
    }
}
