package io.bsoft.echo.echo;

import com.badlogic.gdx.utils.Array;
import io.bsoft.echo.input.InputSource;
import io.bsoft.echo.player.Player;
import io.bsoft.echo.player.PlayerController;
import io.bsoft.echo.player.SpawnState;
import io.bsoft.echo.util.Log;

/**
 * Drives the real player's recording workflow: start/stop recording, remember the latest
 * recording, and spawn Echoes from it (spec §11).
 *
 * <p>Key behaviour:</p>
 * <ul>
 *   <li>Record press toggles recording. When {@code EchoRules.recordFromSpawn} is on, the
 *       player is teleported to the level spawn so the recording (and the Echo) begins
 *       from the level's initial player state.</li>
 *   <li>Create-echo press while recording stops the recording and spawns immediately.</li>
 *   <li>Create-echo press when not recording spawns the latest recording again.</li>
 * </ul>
 */
public final class EchoController {

    private static final String TAG = "EchoController";
    public static final int HISTORY_SIZE = 5;

    public interface Listener {
        default void onRecordingStarted() {
        }

        default void onRecordingStopped(EchoRecording recording, boolean autoStopped) {
        }

        default void onRecordingAborted() {
        }
    }

    private final Player player;
    private final PlayerController playerController;
    private final EchoManager echoManager;
    private final EchoRules rules;
    private final InputRecorder recorder;
    private final SpawnState levelSpawn;
    private Listener listener;

    /** Most recent first, at most {@link #HISTORY_SIZE} entries. Survives rewinds. */
    private final Array<EchoRecording> history = new Array<>(true, HISTORY_SIZE);
    private int nextRecordingId = 1;
    private float totalRecordedTime;

    public EchoController(Player player, PlayerController playerController, EchoManager echoManager,
                          EchoRules rules, SpawnState levelSpawn, float step) {
        this.player = player;
        this.playerController = playerController;
        this.echoManager = echoManager;
        this.rules = rules;
        this.levelSpawn = levelSpawn;
        this.recorder = new InputRecorder(step, rules.maxRecordingTicks(step));
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public boolean isRecording() {
        return recorder.isRecording();
    }

    public InputRecorder recorder() {
        return recorder;
    }

    public EchoRecording lastRecording() {
        return history.size > 0 ? history.first() : null;
    }

    /** Recording history, most recent first. */
    public Array<EchoRecording> history() {
        return history;
    }

    public boolean hasRecording() {
        return history.size > 0;
    }

    /** Seconds of recording produced since the last {@link #resetStats()}. */
    public float totalRecordedTime() {
        return totalRecordedTime;
    }

    /**
     * Handles the meta presses (record / create echo) for this tick. Movement input is not
     * touched here; it is sampled by {@link #sampleTick(InputSource)}.
     */
    public void handleMeta(InputSource input) {
        if (input.record()) {
            toggleRecording();
        }
        if (input.createEcho()) {
            spawnEcho();
        }
        int slot = input.spawnHistorySlot();
        if (slot >= 0) {
            spawnFromHistory(slot);
        }
    }

    /** Samples the player's movement input for the current tick if recording. */
    public void sampleTick(InputSource input) {
        if (!recorder.isRecording()) {
            return;
        }
        recorder.checkpoint(player.x(), player.y());
        boolean hitLimit = recorder.sample(input);
        if (hitLimit) {
            stopRecording(true);
        }
    }

    public void toggleRecording() {
        if (recorder.isRecording()) {
            stopRecording(false);
        } else {
            startRecording();
        }
    }

    public void startRecording() {
        if (recorder.isRecording()) {
            return;
        }
        SpawnState start;
        if (rules.recordFromSpawn) {
            start = levelSpawn;
            player.applySpawnState(start);
            playerController.resetState();
        } else {
            start = player.snapshot();
        }
        recorder.begin(nextRecordingId++, start);
        Log.debug(TAG, "Recording started from " + start);
        if (listener != null) {
            listener.onRecordingStarted();
        }
    }

    public void stopRecording(boolean auto) {
        if (!recorder.isRecording()) {
            return;
        }
        EchoRecording recording = recorder.end();
        totalRecordedTime += recording.duration();
        if (recording.isValid()) {
            history.insert(0, recording);
            if (history.size > HISTORY_SIZE) {
                history.pop();
            }
            echoManager.registerRecording(recording);
            Log.debug(TAG, "Recording stopped: " + recording);
        } else {
            Log.info(TAG, "Recording discarded (invalid): " + recording);
        }
        if (listener != null) {
            listener.onRecordingStopped(recording, auto);
        }
    }

    /** Drops an in-progress recording without keeping it (used on reset / death). */
    public void abortRecording() {
        if (recorder.isRecording()) {
            recorder.abort();
            if (listener != null) {
                listener.onRecordingAborted();
            }
        }
    }

    /** Spawns an Echo from the latest recording (stopping the current recording first). */
    public Echo spawnEcho() {
        if (recorder.isRecording()) {
            // Inheritance: an echo created during a recording is itself part of that recording.
            if (rules.echoInheritance && hasRecording()) {
                return spawnInherited(lastRecording());
            }
            stopRecording(false);
        }
        if (!hasRecording()) {
            Log.info(TAG, "No recording to spawn an Echo from");
            return null;
        }
        return echoManager.createEcho(lastRecording());
    }

    /**
     * Spawns an Echo from the history: slot 0 is the most recent recording. While recording with
     * inheritance enabled the spawn is recorded too; otherwise it ends the current recording first.
     */
    public Echo spawnFromHistory(int slot) {
        if (slot < 0 || slot >= history.size) {
            Log.info(TAG, "No recording in history slot " + (slot + 1));
            return null;
        }
        EchoRecording recording = history.get(slot);
        if (recorder.isRecording()) {
            if (rules.echoInheritance) {
                return spawnInherited(recording);
            }
            stopRecording(false);
            // Stopping shifted the history by one; the requested recording is now one slot later.
            recording = history.get(Math.min(slot + 1, history.size - 1));
        }
        return echoManager.createEcho(recording);
    }

    private Echo spawnInherited(EchoRecording recording) {
        recorder.recordEvent(ActionType.CREATE_ECHO, recording.id());
        return echoManager.createEcho(recording);
    }

    /** Called on level reset: stops recording but keeps the last recording for reuse. */
    public void reset() {
        abortRecording();
    }

    public void resetStats() {
        totalRecordedTime = 0f;
    }
}
