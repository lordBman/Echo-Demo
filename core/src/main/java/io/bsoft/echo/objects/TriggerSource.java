package io.bsoft.echo.objects;

/**
 * A boolean condition that mechanisms (doors, elevators, lasers) react to (spec §23).
 * Implementations: pressure plates, switches, and the combinators in {@link Triggers}.
 */
public interface TriggerSource {

    boolean isActive();

    TriggerSource ALWAYS = () -> true;
    TriggerSource NEVER = () -> false;
}
