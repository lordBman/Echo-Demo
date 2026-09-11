package io.bsoft.echo.level;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.SerializationException;

/**
 * Parses level JSON into {@link LevelData} with explicit validation (spec §27, §52).
 * Parsing is done by hand on {@link JsonValue} so error messages name the offending field.
 */
public final class LevelLoader {

    private final JsonReader reader = new JsonReader();

    public LevelData load(FileHandle file) {
        if (file == null || !file.exists()) {
            throw new LevelLoadException("Level file not found: " + (file == null ? "null" : file.path()));
        }
        try {
            return parse(file.readString("UTF-8"), file.name());
        } catch (LevelLoadException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new LevelLoadException("Failed to read level " + file.path() + ": " + e.getMessage(), e);
        }
    }

    public LevelData parse(String json, String sourceName) {
        JsonValue root;
        try {
            root = reader.parse(json);
        } catch (SerializationException e) {
            throw new LevelLoadException(sourceName + ": invalid JSON: " + e.getMessage(), e);
        }
        if (root == null || !root.isObject()) {
            throw new LevelLoadException(sourceName + ": level must be a JSON object");
        }
        LevelData data = new LevelData();
        data.id = root.getString("id", null);
        if (data.id == null || data.id.isBlank()) {
            throw new LevelLoadException(sourceName + ": missing required field 'id'");
        }
        data.name = root.getString("name", data.id);
        data.chapter = root.getInt("chapter", 1);
        data.hint = root.getString("hint", "");

        JsonValue bounds = root.get("bounds");
        if (bounds != null) {
            data.boundsWidth = bounds.getFloat("width", data.boundsWidth);
            data.boundsHeight = bounds.getFloat("height", data.boundsHeight);
        }
        float[] spawn = vec2(root.get("playerSpawn"), sourceName, "playerSpawn");
        if (spawn == null) {
            throw new LevelLoadException(sourceName + ": missing required field 'playerSpawn' [x, y]");
        }
        data.spawnX = spawn[0];
        data.spawnY = spawn[1];

        data.echoRules.maxEchoes = root.getInt("echoLimit", data.echoRules.maxEchoes);
        data.echoRules.maxRecordingDuration = root.getFloat("recordingDuration", data.echoRules.maxRecordingDuration);
        data.echoRules.recordFromSpawn = root.getBoolean("recordFromSpawn", data.echoRules.recordFromSpawn);
        data.echoRules.loopingEchoes = root.getBoolean("loopingEchoes", data.echoRules.loopingEchoes);
        data.echoRules.echoesCollide = root.getBoolean("echoesCollide", data.echoRules.echoesCollide);
        data.echoRules.echoInheritance = root.getBoolean("echoInheritance", data.echoRules.echoInheritance);
        if (data.echoRules.maxEchoes < 0) {
            throw new LevelLoadException(sourceName + ": 'echoLimit' must be >= 0");
        }
        if (data.echoRules.maxRecordingDuration <= 0f) {
            throw new LevelLoadException(sourceName + ": 'recordingDuration' must be > 0");
        }

        JsonValue platforms = root.get("platforms");
        if (platforms != null) {
            int index = 0;
            for (JsonValue p = platforms.child; p != null; p = p.next, index++) {
                float w = p.getFloat("width", -1f);
                float h = p.getFloat("height", -1f);
                if (w <= 0f || h <= 0f) {
                    throw new LevelLoadException(sourceName + ": platforms[" + index
                            + "] needs positive 'width' and 'height'");
                }
                data.platforms.add(new LevelData.Platform(p.getFloat("x", 0f), p.getFloat("y", 0f), w, h));
            }
        }

        JsonValue objects = root.get("objects");
        if (objects != null) {
            int index = 0;
            for (JsonValue o = objects.child; o != null; o = o.next, index++) {
                String type = o.getString("type", null);
                if (type == null) {
                    throw new LevelLoadException(sourceName + ": objects[" + index + "] missing 'type'");
                }
                float[] pos = vec2(o.get("position"), sourceName, "objects[" + index + "].position");
                if (pos == null) {
                    throw new LevelLoadException(sourceName + ": objects[" + index + "] (" + type
                            + ") missing 'position' [x, y]");
                }
                String id = o.getString("id", type + "_" + index);
                data.objects.add(new LevelData.ObjectDef(type, id, pos[0], pos[1], o));
            }
        }

        JsonValue challenges = root.get("challenges");
        if (challenges != null) {
            int index = 0;
            for (JsonValue c = challenges.child; c != null; c = c.next, index++) {
                String typeName = c.getString("type", null);
                ChallengeType type = ChallengeType.fromJsonName(typeName);
                if (type == null) {
                    throw new LevelLoadException(sourceName + ": challenges[" + index + "] has unknown type '"
                            + typeName + "'");
                }
                data.challenges.add(new Challenge(type, c.getFloat("value", 0f)));
            }
        }
        boolean hasCompletion = false;
        for (Challenge c : data.challenges) {
            hasCompletion |= c.type() == ChallengeType.COMPLETE;
        }
        if (!hasCompletion) {
            data.challenges.insert(0, new Challenge(ChallengeType.COMPLETE, 0f));
        }
        return data;
    }

    private static float[] vec2(JsonValue v, String source, String field) {
        if (v == null) {
            return null;
        }
        if (!v.isArray() || v.size < 2) {
            throw new LevelLoadException(source + ": '" + field + "' must be an array [x, y]");
        }
        return new float[] {v.getFloat(0), v.getFloat(1)};
    }
}
