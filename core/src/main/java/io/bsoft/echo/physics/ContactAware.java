package io.bsoft.echo.physics;

import com.badlogic.gdx.physics.box2d.Fixture;

/**
 * Attached as fixture user data. {@link GamePhysicsWorld} routes Box2D contact
 * callbacks to both fixtures' owners, so gameplay objects react to contacts without
 * a central switch statement.
 */
public interface ContactAware {

    void beginContact(Fixture self, Fixture other);

    void endContact(Fixture self, Fixture other);
}
