package io.bsoft.echo.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import io.bsoft.echo.player.Player;
import io.bsoft.echo.player.PlayerConfig;
import io.bsoft.echo.player.PlayerState;

/**
 * Shared drawing of a character body with squash/stretch, facing and simple eyes.
 * Used by both {@link PlayerRenderer} (opaque) and {@link EchoRenderer} (translucent).
 */
public final class AvatarRenderer {

    private final Assets assets;

    public AvatarRenderer(Assets assets) {
        this.assets = assets;
    }

    /**
     * @param stretch vertical stretch factor (1 = neutral); width is scaled inversely
     * @param alpha   overall opacity
     */
    public void draw(SpriteBatch batch, Player player, Color body, Color dark, float stretch, float alpha,
                     float time) {
        PlayerConfig c = player.config();
        float w = c.width / stretch;
        float h = c.height * stretch;
        float x = player.x();
        float feet = player.feetY();
        PlayerState state = player.state();

        if (state == PlayerState.DEAD) {
            return;
        }
        // Idle breathing.
        if (state == PlayerState.IDLE) {
            h += 0.03f * MathUtils.sin(time * 4f);
        }
        batch.setColor(body.r, body.g, body.b, alpha);
        batch.draw(assets.white, x - w / 2f, feet, w, h);
        // Darker lower band for a hint of shading.
        batch.setColor(dark.r, dark.g, dark.b, alpha * 0.6f);
        batch.draw(assets.white, x - w / 2f, feet, w, h * 0.18f);

        // Eyes indicate facing.
        int dir = player.facingRight() ? 1 : -1;
        float eyeY = feet + h * 0.72f;
        float eyeSize = 0.16f;
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(assets.circle, x + dir * 0.12f - eyeSize / 2f, eyeY, eyeSize, eyeSize);
        batch.draw(assets.circle, x + dir * 0.32f - eyeSize / 2f, eyeY, eyeSize, eyeSize);
        batch.setColor(0.05f, 0.05f, 0.1f, alpha);
        float pupil = 0.07f;
        batch.draw(assets.circle, x + dir * 0.15f - pupil / 2f, eyeY + 0.04f, pupil, pupil);
        batch.draw(assets.circle, x + dir * 0.35f - pupil / 2f, eyeY + 0.04f, pupil, pupil);
    }

    /** Stretch factor derived from vertical velocity and state for a lively feel. */
    public static float stretchFor(Player player) {
        float vy = player.velocity().y;
        return switch (player.state()) {
            case JUMPING -> 1f + MathUtils.clamp(vy / 60f, 0f, 0.18f);
            case FALLING -> 1f + MathUtils.clamp(-vy / 80f, 0f, 0.12f);
            default -> 1f;
        };
    }
}
