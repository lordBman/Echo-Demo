package io.bsoft.echo.level;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import io.bsoft.echo.objects.Door;
import io.bsoft.echo.objects.Enemy;
import io.bsoft.echo.objects.Exit;
import io.bsoft.echo.objects.Hazard;
import io.bsoft.echo.objects.LevelObject;
import io.bsoft.echo.objects.MovingPlatform;
import io.bsoft.echo.objects.ObjectListener;
import io.bsoft.echo.objects.PressurePlate;
import io.bsoft.echo.objects.PushableBox;
import io.bsoft.echo.objects.TimedSwitch;
import io.bsoft.echo.objects.TriggerConsumer;
import io.bsoft.echo.objects.TriggerSource;
import io.bsoft.echo.physics.CollisionBits;
import io.bsoft.echo.physics.GamePhysicsWorld;
import io.bsoft.echo.physics.PhysicsBodyFactory;
import io.bsoft.echo.player.PlayerConfig;
import io.bsoft.echo.player.SpawnState;

/**
 * Turns {@link LevelData} into a live {@link Level} (spec §27, §48).
 *
 * <p>Object types are looked up in a registry so new mechanisms are added by registering a
 * factory, not by editing a switch statement. Construction is two-phase: all objects are
 * created first, then trigger expressions are resolved so any object can reference any other
 * regardless of file order.</p>
 */
public final class LevelBuilder {

    /** Creates one object type from its definition. */
    @FunctionalInterface
    public interface ObjectFactory {
        LevelObject create(LevelData.ObjectDef def, Context ctx);
    }

    /** Everything a factory may need. */
    public record Context(PhysicsBodyFactory bodies, ObjectListener listener, LevelData level) {
    }

    private static final float WALL_THICKNESS = 1f;

    private final ObjectMap<String, ObjectFactory> factories = new ObjectMap<>();

    public LevelBuilder() {
        registerDefaults();
    }

    public void register(String type, ObjectFactory factory) {
        factories.put(type, factory);
    }

    private void registerDefaults() {
        register("pressurePlate", (def, ctx) -> new PressurePlate(def.id, def.x, def.y,
                def.getFloat("width", PressurePlate.DEFAULT_WIDTH), ctx.bodies(), ctx.listener()));
        register("door", (def, ctx) -> new Door(def.id, def.x, def.y,
                def.getFloat("width", Door.DEFAULT_WIDTH), def.getFloat("height", Door.DEFAULT_HEIGHT),
                def.getFloat("openTime", 0.4f), ctx.bodies(), ctx.listener()));
        register("exit", (def, ctx) -> new Exit(def.id, def.x, def.y, ctx.bodies(), ctx.listener()));
        register("movingPlatform", (def, ctx) -> {
            float[] target = def.getVec2("target");
            if (target == null) {
                throw new LevelLoadException(ctx.level().id + ": movingPlatform '" + def.id
                        + "' needs 'target' [x, y]");
            }
            float w = def.getFloat("width", 3f);
            float h = def.getFloat("height", 0.5f);
            MovingPlatform.Mode mode = def.getString("trigger", null) != null
                    ? MovingPlatform.Mode.TRIGGERED : MovingPlatform.Mode.LOOP;
            String modeName = def.getString("mode", null);
            if (modeName != null) {
                mode = MovingPlatform.Mode.valueOf(modeName.toUpperCase());
            }
            // position/target are bottom-center; body center is half a height higher.
            return new MovingPlatform(def.id, def.x, def.y + h / 2f, target[0], target[1] + h / 2f, w, h,
                    def.getFloat("speed", 3f), def.getFloat("waitTime", 0.5f), mode,
                    def.getFloat("startOffset", 0f), ctx.bodies());
        });
        register("pushableBox", (def, ctx) -> new PushableBox(def.id, def.x, def.y,
                def.getFloat("size", 1.2f), def.getFloat("density", 0.6f), def.getFloat("friction", 0.6f),
                ctx.bodies()));
        register("hazard", (def, ctx) -> {
            float w = def.getFloat("width", 1f);
            float h = def.getFloat("height", 0.5f);
            float[] target = def.getVec2("target");
            float bx = target != null ? target[0] : def.x;
            float by = target != null ? target[1] + h / 2f : def.y + h / 2f;
            return new Hazard(def.id, def.x, def.y + h / 2f, bx, by, w, h, def.getFloat("speed", 0f),
                    ctx.bodies(), ctx.listener());
        });
        register("switch", (def, ctx) -> new TimedSwitch(def.id, def.x, def.y, def.getFloat("holdTime", 0f),
                ctx.bodies(), ctx.listener()));
        register("enemy", (def, ctx) -> new Enemy(def.id, def.x, def.getFloat("patrolTo", def.x + 4f), def.y,
                def.getFloat("speed", 2f), ctx.bodies(), ctx.listener()));
    }

    public Level build(LevelData data, GamePhysicsWorld physics, PlayerConfig playerConfig,
                       ObjectListener listener) {
        PhysicsBodyFactory bodies = new PhysicsBodyFactory(physics);
        SpawnState spawn = SpawnState.standing(data.spawnX, data.spawnY, playerConfig.height);
        Level level = new Level(data, physics, spawn);

        for (LevelData.Platform p : data.platforms) {
            level.addPlatformBody(bodies.createStaticBox(p.x(), p.y(), p.width(), p.height(), CollisionBits.WORLD,
                    CollisionBits.ALL, 0.6f, p));
        }
        // Invisible side walls and ceiling keep everything inside the level bounds.
        level.addPlatformBody(bodies.createStaticBox(-WALL_THICKNESS, -WALL_THICKNESS * 10f, WALL_THICKNESS,
                data.boundsHeight + WALL_THICKNESS * 12f, CollisionBits.WORLD, CollisionBits.ALL, 0f, null));
        level.addPlatformBody(bodies.createStaticBox(data.boundsWidth, -WALL_THICKNESS * 10f, WALL_THICKNESS,
                data.boundsHeight + WALL_THICKNESS * 12f, CollisionBits.WORLD, CollisionBits.ALL, 0f, null));
        level.addPlatformBody(bodies.createStaticBox(-WALL_THICKNESS, data.boundsHeight + WALL_THICKNESS,
                data.boundsWidth + WALL_THICKNESS * 2f, WALL_THICKNESS, CollisionBits.WORLD, CollisionBits.ALL, 0f,
                null));

        Context ctx = new Context(bodies, listener, data);
        Array<LevelObject> created = new Array<>();
        for (LevelData.ObjectDef def : data.objects) {
            ObjectFactory factory = factories.get(def.type);
            if (factory == null) {
                level.dispose();
                throw new LevelLoadException(data.id + ": unknown object type '" + def.type + "' (id " + def.id + ")");
            }
            if (level.object(def.id) != null) {
                level.dispose();
                throw new LevelLoadException(data.id + ": duplicate object id '" + def.id + "'");
            }
            LevelObject object = factory.create(def, ctx);
            level.addObject(object);
            created.add(object);
        }

        // Phase 2: resolve trigger expressions now that every object exists.
        TriggerExpression parser = new TriggerExpression(level.triggerMap(), level.statefulTriggers());
        for (int i = 0; i < created.size; i++) {
            LevelObject object = created.get(i);
            if (object instanceof TriggerConsumer consumer) {
                String expr = data.objects.get(i).getString("trigger", null);
                if (expr != null) {
                    TriggerSource trigger = parser.parse(expr, data.id + ": object '" + object.id() + "'");
                    consumer.setTrigger(trigger);
                }
            }
        }
        return level;
    }
}
