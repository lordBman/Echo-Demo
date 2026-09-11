package io.bsoft.echo;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import io.bsoft.echo.audio.AudioManager;
import io.bsoft.echo.input.KeyBindings;
import io.bsoft.echo.level.LevelCatalog;
import io.bsoft.echo.level.LevelLoadException;
import io.bsoft.echo.player.PlayerConfig;
import io.bsoft.echo.rendering.Assets;
import io.bsoft.echo.save.SaveManager;
import io.bsoft.echo.screens.ErrorScreen;
import io.bsoft.echo.screens.GameplayScreen;
import io.bsoft.echo.screens.LevelSelectScreen;
import io.bsoft.echo.screens.MainMenuScreen;
import io.bsoft.echo.util.Log;

/**
 * Application root. Owns shared services (assets, audio, save data, level catalog) and
 * switches between screens (spec §36). Screens are small; all gameplay lives in
 * {@link io.bsoft.echo.world.GameWorld}.
 */
public final class EchoGame extends Game {

    private static final String TAG = "EchoGame";

    private Assets assets;
    private AudioManager audio;
    private SaveManager save;
    private LevelCatalog levels;
    private final KeyBindings bindings = KeyBindings.defaults();
    private final PlayerConfig playerConfig = PlayerConfig.defaults();
    private final LaunchOptions options;

    /** Developer launch options parsed from the command line. */
    public record LaunchOptions(int startLevel, boolean demo) {
        public static LaunchOptions defaults() {
            return new LaunchOptions(-1, false);
        }

        public static LaunchOptions parse(String[] args) {
            int level = -1;
            boolean demo = false;
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--level" -> level = i + 1 < args.length ? Integer.parseInt(args[++i]) - 1 : 0;
                    case "--demo" -> demo = true;
                    default -> {
                    }
                }
            }
            return new LaunchOptions(level, demo);
        }
    }

    public EchoGame() {
        this(LaunchOptions.defaults());
    }

    public EchoGame(LaunchOptions options) {
        this.options = options;
    }

    @Override
    public void create() {
        Gdx.app.setLogLevel(Application.LOG_DEBUG);
        Log.info(TAG, "ECHO starting — LibGDX " + com.badlogic.gdx.Version.VERSION + ", Java "
            + System.getProperty("java.version"));
        com.badlogic.gdx.physics.box2d.Box2D.init();
        assets = new Assets();
        audio = new AudioManager();
        save = new SaveManager();
        try {
            levels = LevelCatalog.fromIndex(Gdx.files.internal("levels/levels.json"));
        } catch (LevelLoadException e) {
            Log.error(TAG, "Could not load level catalog", e);
            setScreen(new ErrorScreen(this, "Could not load levels", e.getMessage()));
            return;
        }
        if (options.demo()) {
            startLevel(Math.max(0, options.startLevel()), true);
        } else if (options.startLevel() >= 0) {
            startLevel(options.startLevel());
        } else {
            showMainMenu();
        }
    }

    public boolean isDemo() {
        return options.demo();
    }

    public Assets assets() {
        return assets;
    }

    public AudioManager audio() {
        return audio;
    }

    public SaveManager save() {
        return save;
    }

    public LevelCatalog levels() {
        return levels;
    }

    public KeyBindings bindings() {
        return bindings;
    }

    public PlayerConfig playerConfig() {
        return playerConfig;
    }

    public void showMainMenu() {
        replaceScreen(new MainMenuScreen(this));
    }

    public void showLevelSelect() {
        replaceScreen(new LevelSelectScreen(this));
    }

    /** Starts the level at the given catalog index, showing an error screen if it fails to load. */
    public void startLevel(int index) {
        startLevel(index, false);
    }

    /** @param demo when true the level is played by the scripted demo input instead of the keyboard */
    public void startLevel(int index, boolean demo) {
        if (index < 0 || index >= levels.size()) {
            showLevelSelect();
            return;
        }
        try {
            replaceScreen(new GameplayScreen(this, index, demo));
        } catch (LevelLoadException e) {
            Log.error(TAG, "Failed to start level " + index, e);
            replaceScreen(new ErrorScreen(this, "Could not load level", e.getMessage()));
        }
    }

    /** Index of the first level not yet completed, or the last level when all are done. */
    public int firstUnfinishedLevel() {
        for (int i = 0; i < levels.size(); i++) {
            if (!save.isCompleted(levels.entry(i).id())) {
                return i;
            }
        }
        return Math.max(0, levels.size() - 1);
    }

    public boolean isUnlocked(int index) {
        return index == 0 || save.isCompleted(levels.entry(index - 1).id());
    }

    private void replaceScreen(Screen next) {
        Screen previous = getScreen();
        setScreen(next);
        if (previous != null) {
            previous.dispose();
        }
    }

    @Override
    public void dispose() {
        Screen current = getScreen();
        if (current != null) {
            current.dispose();
        }
        if (audio != null) {
            audio.dispose();
        }
        if (assets != null) {
            assets.dispose();
        }
        Log.info(TAG, "ECHO shut down");
    }
}

