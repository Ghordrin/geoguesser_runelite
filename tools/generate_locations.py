#!/usr/bin/env python3
"""
GeoGuessr RS — Map Tile Location Generator

Fetches OSRS map tiles from maps.runescape.wiki, crops them to 88x88 circular
minimap images, and writes locations.json for the GeoGuessr RS plugin.

Usage:
    python tools/generate_locations.py           # generate all locations
    python tools/generate_locations.py --test    # fetch one tile and save it for visual check
    python tools/generate_locations.py --version # print detected cache version and exit

Dependencies (pip install -r tools/requirements.txt):
    requests, Pillow
"""

import argparse
import json
import sys
import time
from io import BytesIO
from pathlib import Path

try:
    import requests
    from PIL import Image, ImageDraw
except ImportError:
    print("Missing dependencies. Run:  pip install -r tools/requirements.txt")
    sys.exit(1)

# ── Paths ──────────────────────────────────────────────────────────────────────

REPO_ROOT  = Path(__file__).resolve().parent.parent
IMG_DIR    = REPO_ROOT / "src/main/resources/locations"
JSON_FILE  = REPO_ROOT / "src/main/resources/locations.json"

# ── Tile constants ─────────────────────────────────────────────────────────────

TILE_BASE   = "https://maps.runescape.wiki/osrs/tiles"
MAP_ID      = 0          # Gielinor Surface
ZOOM        = 2          # 4 px per game tile; each tile image = 256×256 px = 64×64 tiles
CHUNK       = 64         # game tiles per tile-image edge at zoom 2
PX_PER_TILE = 4          # pixels per game tile at zoom 2
TILE_PX     = CHUNK * PX_PER_TILE   # 256 — tile image size in pixels
RADIUS      = 88         # px radius for the circular crop (22-tile radius; hints can zoom out to full)
SIZE        = RADIUS * 2            # 176 — final image size

# Probe newest-first by actually fetching a known tile (Varrock West Bank, tx=49, ty=53).
# Standard map versions use numeric suffix (_1, _2 …); letter suffixes are sailing/beta.
# Add new dates here as OSRS map updates are released.
VERSION_CANDIDATES = [
    "2025-04-23_1",
    "2025-03-05_1",
    "2025-01-22_1",
    "2024-10-30_1",
    "2024-07-03_1",
    "2024-04-24_1",
    "2024-01-17_1",
    "2023-10-25_1",
    "2023-07-26_1",
    "2023-05-24_1",
    "2023-03-22_1",
    "2023-01-25_1",
    "2022-11-30_1",
    "2022-09-28_1",
    "2022-07-27_1",
    "2022-03-23_1",
    "2021-11-17_1",
    "2021-06-09_1",
    "2020-11-04_1",
    "2020-04-15_1",
    "2019-10-31_1",  # confirmed working
]

# Known-good tile used for version probing (Varrock West Bank area)
_PROBE_TILE = (0, 49, 53)   # plane, tx, ty

# ── Curated location list ──────────────────────────────────────────────────────
# (id, name, world_x, world_y, plane, difficulty, hints)

LOCATIONS = [
    # ── Misthalin ──────────────────────────────────────────────────────────────
    ("lumbridge",           "Lumbridge Castle",       3222, 3218, 0, "EASY",   []),
    ("varrock_west_bank",   "Varrock West Bank",      3185, 3437, 0, "EASY",   []),
    ("varrock_east_bank",   "Varrock East Bank",      3253, 3422, 0, "MEDIUM", []),
    ("grand_exchange",      "Grand Exchange",         3165, 3487, 0, "EASY",   []),
    ("draynor_village",     "Draynor Village",        3093, 3244, 0, "MEDIUM", []),
    ("edgeville",           "Edgeville",              3087, 3502, 0, "MEDIUM", []),
    ("barbarian_village",   "Barbarian Village",      3082, 3420, 0, "HARD",   []),
    ("champions_guild",     "Champions' Guild",       3191, 3368, 0, "HARD",   []),
    ("al_kharid",           "Al Kharid",              3293, 3174, 0, "MEDIUM", []),
    ("duel_arena",          "Duel Arena",             3368, 3268, 0, "MEDIUM", []),

    # ── Asgarnia ───────────────────────────────────────────────────────────────
    ("falador_east_bank",   "Falador East Bank",      3013, 3355, 0, "EASY",   []),
    ("falador_west_bank",   "Falador West Bank",      2946, 3368, 0, "MEDIUM", []),
    ("port_sarim",          "Port Sarim",             3011, 3222, 0, "MEDIUM", []),
    ("rimmington",          "Rimmington",             2953, 3148, 0, "HARD",   []),
    ("taverley",            "Taverley",               2896, 3481, 0, "HARD",   []),
    ("burthorpe",           "Burthorpe",              2898, 3544, 0, "HARD",   []),
    ("goblin_village",      "Goblin Village",         2956, 3506, 0, "HARD",   []),

    # ── Kandarin ───────────────────────────────────────────────────────────────
    ("camelot",             "Camelot",                2757, 3477, 0, "MEDIUM", []),
    ("seers_village",       "Seers' Village",         2727, 3493, 0, "MEDIUM", []),
    ("catherby",            "Catherby",               2810, 3447, 0, "HARD",   []),
    ("ardougne_east_bank",  "East Ardougne Bank",     2655, 3283, 0, "EASY",   []),
    ("ardougne_west_bank",  "West Ardougne",          2526, 3321, 0, "HARD",   []),
    ("yanille",             "Yanille",                2608, 3093, 0, "MEDIUM", []),
    ("fishing_guild",       "Fishing Guild",          2591, 3421, 0, "HARD",   []),
    ("tree_gnome_stronghold","Tree Gnome Stronghold", 2461, 3444, 0, "MEDIUM", []),
    ("witchaven",           "Witchaven",              2710, 3270, 0, "HARD",   []),

    # ── Morytania ──────────────────────────────────────────────────────────────
    ("canifis",             "Canifis",                3494, 3487, 0, "MEDIUM", []),
    ("barrows",             "Barrows",                3565, 3313, 0, "HARD",   []),
    ("burgh_de_rott",       "Burgh de Rott",          3494, 3208, 0, "HARD",   []),
    ("port_phasmatys",      "Port Phasmatys",         3700, 3503, 0, "HARD",   []),

    # ── Fremennik ──────────────────────────────────────────────────────────────
    ("rellekka",            "Rellekka",               2660, 3657, 0, "MEDIUM", []),
    ("neitiznot",           "Neitiznot",              2336, 3807, 0, "HARD",   []),
    ("jatizso",             "Jatizso",                2415, 3804, 0, "HARD",   []),

    # ── Karamja ────────────────────────────────────────────────────────────────
    ("brimhaven",           "Brimhaven",              2762, 3178, 0, "HARD",   []),
    ("shilo_village",       "Shilo Village",          2852, 2982, 0, "HARD",   []),
    ("tai_bwo_wannai",      "Tai Bwo Wannai",         2794, 3059, 0, "HARD",   []),

    # ── Desert ─────────────────────────────────────────────────────────────────
    ("pollnivneach",        "Pollnivneach",           3352, 2962, 0, "HARD",   []),
    ("nardah",              "Nardah",                 3426, 2894, 0, "HARD",   []),
    ("sophanem",            "Sophanem",               3316, 2750, 0, "HARD",   []),
    ("agility_pyramid",     "Agility Pyramid",        3363, 2840, 0, "HARD",   []),

    # ── Zeah / Kourend ─────────────────────────────────────────────────────────
    ("hosidius",            "Hosidius",               1739, 3527, 0, "MEDIUM", []),
    ("shayzien",            "Shayzien",               1484, 3626, 0, "HARD",   []),
    ("port_piscarilius",    "Port Piscarilius",       1803, 3791, 0, "HARD",   []),
    ("lovakengj",           "Lovakengj",              1487, 3887, 0, "HARD",   []),
    ("arceuus",             "Arceuus",                1624, 3733, 0, "HARD",   []),
    ("woodcutting_guild",   "Woodcutting Guild",      1659, 3505, 0, "HARD",   []),
    ("mount_quidamortem",   "Mount Quidamortem",      1440, 3556, 0, "HARD",   []),

    # ── Miscellaneous ──────────────────────────────────────────────────────────
    ("pest_control",        "Pest Control",           2657, 2659, 0, "HARD",   []),
    ("entrana",             "Entrana",                2827, 3341, 0, "HARD",   []),
    ("castle_wars",         "Castle Wars",            2443, 3092, 0, "MEDIUM", []),
    ("lletya",              "Lletya",                 2353, 3163, 0, "HARD",   []),
]

# ── Version detection ──────────────────────────────────────────────────────────

def detect_version() -> str:
    """Find the newest version that actually serves tiles by probing a known tile."""
    print("Detecting cache version (probing tile server)...")
    plane, tx, ty = _PROBE_TILE
    for v in VERSION_CANDIDATES:
        url = f"{TILE_BASE}/{MAP_ID}_{v}/{ZOOM}/{plane}_{tx}_{ty}.png"
        try:
            r = requests.get(url, timeout=8)
            if r.status_code == 200:
                print(f"  Found: {v}")
                return v
        except requests.RequestException:
            pass
    fallback = VERSION_CANDIDATES[-1]   # 2019-10-31_1 is confirmed working
    print(f"  Warning: no newer version found, falling back to {fallback}")
    return fallback

# ── Tile fetching ──────────────────────────────────────────────────────────────

_tile_cache: dict[tuple, Image.Image | None] = {}

def fetch_tile(version: str, plane: int, tx: int, ty: int) -> Image.Image | None:
    key = (version, plane, tx, ty)
    if key in _tile_cache:
        return _tile_cache[key]

    url = f"{TILE_BASE}/{MAP_ID}_{version}/{ZOOM}/{plane}_{tx}_{ty}.png"
    try:
        r = requests.get(url, timeout=10)
        if r.status_code == 200:
            img = Image.open(BytesIO(r.content)).convert("RGBA")
            _tile_cache[key] = img
            return img
        elif r.status_code == 404:
            _tile_cache[key] = None
            return None
        else:
            print(f"    HTTP {r.status_code}: {url}")
            _tile_cache[key] = None
            return None
    except requests.RequestException as e:
        print(f"    Request error: {e}")
        _tile_cache[key] = None
        return None

# ── Coordinate math ────────────────────────────────────────────────────────────

def world_to_tile_pixel(wx: int, wy: int) -> tuple[int, int, int, int]:
    """
    Returns (tile_x, tile_y, pixel_col, pixel_row) within the tile image.

    At zoom 2:
      - Each tile image covers CHUNK×CHUNK (64×64) game tiles
      - Tile image is TILE_PX×TILE_PX (256×256) pixels
      - Column 0 = west edge of tile  (wx = tile_x * 64)
      - Row 0    = north edge of tile (wy = (tile_y+1)*64 - 1)  ← Y is flipped
    """
    tile_x  = wx // CHUNK
    tile_y  = wy // CHUNK
    local_x = wx % CHUNK          # 0 = west, 63 = east
    local_y = wy % CHUNK          # 0 = south, 63 = north
    col = local_x * PX_PER_TILE + PX_PER_TILE // 2          # centre of game tile
    row = (CHUNK - 1 - local_y) * PX_PER_TILE + PX_PER_TILE // 2  # Y flipped
    return tile_x, tile_y, col, row


def get_crop(wx: int, wy: int, plane: int, version: str, radius: int = RADIUS) -> Image.Image:
    """
    Build a (radius*2) × (radius*2) RGBA crop centred on (wx, wy).
    Fetches neighbouring tiles when the crop crosses a tile boundary.
    """
    tx, ty, col, row = world_to_tile_pixel(wx, wy)

    # Which neighbours do we need?
    need_left  = (col - radius) < 0
    need_right = (col + radius) >= TILE_PX
    need_up    = (row - radius) < 0           # row decreases going north → need higher ty
    need_down  = (row + radius) >= TILE_PX    # row increases going south → need lower ty

    min_tx = tx - (1 if need_left  else 0)
    max_tx = tx + (1 if need_right else 0)
    min_ty = ty - (1 if need_down  else 0)    # south neighbour has lower tile_y
    max_ty = ty + (1 if need_up    else 0)    # north neighbour has higher tile_y

    grid_w = (max_tx - min_tx + 1) * TILE_PX
    grid_h = (max_ty - min_ty + 1) * TILE_PX
    canvas = Image.new("RGBA", (grid_w, grid_h), (0, 0, 0, 255))

    for cx_tx in range(min_tx, max_tx + 1):
        for cy_ty in range(min_ty, max_ty + 1):
            tile_img = fetch_tile(version, plane, cx_tx, cy_ty)
            if tile_img is None:
                continue
            # Higher ty = further north = top of canvas (lower canvas Y)
            paste_x = (cx_tx - min_tx) * TILE_PX
            paste_y = (max_ty - cy_ty) * TILE_PX
            canvas.paste(tile_img, (paste_x, paste_y))

    # Centre of target coordinate in canvas space
    centre_x = (tx - min_tx) * TILE_PX + col
    centre_y = (max_ty - ty) * TILE_PX + row

    left   = max(0, centre_x - radius)
    top    = max(0, centre_y - radius)
    right  = min(canvas.width,  centre_x + radius)
    bottom = min(canvas.height, centre_y + radius)
    crop   = canvas.crop((left, top, right, bottom))

    # Pad back to exact size if clamped (shouldn't happen with neighbour fetching)
    if crop.size != (SIZE, SIZE):
        padded = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 255))
        padded.paste(crop, (0, 0))
        crop = padded

    return crop

# ── Circular mask ──────────────────────────────────────────────────────────────

def apply_circle_mask(img: Image.Image) -> Image.Image:
    img  = img.convert("RGBA")
    mask = Image.new("L", img.size, 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, img.width, img.height), fill=255)
    img.putalpha(mask)
    return img

# ── Per-location generation ────────────────────────────────────────────────────

def generate_one(loc_id, name, wx, wy, plane, difficulty, hints, version) -> dict | None:
    image_name = f"{wx}_{wy}_{plane}.png"
    out_path   = IMG_DIR / image_name

    crop   = get_crop(wx, wy, plane, version)
    masked = apply_circle_mask(crop)
    masked.save(str(out_path))

    return {
        "id":         loc_id,
        "name":       name,
        "x":          wx,
        "y":          wy,
        "plane":      plane,
        "difficulty": difficulty,
        "image":      image_name,
        "tags":       [],
        "hints":      hints,
    }

# ── Test mode ──────────────────────────────────────────────────────────────────

def run_test(version: str):
    """
    Fetch the single tile containing Varrock West Bank (3185, 3437) and save
    it to tools/test_tile.png so you can open it and confirm the map looks right.
    Also saves the cropped 88x88 image to tools/test_crop.png.
    """
    wx, wy, plane = 3185, 3437, 0   # Varrock West Bank
    tx, ty, col, row = world_to_tile_pixel(wx, wy)

    print(f"Test coordinate: Varrock West Bank ({wx}, {wy})")
    print(f"  → tile ({tx}, {ty}), pixel ({col}, {row}) within tile")
    url = f"{TILE_BASE}/{MAP_ID}_{version}/{ZOOM}/{plane}_{tx}_{ty}.png"
    print(f"  → URL: {url}")

    tile = fetch_tile(version, plane, tx, ty)
    if tile is None:
        print("\n  FAIL — tile returned 404 or error.")
        print("  Try opening the URL above in a browser to diagnose.")
        return False

    # Save full tile for inspection
    tile_path = Path(__file__).parent / "test_tile.png"
    tile.save(str(tile_path))
    print(f"\n  Saved full tile ({tile.size[0]}×{tile.size[1]}px) → {tile_path}")

    # Draw a red cross-hair on the tile at the target pixel for visual verification
    debug = tile.copy()
    draw  = ImageDraw.Draw(debug)
    draw.line([(col - 8, row), (col + 8, row)], fill=(255, 0, 0, 255), width=2)
    draw.line([(col, row - 8), (col, row + 8)], fill=(255, 0, 0, 255), width=2)
    debug_path = Path(__file__).parent / "test_tile_marked.png"
    debug.save(str(debug_path))
    print(f"  Saved marked tile (red cross = target tile) → {debug_path}")

    # Save cropped result
    IMG_DIR.mkdir(parents=True, exist_ok=True)
    crop   = get_crop(wx, wy, plane, version)
    masked = apply_circle_mask(crop)
    crop_path = Path(__file__).parent / "test_crop.png"
    masked.save(str(crop_path))
    print(f"  Saved 88×88 crop → {crop_path}")

    print("\n  SUCCESS — open the three images above to verify the map looks correct.")
    return True

# ── Main ───────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="GeoGuessr RS location generator")
    parser.add_argument("--test",    action="store_true", help="Fetch one tile to verify URL works")
    parser.add_argument("--version", action="store_true", help="Print detected cache version and exit")
    args = parser.parse_args()

    version = detect_version()

    if args.version:
        print(f"Cache version: {version}")
        return

    if args.test:
        run_test(version)
        return

    # ── Full generation ────────────────────────────────────────────────────────
    print(f"\nGenerating {len(LOCATIONS)} locations...")
    IMG_DIR.mkdir(parents=True, exist_ok=True)

    results = []
    failed  = []
    for i, (loc_id, name, wx, wy, plane, difficulty, hints) in enumerate(LOCATIONS, 1):
        print(f"  [{i:02d}/{len(LOCATIONS)}] {name} ({wx}, {wy}, plane={plane})")
        try:
            entry = generate_one(loc_id, name, wx, wy, plane, difficulty, hints, version)
            if entry:
                results.append(entry)
        except Exception as e:
            print(f"    ERROR: {e}")
            failed.append(name)
        time.sleep(0.05)   # stay polite to the wiki tile server

    print(f"\nWriting {JSON_FILE} ...")
    with open(JSON_FILE, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2)

    print(f"\nDone — {len(results)} locations generated, {len(failed)} failed.")
    if failed:
        print(f"  Failed: {', '.join(failed)}")
    print(f"  Images → {IMG_DIR}")
    print(f"  JSON   → {JSON_FILE}")


if __name__ == "__main__":
    main()
