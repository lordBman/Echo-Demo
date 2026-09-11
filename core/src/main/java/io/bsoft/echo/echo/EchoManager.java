package io.bsoft.echo.echo;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import io.bsoft.echo.physics.CollisionBits;
import io.bsoft.echo.physics.PhysicsBodyFactory;
import io.bsoft.echo.player.PlayerConfig;
import io.bsoft.echo.util.Log;

/**
 * Creates, updates, limits and removes Echoes (spec §19). Has no rendering dependency.
 */
public final class EchoManager {

    private static final String TAG = "EchoManager";

    private final EchoRules rules;
    private final PlayerConfig playerConfig;
    private final PhysicsBodyFactory factory;
    private final Array<Echo> echoes = new Array<>(true, 8);
    private final Array<Echo> finished = new Array<>(false, 8);
    /** Recordings addressable by id for CREATE_ECHO inheritance. */
    private final IntMap<EchoRecording> library = new IntMap<>();
    private EchoListener listener;
    private int nextEchoId = 1;
    private int totalCreated;
    private int paradoxes;

    public EchoManager(EchoRules rules, PlayerConfig playerConfig, PhysicsBodyFactory factory) {
        this.rules = rules;
        this.playerConfig = playerConfig;
        this.factory = factory;
    }

    public void setListener(EchoListener listener) {
        this.listener = listener;
    }

    public EchoRules rules() {
        return rules;
    }

    public Array<Echo> echoes() {
        return echoes;
    }

    public int count() {
        return echoes.size;
    }

    public int maxEchoes() {
        return rules.maxEchoes;
    }

    public boolean canCreate() {
        return echoes.size < rules.maxEchoes;
    }

    /** Echoes created since the last reset (statistic). */
    public int totalCreated() {
        return totalCreated;
    }

    /** Paradoxes detected since the last reset (statistic). */
    public int paradoxes() {
        return paradoxes;
    }

    /** Makes a recording addressable for inheritance. */
    public void registerRecording(EchoRecording recording) {
        library.put(recording.id(), recording);
    }

    /**
     * Spawns an Echo replaying the recording, or returns {@code null} when the limit is
     * reached or the recording is unusable. Failures are reported to the listener and logged,
     * never thrown (spec §52).
     */
    public Echo createEcho(EchoRecording recording) {
        if (recording == null || !recording.isValid()) {
            reject(recording, "recording is empty or invalid");
            return null;
        }
        if (!canCreate()) {
            reject(recording, "echo limit reached (" + rules.maxEchoes + ")");
            return null;
        }
        short extraMask = rules.echoesCollide ? CollisionBits.CHARACTERS : 0;
        Echo echo = new Echo(nextEchoId++, recording, playerConfig, factory, extraMask, rules.loopingEchoes);
        echoes.add(echo);
        totalCreated++;
        registerRecording(recording);
        if (listener != null) {
            listener.onEchoCreated(echo);
        }
        return echo;
    }

    private void reject(EchoRecording recording, String reason) {
        Log.info(TAG, "Echo rejected: " + reason);
        if (listener != null) {
            listener.onEchoRejected(recording, reason);
        }
    }

    /** One fixed step for every echo. Call before the physics step. */
    public void update(float dt) {
        for (int i = 0; i < echoes.size; i++) {
            Echo echo = echoes.get(i);
            EchoState before = echo.state();
            echo.update(dt);
            if (before != EchoState.LOOPING && echo.state() == EchoState.LOOPING && listener != null) {
                listener.onEchoLooped(echo);
            }
            if (echo.consumeParadoxEvent()) {
                paradoxes++;
                if (listener != null) {
                    listener.onEchoParadox(echo);
                }
            }
            if (rules.echoInheritance && echo.pendingCreateEchoId() >= 0) {
                EchoRecording inherited = library.get(echo.pendingCreateEchoId());
                if (inherited != null && inherited != echo.recording()) {
                    createEcho(inherited);
                }
            }
            if (echo.isFinished()) {
                finished.add(echo);
            }
        }
        for (Echo echo : finished) {
            echoes.removeValue(echo, true);
            if (listener != null) {
                if (echo.state() == EchoState.DEAD) {
                    listener.onEchoDied(echo);
                } else {
                    listener.onEchoCompleted(echo);
                }
            }
            echo.destroy();
        }
        finished.clear();
    }

    /** Removes all echoes. Recordings stay registered so they can be respawned. */
    public void reset() {
        for (Echo echo : echoes) {
            echo.destroy();
        }
        echoes.clear();
        finished.clear();
        totalCreated = 0;
        paradoxes = 0;
    }

    public void dispose() {
        reset();
        library.clear();
    }
}
