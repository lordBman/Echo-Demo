package io.bsoft.echo.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import io.bsoft.echo.GameConfig;
import io.bsoft.echo.level.Level;
import io.bsoft.echo.level.LevelData;
import io.bsoft.echo.objects.Door;
import io.bsoft.echo.objects.Enemy;
import io.bsoft.echo.objects.Exit;
import io.bsoft.echo.objects.Hazard;
import io.bsoft.echo.objects.LevelObject;
import io.bsoft.echo.objects.MovingPlatform;
import io.bsoft.echo.objects.PressurePlate;
import io.bsoft.echo.objects.PushableBox;
import io.bsoft.echo.objects.TimedSwitch;

/** Draws static geometry and every level object from its public state. */
public final class LevelRenderer {

    private static final float EDGE = 0.12f;

    private final Assets assets;
    private final Color tmp = new Color();
    private final com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();

    public LevelRenderer(Assets assets) {
        this.assets = assets;
    }

    public void render(SpriteBatch batch, Level level, float time) {
        drawBackground(batch, level);
        for (LevelData.Platform p : level.data().platforms) {
            drawPlatform(batch, p.x(), p.y(), p.width(), p.height());
        }
        for (LevelObject o : level.objects()) {
            if (o instanceof PressurePlate plate) {
                drawPlate(batch, plate);
            } else if (o instanceof Door door) {
                drawDoor(batch, door);
            } else if (o instanceof Exit exit) {
                drawExit(batch, exit, time);
            } else if (o instanceof MovingPlatform platform) {
                drawMovingPlatform(batch, platform);
            } else if (o instanceof PushableBox box) {
                drawBox(batch, box);
            } else if (o instanceof Hazard hazard) {
                drawHazard(batch, hazard, time);
            } else if (o instanceof TimedSwitch sw) {
                drawSwitch(batch, sw);
            } else if (o instanceof Enemy enemy) {
                drawEnemy(batch, enemy, time);
            }
        }
        batch.setColor(Color.WHITE);
    }

    private void drawBackground(SpriteBatch batch, Level level) {
        float w = level.width();
        float h = level.height();
        int bands = 12;
        for (int i = 0; i < bands; i++) {
            float t = (float) i / (bands - 1);
            tmp.set(Palette.BACKGROUND_BOTTOM).lerp(Palette.BACKGROUND_TOP, t);
            batch.setColor(tmp);
            batch.draw(assets.white, -2f, h * i / bands, w + 4f, h / bands + 0.01f);
        }
        // Faint grid every meter so distances are readable (temporal puzzles are about timing).
        batch.setColor(1f, 1f, 1f, 0.03f);
        for (int x = 0; x <= (int) w; x++) {
            batch.draw(assets.white, x, 0f, 0.02f, h);
        }
        for (int y = 0; y <= (int) h; y++) {
            batch.draw(assets.white, 0f, y, w, 0.02f);
        }
    }

    private void drawPlatform(SpriteBatch batch, float x, float y, float w, float h) {
        batch.setColor(Palette.PLATFORM);
        batch.draw(assets.white, x, y, w, h);
        batch.setColor(Palette.PLATFORM_EDGE);
        batch.draw(assets.white, x, y + h - EDGE, w, EDGE);
    }

    private void drawPlate(SpriteBatch batch, PressurePlate plate) {
        float press = plate.pressAmount();
        float h = PressurePlate.HEIGHT - PressurePlate.PRESS_DEPTH * press;
        tmp.set(Palette.PLATE).lerp(Palette.PLATE_ACTIVE, press);
        // Base rim
        batch.setColor(Palette.PLATFORM_EDGE);
        batch.draw(assets.white, plate.x() - plate.width() / 2f - 0.1f, plate.y(), plate.width() + 0.2f, 0.06f);
        batch.setColor(tmp);
        batch.draw(assets.white, plate.x() - plate.width() / 2f, plate.y(), plate.width(), h);
        if (press > 0.1f) {
            batch.setColor(tmp.r, tmp.g, tmp.b, 0.35f * press);
            batch.draw(assets.glow, plate.x() - 1.5f, plate.y() - 1.2f, 3f, 3f);
        }
    }

    private void drawDoor(SpriteBatch batch, Door door) {
        float openness = door.openness();
        float visibleHeight = door.height() * (1f - openness);
        tmp.set(Palette.DOOR).lerp(Palette.DOOR_OPEN, openness);
        // Frame
        batch.setColor(Palette.PLATFORM_EDGE);
        batch.draw(assets.white, door.x() - door.width() / 2f - 0.15f, door.y(), 0.15f, door.height() + 0.3f);
        batch.draw(assets.white, door.x() + door.width() / 2f, door.y(), 0.15f, door.height() + 0.3f);
        batch.draw(assets.white, door.x() - door.width() / 2f - 0.15f, door.y() + door.height(),
                door.width() + 0.3f, 0.3f);
        // Slab slides up into the frame.
        if (visibleHeight > 0.01f) {
            batch.setColor(tmp);
            batch.draw(assets.white, door.x() - door.width() / 2f, door.y(), door.width(), visibleHeight);
            batch.setColor(1f, 1f, 1f, 0.25f);
            batch.draw(assets.white, door.x() - door.width() / 2f, door.y(), door.width() * 0.25f, visibleHeight);
        }
        // Indicator light above the door: red closed, green open.
        batch.setColor(tmp);
        batch.draw(assets.circle, door.x() - 0.15f, door.y() + door.height() + 0.45f, 0.3f, 0.3f);
    }

    private void drawExit(SpriteBatch batch, Exit exit, float time) {
        float pulse = 0.75f + 0.25f * MathUtils.sin(time * 3f);
        batch.setColor(Palette.EXIT.r, Palette.EXIT.g, Palette.EXIT.b, 0.35f * pulse);
        batch.draw(assets.glow, exit.x() - 2f, exit.y() + Exit.HEIGHT / 2f - 2f, 4f, 4f);
        batch.setColor(Palette.EXIT.r, Palette.EXIT.g, Palette.EXIT.b, 0.85f);
        batch.draw(assets.white, exit.x() - Exit.WIDTH / 2f, exit.y(), Exit.WIDTH, Exit.HEIGHT);
        batch.setColor(1f, 1f, 1f, 0.9f);
        batch.draw(assets.white, exit.x() - Exit.WIDTH / 2f + 0.15f, exit.y() + 0.15f, Exit.WIDTH - 0.3f,
                Exit.HEIGHT - 0.3f);
        batch.setColor(Palette.BACKGROUND_TOP.r, Palette.BACKGROUND_TOP.g, Palette.BACKGROUND_TOP.b, 0.9f);
        batch.draw(assets.white, exit.x() - Exit.WIDTH / 2f + 0.3f, exit.y() + 0.3f, Exit.WIDTH - 0.6f,
                Exit.HEIGHT - 0.6f);
    }

    private void drawMovingPlatform(SpriteBatch batch, MovingPlatform platform) {
        Vector2 p = platform.body().getPosition();
        float w = platform.width();
        float h = platform.height();
        // Rail between the endpoints.
        batch.setColor(1f, 1f, 1f, 0.08f);
        Vector2 a = platform.pointA();
        Vector2 b = platform.pointB();
        float len = a.dst(b);
        if (len > 0.01f) {
            float angle = MathUtils.atan2(b.y - a.y, b.x - a.x) * MathUtils.radiansToDegrees;
            batch.draw(assets.white, a.x, a.y - 0.03f, 0f, 0.03f, len, 0.06f, 1f, 1f, angle, 0, 0, 2, 2, false,
                    false);
        }
        drawPlatform(batch, p.x - w / 2f, p.y - h / 2f, w, h);
        batch.setColor(platform.mode() == MovingPlatform.Mode.TRIGGERED ? Palette.PLATE_ACTIVE : Palette.EXIT);
        batch.draw(assets.white, p.x - w / 2f, p.y - h / 2f, w, 0.06f);
    }

    private void drawBox(SpriteBatch batch, PushableBox box) {
        Vector2 p = box.body().getPosition();
        float s = box.size();
        float angle = box.body().getAngle() * MathUtils.radiansToDegrees;
        batch.setColor(Palette.BOX);
        batch.draw(assets.white, p.x - s / 2f, p.y - s / 2f, s / 2f, s / 2f, s, s, 1f, 1f, angle, 0, 0, 2, 2,
                false, false);
        batch.setColor(0f, 0f, 0f, 0.25f);
        float inner = s * 0.7f;
        batch.draw(assets.white, p.x - inner / 2f, p.y - inner / 2f, inner / 2f, inner / 2f, inner, inner, 1f, 1f,
                angle, 0, 0, 2, 2, false, false);
    }

    private void drawHazard(SpriteBatch batch, Hazard hazard, float time) {
        Vector2 p = hazard.body().getPosition();
        float w = hazard.width();
        float h = hazard.height();
        boolean armed = hazard.isArmed();
        Color c = armed ? Palette.HAZARD : Palette.HAZARD_OFF;
        if (armed) {
            float pulse = 0.6f + 0.4f * MathUtils.sin(time * 12f);
            batch.setColor(c.r, c.g, c.b, 0.3f * pulse);
            batch.draw(assets.glow, p.x - w, p.y - h, w * 2f, h * 2f);
        }
        batch.setColor(c);
        batch.draw(assets.white, p.x - w / 2f, p.y - h / 2f, w, h);
        // Stripes.
        batch.setColor(0f, 0f, 0f, 0.35f);
        int stripes = Math.max(1, (int) (w / 0.5f));
        for (int i = 0; i < stripes; i++) {
            batch.draw(assets.white, p.x - w / 2f + i * (w / stripes), p.y - h / 2f, w / stripes / 2f, h);
        }
    }

    private void drawSwitch(SpriteBatch batch, TimedSwitch sw) {
        boolean active = sw.isActive();
        // Housing with a darker panel and a lever that tilts when active.
        batch.setColor(Palette.PLATFORM_EDGE);
        batch.draw(assets.white, sw.x() - TimedSwitch.WIDTH / 2f, sw.y(), TimedSwitch.WIDTH, TimedSwitch.HEIGHT);
        batch.setColor(Palette.PLATFORM);
        batch.draw(assets.white, sw.x() - TimedSwitch.WIDTH / 2f + 0.08f, sw.y() + 0.08f, TimedSwitch.WIDTH - 0.16f,
                TimedSwitch.HEIGHT - 0.16f);
        tmp.set(active ? Palette.PLATE_ACTIVE : Palette.SWITCH);
        batch.setColor(tmp);
        float angle = active ? -35f : 35f;
        batch.draw(assets.white, sw.x() - 0.08f, sw.y() + 0.5f, 0.08f, 0f, 0.16f, 0.6f, 1f, 1f, angle, 0, 0, 2, 2,
                false, false);
        batch.setColor(tmp);
        batch.draw(assets.circle, sw.x() - 0.14f, sw.y() + 0.36f, 0.28f, 0.28f);
        if (sw.isPlayerNearby()) {
            drawKeyPrompt(batch, "E", sw.x(), sw.y() + TimedSwitch.HEIGHT + 0.55f);
        }
        if (sw.isTimed() && active) {
            float frac = sw.remaining() / sw.holdTime();
            batch.setColor(Palette.PLATE_ACTIVE);
            batch.draw(assets.white, sw.x() - TimedSwitch.WIDTH / 2f, sw.y() + TimedSwitch.HEIGHT + 0.1f,
                    TimedSwitch.WIDTH * frac, 0.1f);
        }
    }

    /** Draws a key cap with a letter in world space (font temporarily scaled to meters). */
    private void drawKeyPrompt(SpriteBatch batch, String key, float cx, float cy) {
        float size = 0.7f;
        batch.setColor(Palette.UI_TEXT);
        batch.draw(assets.white, cx - size / 2f, cy - size / 2f, size, size);
        batch.setColor(Palette.BACKGROUND_TOP);
        batch.draw(assets.white, cx - size / 2f + 0.06f, cy - size / 2f + 0.06f, size - 0.12f, size - 0.12f);
        var font = assets.fontLarge;
        float oldX = font.getScaleX();
        float oldY = font.getScaleY();
        font.getData().setScale(oldX / GameConfig.PIXELS_PER_METER, oldY / GameConfig.PIXELS_PER_METER);
        font.setColor(Palette.UI_TEXT);
        layout.setText(font, key);
        font.draw(batch, layout, cx - layout.width / 2f, cy + layout.height / 2f);
        font.getData().setScale(oldX, oldY);
        batch.setColor(Color.WHITE);
    }

    private void drawEnemy(SpriteBatch batch, Enemy enemy, float time) {
        if (!enemy.isAlive()) {
            return;
        }
        Vector2 p = enemy.body().getPosition();
        float bob = 0.05f * MathUtils.sin(time * 10f);
        batch.setColor(Palette.ENEMY);
        batch.draw(assets.white, p.x - Enemy.WIDTH / 2f, p.y - Enemy.HEIGHT / 2f + bob, Enemy.WIDTH,
                Enemy.HEIGHT - bob);
        // Eye looking in the direction of travel.
        batch.setColor(Color.WHITE);
        float eyeX = p.x + enemy.direction() * 0.2f;
        batch.draw(assets.circle, eyeX - 0.12f, p.y + 0.1f, 0.24f, 0.24f);
        batch.setColor(Color.BLACK);
        batch.draw(assets.circle, eyeX - 0.05f + enemy.direction() * 0.04f, p.y + 0.16f, 0.1f, 0.1f);
    }
}
