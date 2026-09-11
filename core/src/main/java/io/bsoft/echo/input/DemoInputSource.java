package io.bsoft.echo.input;

import io.bsoft.echo.objects.Door;
import io.bsoft.echo.player.Player;
import io.bsoft.echo.world.GameWorld;

/**
 * Scripted input that plays the intended solution of "The First Echo" (level 1) against the
 * live simulation: record a walk onto the plate, spawn the Echo, run through the door to the
 * exit. Used by the desktop {@code --demo} flag as an attract mode and as a visual smoke test
 * of the whole rendering path. It reads world state, so it stays valid if timings shift slightly.
 */
public final class DemoInputSource implements InputSource {

    private enum Phase {
        WAIT_START,
        RECORD_WALK,
        RECORD_HOLD,
        SPAWN,
        WAIT_DOOR,
        RUN,
        DONE
    }

    private static final int START_DELAY_TICKS = 90;
    private static final int HOLD_TICKS = 300;

    private GameWorld world;
    private Phase phase = Phase.WAIT_START;
    private int phaseTicks;
    private boolean left;
    private boolean right;
    private boolean jump;
    private boolean record;
    private boolean createEcho;

    public void attach(GameWorld world) {
        this.world = world;
    }

    /** Advances the script by one fixed step. Call before the world samples input. */
    public void tick() {
        if (world == null) {
            return;
        }
        Player p = world.player();
        phaseTicks++;
        switch (phase) {
            case WAIT_START -> {
                if (phaseTicks > START_DELAY_TICKS) {
                    record = true;
                    next(Phase.RECORD_WALK);
                }
            }
            case RECORD_WALK -> {
                right = p.x() < 8.9f;
                if (!right) {
                    next(Phase.RECORD_HOLD);
                }
            }
            case RECORD_HOLD -> {
                if (phaseTicks > HOLD_TICKS) {
                    record = true; // stop
                    next(Phase.SPAWN);
                }
            }
            case SPAWN -> {
                if (phaseTicks > 20) {
                    createEcho = true;
                    next(Phase.WAIT_DOOR);
                }
            }
            case WAIT_DOOR -> {
                right = p.x() < 17.5f;
                Door door = (Door) world.level().object("door1");
                if (door != null && door.isPassable()) {
                    next(Phase.RUN);
                }
            }
            case RUN -> {
                right = true;
                boolean nearStep = (p.x() > 22.3f && p.x() < 23.6f) || (p.x() > 27.2f && p.x() < 28.6f);
                jump = (nearStep && p.isGrounded()) || (jump && p.velocity().y > 0f);
                if (world.isLevelCompleted()) {
                    right = false;
                    jump = false;
                    next(Phase.DONE);
                }
            }
            default -> {
            }
        }
    }

    private void next(Phase p) {
        phase = p;
        phaseTicks = 0;
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
        return false;
    }

    @Override
    public boolean record() {
        return record;
    }

    @Override
    public boolean createEcho() {
        return createEcho;
    }

    @Override
    public boolean reset() {
        return false;
    }

    @Override
    public void consumePresses() {
        record = false;
        createEcho = false;
    }
}
