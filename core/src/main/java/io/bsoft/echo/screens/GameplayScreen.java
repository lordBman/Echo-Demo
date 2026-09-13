package io.bsoft.echo.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import io.bsoft.echo.EchoGame;
import io.bsoft.echo.input.DemoInputSource;
import io.bsoft.echo.input.InputSource;
import io.bsoft.echo.input.KeyboardInputSource;
import io.bsoft.echo.input.TouchInputSource;
import io.bsoft.echo.level.LevelData;
import io.bsoft.echo.level.LevelResult;
import io.bsoft.echo.rendering.GameCamera;
import io.bsoft.echo.rendering.GameRenderer;
import io.bsoft.echo.rendering.Palette;
import io.bsoft.echo.save.LevelProgress;
import io.bsoft.echo.ui.DebugOverlay;
import io.bsoft.echo.ui.Hud;
import io.bsoft.echo.ui.PauseOverlay;
import io.bsoft.echo.ui.ResultsOverlay;
import io.bsoft.echo.ui.TouchOverlay;
import io.bsoft.echo.ui.UiCanvas;
import io.bsoft.echo.util.Log;
import io.bsoft.echo.world.GameEventListener;
import io.bsoft.echo.world.GameWorld;

/**
 * Hosts one level: wires the simulation ({@link GameWorld}) to the presentation
 * ({@link GameRenderer}, {@link Hud}) and handles the meta keys (pause, debug toggles).
 * Deliberately thin — see spec §56.
 */
public final class GameplayScreen extends ScreenAdapter implements GameEventListener {

    private static final String TAG = "Gameplay";

    private enum Mode {
        PLAYING,
        PAUSED,
        RESULTS
    }

    private final EchoGame game;
    private final int levelIndex;
    private final LevelData levelData;
    private final InputSource input;
    private final GameWorld world;
    private final GameRenderer renderer;
    private final UiCanvas canvas;
    private final Hud hud;
    private final DebugOverlay debug;
    private final PauseOverlay pause;
    private final ResultsOverlay results;
    private TouchOverlay touchOverlay;
    private Mode mode = Mode.PLAYING;
    private LevelProgress progress;
    private final Vector2 temp = new Vector2();

    public GameplayScreen(EchoGame game, int levelIndex) {
        this(game, levelIndex, false);
    }

    public GameplayScreen(EchoGame game, int levelIndex, boolean demo) {
        this.game = game;
        this.levelIndex = levelIndex;
        this.levelData = game.levels().load(levelIndex);

        boolean isAndroid = Gdx.app.getType() == Application.ApplicationType.Android;

        if (demo) {
            DemoInputSource script = new DemoInputSource();
            this.input = script;
            this.world = new GameWorld(levelData, script, game.playerConfig());
            script.attach(world);
        } else {
            if (isAndroid) {
                this.input = new TouchInputSource();
            } else {
                this.input = new KeyboardInputSource(game.bindings());
            }
            this.world = new GameWorld(levelData, input, game.playerConfig());
        }

        this.renderer = new GameRenderer(game.assets(), new GameCamera(new GameCamera.Config()));
        this.canvas = new UiCanvas(game.assets());
        this.hud = new Hud(game.assets(), canvas);
        this.debug = new DebugOverlay(game.assets(), canvas);
        this.pause = new PauseOverlay(game.assets(), canvas);
        this.results = new ResultsOverlay(game.assets(), canvas);

        if (isAndroid && !demo) {
            this.touchOverlay = new TouchOverlay(game.assets(), canvas, (TouchInputSource) input);
        }

        renderer.attach(world);
        hud.attach(world);
        world.events().add(game.audio());
        world.events().add(this);
        Log.info(TAG, "Started " + levelData);
    }

    @Override
    public void render(float delta) {
        if (input instanceof KeyboardInputSource) {
            ((KeyboardInputSource) input).poll();
        } else if (touchOverlay != null) {
            touchOverlay.update();
        }

        handleMetaInput();
        if (mode == Mode.PLAYING) {
            world.update(delta);
            renderer.update(delta);
            hud.update(delta);
        } else if (mode == Mode.RESULTS) {
            renderer.update(delta);
            results.update(delta);
        }

        ScreenUtils.clear(Palette.BACKGROUND_BOTTOM);
        renderer.render();
        hud.render();
        debug.render(world, renderer.particles().count());
        if (mode == Mode.PAUSED) {
            pause.render();
            if (touchOverlay != null) touchOverlay.render();
        } else if (mode == Mode.RESULTS) {
            results.render();
        } else if (mode == Mode.PLAYING && touchOverlay != null) {
            touchOverlay.render();
        }
    }

    private void handleMetaInput() {
        if (input instanceof KeyboardInputSource) {
            KeyboardInputSource kbd = (KeyboardInputSource) input;
            if (kbd.debugOverlayJustPressed()) {
                debug.toggle();
            }
            if (kbd.debugPhysicsJustPressed()) {
                renderer.toggleDebugPhysics();
            }
        }

        switch (mode) {
            case PLAYING -> {
                if (input.pauseJustPressed()) {
                    setMode(Mode.PAUSED);
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
                    hud.showHint();
                }
            }
            case PAUSED -> {
                if (input.pauseJustPressed()) {
                    setMode(Mode.PLAYING);
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.R) || input.reset()) {
                    world.reset(true);
                    setMode(Mode.PLAYING);
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
                    game.showLevelSelect();
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
                    game.showMainMenu();
                }

                if (Gdx.input.justTouched()) {
                    float tx = Gdx.input.getX();
                    float ty = Gdx.input.getY();
                    canvas.viewport().unproject(temp.set(tx, ty));
                    float cx = UiCanvas.WIDTH / 2f;
                    float y = UiCanvas.HEIGHT / 2f + 10f;

                    if (Math.abs(temp.x - cx) < 200f) {
                        if (Math.abs(temp.y - (y - 15f)) < 20f) setMode(Mode.PLAYING); // Resume
                        else if (Math.abs(temp.y - (y - 55f)) < 20f) { // Restart
                            world.reset(true);
                            setMode(Mode.PLAYING);
                        }
                        else if (Math.abs(temp.y - (y - 95f)) < 20f) game.showLevelSelect();
                        else if (Math.abs(temp.y - (y - 135f)) < 20f) game.showMainMenu();
                    }
                }
                input.consumePresses();
            }
            case RESULTS -> {
                boolean hasNext = levelIndex + 1 < game.levels().size();
                if (hasNext && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                    game.startLevel(levelIndex + 1);
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.R) || input.reset()) {
                    world.reset(false);
                    setMode(Mode.PLAYING);
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.L)
                        || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                    game.showLevelSelect();
                }

                if (Gdx.input.justTouched()) {
                    float tx = Gdx.input.getX();
                    float ty = Gdx.input.getY();
                    canvas.viewport().unproject(temp.set(tx, ty));
                    float cx = UiCanvas.WIDTH / 2f;

                    if (temp.y < 120f) {
                        if (hasNext && temp.x < cx - 100f) game.startLevel(levelIndex + 1);
                        else if (temp.x > cx - 100f && temp.x < cx + 100f) {
                            world.reset(false);
                            setMode(Mode.PLAYING);
                        }
                        else if (temp.x > cx + 100f) game.showLevelSelect();
                    }
                }
                input.consumePresses();
            }
        }
    }

    private void setMode(Mode next) {
        mode = next;
        input.setEnabled(next == Mode.PLAYING);
    }

    @Override
    public void onLevelComplete(LevelResult result) {
        progress = game.save().record(result, levelData.challenges);
        boolean hasNext = levelIndex + 1 < game.levels().size();
        results.show(result, levelData.challenges, progress, hasNext);
        setMode(Mode.RESULTS);
        Log.info(TAG, "Level complete: " + result);
    }

    @Override
    public void resize(int width, int height) {
        renderer.camera().resize(width, height);
        canvas.resize(width, height);
    }

    @Override
    public void pause() {
        if (mode == Mode.PLAYING) {
            setMode(Mode.PAUSED);
        }
    }

    @Override
    public void dispose() {
        world.events().remove(game.audio());
        renderer.dispose();
        world.dispose();
    }
}
