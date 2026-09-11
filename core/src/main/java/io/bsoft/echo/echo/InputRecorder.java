package io.bsoft.echo.echo;

import io.bsoft.echo.input.InputSource;
import io.bsoft.echo.player.SpawnState;

/**
 * Samples an {@link InputSource} once per fixed tick and turns state changes into
 * {@link RecordedAction}s (spec §12, §13).
 *
 * <p>Time is measured in ticks relative to {@link #begin}; the recorder never touches the
 * wall clock. When the configured maximum length is reached the recorder stops itself and
 * {@link #sample} returns {@code true} so the owner can react.</p>
 */
public final class InputRecorder {

    private final float step;
    private int maxTicks;

    private EchoRecording recording;
    private int tick;
    private int lastAxis;
    private boolean lastJump;
    private boolean lastInteract;

    public InputRecorder(float step, int maxTicks) {
        this.step = step;
        this.maxTicks = maxTicks;
    }

    public void setMaxTicks(int maxTicks) {
        this.maxTicks = Math.max(1, maxTicks);
    }

    public int maxTicks() {
        return maxTicks;
    }

    public boolean isRecording() {
        return recording != null;
    }

    /** Starts a fresh recording. Initial input state is "nothing held". */
    public void begin(int recordingId, SpawnState start) {
        recording = new EchoRecording(recordingId, step, start);
        tick = 0;
        lastAxis = 0;
        lastJump = false;
        lastInteract = false;
    }

    /**
     * Samples the input for the current tick and advances.
     *
     * @return true if the recording just hit its maximum length and has been stopped
     *         automatically (retrieve it with {@link #end()})
     */
    public boolean sample(InputSource input) {
        if (recording == null) {
            throw new IllegalStateException("not recording");
        }
        int axis = input.horizontal();
        if (axis != lastAxis) {
            recording.add(RecordedAction.of(tick, step, axisAction(axis)));
            lastAxis = axis;
        }
        boolean jump = input.jump();
        if (jump != lastJump) {
            recording.add(RecordedAction.of(tick, step, jump ? ActionType.JUMP_PRESSED : ActionType.JUMP_RELEASED));
            lastJump = jump;
        }
        boolean interact = input.interact();
        if (interact != lastInteract) {
            recording.add(RecordedAction.of(tick, step,
                    interact ? ActionType.INTERACT_PRESSED : ActionType.INTERACT_RELEASED));
            lastInteract = interact;
        }
        tick++;
        return tick >= maxTicks;
    }

    /** Records an explicit action at the current tick (e.g. CREATE_ECHO for inheritance). */
    public void recordEvent(ActionType type, float value) {
        if (recording == null) {
            throw new IllegalStateException("not recording");
        }
        recording.add(RecordedAction.of(tick, step, type, value));
    }

    /** Stores a position checkpoint for the current tick if the interval says so. */
    public void checkpoint(float x, float y) {
        if (recording != null && tick % EchoRecording.CHECKPOINT_INTERVAL == 0) {
            recording.addCheckpoint(tick, x, y);
        }
    }

    /** Stops recording and returns the finished recording (duration = ticks sampled). */
    public EchoRecording end() {
        if (recording == null) {
            throw new IllegalStateException("not recording");
        }
        EchoRecording done = recording;
        done.setDurationTicks(Math.max(tick, 1));
        recording = null;
        return done;
    }

    /** Discards the in-progress recording. */
    public void abort() {
        recording = null;
    }

    public int elapsedTicks() {
        return tick;
    }

    public float elapsed() {
        return tick * step;
    }

    public float progress() {
        return Math.min(1f, (float) tick / maxTicks);
    }

    private static ActionType axisAction(int axis) {
        if (axis < 0) {
            return ActionType.MOVE_LEFT;
        }
        return axis > 0 ? ActionType.MOVE_RIGHT : ActionType.MOVE_STOP;
    }
}
