# GeoGuessr RS — RuneLite Plugin

A GeoGuessr-style location guessing game for Old School RuneScape. You are shown a minimap screenshot of a location somewhere in Gielinor and must figure out where it is — either by navigating there on foot or by clicking on the world map.

---

## Game Modes

### Hunt Mode (default)
You are shown a circular minimap clue and a compass overlay that points toward the target tile. Walk through the world using your knowledge of OSRS geography — the compass needle rotates with your camera, so it always points forward toward the target. When you think you are standing on the correct tile (or close enough), press the **Guess Here!** button in the plugin panel.

### Classic Mode
You are shown the same minimap clue but instead of walking you open the world map and click on the tile you believe matches the screenshot. Your click is recorded as your guess.

---

## Scoring

Scoring uses an exponential decay curve — guesses near the target score close to the maximum, and being in the wrong region or continent collapses your score to near zero.

| Distance | Score (no penalties) |
|---|---|
| 0 tiles (exact) | 5 000 |
| ~50 tiles (same area) | ~3 600 |
| ~150 tiles (nearby region) | ~1 800 |
| ~300 tiles (wrong region) | ~680 |
| ~500 tiles (wrong continent) | ~180 |
| ~800 tiles (Zeah / far miss) | ~24 |

**Hint penalty:** −500 pts per hint used.  
**Time penalty:** −5 pts per second after the first 60 seconds.

---

## Hints

Each round allows up to three hints (configurable). Hints are generated automatically when a location is captured, so they always describe the target tile:

1. **Hint 1** — names of nearby NPCs at the target location (_"Nearby: Banker, Guard."_)
2. **Hint 2** — the named region area (_"You are near Varrock."_)
3. **Hint 3** — any additional static hint written by the location author

---

## Difficulty

Locations are tagged Easy, Medium, or Hard at capture time based on how tight the minimap crop is:

| Difficulty | Minimap radius | Tile coverage |
|---|---|---|
| Easy | 72 px | ~18 tiles — wide view, many landmarks visible |
| Medium | 44 px | ~11 tiles — moderate context |
| Hard | 28 px | ~7 tiles — tight crop, very little context |

Set **Difficulty** in the plugin config to filter the location pool, or leave it on **Random** to draw from all difficulties.

---

## Contributing Locations

The game has no built-in locations — every location is captured by players and stored locally on your machine.

### How to capture

1. Open the plugin config and enable **Capture Mode**.
2. Navigate in-game to any interesting or tricky tile.
3. Press **Shift+G** (configurable) while standing on the tile.
4. A dialog will ask for a location name. The name you enter is shown to the guesser on the result screen.
5. The entry (minimap PNG + JSON metadata) is saved to:

```
%USERPROFILE%\.runelite\geoguessr-rs\
    captures.json          <- location index
    3161_3445_0.png        <- minimap crop (filename = x_y_plane)
    ...
```

Captures are loaded automatically the next time the plugin starts. You can share your `captures.json` and the accompanying PNG files with other players by dropping them into the same directory.

### Notes

- Capturing the same tile twice is detected and skipped automatically.
- Hints are auto-generated at capture time from NPCs visible on the scene. You can edit `captures.json` by hand afterward to refine or add extra hints.
- The minimap crop difficulty is set by whichever **Difficulty** you have selected in the config at the time of capture.

---

## Configuration

| Setting | Default | Description |
|---|---|---|
| Game Mode | Hunt | Hunt (walk there) or Classic (click world map) |
| Difficulty | Random | Location pool filter: Easy / Medium / Hard / Random |
| Max Hints | 3 | Number of hints available per round (0–5) |
| Show Distance | On | Display tile distance in the Hunt compass overlay |
| Capture Mode | Off | Enable the capture tool and hotkey |
| Capture Hotkey | Shift+G | Hotkey to capture the current tile |

---

## Installation

This plugin is not on the RuneLite plugin hub. To install it manually:

1. Clone or download this repository.
2. Build with Maven: `mvn package -DskipTests`
3. Start RuneLite with the `--developer-mode` flag.
4. Load the built jar via **RuneLite → Plugin Hub → Load from file**.
