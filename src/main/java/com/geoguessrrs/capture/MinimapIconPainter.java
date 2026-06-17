package com.geoguessrrs.capture;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Tile;
import net.runelite.api.WallObject;

/**
 * Paints minimap-style letter icons onto a drawInstanceMap scene image.
 * Must be called on the client thread.
 */
public class MinimapIconPainter
{
	private static final int SCENE_TILES      = 104;
	private static final int PIXELS_PER_TILE  = 4;
	private static final int ICON             = 16;
	private static final int SUPPRESSION_DIST = 6;

	private enum IconType
	{
		BANK, GE, FURNACE, ANVIL, ALTAR, WATER, RANGE, FAIRY_RING, SHOP, HOUSE, SPINNING_WHEEL, LOOM, TANNING
	}

	// label, background colour
	private static final String[] LABELS = {
		"B",  // BANK
		"GE", // GE
		"F",  // FURNACE
		"A",  // ANVIL
		"P",  // ALTAR (prayer)
		"W",  // WATER
		"R",  // RANGE
		"FR", // FAIRY_RING
		"G",  // SHOP (general store)
		"H",  // HOUSE portal
		"SW", // SPINNING_WHEEL
		"LM", // LOOM
		"TN", // TANNING
	};

	private static final Color[] COLORS = {
		new Color(0x001480), // BANK      – navy
		new Color(0xA07800), // GE        – gold
		new Color(0xAA2800), // FURNACE   – dark red-orange
		new Color(0x484848), // ANVIL     – dark grey
		new Color(0x5000A0), // ALTAR     – purple
		new Color(0x005878), // WATER     – teal
		new Color(0x7A2800), // RANGE     – dark brown-orange
		new Color(0xA000A0), // FAIRY_RING– magenta
		new Color(0x606000), // SHOP      – olive
		new Color(0x005000), // HOUSE     – dark green
		new Color(0x6B3A2A), // SPINNING_WHEEL – brown
		new Color(0x5A3018), // LOOM      – dark brown
		new Color(0x7A5030), // TANNING   – tan-brown
	};

	public static void paint(Graphics2D g, Client client, int plane)
	{
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		Tile[][][] sceneTiles = client.getScene().getTiles();

		// Pass 1 — collect all matching tiles
		List<int[]> detections = new ArrayList<>();
		for (int tx = 0; tx < SCENE_TILES; tx++)
		{
			for (int ty = 0; ty < SCENE_TILES; ty++)
			{
				Tile tile = sceneTiles[plane][tx][ty];
				if (tile == null) continue;
				IconType type = resolveIcon(tile, client);
				if (type != null)
				{
					detections.add(new int[]{tx, ty, type.ordinal()});
				}
			}
		}

		// Pass 2 — suppress same-type icons within SUPPRESSION_DIST tiles
		List<int[]> accepted = new ArrayList<>();
		outer:
		for (int[] det : detections)
		{
			for (int[] acc : accepted)
			{
				if (acc[2] == det[2]
					&& Math.abs(acc[0] - det[0]) <= SUPPRESSION_DIST
					&& Math.abs(acc[1] - det[1]) <= SUPPRESSION_DIST)
				{
					continue outer;
				}
			}
			accepted.add(det);
		}

		// Pass 3 — draw
		IconType[] types = IconType.values();
		for (int[] acc : accepted)
		{
			int cx = acc[0] * PIXELS_PER_TILE + PIXELS_PER_TILE / 2;
			int cy = (SCENE_TILES - 1 - acc[1]) * PIXELS_PER_TILE + PIXELS_PER_TILE / 2;
			drawIcon(g, types[acc[2]], cx, cy);
		}
	}

	// -------------------------------------------------------------------------
	// Detection
	// -------------------------------------------------------------------------

	private static IconType resolveIcon(Tile tile, Client client)
	{
		for (GameObject obj : tile.getGameObjects())
		{
			if (obj == null) continue;
			IconType t = iconTypeForId(obj.getId(), client);
			if (t != null) return t;
		}
		if (tile.getWallObject() != null)
		{
			IconType t = iconTypeForId(tile.getWallObject().getId(), client);
			if (t != null) return t;
		}
		DecorativeObject dec = tile.getDecorativeObject();
		if (dec != null)
		{
			IconType t = iconTypeForId(dec.getId(), client);
			if (t != null) return t;
		}
		return null;
	}

	private static IconType iconTypeForId(int objectId, Client client)
	{
		ObjectComposition def = client.getObjectDefinition(objectId);
		if (def.getImpostorIds() != null)
		{
			ObjectComposition imp = def.getImpostor();
			if (imp != null) def = imp;
		}
		return categorize(def.getName());
	}

	private static IconType categorize(String name)
	{
		if (name == null || name.isEmpty() || name.equals("null")) return null;
		String n = name.toLowerCase();

		// Grand Exchange before generic bank/shop checks
		if (n.contains("grand exchange") || n.contains("exchange booth"))
		{
			return IconType.GE;
		}
		if (n.contains("bank booth") || n.contains("bank chest") || n.contains("bank counter")
			|| n.contains("bank deposit") || n.equals("bank"))
		{
			return IconType.BANK;
		}
		if (n.contains("furnace"))
		{
			return IconType.FURNACE;
		}
		if (n.contains("anvil"))
		{
			return IconType.ANVIL;
		}
		if (n.contains("altar"))
		{
			return IconType.ALTAR;
		}
		if (n.contains("water pump") || n.contains("water source")
			|| n.equals("well") || n.contains("wishing well") || n.contains("old well")
			|| n.contains("fountain of")
			|| n.equals("sink") || n.contains("kitchen sink"))
		{
			return IconType.WATER;
		}
		if ((n.equals("range") || n.contains("cooking range") || n.endsWith(" range"))
			&& !n.contains("archery"))
		{
			return IconType.RANGE;
		}
		if (n.contains("fairy ring"))
		{
			return IconType.FAIRY_RING;
		}
		if (n.contains("general store")
			|| ((n.contains(" shop") || n.equals("shop")) && !n.contains("workshop")))
		{
			return IconType.SHOP;
		}
		if (n.equals("portal") || n.contains("house portal"))
		{
			return IconType.HOUSE;
		}
		if (n.contains("spinning wheel"))
		{
			return IconType.SPINNING_WHEEL;
		}
		if (n.equals("loom"))
		{
			return IconType.LOOM;
		}
		if (n.contains("tanning rack") || n.equals("tanner"))
		{
			return IconType.TANNING;
		}
		return null;
	}

	// -------------------------------------------------------------------------
	// Icon drawing — uniform letter-square style
	// -------------------------------------------------------------------------

	private static void drawIcon(Graphics2D g, IconType type, int cx, int cy)
	{
		int idx = type.ordinal();
		drawLetterIcon(g, LABELS[idx], COLORS[idx], cx, cy);
	}

	private static void drawLetterIcon(Graphics2D g, String label, Color bg, int cx, int cy)
	{
		int r = ICON / 2;
		// Background square with 1px dark border
		g.setColor(bg.darker());
		g.fillRect(cx - r - 1, cy - r - 1, ICON + 2, ICON + 2);
		g.setColor(bg);
		g.fillRect(cx - r, cy - r, ICON, ICON);
		// White label
		g.setColor(Color.WHITE);
		int fontSize = label.length() == 1 ? 8 : 6;
		g.setFont(new Font("Arial", Font.BOLD, fontSize));
		FontMetrics fm = g.getFontMetrics();
		int tx = cx - fm.stringWidth(label) / 2;
		int ty = cy + fm.getAscent() / 2 - 1;
		g.drawString(label, tx, ty);
	}
}
