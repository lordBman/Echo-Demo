# Android Port Touch Controls Implementation Plan

Adopt touch input for the Android port, including menu navigation and in-game controls.

## Proposed Changes

### Input System

#### [NEW] [TouchInputSource.java](file:///C:/Users/okele/Documents/Echo/core/src/main/java/io/bsoft/echo/input/TouchInputSource.java)
- Implement `InputSource` to handle touch-based movement and actions.
- Provide methods to set state from the UI overlay (joystick axis, button presses).
- Implement `poll()` and `consumePresses()` similar to `KeyboardInputSource`.

### UI Components

#### [NEW] [TouchOverlay.java](file:///C:/Users/okele/Documents/Echo/core/src/main/java/io/bsoft/echo/ui/TouchOverlay.java)
- Render semi-transparent touch controls using `UiCanvas`.
- **Bottom Left:** Virtual analog stick for horizontal movement.
- **Bottom Right:** Buttons for `Jump` (Space), `Record` (Q), and `Spawn Echo` (F).
- **Top Right:** `Reset` button (R).
- Handle touch events via `Gdx.input` and update `TouchInputSource`.
- Use a `Viewport` to map screen coordinates to reference resolution (1280x720).

### Screens

#### [MODIFY] [GameplayScreen.java](file:///C:/Users/okele/Documents/Echo/core/src/main/java/io/bsoft/echo/screens/GameplayScreen.java)
- Detect if the app is running on Android (`Gdx.app.getType() == ApplicationType.Android`).
- If on Android, instantiate `TouchInputSource` and `TouchOverlay`.
- Wire `TouchOverlay` to `TouchInputSource`.
- Pass `TouchInputSource` to `GameWorld`.
- Update and render `TouchOverlay` in the `render()` loop.

#### [MODIFY] [MainMenuScreen.java](file:///C:/Users/okele/Documents/Echo/core/src/main/java/io/bsoft/echo/screens/MainMenuScreen.java)
- Add touch handling to the `render()` loop or `handleInput()`.
- Detect taps on menu options to select and confirm.

#### [MODIFY] [LevelSelectScreen.java](file:///C:/Users/okele/Documents/Echo/core/src/main/java/io/bsoft/echo/screens/LevelSelectScreen.java)
- Add touch handling to detect taps on level entries.

#### [MODIFY] [PauseOverlay.java](file:///C:/Users/okele/Documents/Echo/core/src/main/java/io/bsoft/echo/ui/PauseOverlay.java)
- (I should check this file) Add touch handling for Resume, Restart, Level Select, and Menu options.

#### [MODIFY] [ResultsOverlay.java](file:///C:/Users/okele/Documents/Echo/core/src/main/java/io/bsoft/echo/ui/ResultsOverlay.java)
- (I should check this file) Add touch handling for Next Level, Restart, and Level Select.

## Verification Plan

### Automated Tests
- N/A (UI and Input focused)

### Manual Verification
- Deploy to an Android device/emulator.
- Verify the Virtual Joystick correctly moves the player.
- Verify Jump, Record, Spawn Echo, and Reset buttons work as expected.
- Verify menu navigation works via touch.
- Verify the overlay is semi-transparent and doesn't obscure critical gameplay elements.
