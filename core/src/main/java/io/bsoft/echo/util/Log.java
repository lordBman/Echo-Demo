package io.bsoft.echo.util;

import com.badlogic.gdx.Gdx;

/**
 * Thin logging facade. Routes through {@code Gdx.app} when an application exists
 * and falls back to stdout/stderr so headless tests and tools can log too.
 */
public final class Log {

    private Log() {
    }

    public static void info(String tag, String message) {
        if (Gdx.app != null) {
            Gdx.app.log(tag, message);
        } else {
            System.out.println("[" + tag + "] " + message);
        }
    }

    public static void debug(String tag, String message) {
        if (Gdx.app != null) {
            Gdx.app.debug(tag, message);
        } else {
            System.out.println("[" + tag + "] " + message);
        }
    }

    public static void error(String tag, String message) {
        if (Gdx.app != null) {
            Gdx.app.error(tag, message);
        } else {
            System.err.println("[" + tag + "] " + message);
        }
    }

    public static void error(String tag, String message, Throwable t) {
        if (Gdx.app != null) {
            Gdx.app.error(tag, message, t);
        } else {
            System.err.println("[" + tag + "] " + message);
            t.printStackTrace();
        }
    }
}
