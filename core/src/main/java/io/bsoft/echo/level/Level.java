package io.bsoft.echo.level;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import io.bsoft.echo.objects.LevelObject;
import io.bsoft.echo.objects.TriggerSource;
import io.bsoft.echo.objects.Triggers;
import io.bsoft.echo.physics.GamePhysicsWorld;
import io.bsoft.echo.player.SpawnState;

/**
 * The runtime level: static geometry, simulated objects and the trigger graph.
 * Built by {@link LevelBuilder}; knows nothing about rendering.
 */
public final class Level {

    private final LevelData data;
    private final GamePhysicsWorld physics;
    private final SpawnState playerSpawn;
    private final Array<Body> platformBodies = new Array<>();
    private final Array<LevelObject> objects = new Array<>(true, 16);
    private final ObjectMap<String, LevelObject> objectsById = new ObjectMap<>();
    private final ObjectMap<String, TriggerSource> triggers = new ObjectMap<>();
    /** Timed / cyclic triggers that need per-tick updates. */
    private final Array<Object> statefulTriggers = new Array<>();

    Level(LevelData data, GamePhysicsWorld physics, SpawnState playerSpawn) {
        this.data = data;
        this.physics = physics;
        this.playerSpawn = playerSpawn;
    }

    void addPlatformBody(Body body) {
        platformBodies.add(body);
    }

    void addObject(LevelObject object) {
        objects.add(object);
        objectsById.put(object.id(), object);
        if (object instanceof TriggerSource source) {
            triggers.put(object.id(), source);
        }
    }

    ObjectMap<String, TriggerSource> triggerMap() {
        return triggers;
    }

    Array<Object> statefulTriggers() {
        return statefulTriggers;
    }

    public LevelData data() {
        return data;
    }

    public SpawnState playerSpawn() {
        return playerSpawn;
    }

    public float width() {
        return data.boundsWidth;
    }

    public float height() {
        return data.boundsHeight;
    }

    public Array<LevelObject> objects() {
        return objects;
    }

    public LevelObject object(String id) {
        return objectsById.get(id);
    }

    /** Typed lookup helper for renderers and tests. */
    public <T> Array<T> objectsOfType(Class<T> type, Array<T> out) {
        out.clear();
        for (LevelObject o : objects) {
            if (type.isInstance(o)) {
                out.add(type.cast(o));
            }
        }
        return out;
    }

    /** Registers everything resettable with the manager, in dependency order. */
    public void registerResettables(ResetManager resetManager) {
        for (Object t : statefulTriggers) {
            if (t instanceof Resettable r) {
                resetManager.register(r);
            }
        }
        for (LevelObject o : objects) {
            resetManager.register(o);
        }
    }

    /** One fixed step for all objects. Triggers first so mechanisms read fresh state. */
    public void update(float dt) {
        for (int i = 0; i < statefulTriggers.size; i++) {
            Object t = statefulTriggers.get(i);
            if (t instanceof Triggers.Timed timed) {
                timed.update(dt);
            } else if (t instanceof Triggers.Cycle cycle) {
                cycle.update(dt);
            } else if (t instanceof Triggers.Latch latch) {
                latch.update();
            }
        }
        for (int i = 0; i < objects.size; i++) {
            objects.get(i).update(dt);
        }
    }

    public void dispose() {
        for (LevelObject o : objects) {
            o.dispose();
        }
        objects.clear();
        objectsById.clear();
        triggers.clear();
        for (Body b : platformBodies) {
            physics.destroyBody(b);
        }
        platformBodies.clear();
    }
}
