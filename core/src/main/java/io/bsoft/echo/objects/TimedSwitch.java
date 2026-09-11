package io.bsoft.echo.objects;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import io.bsoft.echo.physics.CollisionBits;
import io.bsoft.echo.physics.ContactAware;
import io.bsoft.echo.physics.GamePhysicsWorld;
import io.bsoft.echo.physics.PhysicsBodyFactory;
import io.bsoft.echo.player.Player;
import io.bsoft.echo.player.PlayerKind;

/**
 * A wall switch activated with the interact key by any avatar standing in front of it.
 * If {@code holdTime > 0} it stays active for that long (timed switch, spec §9); otherwise
 * it toggles and stays.
 */
public final class TimedSwitch implements LevelObject, TriggerSource, ContactAware, Interactable {

    public static final float WIDTH = 0.8f;
    public static final float HEIGHT = 1.2f;

    private final String id;
    private final float x;
    private final float y;
    private final float holdTime;
    private final GamePhysicsWorld physics;
    private final Body body;
    private final ObjectListener listener;

    private boolean toggled;
    private float remaining;
    private int playersNearby;

    /** @param x center x, @param y bottom y. */
    public TimedSwitch(String id, float x, float y, float holdTime, PhysicsBodyFactory factory,
                       ObjectListener listener) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.holdTime = holdTime;
        this.physics = factory.physics();
        this.listener = listener;
        // Sensor slightly wider than the visual so standing next to it counts.
        body = factory.createStaticBox(x - WIDTH, y, WIDTH * 2f, HEIGHT, CollisionBits.SENSOR,
                CollisionBits.CHARACTERS, 0f, this);
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

    public boolean isTimed() {
        return holdTime > 0f;
    }

    public float holdTime() {
        return holdTime;
    }

    public float remaining() {
        return remaining;
    }

    /** True while the real player (not an Echo) is close enough to interact; drives the "E" prompt. */
    public boolean isPlayerNearby() {
        return playersNearby > 0;
    }

    @Override
    public boolean isActive() {
        return isTimed() ? remaining > 0f : toggled;
    }

    @Override
    public void interact(Player actor) {
        if (isTimed()) {
            remaining = holdTime;
        } else {
            toggled = !toggled;
        }
        listener.onSwitchActivated(this, actor);
    }

    @Override
    public void update(float dt) {
        if (remaining > 0f) {
            remaining = Math.max(0f, remaining - dt);
        }
    }

    @Override
    public void beginContact(Fixture self, Fixture other) {
        // Only the solid body fixture counts, so the foot sensor leaving does not end the interaction.
        if (!other.isSensor() && other.getBody().getUserData() instanceof Player player) {
            player.addNearby(this);
            if (player.kind() == PlayerKind.PLAYER) {
                playersNearby++;
            }
        }
    }

    @Override
    public void endContact(Fixture self, Fixture other) {
        if (!other.isSensor() && other.getBody().getUserData() instanceof Player player) {
            player.removeNearby(this);
            if (player.kind() == PlayerKind.PLAYER) {
                playersNearby = Math.max(0, playersNearby - 1);
            }
        }
    }

    @Override
    public void reset() {
        toggled = false;
        remaining = 0f;
    }

    @Override
    public void dispose() {
        physics.destroyBody(body);
    }
}
