package io.bsoft.echo.player;

/** Movement events emitted by {@link PlayerController} (audio, particles, stats). */
public interface PlayerListener {

    default void onJump(Player player) {
    }

    default void onLand(Player player) {
    }

    default void onInteract(Player player) {
    }
}
