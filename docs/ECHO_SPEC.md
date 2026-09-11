# ECHO — A 2D Temporal Puzzle Platformer

> Instructions and specifications. This is the authoritative reference for the project.

## 1. Project Overview

Create a complete 2D puzzle-platformer prototype called ECHO using:

* Java 17
* LibGDX 1.14.1
* Box2D for physics and collision
* LibGDX Scene2D only where appropriate for UI
* Desktop as the primary development target
* 1280×720 reference resolution
* Fixed logical gameplay coordinate system
* Maven or Gradle project structure appropriate for LibGDX

The game should be designed as a scalable foundation for a larger production game, not as a one-off demo.

The defining gameplay mechanic is: **The player can record their actions and create a temporal Echo that deterministically replays those actions.** The player's past self becomes a temporary companion that can interact with the environment.

The game should combine: 2D platforming, puzzle solving, timing, physics, temporal planning, deterministic replay, level mastery, optional speedrunning, optional challenge objectives.

The primary gameplay fantasy is: **"I can cooperate with my past self."**

## 2. Important Development Principles

The implementation must prioritize:

1. Deterministic Echo playback
2. Responsive player controls
3. Fast experimentation
4. Instant level reset
5. Clear separation of gameplay systems
6. Reusable components
7. Data-driven level design
8. Scalable architecture
9. Testability
10. Performance

Do NOT implement the entire game at once. First build a small playable vertical slice proving that the Echo mechanic is fun and reliable.

## 3. Initial Vertical-Slice Goal

The first playable prototype should contain only:

**Player**: horizontal movement, jumping, gravity, collision, basic animation/state.

**Echo system**: start recording, stop recording, store player actions, create Echo, replay recorded actions deterministically.

**Environment**: static platforms, pressure plate, door, exit.

**Level** — the player must:

1. Record themselves walking onto a pressure plate.
2. Create an Echo.
3. Allow the Echo to stand on the pressure plate.
4. Use the newly opened door.
5. Reach the exit.

```
                EXIT

                 │
          ┌──────┘
          │
          │
       ┌──┴────┐
       │       │
      [PLATE]  │
       │       │
───────┴───────┴────────
        PLAYER
```

This simple interaction is the foundation of the entire game. Do not add combat, complex temporal paradoxes, or procedural generation until this core interaction is working reliably.

## 4. Gameplay Loop

```
EXPLORE → OBSERVE → RECORD → CREATE ECHO → OBSERVE ECHO
→ CONTROL PRESENT PLAYER → SOLVE PUZZLE → REACH EXIT
→ OPTIONALLY OPTIMIZE → RETRY
```

Failure should be inexpensive. The player should be encouraged to experiment.

Desired psychological loop:

```
"I don't understand this." → "Let me try something." → "That didn't work."
→ "Ah, I see why." → "Let me rewind." → "I'll make my Echo do this."
→ "YES!" → "Can I do it with fewer Echoes?"
```

## 5. Gameplay Controls

```
A / Left Arrow     Move left
D / Right Arrow    Move right
Space              Jump
E                  Interact
R                  Reset / Rewind attempt
Q                  Start/stop recording
F                  Create Echo
ESC                Pause
```

The exact controls should be configurable later. Use an input abstraction rather than hardcoding input logic directly into the Player class:

```java
interface InputSource {
    boolean left();
    boolean right();
    boolean jump();
    boolean interact();
    boolean record();
    boolean createEcho();
    boolean reset();
}
```

The real player uses keyboard input. Echoes use a different `InputSource` implementation that reads from recorded actions. This allows the same player controller to drive both the Real Player and the Echo.

## 6. Player Controller

Create a reusable `PlayerController`. Responsibilities: read input, move player, jump, apply movement forces/velocity, detect ground, handle coyote time, handle jump buffering, maintain player state, interact with the physics world.

Do not put Echo-specific logic inside the PlayerController. It simply receives an `InputSource`.

```
KeyboardInput → PlayerController → Box2D Body
RecordedInputSource → PlayerController → Box2D Body
```

## 7. Movement Feel

Controls should be responsive.

* **Horizontal movement**: controlled horizontal velocity rather than uncontrolled acceleration.
* **Jump**: jump impulse, variable jump height, coyote time, jump buffering.

Expose values as configuration:

```java
public class PlayerConfig {
    float moveSpeed;
    float acceleration;
    float deceleration;
    float jumpVelocity;
    float coyoteTime;
    float jumpBufferTime;
}
```

Do not scatter numerical values throughout the code.

## 8. Box2D Architecture

Use Box2D for: player physics, Echo physics, platforms, pushable objects, pressure plates, doors, moving platforms, hazards, enemies.

Use a fixed physics timestep. Do not directly manipulate Box2D transforms every render frame unless there is a specific reason.

```
Input → Gameplay logic → Physics body velocity/forces → World.step() → Rendering
```

Keep physics simulation separate from rendering.

## 9. Physics World

Create a dedicated `GamePhysicsWorld`. Responsibilities: create Box2D world, maintain gravity, step simulation, create bodies, destroy bodies safely, manage collision listeners.

Do not allow arbitrary game classes to directly manage the Box2D world lifecycle.

## 10. Collision Categories

```java
public final class CollisionBits {
    public static final short PLAYER      = 1 << 0;
    public static final short ECHO        = 1 << 1;
    public static final short WORLD       = 1 << 2;
    public static final short INTERACTIVE = 1 << 3;
    public static final short HAZARD      = 1 << 4;
    public static final short ENEMY       = 1 << 5;
    public static final short SENSOR      = 1 << 6;
}
```

Do not use magic bit values throughout the project.

## 11. Echo System

Suggested classes: `EchoManager`, `Echo`, `EchoController`, `EchoRecording`, `RecordedAction`, `InputRecorder`, `RecordedInputSource`.

```
Player → InputRecorder → EchoRecording → EchoManager → Echo
→ RecordedInputSource → PlayerController → Box2D
```

## 12. Input Recording

Do NOT record only the player's world position. Record gameplay actions and their timing:

```java
public record RecordedAction(float timestamp, ActionType action, float value) {}

enum ActionType {
    MOVE_LEFT, MOVE_RIGHT, JUMP_PRESSED, JUMP_RELEASED, INTERACT, ATTACK, DASH
}
```

The recording represents what the player *attempted* to do.

```
0.00  MOVE_RIGHT
0.50  MOVE_RIGHT
0.82  JUMP_PRESSED
0.90  MOVE_RIGHT
1.20  JUMP_RELEASED
1.80  INTERACT
```

## 13. Recording Clock

Do not depend on `System.currentTimeMillis()` for gameplay timing. Use the game's deterministic simulation time (`float recordingTime`).

## 14. Echo Playback

An Echo uses the *same* `PlayerController` as the real player. The only difference is its input source. Do NOT create a separate movement implementation for Echoes.

## 15. Deterministic Playback

Given same level state + same recording + same physics settings, the Echo performs the same actions. Avoid random behavior. Avoid AI decision-making. The Echo is a replayable player entity.

## 16. Echo Creation

```java
public class EchoRules {
    public int maxEchoes = 3;
    public float maxRecordingDuration = 10f;
}
```

Later levels can modify these values.

## 17. Echo Lifecycle

```java
enum EchoState { SPAWNING, REPLAYING, COMPLETED, DEAD, LOOPING }
```

Normal: Create → Spawn → Replay → Recording ends → Complete. For the first prototype an Echo disappears after completing. Looping is added later.

## 18. Echo Spawn Position

Recommended rule: **Every Echo represents a replay of an attempt starting from the level's initial player state.** The Echo's initial position, velocity, orientation, and state must be well-defined. Do not simply spawn at the current player's position if that creates inconsistent replay.

## 19. Multiple Echoes

Each Echo has its own `EchoRecording`. `EchoManager` manages creation, destruction, maximum count, playback, rendering, state, reset.

```java
public class EchoManager {
    private final Array<Echo> echoes;
    public Echo createEcho(EchoRecording recording);
    public void update(float delta);
    public void reset();
    public void dispose();
}
```

## 20. Reset / Rewind

Pressing `R` immediately resets the current attempt: player, Echoes, doors, switches, platforms, physics objects, enemies, timers, hazards. Do not reload the application or recreate the screen. Prefer `LevelState.reset()`. Reset should feel instantaneous.

## 21. Level State

```java
public interface Resettable { void reset(); }
```

`LevelManager → ResetManager → Resettable objects` (Door, PressurePlate, MovingPlatform, PhysicsObject, Enemy, Echo).

## 22. Pressure Plates

Reusable `PressurePlate` detecting Player, Echo, and physics objects through a Box2D sensor fixture. Do not hardcode a specific Door inside PressurePlate; use an interaction/event system.

## 23. Interaction System

```java
interface TriggerSource { boolean isActive(); }
```

A door can depend on PressurePlate, Switch, Timer, MultipleConditions (e.g. Plate A + Plate B → Door).

## 24. Doors

```java
enum DoorState { CLOSED, OPENING, OPEN, CLOSING }
```

Door responds to trigger conditions.

## 25. Moving Platforms

`MovingPlatform`: Point A → B → A, deterministic movement. Important when Echoes interact with timed platforms.

## 26. Physics Objects

`PushableBox`. Player and Echoes can push. Store initial position/rotation/velocity/angular velocity; restore on reset.

## 27. Level Data

Data-driven level format (JSON or Tiled recommended):

```json
{
  "id": "level_01",
  "echoLimit": 1,
  "recordingDuration": 10,
  "playerSpawn": [3, 2],
  "exit": [18, 5],
  "objects": [
    { "type": "pressurePlate", "position": [8, 2] },
    { "type": "door", "position": [14, 4] }
  ]
}
```

## 28. Level Editing

Architecture should eventually support Tiled. Levels define: platforms, player spawn, exit, pressure plates, doors, moving platforms, physics objects, hazards, enemies, Echo limits, recording duration, challenge objectives. Avoid code changes per level.

## 29. Camera

`OrthographicCamera` following the real player (not Echoes): smooth following, configurable dead zone, level boundaries, zoom, screen shake support later.

## 30. Rendering

Keep rendering separate from game logic: `GameWorld → GameRenderer → PlayerRenderer / EchoRenderer / LevelRenderer / UIRenderer`. Do not put large rendering methods inside gameplay entities.

## 31. Echo Visual Design

Echoes look distinct: semi-transparent, temporal trail, ghost-like afterimage, distinct outline, slight distortion, different identifier per Echo. The real player must remain visually obvious (THIS = ME / THIS = ECHO).

## 32. Echo Trails

Optional temporal trail using a limited number of position samples rendered as faded afterimages. Avoid allocating every frame; use pooling / reusable arrays.

## 33. UI

Minimal HUD: Echo count (● ● ○), recording status/bar, recording time, level timer, optional objective status. Do not overwhelm the player.

## 34. Debug UI

Toggle `F1`: FPS, physics step time, player position/velocity, Echo count, recording time, Echo state, Echo playback time, body count, fixture count. Disabled in release.

## 35. Debug Draw

`F2` toggles Box2D debug rendering.

## 36. Game States

Game → MainMenu / LevelSelect / Gameplay / Pause / Results. Prototype needs only Gameplay, Pause, Results. Don't over-engineer menus.

## 37. Game Loop

```
render() → accumulate delta → fixed physics updates → input processing
→ player update → echo update → world interactions → Box2D step
→ camera update → render → UI render
```

## 38. Fixed Timestep

`FIXED_STEP = 1f / 60f`. Accumulate render delta; clamp maximum frame delta.

## 39. Memory Management

Avoid `new Vector2()` per frame and `new RecordedAction()` per render frame. Prefer object pooling, reusable vectors, preallocated arrays, LibGDX `Array`/`Pool`, reusable recording buffers.

## 40. Disposal

Clear ownership for textures, atlases, batches, fonts, sounds, music, Box2D World, DebugRenderer, shape resources. Avoid double disposal.

## 41. Audio

Prototype: Player (jump, land); Echo (creation, playback, completion, death); Environment (door open/close, pressure plate, level completion). Audio should reinforce the temporal nature of Echoes.

## 42. Level Progression

* **Chapter 1 — THE PAST**: recording, Echo creation, buttons, doors
* **Chapter 2 — TIMING**: timed switches, moving platforms, lasers, timing windows
* **Chapter 3 — COOPERATION**: multiple Echoes, multiple switches, elevators, physics objects
* **Chapter 4 — CONFLICT**: enemies, combat, Echo sacrifice
* **Chapter 5 — LOOPS**: looping Echoes, repeating mechanisms, cyclic puzzles
* **Chapter 6 — INTERACTION**: Echo-to-Echo interactions, complex physics, object chains
* **Chapter 7 — PARADOX**: Echo inheritance, causal loops, temporal paradoxes
* **Chapter 8 — MASTERY**: learned mechanics in difficult combinations

## 43. Level Difficulty Model

INTRODUCTION → SAFE EXPERIMENT → SIMPLE APPLICATION → COMBINATION → TIMING → CHALLENGE → MASTERY

```
Level 1  Button → Door
Level 2  Echo → Button → Door
Level 3  Echo → Button, Player → Door
Level 4  Echo → Timed Button, Player → Door
Level 5  Echo → Button, Player → Moving Platform → Door
Level 6  Echo 1 → Button A, Echo 2 → Button B, Player → Door
```

## 44. Mastery System

Basic objective: reach the exit. Optional: Use ≤ N Echoes, complete under N seconds, use ≤ N seconds of recording, no Echo deaths, no rewind.

```java
public class LevelResult {
    float completionTime; int echoesUsed; int rewindCount; float recordingTime; int deaths;
}
```

## 45. Level Results

```
LEVEL COMPLETE
Time 31.4s  Echoes 3  Rewinds 2  Deaths 0  Recording 18s

✓ Complete   ✓ Under 40 seconds   ✗ Use ≤ 2 Echoes   ✗ No rewind
```

## 46. Solution Elegance

Reward efficiency (Echo count, recording duration, rewinds, time) without punishing inefficient solutions. Mastery challenges are optional.

## 47. Future Leaderboards

Result data compatible with: Fastest Time, Fewest Echoes, Fewest Rewinds, Shortest Recording, Perfect Run. No networking yet.

## 48. Future Level Editor

Objects: Platform, Pressure Plate, Door, Elevator, Moving Platform, Laser, Hazard, Pushable Box, Enemy, Echo Spawn, Exit. Config: Echo limit, recording duration, required Echo count, time limit, challenge objectives.

## 49. Advanced Echo Features (later)

Looping Echoes, Echo-to-Echo interaction, Echo sacrifice, Echo combat, Echo inheritance, temporal paradoxes. Not in the first prototype; design architecture to allow them.

## 50. Technical Design for Future Temporal Systems

Possible abstractions: `TemporalEntity`, `Resettable` (`captureInitialState()`, `reset()`), `Recordable`, `Replayable`, `Interactable`. Only introduce abstractions with clear architectural value.

## 51. Save System

Lightweight: unlocked levels, completed levels, best time, best Echo count, challenge completion. LibGDX `Preferences` initially (`echo_level_01_complete`, `echo_level_01_best_time`, ...).

## 52. Error Handling

Invalid recording → do not crash. Echo cannot spawn → display debug info. Level cannot load → show clear error. Do not silently ignore serious errors.

## 53. Testing

Automated tests without rendering: recording (action, timestamp, retrieval), playback (recording → same sequence), Echo limits, recording duration cap, reset restores state, level result best-time updates. Deterministic Echo system needs especially strong tests.

## 54. Important Architectural Rule

Echo system must NOT depend on rendering. `EchoRecording → RecordedInputSource → PlayerController → Physics` must work headless.

## 55. Recommended Package Structure

```
com.example.echo
├── EchoGame.java
├── screens/    GameplayScreen, MainMenuScreen, LevelSelectScreen
├── player/     Player, PlayerController, PlayerConfig, PlayerState
├── echo/       Echo, EchoManager, EchoController, EchoRecording, RecordedAction,
│               ActionType, InputRecorder, RecordedInputSource
├── physics/    GamePhysicsWorld, CollisionBits, PhysicsBodyFactory
├── level/      Level, LevelManager, LevelLoader, LevelState, ResetManager
├── objects/    Door, PressurePlate, MovingPlatform, PushableBox, Exit
├── rendering/  GameRenderer, PlayerRenderer, EchoRenderer, LevelRenderer
├── input/      InputSource, KeyboardInputSource
├── ui/         Hud, PauseMenu, ResultsScreen
├── audio/      AudioManager
├── save/       SaveManager
└── util/
```

Adapt if a better architecture is justified.

## 56. Code Quality

Java 17, clear naming, small classes, single responsibility, composition over inheritance, no global mutable state, avoid static managers, no magic numbers, config objects, document non-obvious algorithms, separate rendering and simulation. No enormous classes (e.g. `GameplayScreen` doing everything).

## 57. Performance Target

60 FPS on modest desktop hardware; eventually 10–20 Echoes. Watch body count, collision filtering, batches, allocations, trails, recording memory. Architecture first, then profile.

## 58. Development Phases

* **PHASE 1 — PROJECT SETUP**: LibGDX 1.14.1 project, Java 17, desktop launcher, basic screen, asset loading, logging. Verify launch.
* **PHASE 2 — BASIC PLATFORMER**: Box2D world, player, platforms, camera, movement, jump, collision. Small test level.
* **PHASE 3 — RECORDING**: InputSource, InputRecorder, RecordedAction, EchoRecording, recording timer. Display recording status.
* **PHASE 4 — ECHO PLAYBACK**: Echo, RecordedInputSource, EchoManager, deterministic playback. Verify recording = Echo behavior.
* **PHASE 5 — INTERACTION**: pressure plate, door, exit. First real puzzle.
* **PHASE 6 — RESET**: ResetManager, Resettable objects, player/Echo/physics reset. Instant restart.
* **PHASE 7 — MULTIPLE ECHOES**: Echo limit, multiple recordings, multiple Echoes, identification, visualization.
* **PHASE 8 — PHYSICS PUZZLES**: pushable boxes, moving platforms, elevators, physics interactions.
* **PHASE 9 — TIMING PUZZLES**: timed switches, timed doors, moving hazards, timing challenges.
* **PHASE 10 — MASTERY SYSTEM**: level timer, Echo count, rewinds, recording duration, challenge objectives, best scores.
* **PHASE 11 — POLISH**: animations, particles, sound, Echo trails, screen effects, UI polish.
* **PHASE 12 — ADVANCED SYSTEMS**: looping Echoes, Echo interactions, combat, sacrifice, Echo inheritance, paradoxes (only after core is proven fun).

## 59. First Prototype Level — "The First Echo"

```
                   EXIT

                    │
              ┌─────┘
              │
              │
       ┌──────┘
       │
     [PLATE]
       │
───────┴──────────────────
          PLAYER
```

Expected solution: Q (record) → walk onto plate → stop → Q (stop) → F (create Echo) → Echo walks to plate and stands → door opens → player walks through → exit → LEVEL COMPLETE (Time, Echoes: 1, Rewinds: 0).

## 60. Second Prototype Level

Timed door. Echo activates the plate; door stays open for limited time; player must time movement. Demonstrates temporal puzzles, not just switch puzzles.

## 61. Third Prototype Level

Multiple Echoes: Echo 1 → Plate A, Echo 2 → Plate B, Player → Door → Exit. Proves architecture scales beyond one Echo.

## 62. Final Design Philosophy

Test observation + planning + timing + spatial reasoning + temporal reasoning + execution, not reflexes. Teach: "Your past actions are tools." → "Your past self can solve problems for your present self." → "Multiple versions of yourself can cooperate." → "You can construct an entire solution across time."

## 63. The Core Question

"What does my past self need to do so that my present self can succeed?" → "What do several versions of myself need to do, and when?" → "How do my actions across different timelines affect each other?"

## 64. Implementation Rule for the AI

1. Do not skip directly to the complete game.
2. Build one subsystem at a time.
3. Verify compilation after each major subsystem.
4. Keep the project runnable at every stage.
5. No unnecessary dependencies.
6. Prefer LibGDX and Box2D built-ins.
7. No complex ECS unless necessary.
8. Echo system independent from rendering.
9. Physics deterministic and fixed-step.
10. Use Java 17 features where useful.
11. Avoid deprecated APIs.
12. Explain important architectural decisions.
13. Write tests for deterministic recording/playback.
14. No placeholder implementations for core mechanics.
15. No important logic hidden behind TODOs.
16. Do not implement future mechanics until the current prototype works.

## 65. Immediate Task

Implement only: project init, Box2D world, Player, Platforms, Camera, movement, jump, InputSource, InputRecorder, EchoRecording, RecordedInputSource, Echo, EchoManager, PressurePlate, Door, Exit, ResetManager, one prototype level, minimal HUD, debug mode.

Do not implement combat, enemies, bosses, looping Echoes, Echo inheritance, paradoxes, procedural levels, multiplayer, networking, or a level editor until the initial prototype has been validated.

**The immediate goal is to prove one thing: Is it fun to record yourself, create an Echo, and cooperate with your past self to solve a platforming puzzle?**
