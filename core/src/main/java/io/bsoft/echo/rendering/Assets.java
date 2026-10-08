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
    public final Texture iconJump;
    public final Texture iconRecord;
    public final Texture iconEcho;
    public final Texture iconPause;
    public final Texture iconReset;
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
        iconJump = jumpIcon(64);
        iconRecord = recordIcon(64);
        iconEcho = echoIcon(64);
        iconPause = pauseIcon(64);
        iconReset = resetIcon(64);
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

    private static Texture pauseIcon(int size) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 0);
        p.fill();
        p.setColor(Color.WHITE);
        int w = size / 6;
        int h = size / 2;
        p.fillRectangle(size / 2 - w - w / 2, size / 2 - h / 2, w, h);
        p.fillRectangle(size / 2 + w / 2, size / 2 - h / 2, w, h);
        Texture t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        p.dispose();
        return t;
    }

    private static Texture recordIcon(int size) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 0);
        p.fill();
        p.setColor(Color.WHITE);
        // Camera body
        p.fillRectangle(size / 4, size / 3, size / 2, size / 3);
        // Lens
        p.fillCircle(size / 2, size / 2, size / 8);
        // Shutter button
        p.fillRectangle(size / 3, size / 3 - 4, size / 6, 4);
        Texture t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        p.dispose();
        return t;
    }

    private static Texture jumpIcon(int size) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 0);
        p.fill();

        // Silhouette 1 (lower/back) - moved to right
        p.setColor(1, 1, 1, 0.4f);
        drawSilhouette(p, size / 2 + 12, size / 2 + 8, size / 8);

        // Silhouette 2 (higher/forward) - moved to left
        p.setColor(1, 1, 1, 1f);
        drawSilhouette(p, size / 2 - 4, size / 2 - 4, size / 8);

        // Wind effect - moved to sit under the silhouettes
        p.setColor(1, 1, 1, 0.6f);
        p.drawLine(size / 2 - 15, size / 2 + 15, size / 2 + 5, size / 2 + 15);
        p.drawLine(size / 2 - 10, size / 2 + 20, size / 2 + 10, size / 2 + 20);

        Texture t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        p.dispose();
        return t;
    }

    private static Texture echoIcon(int size) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 0);
        p.fill();

        // Silhouette 2 (echo - behind/faded) - moved to right
        p.setColor(1, 1, 1, 0.3f);
        drawSilhouette(p, size / 2 + 8, size / 2 + 2, size / 8);

        // Silhouette 1 (main) - moved to left
        p.setColor(1, 1, 1, 0.9f);
        drawSilhouette(p, size / 2 - 4, size / 2, size / 8);

        Texture t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        p.dispose();
        return t;
    }

    private static void drawSilhouette(Pixmap p, int x, int y, int headRadius) {
        // Head - moved above the body (smaller Y in Pixmap)
        p.fillCircle(x, y - headRadius - 2, headRadius);
        // Body
        p.fillRectangle(x - headRadius, y - headRadius, headRadius * 2, headRadius * 2);
    }

    private static Texture resetIcon(int size) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 0);
        p.fill();
        p.setColor(1, 1, 1, 0.8f);
        // Circular arrow / Rewind feel
        p.drawCircle(size / 2, size / 2, size / 4);
        p.setColor(0, 0, 0, 0);
        p.fillRectangle(size / 2, size / 2 - size / 4 - 2, size / 2, size / 4); // gap in circle
        p.setColor(1, 1, 1, 0.8f);
        // Arrow head
        int headX = size / 2;
        int headY = size / 2 - size / 4;
        p.fillTriangle(headX, headY - 6, headX, headY + 6, headX - 10, headY);
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
        iconJump.dispose();
        iconRecord.dispose();
        iconEcho.dispose();
        iconPause.dispose();
        iconReset.dispose();
        font.dispose();
        fontLarge.dispose();
        fontTitle.dispose();
        batch.dispose();
    }
}
