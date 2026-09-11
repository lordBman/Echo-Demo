package io.bsoft.echo.objects;

import com.badlogic.gdx.physics.box2d.Body;
import io.bsoft.echo.physics.CollisionBits;
import io.bsoft.echo.physics.GamePhysicsWorld;
import io.bsoft.echo.physics.PhysicsBodyFactory;

/**
 * A vertical barrier that slides open while its trigger is active (spec §24).
 *
 * <p>The door body is static; the fixture is deactivated once the door is fully open so
 * characters pass through. Opening/closing progress advances deterministically per tick.</p>
 */
public final class Door implements LevelObject, TriggerConsumer {

    public enum DoorState {
        CLOSED,
        OPENING,
        OPEN,
        CLOSING
    }

    public static final float DEFAULT_WIDTH = 0.6f;
    public static final float DEFAULT_HEIGHT = 3f;

    private final String id;
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final float openSpeed;
    private final GamePhysicsWorld physics;
    private final Body body;
    private final ObjectListener listener;

    private TriggerSource trigger = TriggerSource.NEVER;
    private DoorState state = DoorState.CLOSED;
    /** 0 = closed, 1 = open. */
    private float openness;
    private boolean bodyActive = true;

    /** @param x center x, @param y bottom y. */
    public Door(String id, float x, float y, float width, float height, float openTime, PhysicsBodyFactory factory,
                ObjectListener listener) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.openSpeed = 1f / Math.max(openTime, 0.01f);
        this.physics = factory.physics();
        this.listener = listener;
        body = factory.createStaticBox(x - width / 2f, y, width, height, CollisionBits.WORLD,
                CollisionBits.ALL, 0.2f, this);
    }

    @Override
    public void setTrigger(TriggerSource trigger) {
        this.trigger = trigger == null ? TriggerSource.NEVER : trigger;
    }

    public TriggerSource trigger() {
        return trigger;
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

    public float height() {
        return height;
    }

    public DoorState state() {
        return state;
    }

    public float openness() {
        return openness;
    }

    public boolean isPassable() {
        return !bodyActive;
    }

    @Override
    public void update(float dt) {
        boolean wantOpen = trigger.isActive();
        if (wantOpen && state != DoorState.OPEN && state != DoorState.OPENING) {
            state = DoorState.OPENING;
            listener.onDoorOpening(this);
        } else if (!wantOpen && state != DoorState.CLOSED && state != DoorState.CLOSING) {
            state = DoorState.CLOSING;
            listener.onDoorClosing(this);
        }
        switch (state) {
            case OPENING -> {
                openness = Math.min(1f, openness + openSpeed * dt);
                if (openness >= 1f) {
                    state = DoorState.OPEN;
                }
            }
            case CLOSING -> {
                openness = Math.max(0f, openness - openSpeed * dt);
                if (openness <= 0f) {
                    state = DoorState.CLOSED;
                }
            }
            default -> {
            }
        }
        // The collider drops out only when the door is (almost) fully open, so a closing door
        // is solid again immediately: timing puzzles must be honest.
        boolean shouldBlock = openness < 0.85f;
        if (shouldBlock != bodyActive) {
            bodyActive = shouldBlock;
            body.setActive(shouldBlock);
        }
    }

    @Override
    public void reset() {
        state = DoorState.CLOSED;
        openness = 0f;
        if (!bodyActive) {
            bodyActive = true;
            body.setActive(true);
        }
    }

    @Override
    public void dispose() {
        physics.destroyBody(body);
    }
}
