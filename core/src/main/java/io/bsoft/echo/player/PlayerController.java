package io.bsoft.echo.player;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import io.bsoft.echo.input.InputSource;

/**
 * Turns an {@link InputSource} into avatar motion (spec §6, §7).
 *
 * <p>The controller is deliberately unaware of whether it drives the real player or an
 * Echo. It works purely on the fixed step {@code dt} it is given; all of its timers are
 * simulation-time based, so with identical input samples it produces identical motion.</p>
 *
 * <p>Horizontal motion is velocity-controlled: each tick the horizontal velocity moves toward
 * {@code axis * moveSpeed} (plus the velocity of the ground we stand on) at a fixed
 * acceleration. Vertical motion is left to gravity except for the jump impulse, the early
 * release "jump cut", and a terminal-velocity clamp.</p>
 */
public final class PlayerController {

    private final Player player;
    private final PlayerConfig config;
    private InputSource input;
    private PlayerListener listener;

    private float coyoteTimer;
    private float jumpBufferTimer;
    private boolean jumpHeldLastTick;
    private boolean interactHeldLastTick;
    private boolean wasGrounded;
    /** True from the moment we jump until the jump apex or until the jump cut has been applied. */
    private boolean jumpActive;
    private boolean jumpCutApplied;

    private final Vector2 velocity = new Vector2();

    public PlayerController(Player player, InputSource input) {
        this.player = player;
        this.config = player.config();
        this.input = input;
    }

    public void setListener(PlayerListener listener) {
        this.listener = listener;
    }

    public void setInput(InputSource input) {
        this.input = input;
    }

    public InputSource input() {
        return input;
    }

    public Player player() {
        return player;
    }

    /** Clears all transient timers; call whenever the avatar is teleported / respawned. */
    public void resetState() {
        coyoteTimer = 0f;
        jumpBufferTimer = 0f;
        jumpHeldLastTick = false;
        interactHeldLastTick = false;
        wasGrounded = false;
        jumpActive = false;
        jumpCutApplied = false;
    }

    /** Advances the controller by one fixed step. Must be called before the physics step. */
    public void update(float dt) {
        if (!player.isAlive()) {
            return;
        }
        int axis = input.horizontal();
        boolean jumpHeld = input.jump();
        boolean jumpPressed = jumpHeld && !jumpHeldLastTick;
        boolean interactHeld = input.interact();
        boolean interactPressed = interactHeld && !interactHeldLastTick;
        jumpHeldLastTick = jumpHeld;
        interactHeldLastTick = interactHeld;

        boolean grounded = player.isGrounded();
        velocity.set(player.velocity());

        if (grounded && !wasGrounded && velocity.y <= 0.01f) {
            jumpActive = false;
            if (listener != null) {
                listener.onLand(player);
            }
        }
        wasGrounded = grounded;

        // --- timers -----------------------------------------------------------
        if (grounded && !jumpActive) {
            coyoteTimer = config.coyoteTime;
        } else {
            coyoteTimer = Math.max(0f, coyoteTimer - dt);
        }
        if (jumpPressed) {
            jumpBufferTimer = config.jumpBufferTime;
        } else {
            jumpBufferTimer = Math.max(0f, jumpBufferTimer - dt);
        }

        // --- horizontal -----------------------------------------------------
        float groundVx = grounded ? player.groundVelocityX() : 0f;
        float targetVx = axis * config.moveSpeed + groundVx;
        float rate = axis != 0 ? config.acceleration : config.deceleration;
        if (!grounded) {
            rate *= config.airControl;
        }
        velocity.x = moveToward(velocity.x, targetVx, rate * dt);

        // --- vertical -------------------------------------------------------
        boolean canJump = coyoteTimer > 0f && jumpBufferTimer > 0f;
        if (canJump) {
            velocity.y = config.jumpVelocity;
            coyoteTimer = 0f;
            jumpBufferTimer = 0f;
            jumpActive = true;
            jumpCutApplied = false;
            if (listener != null) {
                listener.onJump(player);
            }
        } else if (jumpActive && !jumpCutApplied && !jumpHeld && velocity.y > 0f) {
            // Variable jump height: releasing early shortens the jump.
            velocity.y *= config.jumpCutMultiplier;
            jumpCutApplied = true;
        }
        if (jumpActive && velocity.y <= 0f) {
            jumpActive = false;
        }
        velocity.y = Math.max(velocity.y, -config.maxFallSpeed);

        player.body().setLinearVelocity(velocity);

        // --- interaction ----------------------------------------------------
        if (interactPressed) {
            player.interact();
            if (listener != null) {
                listener.onInteract(player);
            }
        }

        // --- presentation state --------------------------------------------
        if (axis != 0) {
            player.setFacingRight(axis > 0);
        }
        float relativeVy = velocity.y - (grounded ? player.groundVelocityY() : 0f);
        if (grounded && Math.abs(relativeVy) < 0.5f) {
            player.setState(axis != 0 ? PlayerState.RUNNING : PlayerState.IDLE);
        } else {
            player.setState(velocity.y > 0f ? PlayerState.JUMPING : PlayerState.FALLING);
        }
    }

    private static float moveToward(float current, float target, float maxDelta) {
        if (Math.abs(target - current) <= maxDelta) {
            return target;
        }
        return current + Math.signum(target - current) * maxDelta;
    }

    /** For the debug overlay. */
    public float coyoteTimer() {
        return coyoteTimer;
    }

    public float jumpBufferTimer() {
        return jumpBufferTimer;
    }

    static float clamp(float v, float min, float max) {
        return MathUtils.clamp(v, min, max);
    }
}
