package io.bsoft.echo.echo;

/** Lifecycle notifications from the {@link EchoManager}. */
public interface EchoListener {

    default void onEchoCreated(Echo echo) {
    }

    default void onEchoCompleted(Echo echo) {
    }

    default void onEchoDied(Echo echo) {
    }

    default void onEchoLooped(Echo echo) {
    }

    default void onEchoRejected(EchoRecording recording, String reason) {
    }

    /** The echo's replay diverged from its recording: the world changed under it. */
    default void onEchoParadox(Echo echo) {
    }
}
