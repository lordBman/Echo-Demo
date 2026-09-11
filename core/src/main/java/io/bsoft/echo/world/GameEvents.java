package io.bsoft.echo.world;

import com.badlogic.gdx.utils.Array;
import io.bsoft.echo.echo.Echo;
import io.bsoft.echo.echo.EchoRecording;
import io.bsoft.echo.level.LevelResult;
import io.bsoft.echo.objects.Door;
import io.bsoft.echo.objects.Enemy;
import io.bsoft.echo.objects.Exit;
import io.bsoft.echo.objects.Hazard;
import io.bsoft.echo.objects.PressurePlate;
import io.bsoft.echo.objects.TimedSwitch;
import io.bsoft.echo.player.Player;

/** Fan-out multicaster so several systems can observe the simulation. */
public final class GameEvents implements GameEventListener {

    private final Array<GameEventListener> listeners = new Array<>(false, 8);

    public void add(GameEventListener l) {
        if (l != null && !listeners.contains(l, true)) {
            listeners.add(l);
        }
    }

    public void remove(GameEventListener l) {
        listeners.removeValue(l, true);
    }

    @Override
    public void onJump(Player player) {
        for (GameEventListener l : listeners) {
            l.onJump(player);
        }
    }

    @Override
    public void onLand(Player player) {
        for (GameEventListener l : listeners) {
            l.onLand(player);
        }
    }

    @Override
    public void onInteract(Player player) {
        for (GameEventListener l : listeners) {
            l.onInteract(player);
        }
    }

    @Override
    public void onEchoCreated(Echo echo) {
        for (GameEventListener l : listeners) {
            l.onEchoCreated(echo);
        }
    }

    @Override
    public void onEchoCompleted(Echo echo) {
        for (GameEventListener l : listeners) {
            l.onEchoCompleted(echo);
        }
    }

    @Override
    public void onEchoDied(Echo echo) {
        for (GameEventListener l : listeners) {
            l.onEchoDied(echo);
        }
    }

    @Override
    public void onEchoLooped(Echo echo) {
        for (GameEventListener l : listeners) {
            l.onEchoLooped(echo);
        }
    }

    @Override
    public void onEchoRejected(EchoRecording recording, String reason) {
        for (GameEventListener l : listeners) {
            l.onEchoRejected(recording, reason);
        }
    }

    @Override
    public void onEchoParadox(Echo echo) {
        for (GameEventListener l : listeners) {
            l.onEchoParadox(echo);
        }
    }

    @Override
    public void onPlatePressed(PressurePlate plate) {
        for (GameEventListener l : listeners) {
            l.onPlatePressed(plate);
        }
    }

    @Override
    public void onPlateReleased(PressurePlate plate) {
        for (GameEventListener l : listeners) {
            l.onPlateReleased(plate);
        }
    }

    @Override
    public void onDoorOpening(Door door) {
        for (GameEventListener l : listeners) {
            l.onDoorOpening(door);
        }
    }

    @Override
    public void onDoorClosing(Door door) {
        for (GameEventListener l : listeners) {
            l.onDoorClosing(door);
        }
    }

    @Override
    public void onSwitchActivated(TimedSwitch sw, Player actor) {
        for (GameEventListener l : listeners) {
            l.onSwitchActivated(sw, actor);
        }
    }

    @Override
    public void onExitReached(Exit exit, Player player) {
        for (GameEventListener l : listeners) {
            l.onExitReached(exit, player);
        }
    }

    @Override
    public void onHazardKill(Hazard hazard, Player victim) {
        for (GameEventListener l : listeners) {
            l.onHazardKill(hazard, victim);
        }
    }

    @Override
    public void onEnemyKilled(Enemy enemy, Player killer) {
        for (GameEventListener l : listeners) {
            l.onEnemyKilled(enemy, killer);
        }
    }

    @Override
    public void onEnemyKill(Enemy enemy, Player victim) {
        for (GameEventListener l : listeners) {
            l.onEnemyKill(enemy, victim);
        }
    }

    @Override
    public void onRecordingStarted() {
        for (GameEventListener l : listeners) {
            l.onRecordingStarted();
        }
    }

    @Override
    public void onRecordingStopped(EchoRecording recording, boolean autoStopped) {
        for (GameEventListener l : listeners) {
            l.onRecordingStopped(recording, autoStopped);
        }
    }

    @Override
    public void onRecordingAborted() {
        for (GameEventListener l : listeners) {
            l.onRecordingAborted();
        }
    }

    @Override
    public void onLevelComplete(LevelResult result) {
        for (GameEventListener l : listeners) {
            l.onLevelComplete(result);
        }
    }

    @Override
    public void onPlayerDied(Player player) {
        for (GameEventListener l : listeners) {
            l.onPlayerDied(player);
        }
    }

    @Override
    public void onPlayerRespawned(Player player) {
        for (GameEventListener l : listeners) {
            l.onPlayerRespawned(player);
        }
    }

    @Override
    public void onLevelReset(boolean countedAsRewind) {
        for (GameEventListener l : listeners) {
            l.onLevelReset(countedAsRewind);
        }
    }
}
