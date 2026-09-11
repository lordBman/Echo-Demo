package io.bsoft.echo.objects;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.ObjectSet;
import io.bsoft.echo.physics.CollisionBits;
import io.bsoft.echo.physics.ContactAware;
import io.bsoft.echo.physics.GamePhysicsWorld;
import io.bsoft.echo.physics.PhysicsBodyFactory;

/**
 * A floor plate pressed by any character or dynamic object standing on it (spec §22).
 *
 * <p>Implemented as a static sensor fixture. Pressed state is derived from the set of bodies
 * currently overlapping the sensor, which cannot drift negative even when bodies are
 * teleported or destroyed mid-contact.</p>
 */
public final class PressurePlate implements LevelObject, TriggerSource, ContactAware {

    public static final float DEFAULT_WIDTH = 1.5f;
    public static final float HEIGHT = 0.25f;
    /** Visual: how far the plate sinks when pressed. */
    public static final float PRESS_DEPTH = 0.15f;
    private static final float ANIMATION_SPEED = 12f;

    private final String id;
    private final float x;
    private final float y;
    private final float width;
    private final GamePhysicsWorld physics;
    private final Body body;
    private final ObjectSet<Body> pressingBodies = new ObjectSet<>(4);
    private final ObjectListener listener;

    private boolean wasPressed;
    private float pressAmount;

    /** @param x center x, @param y bottom y. */
    public PressurePlate(String id, float x, float y, float width, PhysicsBodyFactory factory,
                         ObjectListener listener) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.physics = factory.physics();
        this.listener = listener;
        // The sensor extends slightly above the plate so characters resting on the floor around it
        // still register when their feet overlap.
        body = factory.createStaticBox(x - width / 2f, y, width, HEIGHT, CollisionBits.SENSOR,
                (short) (CollisionBits.CHARACTERS | CollisionBits.INTERACTIVE), 0f, this);
        body.getFixtureList().first().setSensor(true);
    }

    @Override
    public String id() {
        return id;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float width() {
        return width;
    }

    /** 0 = raised, 1 = fully pressed; animates toward the logical state. */
    public float pressAmount() {
        return pressAmount;
    }

    @Override
    public boolean isActive() {
        return pressingBodies.size > 0;
    }

    @Override
    public void update(float dt) {
        boolean pressed = isActive();
        if (pressed != wasPressed) {
            if (pressed) {
                listener.onPlatePressed(this);
            } else {
                listener.onPlateReleased(this);
            }
            wasPressed = pressed;
        }
        float target = pressed ? 1f : 0f;
        pressAmount += (target - pressAmount) * Math.min(1f, ANIMATION_SPEED * dt);
    }

    @Override
    public void beginContact(Fixture self, Fixture other) {
        if (!other.isSensor()) {
            pressingBodies.add(other.getBody());
        }
    }

    @Override
    public void endContact(Fixture self, Fixture other) {
        pressingBodies.remove(other.getBody());
    }

    @Override
    public void reset() {
        // pressingBodies is left alone: Box2D issues endContact for bodies that leave (or are
        // destroyed) and keeps contacts for bodies that remain, e.g. a box whose initial position is
        // on this plate. Clearing here would desynchronise the set from the physics state.
        wasPressed = false;
        pressAmount = 0f;
    }

    @Override
    public void dispose() {
        physics.destroyBody(body);
    }
}
