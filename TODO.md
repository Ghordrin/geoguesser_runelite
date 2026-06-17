# GeoGuessr RS — TODO

## Done
- [x] Core Hunt mode with compass overlay (direction + distance)
- [x] Compass rotates with camera yaw
- [x] Capture mode: Shift+G saves minimap PNG + entry to captures.json
- [x] Captures loaded at startup, images loaded from disk automatically
- [x] Duplicate tile detection on capture (warns and skips)
- [x] Guess Here! button replaces auto-arrival in Hunt mode
- [x] Points scored by distance — exponential decay (150-tile half-score, ~24 pts at 800 tiles)
- [x] Debug world-map pin (yellow, snap-to-edge, jump-on-click) during active round
- [x] Target tile highlight in overworld (gold outline) during active round
- [x] Hint system (3 static hints per location from JSON)
- [x] Classic mode: guess by clicking world map
- [x] Remove bundled test locations — game is crowdsourced-only

## In Progress / Known Issues
- [ ] **Capture minimap alignment** — preview may be offset NW depending on actual
      drawInstanceMap sprite size. Run with debug logging enabled and check the
      "drawInstanceMap: WxH sprite padX=N padY=N" line to verify padding is correct.
- [ ] **Classic mode world-map widget ID** — interface 595 / child 18 needs in-game
      verification. If clicking the world map does nothing in Classic mode, open the
      RuneLite dev tools (Examine widget) and find the correct component ID.

## Capture Workflow
- [ ] Add difficulty selector in the capture prompt (Easy / Medium / Hard)
- [ ] Add hint entry UI in the capture prompt (pre-fill up to 3 hints)
- [ ] Show "No locations captured yet — enable Capture Mode and press Shift+G"
      message in panel when pool is empty
- [ ] Validate captured WorldPoint is in a sensible OSRS coordinate range before saving

## Gameplay
- [ ] Score animation / visual feedback on round end
- [ ] Show distance to target on result screen ("You were X tiles away")
- [ ] Configurable round count (e.g., 5-round series with total score)
- [ ] Timer display in panel (mirrors compass elapsed seconds)
- [ ] Difficulty filter UI — allow player to choose Easy / Medium / Hard / Random
      in the panel, not just config

## Location Data
- [ ] Capture tag editor — let capturer add tags (region, type, etc.) after the fact
- [ ] Location pack export: button to copy captures.json path / open folder
- [ ] Location pack import: drop a JSON file into the captures dir and reload
- [ ] Deduplication across pack imports (same tile from different sources)

## Polish
- [ ] Panel shows clue image scaled with correct aspect ratio (no distortion)
- [ ] Result overlay shows actual target name + distance tile count
- [ ] World-map result pin (red) also uses snap-to-edge so it's always findable
- [ ] Overlay positions configurable (compass, result overlay)
