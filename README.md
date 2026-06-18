# GeoGuessr RS — RuneLite Plugin

A GeoGuessr-style location guessing game for Old School RuneScape. You are shown a circular minimap screenshot of a location somewhere in Gielinor and must figure out where it is. Each day brings a fixed set of locations — everyone plays the same challenge.

---

## Daily Challenge

Every day you get **4 attempts**, each at a different location. Once all four are played, the challenge is locked until the next day. Your total score across all four rounds is submitted to the leaderboard automatically.

Progress is tracked with dots in the panel header (● used / ○ remaining).

---

## Hints

Each round starts with a tight minimap crop. There are up to 3 hint buttons. Pressing a hint button **progressively zooms out** the minimap image, revealing more of the surrounding area:

| Hint | Crop radius | What you see |
|---|---|---|
| None (start) | ~26 px | Small central area |
| Hint 1 | ~50% of full | Wider area, more context |
| Hint 2 | ~75% of full | Most of the minimap |
| Hint 3 | Full image | Entire captured scene |

**Hint penalty:** −500 pts per hint used.

---

## Scoring

Scoring uses an exponential decay curve — the closer your guess, the higher your score. Being in the wrong region or continent collapses your score toward zero.

```
distanceScore = 5000 × e^(−distance / 150)
hintPenalty   = hintsUsed × 500
timePenalty   = max(0, (elapsedSeconds − 60) × 5)
finalScore    = max(0, distanceScore − hintPenalty − timePenalty)
```

| Distance | Score (no penalties) |
|---|---|
| 0 tiles (exact) | 5 000 |
| ~50 tiles | ~3 600 |
| ~150 tiles | ~1 839 |
| ~300 tiles | ~677 |
| ~800 tiles | ~24 |

**Time penalty** only kicks in after 60 seconds.

---

## After the Round

When a round ends, two things are shown on the world map:

- **Blue polyline** — the path you walked during the round
- **Yellow line** — straight-line distance from your guess to the target, with tile count

The **result overlay** (top-centre of screen) shows your score, distance, time, hints used, and the nearest teleport destination to the target — useful for learning efficient routing.

---

## How to Play

Navigate to the location on foot. A compass overlay in the top-right shows a rotating arrow pointing toward the target (relative to camera yaw) and the straight-line distance. Press **Guess Here!** in the plugin panel when you are standing where you think the target is.

---

## Leaderboard

Scores are submitted automatically after you complete all 4 daily rounds. The leaderboard in the plugin panel shows today's top 10 with your own rank below if you are outside the top 10.

### Clan leaderboard
Set **Clan Name** and **Clan Passkey** in the plugin config to scope scores to your clan. All clan members must use the same name and passkey.

---

## Personal Bests

For each location you have played, your best score, distance, and time are tracked and shown in a collapsible section at the bottom of the panel.

---

## Configuration

| Setting | Default | Description |
|---|---|---|
| Max Hints | 3 | Number of zoom-out reveals available per round (0–3) |
| Show Distance | On | Show tile distance in the hunt compass overlay |
| Clan Name | — | Clan name for leaderboard scoping |
| Clan Passkey | — | Shared passkey (hashed before sending) |
| Capture Mode | Off | Enable the capture tool and hotkey (dev only) |
| Capture Hotkey | Shift+G | Hotkey to capture the current tile (dev only) |
| Scout Hotkey | Shift+M | Mark current tile in batch_coords.txt without capturing (dev only) |

---

## Capture Tool (Developer Only)

The capture tool is only available in RuneLite developer mode (`--developer-mode` flag). It is not exposed to normal users.

### Single capture (manual)

1. Enable **Capture Mode** in the plugin config.
2. Stand on the tile you want to add.
3. Press **Shift+G** (configurable).
4. Enter a name in the dialog that appears.

Files are saved to:

```
%USERPROFILE%\.runelite\geoguessr-rs\
    captures.json
    {x}_{y}_{plane}.png
```

Capturing the same tile twice is detected and skipped. Captures are loaded into the location pool automatically on next plugin start.

### Viewport mode

Enable **Capture Viewport** in the plugin config to capture the full game viewport instead of the minimap. Viewport screenshots look like real GeoGuessr — isometric 3D game view rather than a top-down tile map. The hint system still applies: the image is cropped progressively to reveal more context across hints.

Hints for viewport images use the same proportional zoom-out logic as minimap images, so no configuration changes are needed.

### Scout mode

Scout mode lets you build a coordinate list while playing normally — no screenshots taken, just coordinates recorded.

1. Enable **Capture Mode** in the config.
2. Walk or teleport to an interesting location.
3. Press **Shift+M** (configurable via **Scout Hotkey**).
4. Enter a name in the dialog.
5. The tile is appended to `batch_coords.txt` immediately.

Repeat across a play session to build up a list of hundreds of locations. Then do a second pass in batch capture mode to take the actual screenshots.

### Batch capture

To mass-produce captures from a pre-built coordinate list:

```
%USERPROFILE%\.runelite\geoguessr-rs\batch_coords.txt
```

Format — one location per line, lines starting with `#` are ignored:

```
# name,x,y[,plane]
Grand Exchange,3165,3487,0
Varrock Square,3210,3424,0
Lumbridge Castle,3222,3219,0
```

When a batch file is present the capture overlay shows the current target, its name, and how many tiles away you are. Press **Shift+G** when you arrive and the location is saved automatically with the preset name — no dialog. The overlay then advances to the next target.

Workflow:

1. Prepare `batch_coords.txt` with your coordinate list (hundreds of lines is fine).
2. Start RuneLite in dev mode with **Capture Mode** on.
3. Teleport or walk to each location (fairy rings, spirit trees, and teleport tablets cover most of the map).
4. When the overlay shows ≤ 5 tiles, press **Shift+G** — saved and advanced automatically.
5. Duplicate coordinates are silently skipped.

---

## Building

```bash
./gradlew build        # compile + test
./gradlew shadowJar    # fat JAR → build/libs/geoguessr-rs-*-all.jar
./gradlew run          # start RuneLite in dev mode with plugin loaded
```

This plugin is not on the RuneLite plugin hub. Run it via `./gradlew run` during development, or load the shadow JAR manually in RuneLite developer mode.
