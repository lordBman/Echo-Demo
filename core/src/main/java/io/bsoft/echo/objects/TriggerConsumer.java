package io.bsoft.echo.objects;

/** A level object that reacts to a {@link TriggerSource}; bound after all objects exist. */
public interface TriggerConsumer {

    void setTrigger(TriggerSource trigger);
}
