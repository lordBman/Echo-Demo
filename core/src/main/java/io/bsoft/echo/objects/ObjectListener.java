package io.bsoft.echo.objects;

import io.bsoft.echo.player.Player;

/** Gameplay events from level objects, consumed by audio/VFX/stats. Simulation stays agnostic. */
public interface ObjectListener {

    default void onPlatePressed(PressurePlate plate) {
    }

    default void onPlateReleased(PressurePlate plate) {
    }

    default void onDoorOpening(Door door) {
    }

    default void onDoorClosing(Door door) {
    }

    default void onSwitchActivated(TimedSwitch sw, Player actor) {
    }

    default void onExitReached(Exit exit, Player player) {
    }

    default void onHazardKill(Hazard hazard, Player victim) {
    }

    default void onEnemyKilled(Enemy enemy, Player killer) {
    }

    default void onEnemyKill(Enemy enemy, Player victim) {
    }

    ObjectListener NONE = new ObjectListener() {
    };
}
