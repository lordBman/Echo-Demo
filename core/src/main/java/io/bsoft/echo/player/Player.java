package io.bsoft.echo.player;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import io.bsoft.echo.level.Resettable;
import io.bsoft.echo.objects.Interactable;
import io.bsoft.echo.physics.CollisionBits;
import io.bsoft.echo.physics.ContactAware;
import io.bsoft.echo.physics.GamePhysicsWorld;
import io.bsoft.echo.physics.PhysicsBodyFactory;

/**
 * The physical avatar: a Box2D body plus the sensors and bookkeeping a
 * {@link PlayerController} needs. Used unchanged for the real player and for Echoes;
 * only {@link PlayerKind} (collision category) differs.
 *
 * <p>Contains no input logic and no rendering.</p>
 */
public final class Player implements Resettable {

    /** Height of the foot sensor below the body. */
    private static final float FOOT_SENSOR_HEIGHT = 0.12f;
    /** Foot sensor is slightly narrower than the body so wall contacts don't count as ground. */
    private static final float FOOT_SENSOR_INSET = 0.12f;

    private final PlayerKind kind;
    private final PlayerConfig config;
    private final GamePhysicsWorld physics;
    private final Body body;
    private final Fixture bodyFixture;
    private final Fixture footFixture;

    private final ObjectSet<Fixture> groundContacts = new ObjectSet<>(8);
    private final Array<Interactable> nearbyInteractables = new Array<>(false, 4);
    private final Vector2 tmp = new Vector2();

    private SpawnState initialState;
    private PlayerState state = PlayerState.IDLE;
    private boolean facingRight = true;
    private boolean alive = true;

    public Player(PlayerKind kind, PlayerConfig config, PhysicsBodyFactory factory, SpawnState spawn,
                  short extraMaskBits) {
        this.kind = kind;
        this.config = config;
        this.physics = factory.physics();
        this.initialState = spawn;

        short category = kind == PlayerKind.PLAYER ? CollisionBits.PLAYER : CollisionBits.ECHO;
        short mask = (short) (CollisionBits.CHARACTER_SOLIDS | CollisionBits.HAZARD | CollisionBits.SENSOR
                | extraMaskBits);

        float density = config.mass / (config.width * config.height);
        body = factory.createBox(BodyDef.BodyType.DynamicBody, spawn.x(), spawn.y(), config.width, config.height,
                category, mask, density, 0f, true, this);
        body.setSleepingAllowed(false);
        body.setBullet(false);
        bodyFixture = body.getFixtureList().first();

        FootSensor footSensor = new FootSensor();
        footFixture = factory.addBox(body, config.width - FOOT_SENSOR_INSET * 2f, FOOT_SENSOR_HEIGHT,
                0f, -config.height / 2f, category, CollisionBits.CHARACTER_SOLIDS, 0f, 0f, true, footSensor);
        applySpawnState(spawn);
    }

    // ---------------------------------------------------------------- state

    public PlayerKind kind() {
        return kind;
    }

    public PlayerConfig config() {
        return config;
    }

    public Body body() {
        return body;
    }

    public Fixture bodyFixture() {
        return bodyFixture;
    }

    public Fixture footFixture() {
        return footFixture;
    }

    public Vector2 position() {
        return body.getPosition();
    }

    public float x() {
        return body.getPosition().x;
    }

    public float y() {
        return body.getPosition().y;
    }

    public float feetY() {
        return body.getPosition().y - config.height / 2f;
    }

    public Vector2 velocity() {
        return body.getLinearVelocity();
    }

    public boolean isGrounded() {
        return groundContacts.size > 0;
    }

    /** Horizontal velocity of whatever we are standing on (moving platforms, boxes). */
    public float groundVelocityX() {
        float best = 0f;
        for (Fixture f : groundContacts) {
            Body other = f.getBody();
            if (other.getType() != BodyDef.BodyType.StaticBody) {
                best = other.getLinearVelocity().x;
            }
        }
        return best;
    }

    /** Vertical velocity of the ground we stand on (elevators moving down must not read as falling). */
    public float groundVelocityY() {
        float best = 0f;
        for (Fixture f : groundContacts) {
            Body other = f.getBody();
            if (other.getType() != BodyDef.BodyType.StaticBody) {
                best = other.getLinearVelocity().y;
            }
        }
        return best;
    }

    public PlayerState state() {
        return state;
    }

    void setState(PlayerState state) {
        this.state = state;
    }

    public boolean facingRight() {
        return facingRight;
    }

    void setFacingRight(boolean facingRight) {
        this.facingRight = facingRight;
    }

    public boolean isAlive() {
        return alive;
    }

    /** Marks the avatar dead. The owning system decides what to do (respawn / remove echo). */
    public void kill() {
        if (alive) {
            alive = false;
            state = PlayerState.DEAD;
        }
    }

    // ------------------------------------------------------------ interaction

    public void addNearby(Interactable interactable) {
        if (!nearbyInteractables.contains(interactable, true)) {
            nearbyInteractables.add(interactable);
        }
    }

    public void removeNearby(Interactable interactable) {
        nearbyInteractables.removeValue(interactable, true);
    }

    /** Invoked by the controller on an interact press. */
    public void interact() {
        for (int i = 0; i < nearbyInteractables.size; i++) {
            nearbyInteractables.get(i).interact(this);
        }
    }

    // --------------------------------------------------------------- reset

    public SpawnState initialState() {
        return initialState;
    }

    @Override
    public void captureInitialState() {
        Vector2 p = body.getPosition();
        Vector2 v = body.getLinearVelocity();
        initialState = new SpawnState(p.x, p.y, v.x, v.y, facingRight);
    }

    @Override
    public void reset() {
        applySpawnState(initialState);
    }

    /** Snapshot of the current state; used when recording does not start from the spawn. */
    public SpawnState snapshot() {
        Vector2 p = body.getPosition();
        Vector2 v = body.getLinearVelocity();
        return new SpawnState(p.x, p.y, v.x, v.y, facingRight);
    }

    /** Teleports the avatar to the given state and clears all transient contact bookkeeping. */
    public void applySpawnState(SpawnState s) {
        body.setTransform(s.x(), s.y(), 0f);
        body.setLinearVelocity(tmp.set(s.vx(), s.vy()));
        body.setAngularVelocity(0f);
        body.setAwake(true);
        facingRight = s.facingRight();
        alive = true;
        state = PlayerState.IDLE;
        // Deliberately do NOT clear groundContacts / nearbyInteractables here. Box2D keeps a contact
        // alive across setTransform when the pair still overlaps (no new beginContact is issued) and
        // sends endContact for pairs that stopped overlapping, so the sets stay consistent only if we
        // trust those paired callbacks. Clearing them left the avatar permanently "airborne" after a
        // teleport onto the same ground fixture. Refreshing makes the callbacks fire now rather than
        // on the next step, so a freshly spawned Echo and a teleported player agree on tick 0.
        physics.refreshContacts();
    }

    public void destroy() {
        physics.destroyBody(body);
        groundContacts.clear();
        nearbyInteractables.clear();
    }

    @Override
    public String toString() {
        return kind + "@" + body.getPosition();
    }

    /** Foot sensor bookkeeping; lives on the foot fixture as user data. */
    private final class FootSensor implements ContactAware {

        @Override
        public void beginContact(Fixture self, Fixture other) {
            if (!other.isSensor()) {
                groundContacts.add(other);
            }
        }

        @Override
        public void endContact(Fixture self, Fixture other) {
            groundContacts.remove(other);
        }
    }
}
