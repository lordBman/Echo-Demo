package io.bsoft.echo.objects;

import io.bsoft.echo.player.Player;

/** Something a nearby avatar can activate with the interact key. */
public interface Interactable {

    void interact(Player actor);
}
