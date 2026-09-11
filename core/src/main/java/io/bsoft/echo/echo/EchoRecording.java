package io.bsoft.echo.echo;

import com.badlogic.gdx.utils.Array;
import io.bsoft.echo.player.SpawnState;

/**
 * A complete recording: the well-defined starting state of the avatar, the ordered list of
 * intents, and the duration in ticks (spec §11, §18).
 *
 * <p>Optionally stores sparse position checkpoints captured during recording. They are never
 * used to drive playback (playback is input-driven) but let the game detect when a replay
 * <em>diverges</em> from the original run because the world changed — the basis of the
 * "paradox" detection in the advanced systems.</p>
 */
public final class EchoRecording {

    /** Every {@code CHECKPOINT_INTERVAL} ticks a position checkpoint is stored. */
    public static final int CHECKPOINT_INTERVAL = 15;

    /** Position sample used for divergence detection. */
    public record Checkpoint(int tick, float x, float y) {
    }

    private final int id;
    private final float step;
    private final SpawnState start;
    private final Array<RecordedAction> actions = new Array<>(true, 64);
    private final Array<Checkpoint> checkpoints = new Array<>(true, 32);
    private int durationTicks;

    public EchoRecording(int id, float step, SpawnState start) {
        if (start == null) {
            throw new IllegalArgumentException("start state must not be null");
        }
        this.id = id;
        this.step = step;
        this.start = start;
    }

    public int id() {
        return id;
    }

    public float step() {
        return step;
    }

    public SpawnState start() {
        return start;
    }

    /** Appends an action. Ticks must be non-decreasing. */
    public void add(RecordedAction action) {
        if (actions.size > 0 && action.tick() < actions.peek().tick()) {
            throw new IllegalArgumentException("actions must be appended in tick order: " + action
                    + " after " + actions.peek());
        }
        actions.add(action);
        if (action.tick() >= durationTicks) {
            durationTicks = action.tick() + 1;
        }
    }

    public void addCheckpoint(int tick, float x, float y) {
        checkpoints.add(new Checkpoint(tick, x, y));
    }

    public Array<Checkpoint> checkpoints() {
        return checkpoints;
    }

    public int actionCount() {
        return actions.size;
    }

    public RecordedAction action(int index) {
        return actions.get(index);
    }

    public Array<RecordedAction> actions() {
        return actions;
    }

    public boolean isEmpty() {
        return durationTicks == 0;
    }

    public int durationTicks() {
        return durationTicks;
    }

    /** Duration in seconds. */
    public float duration() {
        return durationTicks * step;
    }

    /** Sets the total length; must be at least the tick of the last action + 1. */
    public void setDurationTicks(int ticks) {
        if (actions.size > 0 && ticks <= actions.peek().tick()) {
            throw new IllegalArgumentException("duration " + ticks + " shorter than last action "
                    + actions.peek());
        }
        this.durationTicks = Math.max(ticks, 0);
    }

    /** True when the recording is well-formed and can be replayed. */
    public boolean isValid() {
        if (durationTicks <= 0) {
            return false;
        }
        int last = -1;
        for (RecordedAction a : actions) {
            if (a.tick() < last || a.tick() >= durationTicks) {
                return false;
            }
            last = a.tick();
        }
        return true;
    }

    @Override
    public String toString() {
        return "Recording#" + id + "[" + actions.size + " actions, " + String.format("%.2fs", duration()) + "]";
    }
}
