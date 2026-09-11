package io.bsoft.echo.level;

/** Thrown when a level file is missing or malformed. Carries a message fit for the screen. */
public final class LevelLoadException extends RuntimeException {

    public LevelLoadException(String message) {
        super(message);
    }

    public LevelLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
