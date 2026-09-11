package io.bsoft.echo.physics;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;

/**
 * Small helpers for the most common body shapes. Every body created through here has
 * its collision filtering set explicitly from {@link CollisionBits}.
 */
public final class PhysicsBodyFactory {

    private final GamePhysicsWorld physics;

    public PhysicsBodyFactory(GamePhysicsWorld physics) {
        this.physics = physics;
    }

    public GamePhysicsWorld physics() {
        return physics;
    }

    /** Creates a static, axis-aligned box whose bottom-left corner is at (x, y). */
    public Body createStaticBox(float x, float y, float width, float height, short category, short mask,
                                float friction, Object userData) {
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.StaticBody;
        def.position.set(x + width / 2f, y + height / 2f);
        Body body = physics.createBody(def);
        addBox(body, width, height, 0f, 0f, category, mask, friction, 0f, false, userData);
        body.setUserData(userData);
        return body;
    }

    /** Creates a body with a centered box fixture. Center given directly. */
    public Body createBox(BodyDef.BodyType type, float cx, float cy, float width, float height,
                          short category, short mask, float density, float friction, boolean fixedRotation,
                          Object userData) {
        BodyDef def = new BodyDef();
        def.type = type;
        def.position.set(cx, cy);
        def.fixedRotation = fixedRotation;
        Body body = physics.createBody(def);
        addBox(body, width, height, 0f, 0f, category, mask, friction, density, false, userData);
        body.setUserData(userData);
        return body;
    }

    /** Adds a box fixture to an existing body, offset from the body origin. */
    public Fixture addBox(Body body, float width, float height, float offsetX, float offsetY,
                          short category, short mask, float friction, float density, boolean sensor,
                          Object userData) {
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / 2f, height / 2f, new com.badlogic.gdx.math.Vector2(offsetX, offsetY), 0f);
        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.density = density;
        fd.friction = friction;
        fd.restitution = 0f;
        fd.isSensor = sensor;
        fd.filter.categoryBits = category;
        fd.filter.maskBits = mask;
        Fixture fixture = body.createFixture(fd);
        fixture.setUserData(userData);
        shape.dispose();
        return fixture;
    }
}
