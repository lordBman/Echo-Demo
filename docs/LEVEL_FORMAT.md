# Level file format

Levels are JSON files in `assets/levels/`, listed in order in `assets/levels/levels.json`:

```json
{ "levels": ["level_01.json", "level_02.json"] }
```

Units are meters; the camera shows 40 × 22.5 m at zoom 1. `y` grows upward.

## Top level

| Field | Type | Default | Meaning |
| --- | --- | --- | --- |
| `id` | string | required | Stable id, used for save keys |
| `name` | string | id | Shown in menus and HUD |
| `chapter` | int | 1 | Grouping in level select |
| `hint` | string | "" | Shown for a few seconds at level start |
| `bounds` | `{width, height}` | 40 × 22.5 | Camera clamp and invisible walls |
| `playerSpawn` | `[x, y]` | required | **Feet** position of the player |
| `echoLimit` | int | 3 | Max simultaneous Echoes |
| `recordingDuration` | float | 10 | Max seconds per recording |
| `recordFromSpawn` | bool | true | Starting a recording teleports the player to the spawn |
| `loopingEchoes` | bool | false | Echoes restart their recording instead of vanishing |
| `echoesCollide` | bool | false | Echoes collide with the player and each other |
| `echoInheritance` | bool | false | F during a recording is recorded as `CREATE_ECHO` and replayed |
| `platforms` | array | [] | Static boxes: `{x, y, width, height}` with `(x, y)` the **bottom-left** corner |
| `objects` | array | [] | See below |
| `challenges` | array | [] | `{type, value}`; `complete` is always added |

## Objects

Every object has `type`, an optional `id` (defaults to `type_index`) and `position: [x, y]` meaning
**(center x, bottom y)**. Objects that react to triggers accept `"trigger": "<expression>"`.

| type | Extra fields | Notes |
| --- | --- | --- |
| `pressurePlate` | `width` (1.5) | Trigger source: active while any character or box overlaps it |
| `door` | `width` (0.6), `height` (3), `openTime` (0.4), `trigger` | Solid until ~85 % open |
| `exit` | — | Only the real player completes the level |
| `movingPlatform` | `target: [x, y]` (required), `width` (3), `height` (0.5), `speed` (3), `waitTime` (0.5), `mode` (`loop` / `triggered`), `startOffset` (0), `trigger` | `position`/`target` are bottom-center. With a `trigger` the default mode is `triggered` (an elevator that rises while active) |
| `pushableBox` | `size` (1.2), `density` (0.6), `friction` (0.6) | Full kinematic state restored on reset |
| `hazard` | `width` (1), `height` (0.5), `target`, `speed`, `trigger` | Kills characters while armed (`trigger` defaults to always). With `target` + `speed` it moves A↔B. Also a trigger source: active once something died in it (sacrifice) |
| `switch` | `holdTime` (0 = toggle) | Interact with E when standing near it; trigger source |
| `enemy` | `patrolTo` (x + 4), `speed` (2) | Patrols `[position.x, patrolTo]`; side contact kills, landing on top defeats it. Trigger source: active once defeated |

## Trigger expressions

```
plate1                  another object's id
all(a, b, ...)          every source active
any(a, b, ...)          any source active
not(a)
timed(a, seconds)       active for `seconds` after a's rising edge (re-triggerable)
cycle(period, on[, offset])   free-running; active for the first `on` seconds of each period
latch(a)                stays active once a has been active
always / never
```

Expressions nest: `all(plateA, timed(switch1, 4))`.

## Challenges

| type | value | Meaning |
| --- | --- | --- |
| `complete` | — | Reach the exit (implicit) |
| `maxEchoes` | n | Echoes created in the winning attempt ≤ n |
| `maxTime` | seconds | Attempt time ≤ value |
| `maxRecording` | seconds | Total seconds recorded in the attempt ≤ value |
| `maxRewinds` | n | Rewinds ≤ n |
| `noRewind` | — | Zero rewinds |
| `noDeaths` | — | Zero player deaths in the attempt |
| `noParadox` | — | No Echo diverged more than 1 m from its recorded run |

## Errors

Missing required fields, unknown object types, unknown trigger references and malformed expressions raise a
`LevelLoadException` with the level id and field name; the game shows it on an error screen instead of
crashing.
