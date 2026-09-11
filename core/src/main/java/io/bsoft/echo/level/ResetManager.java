package io.bsoft.echo.level;

import com.badlogic.gdx.utils.Array;

/**
 * Registry of everything that must return to its initial state on a rewind (spec §21).
 * Reset happens in registration order so dependencies (e.g. triggers before doors) hold.
 */
public final class ResetManager {

    private final Array<Resettable> resettables = new Array<>(true, 32);

    public void register(Resettable r) {
        if (r != null && !resettables.contains(r, true)) {
            resettables.add(r);
        }
    }

    public void unregister(Resettable r) {
        resettables.removeValue(r, true);
    }

    public void captureAll() {
        for (Resettable r : resettables) {
            r.captureInitialState();
        }
    }

    public void resetAll() {
        for (Resettable r : resettables) {
            r.reset();
        }
    }

    public int size() {
        return resettables.size;
    }

    public void clear() {
        resettables.clear();
    }
}
