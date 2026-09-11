package io.bsoft.echo.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Align;
import io.bsoft.echo.echo.Echo;
import io.bsoft.echo.rendering.Assets;
import io.bsoft.echo.rendering.Palette;
import io.bsoft.echo.world.GameWorld;

/** Developer overlay toggled with F1 (spec §34). */
public final class DebugOverlay {

    private final Assets assets;
    private final UiCanvas canvas;
    private final StringBuilder sb = new StringBuilder(512);
    private boolean visible;
    private float physicsMillisSmoothed;

    public DebugOverlay(Assets assets, UiCanvas canvas) {
        this.assets = assets;
        this.canvas = canvas;
    }

    public void toggle() {
        visible = !visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public void render(GameWorld world, int particles) {
        if (!visible) {
            return;
        }
        physicsMillisSmoothed += (world.physics().lastStepMillis() - physicsMillisSmoothed) * 0.1f;
        var p = world.player();
        sb.setLength(0);
        sb.append("FPS ").append(Gdx.graphics.getFramesPerSecond()).append('\n');
        sb.append(String.format("Physics step %.3f ms   tick %d   sim %.2fs%n", physicsMillisSmoothed,
                world.clock().tick(), world.clock().time()));
        sb.append(String.format("Player pos (%.2f, %.2f)  vel (%.2f, %.2f)  %s  grounded=%b%n", p.x(), p.y(),
                p.velocity().x, p.velocity().y, p.state(), p.isGrounded()));
        sb.append(String.format("Coyote %.3f  buffer %.3f%n", world.playerController().coyoteTimer(),
                world.playerController().jumpBufferTimer()));
        sb.append("Echoes ").append(world.echoManager().count()).append('/').append(world.echoManager().maxEchoes())
                .append("  created ").append(world.echoManager().totalCreated()).append('\n');
        var rec = world.echoController();
        sb.append(String.format("Recording %b  %.2fs  last=%s%n", rec.isRecording(), rec.recorder().elapsed(),
                rec.lastRecording()));
        for (Echo e : world.echoManager().echoes()) {
            sb.append(String.format("  %s  pos (%.2f, %.2f)  div %.2f%n", e, e.avatar().x(), e.avatar().y(),
                    e.divergence()));
        }
        sb.append("Bodies ").append(world.physics().bodyCount()).append("  fixtures ")
                .append(world.physics().fixtureCount()).append("  contacts ").append(world.physics().contactCount())
                .append("  particles ").append(particles).append('\n');
        sb.append("Resettables ").append(world.resetManager().size()).append("  Java heap ")
                .append(Gdx.app.getJavaHeap() / (1024 * 1024)).append(" MB\n");
        sb.append("[F1] debug  [F2] physics shapes");
        canvas.begin();
        canvas.rect(14f, UiCanvas.HEIGHT - 340f, 620f, 200f, com.badlogic.gdx.graphics.Color.BLACK, 0.55f);
        canvas.text(assets.font, sb, 24f, UiCanvas.HEIGHT - 150f, Align.left, Palette.UI_TEXT, 1f);
        canvas.end();
    }
}
