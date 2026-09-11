package io.bsoft.echo.world;

import io.bsoft.echo.echo.EchoController;
import io.bsoft.echo.echo.EchoListener;
import io.bsoft.echo.level.LevelResult;
import io.bsoft.echo.objects.ObjectListener;
import io.bsoft.echo.player.Player;
import io.bsoft.echo.player.PlayerListener;

/**
 * Union of every gameplay event the simulation emits. Presentation layers (audio, VFX,
 * HUD) implement the parts they care about; the simulation never references them.
 */
public interface GameEventListener extends PlayerListener, EchoListener, ObjectListener, EchoController.Listener {

    default void onLevelComplete(LevelResult result) {
    }

    default void onPlayerDied(Player player) {
    }

    default void onPlayerRespawned(Player player) {
    }

    default void onLevelReset(boolean countedAsRewind) {
    }
}
