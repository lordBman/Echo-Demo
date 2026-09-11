package io.bsoft.echo.util;

import io.bsoft.echo.GameConfig;

/**
 * Accumulates variable render deltas and hands out fixed simulation steps.
 *
 * <p>The clock also tracks the deterministic simulation time (tick count multiplied by
 * the fixed step). All gameplay timing, including Echo recordings, must be derived from
 * this clock and never from wall-clock time.</p>
 */
public final class FixedStepClock {

    private final float step;
    private final float maxFrameDelta;
    private final int maxStepsPerFrame;

    private float accumulator;
    private long tick;

    public FixedStepClock() {
        this(GameConfig.FIXED_STEP, GameConfig.MAX_FRAME_DELTA, GameConfig.MAX_STEPS_PER_FRAME);
    }

    public FixedStepClock(float step, float maxFrameDelta, int maxStepsPerFrame) {
        this.step = step;
        this.maxFrameDelta = maxFrameDelta;
        this.maxStepsPerFrame = maxStepsPerFrame;
    }

    /**
     * Adds a render delta and returns how many fixed steps should be executed this frame.
     * The accumulator is drained by that amount; the caller executes the steps and calls
     * {@link #onStepExecuted()} for each of them.
     */
    public int accumulate(float renderDelta) {
        accumulator += Math.min(Math.max(renderDelta, 0f), maxFrameDelta);
        int steps = 0;
        while (accumulator >= step && steps < maxStepsPerFrame) {
            accumulator -= step;
            steps++;
        }
        if (steps == maxStepsPerFrame) {
            // Drop leftover time rather than letting the simulation fall further behind.
            accumulator = 0f;
        }
        return steps;
    }

    public void onStepExecuted() {
        tick++;
    }

    public float step() {
        return step;
    }

    public long tick() {
        return tick;
    }

    /** Deterministic simulation time in seconds. */
    public float time() {
        return tick * step;
    }

    /** Interpolation alpha between the previous and current physics state, for rendering. */
    public float alpha() {
        return accumulator / step;
    }

    public void reset() {
        accumulator = 0f;
        tick = 0;
    }
}
