package io.bsoft.echo.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import io.bsoft.echo.GameConfig;
import io.bsoft.echo.rendering.Assets;

/**
 * Screen-space drawing helpers in the 1280x720 reference resolution. Every UI element draws
 * through this so it scales uniformly with the window.
 */
public final class UiCanvas {

    public static final float WIDTH = GameConfig.REFERENCE_WIDTH;
    public static final float HEIGHT = GameConfig.REFERENCE_HEIGHT;

    private final Assets assets;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport = new FitViewport(WIDTH, HEIGHT, camera);
    private final GlyphLayout layout = new GlyphLayout();

    public UiCanvas(Assets assets) {
        this.assets = assets;
        camera.position.set(WIDTH / 2f, HEIGHT / 2f, 0f);
        camera.update();
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    public Viewport viewport() {
        return viewport;
    }

    public SpriteBatch begin() {
        viewport.apply();
        SpriteBatch batch = assets.batch;
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        return batch;
    }

    public void end() {
        assets.batch.setColor(Color.WHITE);
        assets.batch.end();
    }

    public void rect(float x, float y, float w, float h, Color color, float alpha) {
        assets.batch.setColor(color.r, color.g, color.b, alpha);
        assets.batch.draw(assets.white, x, y, w, h);
        assets.batch.setColor(Color.WHITE);
    }

    public void circle(float cx, float cy, float radius, Color color, float alpha) {
        assets.batch.setColor(color.r, color.g, color.b, alpha);
        assets.batch.draw(assets.circle, cx - radius, cy - radius, radius * 2f, radius * 2f);
        assets.batch.setColor(Color.WHITE);
    }

    /** Draws text with its top at {@code y}. {@code align} is an {@link Align} constant. */
    public float text(BitmapFont font, CharSequence str, float x, float y, int align, Color color, float alpha) {
        font.setColor(color.r, color.g, color.b, alpha);
        layout.setText(font, str);
        float drawX = x;
        if ((align & Align.right) != 0) {
            drawX = x - layout.width;
        } else if ((align & Align.center) != 0) {
            drawX = x - layout.width / 2f;
        }
        font.draw(assets.batch, layout, drawX, y);
        return layout.height;
    }

    /**
     * Draws word-wrapped text within {@code width}, top at {@code y}, centered on {@code cx}.
     * Returns the height used.
     */
    public float textWrapped(BitmapFont font, CharSequence str, float cx, float y, float width, Color color,
                             float alpha) {
        font.setColor(color.r, color.g, color.b, alpha);
        layout.setText(font, str, font.getColor(), width, Align.center, true);
        font.draw(assets.batch, layout, cx - width / 2f, y);
        return layout.height;
    }

    /** Height that {@link #textWrapped} would use. */
    public float wrappedHeight(BitmapFont font, CharSequence str, float width) {
        layout.setText(font, str, Color.WHITE, width, Align.center, true);
        return layout.height;
    }

    public float textWidth(BitmapFont font, CharSequence str) {
        layout.setText(font, str);
        return layout.width;
    }

    public float lineHeight(BitmapFont font) {
        return font.getLineHeight();
    }
}
