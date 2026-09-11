package io.bsoft.echo.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

import io.bsoft.echo.echo.Echo;
import io.bsoft.echo.echo.EchoRecording;
import io.bsoft.echo.level.LevelResult;
import io.bsoft.echo.objects.Door;
import io.bsoft.echo.objects.Enemy;
import io.bsoft.echo.objects.PressurePlate;
import io.bsoft.echo.objects.TimedSwitch;
import io.bsoft.echo.player.Player;
import io.bsoft.echo.player.PlayerKind;
import io.bsoft.echo.util.Log;
import io.bsoft.echo.world.GameEventListener;

/**
 * Plays sound effects in response to simulation events (spec §41). Missing files degrade to
 * silence with a logged warning rather than a crash. Echo actions play slightly detuned so
 * the ear also separates "me" from "past me".
 */
public final class AudioManager implements GameEventListener, Disposable {

    private static final String TAG = "Audio";
    private static final float ECHO_PITCH = 0.85f;

    private final ObjectMap<SoundId, Sound> sounds = new ObjectMap<>();
    private float volume = 0.8f;
    private boolean enabled = true;

    public AudioManager() {
        for (SoundId id : SoundId.values()) {
            FileHandle file = Gdx.files.internal(id.fileName());
            if (!file.exists()) {
                Log.info(TAG, "Missing sound " + file.path() + " (run 'gradle generateSounds'); staying silent");
                continue;
            }
            try {
                sounds.put(id, Gdx.audio.newSound(file));
            } catch (RuntimeException e) {
                Log.error(TAG, "Could not load " + file.path(), e);
            }
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0f, Math.min(1f, volume));
    }

    public void play(SoundId id) {
        play(id, 1f, 1f);
    }

    public void play(SoundId id, float pitch, float gain) {
        if (!enabled) {
            return;
        }
        Sound sound = sounds.get(id);
        if (sound != null) {
            sound.play(volume * gain, pitch, 0f);
        }
    }

    private void playFor(Player who, SoundId id, float gain) {
        boolean echo = who.kind() == PlayerKind.ECHO;
        play(id, echo ? ECHO_PITCH : 1f, echo ? gain * 0.6f : gain);
    }

    @Override
    public void onJump(Player player) {
        playFor(player, SoundId.JUMP, 0.7f);
    }

    @Override
    public void onLand(Player player) {
        playFor(player, SoundId.LAND, 0.8f);
    }

    @Override
    public void onRecordingStarted() {
        play(SoundId.RECORD_START);
    }

    @Override
    public void onRecordingStopped(EchoRecording recording, boolean autoStopped) {
        play(SoundId.RECORD_STOP);
    }

    @Override
    public void onEchoCreated(Echo echo) {
        play(SoundId.ECHO_CREATE);
    }

    @Override
    public void onEchoCompleted(Echo echo) {
        play(SoundId.ECHO_COMPLETE);
    }

    @Override
    public void onEchoDied(Echo echo) {
        play(SoundId.ECHO_DIE);
    }

    @Override
    public void onEchoLooped(Echo echo) {
        play(SoundId.ECHO_LOOP, 1f, 0.6f);
    }

    @Override
    public void onEchoParadox(Echo echo) {
        play(SoundId.PARADOX);
    }

    @Override
    public void onEchoRejected(EchoRecording recording, String reason) {
        play(SoundId.ERROR);
    }

    @Override
    public void onPlatePressed(PressurePlate plate) {
        play(SoundId.PLATE_PRESS);
    }

    @Override
    public void onPlateReleased(PressurePlate plate) {
        play(SoundId.PLATE_RELEASE, 1f, 0.7f);
    }

    @Override
    public void onDoorOpening(Door door) {
        play(SoundId.DOOR_OPEN);
    }

    @Override
    public void onDoorClosing(Door door) {
        play(SoundId.DOOR_CLOSE);
    }

    @Override
    public void onSwitchActivated(TimedSwitch sw, Player actor) {
        playFor(actor, SoundId.SWITCH, 1f);
    }

    @Override
    public void onLevelComplete(LevelResult result) {
        play(SoundId.EXIT);
    }

    @Override
    public void onPlayerDied(Player player) {
        play(SoundId.PLAYER_DIE);
    }

    @Override
    public void onEnemyKilled(Enemy enemy, Player killer) {
        play(SoundId.ENEMY_DIE);
    }

    @Override
    public void onLevelReset(boolean countedAsRewind) {
        if (countedAsRewind) {
            play(SoundId.REWIND);
        }
    }

    @Override
    public void dispose() {
        for (Sound s : sounds.values()) {
            s.dispose();
        }
        sounds.clear();
    }
}
