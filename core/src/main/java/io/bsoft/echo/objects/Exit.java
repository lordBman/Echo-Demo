package io.bsoft.echo.objects;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import io.bsoft.echo.physics.CollisionBits;
import io.bsoft.echo.physics.ContactAware;
import io.bsoft.echo.physics.GamePhysicsWorld;
import io.bsoft.echo.physics.PhysicsBodyFactory;
import io.bsoft.echo.player.Player;
import io.bsoft.echo.player.PlayerKind;

/** Level goal. Only the real player (not an Echo) can complete the level. */
public final class Exit implements LevelObject, ContactAware {

    public static final float WIDTH = 1.2f;
    public static final float HEIGHT = 2f;

    private final String id;
    private final float x;
    private final float y;
    private final GamePhysicsWorld physics;
    private final Body body;
    private final ObjectListener listener;
    private boolean reached;

    /** @param x center x, @param y bottom y. */
    public Exit(String id, float x, float y, PhysicsBodyFactory factory, ObjectListener listener) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.physics = factory.physics();
        this.listener = listener;
        body = factory.createStaticBox(x - WIDTH / 2f, y, WIDTH, HEIGHT, CollisionBits.SENSOR,
                CollisionBits.PLAYER, 0f, this);
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

    public boolean isReached() {
        return reached;
    }

    @Override
    public void beginContact(Fixture self, Fixture other) {
        if (reached) {
            return;
        }
        if (other.getBody().getUserData() instanceof Player player && player.kind() == PlayerKind.PLAYER
                && player.isAlive()) {
            reached = true;
            listener.onExitReached(this, player);
        }
    }

    @Override
    public void endContact(Fixture self, Fixture other) {
    }

    @Override
    public void reset() {
        reached = false;
    }

    @Override
    public void dispose() {
        physics.destroyBody(body);
    }
}
