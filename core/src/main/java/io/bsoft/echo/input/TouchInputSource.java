package io.bsoft.echo.input;

/**
 * Android-specific input source that receives events from the on-screen TouchOverlay.
 */
public final class TouchInputSource implements InputSource {

    private float horizontalAxis;
    private boolean jumpPressed;
    private boolean jumpHeld;
    private boolean interactPressed;
    private boolean interactHeld;
    private boolean recordPressed;
    private boolean createEchoPressed;
    private boolean resetPressed;
    private boolean pausePressed;
    private boolean enabled = true;

    public void update(float horizontal, boolean jump, boolean interact, boolean record, boolean createEcho, boolean reset) {
        if (reset) resetPressed = true;
        if (!enabled) return;

        this.horizontalAxis = horizontal;
        this.jumpHeld = jump;
        this.interactHeld = interact;

        if (record) recordPressed = true;
        if (createEcho) createEchoPressed = true;
    }

    public void setJumpPressed() {
        if (enabled) jumpPressed = true;
    }

    public void setInteractPressed() {
        if (enabled) interactPressed = true;
    }

    public void setPausePressed() {
        pausePressed = true;
    }

    @Override
    public boolean left() {
        return enabled && horizontalAxis < -0.2f;
    }

    @Override
    public boolean right() {
        return enabled && horizontalAxis > 0.2f;
    }

    @Override
    public boolean jump() {
        return enabled && jumpHeld;
    }

    @Override
    public boolean interact() {
        return enabled && interactHeld;
    }

    @Override
    public boolean record() {
        return recordPressed;
    }

    @Override
    public boolean createEcho() {
        return createEchoPressed;
    }

    @Override
    public boolean reset() {
        return resetPressed;
    }

    @Override
    public boolean pauseJustPressed() {
        return pausePressed;
    }

    @Override
    public void consumePresses() {
        recordPressed = false;
        createEchoPressed = false;
        resetPressed = false;
        jumpPressed = false;
        interactPressed = false;
        pausePressed = false;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            horizontalAxis = 0;
            jumpHeld = false;
            interactHeld = false;
            consumePresses();
        }
    }

    @Override
    public int horizontal() {
        if (!enabled) return 0;
        if (horizontalAxis > 0.2f) return 1;
        if (horizontalAxis < -0.2f) return -1;
        return 0;
    }
}
