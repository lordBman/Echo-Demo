package io.bsoft.echo.echo;

import io.bsoft.echo.input.InputSource;

/**
 * Replays an {@link EchoRecording} as an {@link InputSource} (spec §14).
 *
 * <p>{@link #advance()} must be called exactly once per fixed tick, before the controller
 * reads the source, mirroring how {@link InputRecorder#sample} was called while recording.
 * Held state is reconstructed from the recorded edges, so the controller sees the same
 * sequence of samples it saw during recording.</p>
 */
public final class RecordedInputSource implements InputSource {

    private final EchoRecording recording;
    private int cursor;
    private int tick;
    private boolean left;
    private boolean right;
    private boolean jump;
    private boolean interact;
    private int pendingCreateEchoId = -1;
    private boolean finished;

    public RecordedInputSource(EchoRecording recording) {
        this.recording = recording;
        restart();
    }

    public EchoRecording recording() {
        return recording;
    }

    public void restart() {
        cursor = 0;
        tick = 0;
        left = false;
        right = false;
        jump = false;
        interact = false;
        pendingCreateEchoId = -1;
        finished = recording.durationTicks() <= 0;
    }

    /** Applies all actions recorded for the current tick, then moves to the next tick. */
    public void advance() {
        if (finished) {
            releaseAll();
            return;
        }
        pendingCreateEchoId = -1;
        while (cursor < recording.actionCount() && recording.action(cursor).tick() <= tick) {
            apply(recording.action(cursor));
            cursor++;
        }
        tick++;
        if (tick >= recording.durationTicks()) {
            finished = true;
        }
    }

    private void apply(RecordedAction a) {
        switch (a.action()) {
            case MOVE_LEFT -> {
                left = true;
                right = false;
            }
            case MOVE_RIGHT -> {
                left = false;
                right = true;
            }
            case MOVE_STOP -> {
                left = false;
                right = false;
            }
            case JUMP_PRESSED -> jump = true;
            case JUMP_RELEASED -> jump = false;
            case INTERACT_PRESSED -> interact = true;
            case INTERACT_RELEASED -> interact = false;
            case CREATE_ECHO -> pendingCreateEchoId = (int) a.value();
        }
    }

    private void releaseAll() {
        left = false;
        right = false;
        jump = false;
        interact = false;
        pendingCreateEchoId = -1;
    }

    public boolean isFinished() {
        return finished;
    }

    /** Ticks consumed so far. */
    public int tick() {
        return tick;
    }

    public float time() {
        return tick * recording.step();
    }

    /** Recording id to spawn this tick, or -1. Consumed by the EchoManager (inheritance). */
    public int pendingCreateEchoId() {
        return pendingCreateEchoId;
    }

    @Override
    public boolean left() {
        return left;
    }

    @Override
    public boolean right() {
        return right;
    }

    @Override
    public boolean jump() {
        return jump;
    }

    @Override
    public boolean interact() {
        return interact;
    }

    @Override
    public boolean record() {
        return false;
    }

    @Override
    public boolean createEcho() {
        return false;
    }

    @Override
    public boolean reset() {
        return false;
    }
}
