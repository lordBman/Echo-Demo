package io.bsoft.echo.audio;

/** Every sound effect in the game; the file name is {@code sounds/<lowercase>.wav}. */
public enum SoundId {
    JUMP,
    LAND,
    RECORD_START,
    RECORD_STOP,
    ECHO_CREATE,
    ECHO_COMPLETE,
    ECHO_DIE,
    ECHO_LOOP,
    PLATE_PRESS,
    PLATE_RELEASE,
    DOOR_OPEN,
    DOOR_CLOSE,
    SWITCH,
    EXIT,
    PLAYER_DIE,
    ENEMY_DIE,
    REWIND,
    PARADOX,
    ERROR;

    public String fileName() {
        return "sounds/" + name().toLowerCase() + ".wav";
    }
}
