package io.bsoft.echo.rendering;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.bsoft.echo.player.Player;

/** Draws the real player: fully opaque, warm colour, subtle landing squash, recording halo. */
public final class PlayerRenderer {

    private static final float SQUASH_DURATION = 0.15f;

    private final Assets assets;
    private final AvatarRenderer avatar;
    private float squashTimer;

    public PlayerRenderer(Assets assets, AvatarRenderer avatar) {
        this.assets = assets;
        this.avatar = avatar;
    }

    public void onLand() {
        squashTimer = SQUASH_DURATION;
    }

    public void update(float delta) {
        squashTimer = Math.max(0f, squashTimer - delta);
    }

    public void render(SpriteBatch batch, Player player, boolean recording, float time) {
        float stretch = AvatarRenderer.stretchFor(player);
        if (squashTimer > 0f) {
            float t = squashTimer / SQUASH_DURATION;
            stretch *= 1f - 0.2f * t;
        }
        if (recording) {
            batch.setColor(Palette.RECORDING.r, Palette.RECORDING.g, Palette.RECORDING.b,
                    0.25f + 0.1f * (float) Math.sin(time * 8f));
            batch.draw(assets.glow, player.x() - 1.6f, player.y() - 1.6f, 3.2f, 3.2f);
        }
        avatar.draw(batch, player, Palette.PLAYER, Palette.PLAYER_DARK, stretch, 1f, time);
    }
}
