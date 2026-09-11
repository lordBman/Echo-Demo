package io.bsoft.echo.level;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import io.bsoft.echo.util.Log;

/**
 * Ordered list of level files (from {@code levels/levels.json}) and lazy loading of their
 * data. This is the "LevelManager" of spec §55: progression order lives in data, not code.
 */
public final class LevelCatalog {

    private static final String TAG = "LevelCatalog";

    public record Entry(String id, String name, int chapter, FileHandle file) {
    }

    private final Array<Entry> entries = new Array<>();
    private final LevelLoader loader = new LevelLoader();

    public static LevelCatalog fromIndex(FileHandle indexFile) {
        LevelCatalog catalog = new LevelCatalog();
        if (!indexFile.exists()) {
            throw new LevelLoadException("Level index not found: " + indexFile.path());
        }
        JsonValue root = new JsonReader().parse(indexFile.readString("UTF-8"));
        JsonValue levels = root.get("levels");
        if (levels == null || !levels.isArray()) {
            throw new LevelLoadException(indexFile.path() + ": expected a 'levels' array");
        }
        for (JsonValue entry = levels.child; entry != null; entry = entry.next) {
            String path = entry.isString() ? entry.asString() : entry.getString("file", null);
            if (path == null) {
                throw new LevelLoadException(indexFile.path() + ": each entry needs a 'file'");
            }
            FileHandle file = indexFile.parent().child(path);
            // Read header fields eagerly so menus can list levels without building them.
            LevelData data = catalog.loader.load(file);
            catalog.entries.add(new Entry(data.id, data.name, data.chapter, file));
        }
        Log.info(TAG, "Loaded catalog with " + catalog.entries.size + " levels");
        return catalog;
    }

    public Array<Entry> entries() {
        return entries;
    }

    public int size() {
        return entries.size;
    }

    public Entry entry(int index) {
        return entries.get(index);
    }

    public int indexOf(String levelId) {
        for (int i = 0; i < entries.size; i++) {
            if (entries.get(i).id().equals(levelId)) {
                return i;
            }
        }
        return -1;
    }

    public LevelData load(int index) {
        return loader.load(entries.get(index).file());
    }

    public LevelLoader loader() {
        return loader;
    }
}
