package io.bsoft.echo.objects;

import io.bsoft.echo.level.Resettable;

/**
 * Base contract for everything placed in a level. Objects are simulated with the fixed step
 * and know nothing about rendering; renderers read their public state.
 */
public interface LevelObject extends Resettable {

    /** Stable identifier from level data, or a generated one. */
    String id();

    /** One fixed step, called before the physics step. */
    default void update(float dt) {
    }

    /** Destroys physics bodies. */
    void dispose();
}
