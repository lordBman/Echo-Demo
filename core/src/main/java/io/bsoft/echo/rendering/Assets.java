package io.bsoft.echo.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

/**
 * Owns every GPU resource (spec §40). The prototype uses procedurally generated textures
 * so it runs with zero external art; swapping in a TextureAtlas later only touches this class.
 */
public final class Assets implements Disposable {

    public final Texture white;
    public final Texture circle;
    public final Texture glow;
    public final BitmapFont font;
    public final BitmapFont fontLarge;
    public final BitmapFont fontTitle;
    public final SpriteBatch batch;

    public Assets() {
        white = solid(Color.WHITE);
        circle = circle(64);
        glow = glow(128);
        font = newFont(1f);
        fontLarge = newFont(1.6f);
        fontTitle = newFont(3.2f);
        batch = new SpriteBatch();
    }

    private static BitmapFont newFont(float scale) {
        BitmapFont f = new BitmapFont();
        f.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        f.getData().setScale(scale);
        f.setUseIntegerPositions(false);
        return f;
    }

    private static Texture solid(Color color) {
        Pixmap p = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
        p.setColor(color);
        p.fill();
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    private static Texture circle(int size) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 0);
        p.fill();
        p.setColor(Color.WHITE);
        p.fillCircle(size / 2, size / 2, size / 2 - 1);
        Texture t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        p.dispose();
        return t;
    }

    /** Radial falloff used for soft lights and afterimages. */
    private static Texture glow(int size) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        float r = size / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x + 0.5f - r) / r;
                float dy = (y + 0.5f - r) / r;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float a = Math.max(0f, 1f - d);
                a = a * a;
                p.drawPixel(x, y, Color.rgba8888(1f, 1f, 1f, a));
            }
        }
        Texture t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        p.dispose();
        return t;
    }

    @Override
    public void dispose() {
        white.dispose();
        circle.dispose();
        glow.dispose();
        font.dispose();
        fontLarge.dispose();
        fontTitle.dispose();
        batch.dispose();
    }
}
