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
 * A deterministic patrolling enemy (advanced systems, spec §49). It walks between two x
 * positions. An avatar touching it from the side dies; an avatar whose feet land on its upper
 * part while falling defeats it (a stomp) and bounces. Echoes can therefore fight on the
 * player's behalf. A defeated enemy is deactivated and restored on reset, and doubles as a
 * {@link TriggerSource} that is active once defeated.
 */
public final class Enemy implements LevelObject, ContactAware, TriggerSource {

    public static final float WIDTH = 1f;
    public static final float HEIGHT = 1f;
    /** Feet at or above this fraction of the enemy height count as a stomp. */
    private static final float STOMP_FRACTION = 0.5f;
    private static final float STOMP_BOUNCE_VELOCITY = 8f;

    private final String id;
    private final float minX;
    private final float maxX;
    private final float y;
    private final float speed;
    private final GamePhysicsWorld physics;
    private final Body body;
    private final ObjectListener listener;
    /** Avatars that touched us from above (verdict taken when the contact began). */
    private final Array<Player> stompers = new Array<>(false, 4);
    /** Avatars that touched us from the side. */
    private final Array<Player> victims = new Array<>(false, 4);

    private int direction = 1;
    private boolean alive = true;
    private final Vector2 tmp = new Vector2();

    /** Patrols on the segment [minX, maxX] at feet height {@code y}. */
    public Enemy(String id, float minX, float maxX, float y, float speed, PhysicsBodyFactory factory,
                 ObjectListener listener) {
        this.id = id;
        this.minX = Math.min(minX, maxX);
        this.maxX = Math.max(minX, maxX);
        this.y = y;
        this.speed = speed;
        this.physics = factory.physics();
        this.listener = listener;
        body = factory.createBox(BodyDef.BodyType.KinematicBody, this.minX, y + HEIGHT / 2f, WIDTH, HEIGHT,
                CollisionBits.ENEMY, CollisionBits.CHARACTERS, 0f, 0f, true, this);
        body.getFixtureList().first().setSensor(true);
    }

    @Override
    public String id() {
        return id;
    }

    public Body body() {
        return body;
    }

    public boolean isAlive() {
        return alive;
    }

    public int direction() {
        return direction;
    }

    /** Trigger semantics: active once the enemy has been defeated. */
    @Override
    public boolean isActive() {
        return !alive;
    }

    @Override
    public void update(float dt) {
        if (!alive) {
            return;
        }
        for (int i = 0; i < stompers.size; i++) {
            Player other = stompers.get(i);
            if (other.isAlive()) {
                other.body().setLinearVelocity(other.velocity().x, STOMP_BOUNCE_VELOCITY);
                die(other);
                return;
            }
        }
        for (int i = 0; i < victims.size; i++) {
            Player other = victims.get(i);
            if (other.isAlive()) {
                other.kill();
                listener.onEnemyKill(this, other);
            }
        }
        Vector2 pos = body.getPosition();
        float nextX = pos.x + direction * speed * dt;
        if (nextX >= maxX) {
            nextX = maxX;
            direction = -1;
        } else if (nextX <= minX) {
            nextX = minX;
            direction = 1;
        }
        body.setLinearVelocity((nextX - pos.x) / dt, 0f);
    }

    private void die(Player killer) {
        alive = false;
        body.setLinearVelocity(0f, 0f);
        body.setActive(false);
        stompers.clear();
        victims.clear();
        listener.onEnemyKilled(this, killer);
    }

    /**
     * Box2D reports the contact at the positions the step started with, so the verdict is taken
     * here rather than in {@link #update}, where the avatar has already integrated one more tick.
     */
    @Override
    public void beginContact(Fixture self, Fixture other) {
        // Only the solid body fixture counts; foot sensors would double-register.
        if (other.isSensor() || !(other.getBody().getUserData() instanceof Player player)) {
            return;
        }
        float stompLine = body.getPosition().y + HEIGHT / 2f - HEIGHT * (1f - STOMP_FRACTION);
        boolean stomp = player.feetY() >= stompLine && player.velocity().y <= 1f;
        Array<Player> list = stomp ? stompers : victims;
        if (!list.contains(player, true)) {
            list.add(player);
        }
    }

    @Override
    public void endContact(Fixture self, Fixture other) {
        if (other.getBody().getUserData() instanceof Player player) {
            stompers.removeValue(player, true);
            victims.removeValue(player, true);
        }
    }

    @Override
    public void reset() {
        direction = 1;
        // Contact lists are maintained by Box2D callbacks (setActive(true) below re-issues them).
        body.setTransform(tmp.set(minX, y + HEIGHT / 2f), 0f);
        body.setLinearVelocity(0f, 0f);
        if (!alive) {
            alive = true;
            body.setActive(true);
        }
    }

    @Override
    public void dispose() {
        physics.destroyBody(body);
    }
}
