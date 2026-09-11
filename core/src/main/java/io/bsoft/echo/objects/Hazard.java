package io.bsoft.echo.objects;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.Array;
import io.bsoft.echo.physics.CollisionBits;
import io.bsoft.echo.physics.ContactAware;
import io.bsoft.echo.physics.GamePhysicsWorld;
import io.bsoft.echo.physics.PhysicsBodyFactory;
import io.bsoft.echo.player.Player;

/**
 * Kills any avatar that touches it while armed. Covers spikes, lasers and moving hazards:
 * <ul>
 *   <li>Optional trigger: when set, the hazard is armed only while the trigger is active
 *       (a timed laser uses a {@link Triggers.Cycle}).</li>
 *   <li>Optional path: a kinematic body moving between two points like a platform.</li>
 *   <li>{@link #isActive()}: latches active once something has died in it, so a hazard can
 *       itself be a trigger source ("Echo sacrifice").</li>
 * </ul>
 * Kills are applied after the physics step to keep body destruction out of callbacks.
 */
public final class Hazard implements LevelObject, TriggerConsumer, TriggerSource, ContactAware {

    private final String id;
    private final float width;
    private final float height;
    private final Vector2 pointA = new Vector2();
    private final Vector2 pointB = new Vector2();
    private final float speed;
    private final boolean moving;
    private final GamePhysicsWorld physics;
    private final Body body;
    private final ObjectListener listener;
    private final Array<Player> touching = new Array<>(false, 4);

    private TriggerSource armedTrigger = TriggerSource.ALWAYS;
    private boolean killLatched;
    private float t;
    private int direction = 1;
    private final Vector2 tmp = new Vector2();

    /** @param x,y center of the hazard (point A). If bx/by differ, the hazard moves A↔B. */
    public Hazard(String id, float x, float y, float bx, float by, float width, float height, float speed,
                  PhysicsBodyFactory factory, ObjectListener listener) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.pointA.set(x, y);
        this.pointB.set(bx, by);
        this.speed = speed;
        this.moving = pointA.dst2(pointB) > 1e-6f && speed > 0f;
        this.physics = factory.physics();
        this.listener = listener;
        body = factory.createBox(moving ? BodyDef.BodyType.KinematicBody : BodyDef.BodyType.StaticBody, x, y,
                width, height, CollisionBits.HAZARD, CollisionBits.CHARACTERS, 0f, 0f, true, this);
        Fixture fixture = body.getFixtureList().first();
        fixture.setSensor(true);
    }

    @Override
    public void setTrigger(TriggerSource trigger) {
        this.armedTrigger = trigger == null ? TriggerSource.ALWAYS : trigger;
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

    public boolean isArmed() {
        return armedTrigger.isActive();
    }

    public boolean isMoving() {
        return moving;
    }

    /** Trigger semantics: active once anything has died in this hazard (until reset). */
    @Override
    public boolean isActive() {
        return killLatched;
    }

    @Override
    public void update(float dt) {
        if (moving) {
            float length = pointA.dst(pointB);
            t += direction * speed * dt / length;
            if (t >= 1f) {
                t = 1f;
                direction = -1;
            } else if (t <= 0f) {
                t = 0f;
                direction = 1;
            }
            tmp.set(pointA).lerp(pointB, t);
            Vector2 pos = body.getPosition();
            body.setLinearVelocity((tmp.x - pos.x) / dt, (tmp.y - pos.y) / dt);
        }
        if (isArmed()) {
            for (int i = 0; i < touching.size; i++) {
                Player victim = touching.get(i);
                if (victim.isAlive()) {
                    victim.kill();
                    killLatched = true;
                    listener.onHazardKill(this, victim);
                }
            }
        }
    }

    @Override
    public void beginContact(Fixture self, Fixture other) {
        if (other.getBody().getUserData() instanceof Player player && !touching.contains(player, true)) {
            touching.add(player);
        }
    }

    @Override
    public void endContact(Fixture self, Fixture other) {
        if (other.getBody().getUserData() instanceof Player player) {
            touching.removeValue(player, true);
        }
    }

    @Override
    public void reset() {
        // 'touching' is maintained purely by Box2D begin/end callbacks; see PressurePlate.reset().
        killLatched = false;
        t = 0f;
        direction = 1;
        if (moving) {
            body.setTransform(pointA, 0f);
            body.setLinearVelocity(0f, 0f);
        }
    }

    @Override
    public void dispose() {
        physics.destroyBody(body);
    }
}
