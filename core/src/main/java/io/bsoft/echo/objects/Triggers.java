package io.bsoft.echo.objects;

import io.bsoft.echo.level.Resettable;

/**
 * Trigger combinators (spec §23). Everything here is deterministic and fixed-step based.
 */
public final class Triggers {

    private Triggers() {
    }

    /** Active only while every source is active (Plate A + Plate B → Door). */
    public static TriggerSource allOf(TriggerSource... sources) {
        return () -> {
            for (TriggerSource s : sources) {
                if (!s.isActive()) {
                    return false;
                }
            }
            return sources.length > 0;
        };
    }

    /** Active while any source is active. */
    public static TriggerSource anyOf(TriggerSource... sources) {
        return () -> {
            for (TriggerSource s : sources) {
                if (s.isActive()) {
                    return true;
                }
            }
            return false;
        };
    }

    public static TriggerSource not(TriggerSource source) {
        return () -> !source.isActive();
    }

    /**
     * Stays active for a fixed time after the source's rising edge (timed doors, spec §60).
     * Counts whole fixed steps so the window length is exact and free of float drift.
     * Needs {@link #update(float)} each tick and {@link #reset()} on level reset.
     */
    public static final class Timed implements TriggerSource, Resettable {

        private final TriggerSource source;
        private final float holdTime;
        private final boolean retriggerable;
        private int remainingTicks;
        private float lastDt = 1f / 60f;
        private boolean lastSourceActive;

        public Timed(TriggerSource source, float holdTime, boolean retriggerable) {
            this.source = source;
            this.holdTime = holdTime;
            this.retriggerable = retriggerable;
        }

        public void update(float dt) {
            lastDt = dt;
            boolean active = source.isActive();
            boolean risingEdge = active && !lastSourceActive;
            lastSourceActive = active;
            if (risingEdge && (retriggerable || remainingTicks <= 0)) {
                remainingTicks = Math.max(1, Math.round(holdTime / dt));
            } else if (remainingTicks > 0) {
                remainingTicks--;
            }
        }

        @Override
        public boolean isActive() {
            return remainingTicks > 0;
        }

        public float remaining() {
            return remainingTicks * lastDt;
        }

        public float holdTime() {
            return holdTime;
        }

        @Override
        public void reset() {
            remainingTicks = 0;
            lastSourceActive = false;
        }
    }

    /**
     * Free-running on/off cycle (timed lasers, cycling doors). Deterministic: derived from
     * an internal fixed-step accumulator, not from wall time.
     */
    public static final class Cycle implements TriggerSource, Resettable {

        private final float period;
        private final float onTime;
        private final float offset;
        private float time;

        public Cycle(float period, float onTime, float offset) {
            this.period = Math.max(period, 0.01f);
            this.onTime = onTime;
            this.offset = offset;
            this.time = offset;
        }

        public void update(float dt) {
            time += dt;
            if (time >= period) {
                time -= period;
            }
        }

        @Override
        public boolean isActive() {
            return time < onTime;
        }

        public float phase() {
            return time / period;
        }

        @Override
        public void reset() {
            time = offset % period;
        }
    }

    /** Becomes active once and stays active (latching). */
    public static final class Latch implements TriggerSource, Resettable {

        private final TriggerSource source;
        private boolean latched;

        public Latch(TriggerSource source) {
            this.source = source;
        }

        public void update() {
            if (source.isActive()) {
                latched = true;
            }
        }

        @Override
        public boolean isActive() {
            return latched;
        }

        @Override
        public void reset() {
            latched = false;
        }
    }
}
