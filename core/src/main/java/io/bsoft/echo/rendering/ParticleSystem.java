package io.bsoft.echo.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

/**
 * Tiny pooled particle system for gameplay feedback (spec §39: no per-frame allocation).
 * Purely cosmetic; it is not part of the simulation and may use randomness freely.
 */
public final class ParticleSystem {

    private static final int CAPACITY = 512;

    private final float[] x = new float[CAPACITY];
    private final float[] y = new float[CAPACITY];
    private final float[] vx = new float[CAPACITY];
    private final float[] vy = new float[CAPACITY];
    private final float[] life = new float[CAPACITY];
    private final float[] maxLife = new float[CAPACITY];
    private final float[] size = new float[CAPACITY];
    private final float[] gravity = new float[CAPACITY];
    private final float[] r = new float[CAPACITY];
    private final float[] g = new float[CAPACITY];
    private final float[] b = new float[CAPACITY];
    private int count;

    public void burst(float cx, float cy, int amount, float speed, float lifeSeconds, float particleSize,
                      float gravityY, Color color) {
        for (int i = 0; i < amount && count < CAPACITY; i++) {
            int n = count++;
            float angle = MathUtils.random(0f, MathUtils.PI2);
            float s = speed * MathUtils.random(0.4f, 1f);
            x[n] = cx;
            y[n] = cy;
            vx[n] = MathUtils.cos(angle) * s;
            vy[n] = MathUtils.sin(angle) * s;
            life[n] = maxLife[n] = lifeSeconds * MathUtils.random(0.6f, 1f);
            size[n] = particleSize * MathUtils.random(0.6f, 1.2f);
            gravity[n] = gravityY;
            r[n] = color.r;
            g[n] = color.g;
            b[n] = color.b;
        }
    }

    /** Horizontal dust puff at the feet. */
    public void dust(float cx, float cy, int amount, float dirX, Color color) {
        for (int i = 0; i < amount && count < CAPACITY; i++) {
            int n = count++;
            x[n] = cx + MathUtils.random(-0.3f, 0.3f);
            y[n] = cy;
            vx[n] = dirX * MathUtils.random(0.5f, 2.5f) + MathUtils.random(-1f, 1f);
            vy[n] = MathUtils.random(0.5f, 2f);
            life[n] = maxLife[n] = MathUtils.random(0.25f, 0.5f);
            size[n] = MathUtils.random(0.1f, 0.22f);
            gravity[n] = -4f;
            r[n] = color.r;
            g[n] = color.g;
            b[n] = color.b;
        }
    }

    public void update(float delta) {
        for (int i = 0; i < count; i++) {
            life[i] -= delta;
            if (life[i] <= 0f) {
                // Swap-remove.
                int last = --count;
                x[i] = x[last];
                y[i] = y[last];
                vx[i] = vx[last];
                vy[i] = vy[last];
                life[i] = life[last];
                maxLife[i] = maxLife[last];
                size[i] = size[last];
                gravity[i] = gravity[last];
                r[i] = r[last];
                g[i] = g[last];
                b[i] = b[last];
                i--;
                continue;
            }
            vy[i] += gravity[i] * delta;
            x[i] += vx[i] * delta;
            y[i] += vy[i] * delta;
        }
    }

    public void render(SpriteBatch batch, Assets assets) {
        for (int i = 0; i < count; i++) {
            float a = life[i] / maxLife[i];
            batch.setColor(r[i], g[i], b[i], a);
            float s = size[i] * (0.5f + 0.5f * a);
            batch.draw(assets.circle, x[i] - s / 2f, y[i] - s / 2f, s, s);
        }
        batch.setColor(Color.WHITE);
    }

    public void clear() {
        count = 0;
    }

    public int count() {
        return count;
    }
}
