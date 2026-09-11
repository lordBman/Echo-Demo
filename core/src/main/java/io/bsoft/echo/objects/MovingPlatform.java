package io.bsoft.echo.objects;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import io.bsoft.echo.physics.CollisionBits;
import io.bsoft.echo.physics.GamePhysicsWorld;
import io.bsoft.echo.physics.PhysicsBodyFactory;

/**
 * Kinematic platform that travels between two points (spec §25). Two modes:
 * <ul>
 *   <li>{@link Mode#LOOP}: A → B → A forever, pausing {@code waitTime} at each end.</li>
 *   <li>{@link Mode#TRIGGERED}: moves toward B while the trigger is active and back toward A
 *       otherwise (an elevator).</li>
 * </ul>
 * Motion is integrated with the fixed step and the body's velocity is set explicitly, so
 * characters standing on it are carried (the controller reads the ground velocity).
 */
public final class MovingPlatform implements LevelObject, TriggerConsumer {

    public enum Mode {
        LOOP,
        TRIGGERED
    }

    private final String id;
    private final float width;
    private final float height;
    private final Vector2 pointA = new Vector2();
    private final Vector2 pointB = new Vector2();
    private final float speed;
    private final float waitTime;
    private final Mode mode;
    private final float startOffset;
    private final GamePhysicsWorld physics;
    private final Body body;

    private TriggerSource trigger = TriggerSource.NEVER;
    /** Position along A→B in [0, 1]. */
    private float t;
    private int direction = 1;
    private float waitRemaining;
    private final Vector2 velocity = new Vector2();
    private final Vector2 tmp = new Vector2();

    /**
     * @param ax,ay center of the platform at point A; @param bx,by center at point B.
     * @param startOffset initial position along the path in [0,1] (deterministic phase).
     */
    public MovingPlatform(String id, float ax, float ay, float bx, float by, float width, float height,
                          float speed, float waitTime, Mode mode, float startOffset, PhysicsBodyFactory factory) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.pointA.set(ax, ay);
        this.pointB.set(bx, by);
        this.speed = speed;
        this.waitTime = waitTime;
        this.mode = mode;
        this.startOffset = Math.max(0f, Math.min(1f, startOffset));
        this.physics = factory.physics();
        this.t = this.startOffset;
        tmp.set(pointA).lerp(pointB, t);
        body = factory.createBox(BodyDef.BodyType.KinematicBody, tmp.x, tmp.y, width, height,
                CollisionBits.WORLD, CollisionBits.ALL, 0f, 0.9f, true, this);
    }

    @Override
    public void setTrigger(TriggerSource trigger) {
        this.trigger = trigger == null ? TriggerSource.NEVER : trigger;
    }

    @Override
    public String id() {
        return id;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public Body body() {
        return body;
    }

    public Vector2 pointA() {
        return pointA;
    }

    public Vector2 pointB() {
        return pointB;
    }

    public Mode mode() {
        return mode;
    }

    public float progress() {
        return t;
    }

    @Override
    public void update(float dt) {
        float length = pointA.dst(pointB);
        float dtNorm = length > 0.0001f ? speed * dt / length : 1f;
        float previousT = t;

        if (mode == Mode.LOOP) {
            if (waitRemaining > 0f) {
                waitRemaining = Math.max(0f, waitRemaining - dt);
            } else {
                t += direction * dtNorm;
                if (t >= 1f) {
                    t = 1f;
                    direction = -1;
                    waitRemaining = waitTime;
                } else if (t <= 0f) {
                    t = 0f;
                    direction = 1;
                    waitRemaining = waitTime;
                }
            }
        } else {
            int wanted = trigger.isActive() ? 1 : -1;
            t = Math.max(0f, Math.min(1f, t + wanted * dtNorm));
        }

        // Velocity that moves the body exactly to the new target this step (kinematic bodies
        // integrate velocity in World.step, so this stays perfectly on the path).
        tmp.set(pointA).lerp(pointB, t);
        Vector2 pos = body.getPosition();
        velocity.set((tmp.x - pos.x) / dt, (tmp.y - pos.y) / dt);
        body.setLinearVelocity(velocity);
        if (previousT == t && velocity.len2() < 1e-8f) {
            body.setLinearVelocity(0f, 0f);
        }
    }

    @Override
    public void reset() {
        t = startOffset;
        direction = 1;
        waitRemaining = 0f;
        tmp.set(pointA).lerp(pointB, t);
        body.setTransform(tmp, 0f);
        body.setLinearVelocity(0f, 0f);
    }

    @Override
    public void dispose() {
        physics.destroyBody(body);
    }
}
