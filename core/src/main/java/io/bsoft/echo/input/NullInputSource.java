package io.bsoft.echo.input;

/** An input source that never does anything. Useful for tests and idle avatars. */
public final class NullInputSource implements InputSource {

    public static final NullInputSource INSTANCE = new NullInputSource();

    private NullInputSource() {
    }

    @Override
    public boolean left() {
        return false;
    }

    @Override
    public boolean right() {
        return false;
    }

    @Override
    public boolean jump() {
        return false;
    }

    @Override
    public boolean interact() {
        return false;
    }

    @Override
    public boolean record() {
        return false;
    }

    @Override
    public boolean createEcho() {
        return false;
    }

    @Override
    public boolean reset() {
        return false;
    }
}
