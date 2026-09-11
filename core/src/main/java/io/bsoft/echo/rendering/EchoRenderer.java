package io.bsoft.echo.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import io.bsoft.echo.echo.Echo;
import io.bsoft.echo.echo.EchoTrail;
import io.bsoft.echo.player.Player;

/**
 * Draws Echoes as translucent, cool-tinted copies with a fading afterimage trail (spec §31, §32).
 * Trail rendering reads the pre-allocated ring buffer; nothing is allocated per frame.
 */
public final class EchoRenderer {

    private static final float ECHO_ALPHA = 0.6f;
    private static final float TRAIL_ALPHA = 0.22f;

    private final Assets assets;
    private final AvatarRenderer avatar;
    private final Color dark = new Color();
    private final Color paradoxColor = new Color();

    public EchoRenderer(Assets assets, AvatarRenderer avatar) {
        this.assets = assets;
        this.avatar = avatar;
    }

    public void render(SpriteBatch batch, Array<Echo> echoes, float time) {
        for (int i = 0; i < echoes.size; i++) {
            Echo echo = echoes.get(i);
            Color color = Palette.echoColor(echo.id());
            Player a = echo.avatar();
            if (echo.isParadox()) {
                // A paradoxed echo flickers between its colour and hazard red.
                float blend = 0.5f + 0.5f * (float) Math.sin(time * 18f + echo.id());
                paradoxColor.set(color).lerp(Palette.HAZARD, blend);
                color = paradoxColor;
            }
            drawTrail(batch, echo.trail(), a, color);
            // Soft aura marks it as a recording.
            batch.setColor(color.r, color.g, color.b, 0.18f);
            batch.draw(assets.glow, a.x() - 1.4f, a.y() - 1.4f, 2.8f, 2.8f);
            dark.set(color).mul(0.5f, 0.5f, 0.5f, 1f);
            float flicker = 1f + 0.04f * (float) Math.sin(time * 30f + echo.id());
            avatar.draw(batch, a, color, dark, AvatarRenderer.stretchFor(a) * flicker, ECHO_ALPHA, time);
            drawProgressRing(batch, echo, color);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawTrail(SpriteBatch batch, EchoTrail trail, Player a, Color color) {
        int n = trail.size();
        float w = a.config().width * 0.9f;
        float h = a.config().height * 0.9f;
        for (int i = 0; i < n; i++) {
            float t = (i + 1f) / (n + 1f);
            batch.setColor(color.r, color.g, color.b, TRAIL_ALPHA * t * t);
            float s = 0.6f + 0.4f * t;
            batch.draw(assets.white, trail.x(i) - w * s / 2f, trail.y(i) - h * s / 2f, w * s, h * s);
        }
    }

    /** Thin bar above the echo showing how much of its recording remains. */
    private void drawProgressRing(SpriteBatch batch, Echo echo, Color color) {
        Player a = echo.avatar();
        float remaining = 1f - echo.progress();
        float barW = 1f;
        float y = a.y() + a.config().height / 2f + 0.25f;
        batch.setColor(0f, 0f, 0f, 0.4f);
        batch.draw(assets.white, a.x() - barW / 2f, y, barW, 0.08f);
        batch.setColor(color.r, color.g, color.b, 0.9f);
        batch.draw(assets.white, a.x() - barW / 2f, y, barW * remaining, 0.08f);
    }
}
