package io.bsoft.echo.objects;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import io.bsoft.echo.physics.CollisionBits;
import io.bsoft.echo.physics.GamePhysicsWorld;
import io.bsoft.echo.physics.PhysicsBodyFactory;

/**
 * A dynamic crate that players and Echoes can push and stand on (spec §26). Its complete
 * kinematic state is captured after the level is built and restored on reset.
 */
public final class PushableBox implements LevelObject {

    private final String id;
    private final float size;
    private final GamePhysicsWorld physics;
    private final Body body;

    private final Vector2 initialPosition = new Vector2();
    private final Vector2 initialVelocity = new Vector2();
    private float initialAngle;
    private float initialAngularVelocity;

    /** @param x center x, @param y bottom y. */
    public PushableBox(String id, float x, float y, float size, float density, float friction,
                       PhysicsBodyFactory factory) {
        this.id = id;
        this.size = size;
        this.physics = factory.physics();
        body = factory.createBox(BodyDef.BodyType.DynamicBody, x, y + size / 2f, size, size,
                CollisionBits.INTERACTIVE, CollisionBits.ALL, density, friction, false, this);
        body.setSleepingAllowed(true);
        captureInitialState();
    }

    @Override
    public String id() {
        return id;
    }

    public float size() {
        return size;
    }

    public Body body() {
        return body;
    }

    @Override
    public void captureInitialState() {
        initialPosition.set(body.getPosition());
        initialVelocity.set(body.getLinearVelocity());
        initialAngle = body.getAngle();
        initialAngularVelocity = body.getAngularVelocity();
    }

    @Override
    public void reset() {
        body.setTransform(initialPosition, initialAngle);
        body.setLinearVelocity(initialVelocity);
        body.setAngularVelocity(initialAngularVelocity);
        body.setAwake(true);
    }

    @Override
    public void dispose() {
        physics.destroyBody(body);
    }
}
