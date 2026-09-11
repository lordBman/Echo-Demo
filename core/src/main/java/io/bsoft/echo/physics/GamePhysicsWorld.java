package io.bsoft.echo.physics;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/**
 * Owns the Box2D {@link World} lifecycle (spec §9).
 *
 * <ul>
 *   <li>Steps the world with the fixed time step only.</li>
 *   <li>Queues body destruction so it never happens inside a callback or mid-step.</li>
 *   <li>Dispatches contacts to {@link ContactAware} fixture user data.</li>
 * </ul>
 */
public final class GamePhysicsWorld implements Disposable {

    private final World world;
    private final PhysicsConfig config;
    private final Array<Body> destroyQueue = new Array<>(false, 16);
    private final Array<Runnable> postStepActions = new Array<>(false, 16);
    private boolean stepping;
    private long lastStepNanos;

    public GamePhysicsWorld(PhysicsConfig config) {
        this.config = config;
        this.world = new World(new Vector2(0f, config.gravityY), config.allowSleep);
        this.world.setContactListener(new Dispatcher());
    }

    public Body createBody(BodyDef def) {
        return world.createBody(def);
    }

    /** Destroys a body safely, deferring if we are currently inside a step. */
    public void destroyBody(Body body) {
        if (body == null) {
            return;
        }
        if (stepping) {
            if (!destroyQueue.contains(body, true)) {
                destroyQueue.add(body);
            }
        } else {
            world.destroyBody(body);
        }
    }

    /** Runs an action after the current step finishes (or immediately when not stepping). */
    public void runAfterStep(Runnable action) {
        if (stepping) {
            postStepActions.add(action);
        } else {
            action.run();
        }
    }

    public void step(float dt) {
        long start = System.nanoTime();
        stepping = true;
        world.step(dt, config.velocityIterations, config.positionIterations);
        stepping = false;
        flushDeferred();
        lastStepNanos = System.nanoTime() - start;
    }

    /**
     * Re-evaluates contacts without advancing time (a zero-length Box2D step only runs the
     * collide phase). Call after teleporting or creating bodies so begin/end contact callbacks
     * reflect the new positions immediately instead of one tick later. Ignored mid-step.
     */
    public void refreshContacts() {
        if (stepping) {
            return;
        }
        world.step(0f, 0, 0);
    }

    private void flushDeferred() {
        for (Body body : destroyQueue) {
            world.destroyBody(body);
        }
        destroyQueue.clear();
        for (Runnable action : postStepActions) {
            action.run();
        }
        postStepActions.clear();
    }

    public World raw() {
        return world;
    }

    public int bodyCount() {
        return world.getBodyCount();
    }

    public int fixtureCount() {
        return world.getFixtureCount();
    }

    public int contactCount() {
        return world.getContactCount();
    }

    /** Wall-clock duration of the last step, for the debug overlay only. */
    public float lastStepMillis() {
        return lastStepNanos / 1_000_000f;
    }

    @Override
    public void dispose() {
        destroyQueue.clear();
        postStepActions.clear();
        world.dispose();
    }

    /** Routes contacts to fixture owners. */
    private static final class Dispatcher implements ContactListener {

        @Override
        public void beginContact(Contact contact) {
            Fixture a = contact.getFixtureA();
            Fixture b = contact.getFixtureB();
            if (a.getUserData() instanceof ContactAware aware) {
                aware.beginContact(a, b);
            }
            if (b.getUserData() instanceof ContactAware aware) {
                aware.beginContact(b, a);
            }
        }

        @Override
        public void endContact(Contact contact) {
            Fixture a = contact.getFixtureA();
            Fixture b = contact.getFixtureB();
            if (a.getUserData() instanceof ContactAware aware) {
                aware.endContact(a, b);
            }
            if (b.getUserData() instanceof ContactAware aware) {
                aware.endContact(b, a);
            }
        }

        @Override
        public void preSolve(Contact contact, Manifold oldManifold) {
            // No-op for now. One-way platforms would hook in here.
        }

        @Override
        public void postSolve(Contact contact, ContactImpulse impulse) {
            // No-op.
        }
    }
}
