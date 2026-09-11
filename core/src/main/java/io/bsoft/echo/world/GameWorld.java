package io.bsoft.echo.world;

import com.badlogic.gdx.utils.Disposable;
import io.bsoft.echo.GameConfig;
import io.bsoft.echo.echo.EchoController;
import io.bsoft.echo.echo.EchoManager;
import io.bsoft.echo.echo.EchoRules;
import io.bsoft.echo.input.InputSource;
import io.bsoft.echo.level.Level;
import io.bsoft.echo.level.LevelBuilder;
import io.bsoft.echo.level.LevelData;
import io.bsoft.echo.level.LevelResult;
import io.bsoft.echo.level.LevelSession;
import io.bsoft.echo.level.ResetManager;
import io.bsoft.echo.objects.Exit;
import io.bsoft.echo.physics.GamePhysicsWorld;
import io.bsoft.echo.physics.PhysicsBodyFactory;
import io.bsoft.echo.physics.PhysicsConfig;
import io.bsoft.echo.player.Player;
import io.bsoft.echo.player.PlayerConfig;
import io.bsoft.echo.player.PlayerController;
import io.bsoft.echo.player.PlayerKind;
import io.bsoft.echo.util.FixedStepClock;

/**
 * The complete, renderer-free simulation of one level (spec §37, §54).
 *
 * <p>Owns the physics world, level, player, Echo systems, reset registry and attempt
 * statistics, and advances all of them with the fixed step in a strict order:</p>
 * <pre>
 *   meta input (reset / record / create echo)
 *   → record input sample
 *   → player controller
 *   → echo controllers
 *   → level objects & triggers
 *   → Box2D step
 *   → post-step resolution (deaths, exit, respawn)
 * </pre>
 * Because every stage is fixed-step and reads only simulation state, a recording replayed
 * into an identical world produces identical motion — which is what the tests verify.
 */
public final class GameWorld implements Disposable {

    /** Ticks the player stays dead before respawning at the spawn point. */
    private static final int RESPAWN_DELAY_TICKS = 30;
    /** Anything this far below the level floor is considered fallen into the void. */
    private static final float VOID_DEPTH = 6f;

    private final LevelData levelData;
    private final InputSource input;
    private final GameEvents events = new GameEvents();
    private final FixedStepClock clock = new FixedStepClock();
    private final GamePhysicsWorld physics;
    private final PhysicsBodyFactory bodies;
    private final PlayerConfig playerConfig;
    private final EchoRules echoRules;
    private final Level level;
    private final Player player;
    private final PlayerController playerController;
    private final EchoManager echoManager;
    private final EchoController echoController;
    private final ResetManager resetManager = new ResetManager();
    private final LevelSession session;

    private int respawnCountdown = -1;
    private boolean levelCompleted;
    private boolean exitReachedThisStep;

    public GameWorld(LevelData levelData, InputSource input, PlayerConfig playerConfig) {
        this(levelData, input, playerConfig, new PhysicsConfig(), new LevelBuilder());
    }

    public GameWorld(LevelData levelData, InputSource input, PlayerConfig playerConfig, PhysicsConfig physicsConfig,
                     LevelBuilder builder) {
        this.levelData = levelData;
        this.input = input;
        this.playerConfig = playerConfig;
        this.echoRules = levelData.echoRules.copy();
        this.physics = new GamePhysicsWorld(physicsConfig);
        this.bodies = new PhysicsBodyFactory(physics);
        this.session = new LevelSession(levelData.id);

        // Internal listener bridges object events into world logic before fanning out.
        events.add(new InternalListener());

        this.level = builder.build(levelData, physics, playerConfig, events);
        short playerExtraMask = echoRules.echoesCollide ? io.bsoft.echo.physics.CollisionBits.ECHO : 0;
        this.player = new Player(PlayerKind.PLAYER, playerConfig, bodies, level.playerSpawn(), playerExtraMask);
        this.playerController = new PlayerController(player, input);
        this.playerController.setListener(events);
        this.echoManager = new EchoManager(echoRules, playerConfig, bodies);
        this.echoManager.setListener(events);
        this.echoController = new EchoController(player, playerController, echoManager, echoRules,
                level.playerSpawn(), clock.step());
        this.echoController.setListener(events);

        level.registerResettables(resetManager);
        resetManager.register(player);
        resetManager.captureAll();
    }

    // ------------------------------------------------------------------ access

    public GameEvents events() {
        return events;
    }

    public FixedStepClock clock() {
        return clock;
    }

    public GamePhysicsWorld physics() {
        return physics;
    }

    public Level level() {
        return level;
    }

    public LevelData levelData() {
        return levelData;
    }

    public Player player() {
        return player;
    }

    public PlayerController playerController() {
        return playerController;
    }

    public EchoManager echoManager() {
        return echoManager;
    }

    public EchoController echoController() {
        return echoController;
    }

    public EchoRules echoRules() {
        return echoRules;
    }

    public LevelSession session() {
        return session;
    }

    public ResetManager resetManager() {
        return resetManager;
    }

    public boolean isLevelCompleted() {
        return levelCompleted;
    }

    public boolean isPlayerDead() {
        return respawnCountdown >= 0;
    }

    // ------------------------------------------------------------------ stepping

    /** Advances the simulation by a variable render delta using fixed steps. */
    public void update(float renderDelta) {
        if (levelCompleted) {
            return;
        }
        int steps = clock.accumulate(renderDelta);
        for (int i = 0; i < steps && !levelCompleted; i++) {
            step();
        }
    }

    /** Executes exactly one fixed step. Public so tests can drive the world tick by tick. */
    public void step() {
        float dt = clock.step();

        if (input instanceof io.bsoft.echo.input.DemoInputSource demo) {
            demo.tick();
        }
        if (input.reset()) {
            reset(true);
        }
        if (player.isAlive()) {
            echoController.handleMeta(input);
            echoController.sampleTick(input);
        }
        input.consumePresses();

        playerController.update(dt);
        echoManager.update(dt);
        level.update(dt);
        physics.step(dt);

        postStep();
        session.tick(dt);
        clock.onStepExecuted();
    }

    private void postStep() {
        // Void check for player and echoes.
        float voidY = -VOID_DEPTH;
        if (player.isAlive() && player.y() < voidY) {
            player.kill();
        }
        for (var echo : echoManager.echoes()) {
            if (echo.avatar().isAlive() && echo.avatar().y() < voidY) {
                echo.avatar().kill();
            }
        }

        // Player death → respawn after a short delay.
        if (!player.isAlive() && respawnCountdown < 0) {
            respawnCountdown = RESPAWN_DELAY_TICKS;
            session.onDeath();
            echoController.abortRecording();
            events.onPlayerDied(player);
        }
        if (respawnCountdown > 0) {
            respawnCountdown--;
        } else if (respawnCountdown == 0) {
            respawnCountdown = -1;
            player.reset();
            playerController.resetState();
            events.onPlayerRespawned(player);
        }

        if (exitReachedThisStep && !levelCompleted) {
            exitReachedThisStep = false;
            levelCompleted = true;
            echoController.stopRecording(false);
            LevelResult result = session.complete(echoManager.totalCreated(), echoController.totalRecordedTime(),
                    echoManager.paradoxes());
            events.onLevelComplete(result);
        }
    }

    /**
     * Restores the level's initial state without recreating anything (spec §20).
     *
     * @param countAsRewind false when resetting for reasons that should not count against
     *                      the player (e.g. entering the level)
     */
    public void reset(boolean countAsRewind) {
        echoController.reset();
        echoController.resetStats();
        echoManager.reset();
        resetManager.resetAll();
        physics.refreshContacts();
        playerController.resetState();
        respawnCountdown = -1;
        levelCompleted = false;
        exitReachedThisStep = false;
        if (countAsRewind) {
            session.onRewind();
        } else {
            session.clear();
        }
        events.onLevelReset(countAsRewind);
    }

    @Override
    public void dispose() {
        echoManager.dispose();
        player.destroy();
        level.dispose();
        physics.dispose();
    }

    /** Bridges object-level events into world-level bookkeeping. */
    private final class InternalListener implements GameEventListener {
        @Override
        public void onExitReached(Exit exit, Player who) {
            exitReachedThisStep = true;
        }
    }
}
