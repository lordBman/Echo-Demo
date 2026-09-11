# ECHO — a 2D temporal puzzle platformer

Record your actions, spawn an **Echo** that replays them deterministically, and cooperate with your past self
to solve platforming puzzles. Java 17 · LibGDX 1.14.1 · Box2D.

The full design brief lives in [docs/ECHO_SPEC.md](docs/ECHO_SPEC.md). The level file format is documented in
[docs/LEVEL_FORMAT.md](docs/LEVEL_FORMAT.md).

## Running

Requirements: JDK 17 and Gradle 8.5+ (Gradle 9 works). On macOS the desktop launcher relaunches itself with
`-XstartOnFirstThread` when needed.

```bash
gradle desktop:run          # play
gradle :core:test           # headless simulation tests (no window required)
gradle build                # compile, test, build desktop/build/libs/echo-<version>.jar
gradle desktop:generateSounds   # regenerate the procedural WAV assets in assets/sounds
```

Developer flags for `desktop:run` / the jar: `--level N` starts level N directly, `--demo` plays the level 1
solution with a scripted input (attract mode and rendering smoke test).

If the launcher prints `no active display found`, the screen is asleep or the session is headless; wake the
display and run again.

## Controls

| Key | Action |
| --- | --- |
| A / ← , D / → | Move |
| Space / W | Jump (hold for height; coyote time and jump buffering are on) |
| E | Interact (switches) |
| Q | Start / stop recording. Starting a recording places you at the level spawn: every Echo replays from the level's initial player state. |
| 1–5 | Spawn an Echo from the recording history (1 = most recent). |
| F | Create an Echo from the latest recording. Pressed *while* recording it stops the recording and spawns immediately (with `echoInheritance` it is recorded instead). |
| R | Rewind — instant reset of the attempt (Echoes, doors, plates, boxes, timers). The last recording is kept. |
| H | Show the level hint again |
| Esc | Pause |
| F1 / F2 | Debug overlay / Box2D shapes |

## Architecture in one picture

```
KeyboardInputSource ─┐                       RecordedInputSource ─┐
                     ▼                                            ▼
              PlayerController ──► Player (Box2D body) ◄── PlayerController
                     ▲                                            ▲
            InputRecorder ──► EchoRecording ──► EchoManager ──► Echo
```

* `io.bsoft.echo.world.GameWorld` is the whole simulation of one level and has **no rendering dependency**. It runs a
  fixed 60 Hz step in a strict order: meta input → recording sample → player → echoes → level objects → Box2D →
  post-step (deaths, exit). Tests drive it tick by tick with the headless backend.
* `PlayerController` only ever sees an `InputSource`; Echoes are literally the player controller fed by a
  `RecordedInputSource`. Recordings store input *edges* per simulation tick, never positions.
* `EchoRecording` also stores sparse position checkpoints. They never drive playback; `Echo.divergence()` uses
  them to detect when a replay no longer matches the original run because the world changed (the seed of the
  "paradox" mechanics).
* Level objects (`PressurePlate`, `Door`, `MovingPlatform`, `PushableBox`, `Hazard`, `TimedSwitch`, `Enemy`,
  `Exit`) are wired through `TriggerSource` expressions in level JSON (`all(a,b)`, `timed(a,3)`,
  `cycle(4,1.5)`, …) and registered as `Resettable` with the `ResetManager`.
* Rendering (`io.bsoft.echo.rendering`), HUD (`io.bsoft.echo.ui`) and audio (`io.bsoft.echo.audio`) subscribe to
  `GameWorld.events()`; the simulation never calls them.
* Levels are data (`assets/levels/*.json`, ordered by `levels.json`); new object types are added by registering
  a factory in `LevelBuilder`.

### Decisions worth knowing

* **Echoes do not collide with the player or each other by default** (`echoesCollide` per level). This keeps
  replay independent of where the present player stands, and avoids spawn overlap. Levels can enable it.
* **Contact bookkeeping trusts Box2D's paired begin/end callbacks and is never cleared by hand.** After any
  teleport the world runs a zero-length step (`GamePhysicsWorld.refreshContacts()`) so a spawned Echo and a
  teleported player agree on their very first tick.
* **All timers are tick based** (`Triggers.Timed`, recordings, respawn) so windows are exact and replayable.
* Art and sound are procedural placeholders (`Assets`, `SoundSynth`) so the prototype has zero binary
  dependencies; swapping in a texture atlas only touches `Assets` and the renderers.

## Levels

| # | Name | Teaches |
| --- | --- | --- |
| 1 | The First Echo | Record → Echo → plate → door |
| 2 | Closing Window | Timed door: be in position before the Echo arrives |
| 3 | Two Plates | Two Echoes, `all()` trigger |
| 4 | The Lift | Elevator held up by an Echo |
| 5 | Heavy Lifting | Pushable box persists; sequencing present-self work before recording |
| 6 | Cadence | Timed switch re-armed by an Echo, looping ferry platform, cycling laser |
| 7 | Patrol | Stomping enemies, Echo sacrifice (a hazard that latches when something dies in it) |
| 8 | Loops | Looping Echoes turn a short recording into a rhythm |
| 9 | Inheritance | Spawning an Echo during a recording becomes part of that recording |
| 10 | Paradox | A replay diverges when the world changed; rewind restores the past (`noParadox` challenge) |

Levels 1–5, 8 and 10 are solved end to end by `PrototypeLevelsTest`, so a layout edit that breaks a level's own
solution fails the build.

## Development phases (spec §58)

| Phase | Status |
| --- | --- |
| 1 Project setup | Done — Gradle multi-module, launcher, logging, procedural assets |
| 2 Basic platformer | Done — Box2D world, player, platforms, camera |
| 3 Recording | Done — `InputRecorder`, tick-stamped `RecordedAction`s, HUD status |
| 4 Echo playback | Done — deterministic; verified bit-for-bit in `DeterminismTest` |
| 5 Interaction | Done — plate, door, exit; level 1 |
| 6 Reset | Done — `ResetManager`, instant rewind, contact refresh |
| 7 Multiple Echoes | Done — limits, per-Echo colour, trails, progress bars |
| 8 Physics puzzles | Done — boxes, looping / triggered platforms (elevators) |
| 9 Timing puzzles | Done — timed triggers, timed switches, cycling and moving hazards |
| 10 Mastery | Done — session stats, challenges, results screen, saved bests |
| 11 Polish | Done (prototype level) — squash/stretch, particles, shake, flash, synthesized SFX, trails |
| 12 Advanced | Done as opt-in level rules — looping Echoes, Echo/player collision, sacrifice hazards, stompable enemies, Echo inheritance; divergence ("paradox") detection fires an event, flickers the Echo red, and drives the `noParadox` challenge in level 10 |

## Known limitations / next steps

* Paradoxes are detected and scored but never *cause* anything yet (e.g. an Echo dissolving); that is the next
  design experiment.
* Rendering uses the current physics state without interpolation; fine at 60 Hz, could be smoother at 120 Hz.
* Menus are plain keyboard menus drawn with the batch; Scene2D was not needed at this size.
