package io.bsoft.echo.level;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import io.bsoft.echo.echo.EchoRules;

/**
 * Parsed, validated level definition (spec §27). Pure data; building the runtime
 * {@link Level} from it is {@link LevelBuilder}'s job.
 *
 * <p>Coordinate conventions: platforms are axis-aligned rectangles given by bottom-left corner
 * and size. Objects are given by {@code position} = (center x, bottom y) plus type-specific
 * properties; see {@code docs/LEVEL_FORMAT.md}.</p>
 */
public final class LevelData {

    /** Axis-aligned static platform. */
    public record Platform(float x, float y, float width, float height) {
    }

    /** Untyped object definition; factories read what they need. */
    public static final class ObjectDef {
        public final String type;
        public final String id;
        public final float x;
        public final float y;
        public final JsonValue props;

        public ObjectDef(String type, String id, float x, float y, JsonValue props) {
            this.type = type;
            this.id = id;
            this.x = x;
            this.y = y;
            this.props = props;
        }

        public float getFloat(String name, float defaultValue) {
            return props != null ? props.getFloat(name, defaultValue) : defaultValue;
        }

        public boolean getBoolean(String name, boolean defaultValue) {
            return props != null ? props.getBoolean(name, defaultValue) : defaultValue;
        }

        public String getString(String name, String defaultValue) {
            return props != null ? props.getString(name, defaultValue) : defaultValue;
        }

        /** Reads a [x, y] array, or returns null when absent. */
        public float[] getVec2(String name) {
            if (props == null) {
                return null;
            }
            JsonValue v = props.get(name);
            if (v == null || !v.isArray() || v.size < 2) {
                return null;
            }
            return new float[] {v.getFloat(0), v.getFloat(1)};
        }
    }

    public String id = "untitled";
    public String name = "Untitled";
    public int chapter = 1;
    public String hint = "";
    public float boundsWidth = 40f;
    public float boundsHeight = 22.5f;
    /** Feet position of the player at level start. */
    public float spawnX = 2f;
    public float spawnY = 1f;
    public final EchoRules echoRules = new EchoRules();
    public final Array<Platform> platforms = new Array<>();
    public final Array<ObjectDef> objects = new Array<>();
    public final Array<Challenge> challenges = new Array<>();

    @Override
    public String toString() {
        return "LevelData[" + id + " '" + name + "', " + platforms.size + " platforms, " + objects.size + " objects]";
    }
}
