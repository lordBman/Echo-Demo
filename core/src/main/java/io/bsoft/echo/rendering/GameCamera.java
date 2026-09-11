package io.bsoft.echo.rendering;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import io.bsoft.echo.GameConfig;

/**
 * World camera with smooth follow, dead zone, level bounds clamping, zoom and shake (spec §29).
 * Follows the real player only.
 */
public final class GameCamera {

    public static final class Config {
        public float deadZoneWidth = 4f;
        public float deadZoneHeight = 3f;
        public float followSpeed = 6f;
        public float zoom = 1f;
    }

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final Config config;
    private final Vector2 target = new Vector2();
    private final Vector2 position = new Vector2();
    private float levelWidth = GameConfig.VIEW_WIDTH;
    private float levelHeight = GameConfig.VIEW_HEIGHT;

    private float shakeTime;
    private float shakeDuration;
    private float shakeStrength;
    private final Vector2 shakeOffset = new Vector2();
    private long shakeSeed;

    public GameCamera(Config config) {
        this.config = config;
        this.viewport = new FitViewport(GameConfig.VIEW_WIDTH, GameConfig.VIEW_HEIGHT, camera);
        camera.zoom = config.zoom;
    }

    public OrthographicCamera camera() {
        return camera;
    }

    public Viewport viewport() {
        return viewport;
    }

    public void setLevelBounds(float width, float height) {
        this.levelWidth = width;
        this.levelHeight = height;
    }

    public void resize(int width, int height) {
        viewport.update(width, height, false);
    }

    /** Jumps straight to the target (level start, reset). */
    public void snapTo(float x, float y) {
        target.set(x, y);
        position.set(x, y);
        clamp(position);
        apply();
    }

    public void follow(float x, float y, float delta) {
        // Dead zone: only move the target when the subject leaves the central box.
        float halfW = config.deadZoneWidth / 2f;
        float halfH = config.deadZoneHeight / 2f;
        if (x > target.x + halfW) {
            target.x = x - halfW;
        } else if (x < target.x - halfW) {
            target.x = x + halfW;
        }
        if (y > target.y + halfH) {
            target.y = y - halfH;
        } else if (y < target.y - halfH) {
            target.y = y + halfH;
        }
        clamp(target);
        float t = 1f - (float) Math.exp(-config.followSpeed * delta);
        position.lerp(target, t);
        updateShake(delta);
        apply();
    }

    private void clamp(Vector2 p) {
        float halfViewW = viewport.getWorldWidth() * camera.zoom / 2f;
        float halfViewH = viewport.getWorldHeight() * camera.zoom / 2f;
        if (levelWidth <= halfViewW * 2f) {
            p.x = levelWidth / 2f;
        } else {
            p.x = MathUtils.clamp(p.x, halfViewW, levelWidth - halfViewW);
        }
        if (levelHeight <= halfViewH * 2f) {
            p.y = levelHeight / 2f;
        } else {
            p.y = MathUtils.clamp(p.y, halfViewH, levelHeight - halfViewH);
        }
    }

    public void shake(float strength, float duration) {
        shakeStrength = Math.max(shakeStrength, strength);
        shakeDuration = Math.max(shakeDuration, duration);
        shakeTime = shakeDuration;
    }

    private void updateShake(float delta) {
        if (shakeTime <= 0f) {
            shakeOffset.setZero();
            shakeStrength = 0f;
            return;
        }
        shakeTime = Math.max(0f, shakeTime - delta);
        float falloff = shakeDuration > 0f ? shakeTime / shakeDuration : 0f;
        // Cheap pseudo-random jitter; purely cosmetic so it does not need determinism.
        shakeSeed = shakeSeed * 6364136223846793005L + 1442695040888963407L;
        float rx = ((shakeSeed >>> 40) & 0xFFFF) / 65535f * 2f - 1f;
        float ry = ((shakeSeed >>> 20) & 0xFFFF) / 65535f * 2f - 1f;
        shakeOffset.set(rx, ry).scl(shakeStrength * falloff);
    }

    private void apply() {
        camera.zoom = config.zoom;
        camera.position.set(position.x + shakeOffset.x, position.y + shakeOffset.y, 0f);
        camera.update();
    }

    public void setZoom(float zoom) {
        config.zoom = MathUtils.clamp(zoom, 0.25f, 3f);
    }

    public float zoom() {
        return config.zoom;
    }
}
