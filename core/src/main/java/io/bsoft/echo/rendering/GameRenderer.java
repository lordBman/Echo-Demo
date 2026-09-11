package io.bsoft.echo.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.utils.Disposable;
import io.bsoft.echo.echo.Echo;
import io.bsoft.echo.player.Player;
import io.bsoft.echo.world.GameEventListener;
import io.bsoft.echo.world.GameWorld;

/**
 * Composes the world-space renderers (spec §30) and owns the cosmetic feedback that reacts
 * to simulation events (particles, screen shake, flash). The simulation never calls into here;
 * this class subscribes to {@link GameWorld#events()}.
 */
public final class GameRenderer implements Disposable, GameEventListener {

    private final Assets assets;
    private final GameCamera camera;
    private final LevelRenderer levelRenderer;
    private final PlayerRenderer playerRenderer;
    private final EchoRenderer echoRenderer;
    private final ParticleSystem particles = new ParticleSystem();
    private final Box2DDebugRenderer debugRenderer = new Box2DDebugRenderer();
    private GameWorld world;
    private boolean debugPhysics;
    private float time;
    private float flash;
    private final Color flashColor = new Color(Color.WHITE);

    public GameRenderer(Assets assets, GameCamera camera) {
        this.assets = assets;
        this.camera = camera;
        AvatarRenderer avatar = new AvatarRenderer(assets);
        this.levelRenderer = new LevelRenderer(assets);
        this.playerRenderer = new PlayerRenderer(assets, avatar);
        this.echoRenderer = new EchoRenderer(assets, avatar);
    }

    public void attach(GameWorld world) {
        if (this.world != null) {
            this.world.events().remove(this);
        }
        this.world = world;
        world.events().add(this);
        particles.clear();
        camera.setLevelBounds(world.level().width(), world.level().height());
        camera.snapTo(world.player().x(), world.player().y());
    }

    public void toggleDebugPhysics() {
        debugPhysics = !debugPhysics;
    }

    public boolean isDebugPhysics() {
        return debugPhysics;
    }

    public GameCamera camera() {
        return camera;
    }

    public ParticleSystem particles() {
        return particles;
    }

    public void update(float delta) {
        time += delta;
        particles.update(delta);
        playerRenderer.update(delta);
        flash = Math.max(0f, flash - delta * 3f);
        Player p = world.player();
        camera.follow(p.x(), p.y(), delta);
    }

    public void render() {
        SpriteBatch batch = assets.batch;
        camera.viewport().apply();
        batch.setProjectionMatrix(camera.camera().combined);
        batch.begin();
        levelRenderer.render(batch, world.level(), time);
        echoRenderer.render(batch, world.echoManager().echoes(), time);
        if (!world.isPlayerDead()) {
            playerRenderer.render(batch, world.player(), world.echoController().isRecording(), time);
        }
        particles.render(batch, assets);
        if (flash > 0f) {
            batch.setColor(flashColor.r, flashColor.g, flashColor.b, flash * 0.5f);
            batch.draw(assets.white, -5f, -5f, world.level().width() + 10f, world.level().height() + 10f);
            batch.setColor(Color.WHITE);
        }
        batch.end();
        if (debugPhysics) {
            debugRenderer.render(world.physics().raw(), camera.camera().combined);
        }
    }

    // ------------------------------------------------------------- feedback

    @Override
    public void onJump(Player player) {
        particles.dust(player.x(), player.feetY(), 6, 0f, Palette.PLATFORM_EDGE);
    }

    @Override
    public void onLand(Player player) {
        particles.dust(player.x(), player.feetY(), 8, player.facingRight() ? -1f : 1f, Palette.PLATFORM_EDGE);
        if (player.kind() == io.bsoft.echo.player.PlayerKind.PLAYER) {
            playerRenderer.onLand();
        }
    }

    @Override
    public void onEchoCreated(Echo echo) {
        Player a = echo.avatar();
        particles.burst(a.x(), a.y(), 30, 6f, 0.6f, 0.2f, 0f, Palette.echoColor(echo.id()));
        flashColor.set(Palette.echoColor(echo.id()));
        flash = 0.5f;
    }

    @Override
    public void onEchoCompleted(Echo echo) {
        Player a = echo.avatar();
        particles.burst(a.x(), a.y(), 24, 3f, 0.8f, 0.15f, 2f, Palette.echoColor(echo.id()));
    }

    @Override
    public void onEchoDied(Echo echo) {
        Player a = echo.avatar();
        particles.burst(a.x(), a.y(), 30, 8f, 0.7f, 0.18f, -10f, Palette.echoColor(echo.id()));
        camera.shake(0.15f, 0.25f);
    }

    @Override
    public void onEchoLooped(Echo echo) {
        Player a = echo.avatar();
        particles.burst(a.x(), a.y(), 12, 4f, 0.5f, 0.15f, 0f, Palette.echoColor(echo.id()));
    }

    @Override
    public void onPlayerDied(Player player) {
        particles.burst(player.x(), player.y(), 40, 9f, 0.8f, 0.2f, -12f, Palette.PLAYER);
        camera.shake(0.35f, 0.4f);
        flashColor.set(Palette.HAZARD);
        flash = 0.6f;
    }

    @Override
    public void onPlayerRespawned(Player player) {
        particles.burst(player.x(), player.y(), 16, 3f, 0.5f, 0.15f, 0f, Palette.PLAYER);
    }

    @Override
    public void onLevelComplete(io.bsoft.echo.level.LevelResult result) {
        Player p = world.player();
        particles.burst(p.x(), p.y(), 60, 7f, 1.2f, 0.2f, -3f, Palette.EXIT);
        flashColor.set(Palette.EXIT);
        flash = 0.8f;
    }

    @Override
    public void onLevelReset(boolean countedAsRewind) {
        particles.clear();
        camera.snapTo(world.player().x(), world.player().y());
        flashColor.set(Color.WHITE);
        flash = countedAsRewind ? 0.4f : 0f;
    }

    @Override
    public void onDoorOpening(io.bsoft.echo.objects.Door door) {
        camera.shake(0.05f, 0.15f);
    }

    @Override
    public void onEnemyKilled(io.bsoft.echo.objects.Enemy enemy, Player killer) {
        var p = enemy.body().getPosition();
        particles.burst(p.x, p.y, 30, 6f, 0.7f, 0.18f, -8f, Palette.ENEMY);
        camera.shake(0.1f, 0.2f);
    }

    @Override
    public void dispose() {
        debugRenderer.dispose();
        if (world != null) {
            world.events().remove(this);
        }
    }
}
