package io.bsoft.echo.echo;

import io.bsoft.echo.physics.PhysicsBodyFactory;
import io.bsoft.echo.player.Player;
import io.bsoft.echo.player.PlayerConfig;
import io.bsoft.echo.player.PlayerController;
import io.bsoft.echo.player.PlayerKind;

/**
 * A replaying past self (spec §17). Owns an avatar identical to the player's, a controller,
 * and a {@link RecordedInputSource}; the controller never knows it is driving an Echo.
 */
public final class Echo {

    /** Divergence (meters) from the recorded run beyond which the replay counts as a paradox. */
    public static final float PARADOX_THRESHOLD = 1f;
    private static final int TRAIL_SAMPLES = 12;
    private static final int TRAIL_INTERVAL = 3;

    private final int id;
    private final EchoRecording recording;
    private final Player avatar;
    private final PlayerController controller;
    private final RecordedInputSource input;
    private final EchoTrail trail = new EchoTrail(TRAIL_SAMPLES, TRAIL_INTERVAL);
    private final boolean loop;

    private EchoState state = EchoState.SPAWNING;
    private int loops;
    private float divergence;
    private int nextCheckpoint;
    private boolean paradox;
    private boolean paradoxPending;

    Echo(int id, EchoRecording recording, PlayerConfig config, PhysicsBodyFactory factory, short extraMask,
         boolean loop) {
        this.id = id;
        this.recording = recording;
        this.loop = loop;
        this.avatar = new Player(PlayerKind.ECHO, config, factory, recording.start(), extraMask);
        this.input = new RecordedInputSource(recording);
        this.controller = new PlayerController(avatar, input);
    }

    public int id() {
        return id;
    }

    public EchoRecording recording() {
        return recording;
    }

    public Player avatar() {
        return avatar;
    }

    public PlayerController controller() {
        return controller;
    }

    public RecordedInputSource input() {
        return input;
    }

    public EchoTrail trail() {
        return trail;
    }

    public EchoState state() {
        return state;
    }

    public boolean isFinished() {
        return state == EchoState.COMPLETED || state == EchoState.DEAD;
    }

    public float playbackTime() {
        return input.time();
    }

    public float progress() {
        return recording.durationTicks() == 0 ? 1f : (float) input.tick() / recording.durationTicks();
    }

    public int loops() {
        return loops;
    }

    /**
     * Largest distance (meters) observed between the replayed position and the recorded
     * checkpoint at the same tick. A large value means the world changed and the past no
     * longer replays the way it originally happened.
     */
    public float divergence() {
        return divergence;
    }

    /**
     * True once the replay has drifted more than {@link #PARADOX_THRESHOLD} from where the
     * recorded run was at the same tick: the world changed, so the past no longer happens the
     * same way.
     */
    public boolean isParadox() {
        return paradox;
    }

    /** True for the single update in which the paradox was detected (consumed by the manager). */
    boolean consumeParadoxEvent() {
        boolean p = paradoxPending;
        paradoxPending = false;
        return p;
    }

    /** Recording id requested via CREATE_ECHO this tick, or -1. */
    public int pendingCreateEchoId() {
        return input.pendingCreateEchoId();
    }

    /** One fixed step. Call before the physics step. */
    public void update(float dt) {
        if (isFinished()) {
            return;
        }
        if (!avatar.isAlive()) {
            state = EchoState.DEAD;
            return;
        }
        if (state == EchoState.SPAWNING) {
            state = EchoState.REPLAYING;
        }
        int tickBefore = input.tick();
        input.advance();
        controller.update(dt);
        trail.tick(avatar.x(), avatar.y());
        measureDivergence(tickBefore);

        if (input.isFinished()) {
            if (loop) {
                state = EchoState.LOOPING;
                loops++;
                restartLoop();
            } else {
                state = EchoState.COMPLETED;
            }
        }
    }

    private void measureDivergence(int tick) {
        var checkpoints = recording.checkpoints();
        while (nextCheckpoint < checkpoints.size && checkpoints.get(nextCheckpoint).tick() < tick) {
            nextCheckpoint++;
        }
        if (nextCheckpoint < checkpoints.size && checkpoints.get(nextCheckpoint).tick() == tick) {
            var cp = checkpoints.get(nextCheckpoint);
            float dx = avatar.x() - cp.x();
            float dy = avatar.y() - cp.y();
            divergence = Math.max(divergence, (float) Math.sqrt(dx * dx + dy * dy));
            if (!paradox && divergence > PARADOX_THRESHOLD) {
                paradox = true;
                paradoxPending = true;
            }
            nextCheckpoint++;
        }
    }

    private void restartLoop() {
        avatar.applySpawnState(recording.start());
        controller.resetState();
        input.restart();
        trail.clear();
        nextCheckpoint = 0;
    }

    /** Called by the manager when the echo is removed from the world. */
    void destroy() {
        avatar.destroy();
    }

    @Override
    public String toString() {
        return "Echo#" + id + "[" + state + " " + String.format("%.2f", playbackTime()) + "s]";
    }
}
