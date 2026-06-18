package com.geoguessrrs.capture;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.geoguessrrs.hint.NpcHintProvider;
import com.geoguessrrs.hint.RegionHintProvider;
import com.geoguessrrs.locations.GeoLocation;
import com.geoguessrrs.locations.LocationDatabase;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.SpritePixels;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;

@Slf4j
public class CaptureService
{
	public static final File CAPTURE_DIR = new File(RuneLite.RUNELITE_DIR, "geoguessr-rs");
	private static final int SCENE_TILES    = 104;
	private static final int PIXELS_PER_TILE = 4;
	private static final int SCENE_SIZE     = SCENE_TILES * PIXELS_PER_TILE; // 416

	// All captures saved at the same radius; difficulty zooms in at display time
	public static final int CAPTURE_RADIUS = 44;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private NpcHintProvider npcHintProvider;

	@Inject
	private RegionHintProvider regionHintProvider;

	@Inject
	private LocationDatabase locationDatabase;

	@Inject
	private Gson gson;

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	/** Called from the hotkey listener (EDT). */
	public void captureAndPrompt()
	{
		clientThread.invoke(() ->
		{
			WorldPoint pos = client.getLocalPlayer() != null
				? client.getLocalPlayer().getWorldLocation()
				: null;

			BufferedImage image = renderMinimap();

			if (pos == null || image == null)
			{
				log.warn("Capture failed: pos={} image={} — try moving to a loaded area", pos, image);
				SwingUtilities.invokeLater(() -> javax.swing.JOptionPane.showMessageDialog(
					null,
					"Capture failed — the minimap scene wasn't available.\nMake sure you're fully loaded in-game.",
					"Capture Failed",
					javax.swing.JOptionPane.WARNING_MESSAGE));
				return;
			}

			List<String> autoHints = generateHints(pos);
			SwingUtilities.invokeLater(() -> promptAndSave(pos, image, autoHints));
		});
	}

	private List<String> generateHints(WorldPoint pos)
	{
		List<String> hints = new ArrayList<>();

		String regionHint = regionHintProvider.getHintForLocation(pos);
		if (regionHint != null)
		{
			hints.add(regionHint);
		}

		String npcHint = npcHintProvider.getHintForLocation(pos);
		if (npcHint != null)
		{
			hints.add(npcHint);
		}

		String landmarkHint = buildLandmarkHint();
		if (landmarkHint != null)
		{
			hints.add(landmarkHint);
		}

		return hints;
	}

	/** Scans nearby scene objects for notable landmarks and structures. */
	private String buildLandmarkHint()
	{
		Tile[][][] tiles = client.getScene().getTiles();
		if (tiles == null) return null;
		int plane = client.getTopLevelWorldView().getPlane();
		Set<String> labels = new LinkedHashSet<>();

		for (int tx = 0; tx < 104; tx++)
		{
			for (int ty = 0; ty < 104; ty++)
			{
				Tile tile = tiles[plane][tx][ty];
				if (tile == null) continue;
				for (GameObject obj : tile.getGameObjects())
				{
					if (obj == null) continue;
					String label = landmarkLabel(obj.getId());
					if (label != null) labels.add(label);
				}
			}
		}

		if (labels.isEmpty()) return null;
		return "Visible: " + String.join(", ", labels) + ".";
	}

	private String landmarkLabel(int objectId)
	{
		ObjectComposition def = client.getObjectDefinition(objectId);
		if (def == null) return null;
		if (def.getImpostorIds() != null)
		{
			ObjectComposition imp = def.getImpostor();
			if (imp != null) def = imp;
		}
		String n = def.getName();
		if (n == null || n.isEmpty() || n.equals("null")) return null;
		String lower = n.toLowerCase().trim();

		// Functional landmarks
		if (lower.contains("bank booth") || lower.contains("bank chest") || lower.contains("bank counter"))
			return "Bank";
		if (lower.contains("grand exchange"))    return "Grand Exchange";
		if (lower.contains("furnace"))           return "Furnace";
		if (lower.contains("altar"))             return "Prayer Altar";
		if (lower.contains("anvil"))             return "Anvil";
		if (lower.contains("fairy ring"))        return "Fairy Ring";
		if (lower.contains("spinning wheel"))    return "Spinning Wheel";
		if (lower.contains("loom"))              return "Loom";
		if (lower.equals("range") || lower.contains("cooking range")) return "Cooking Range";
		if (lower.contains("general store"))     return "General Store";
		if (lower.contains("spirit tree"))       return "Spirit Tree";
		if (lower.equals("portal") || lower.contains("house portal")) return "House Portal";
		if (lower.contains("portal nexus"))      return "Portal Nexus";
		if (lower.contains("slayer tower"))      return "Slayer Tower";
		if (lower.contains("tanning rack"))      return "Tanner";

		// Location-specific named objects — anything whose name contains a place name
		String[] placeKeywords = {
			"lumbridge", "varrock", "falador", "edgeville", "camelot", "ardougne",
			"yanille", "al kharid", "seers", "barbarian", "rellekka", "fremennik",
			"karamja", "brimhaven", "morytania", "canifis", "slayer", "kourend",
			"hosidius", "shayzien", "prifddinas", "taverley", "burthorpe",
			"port sarim", "draynor", "wizard", "legends", "champions", "gnome",
			"agility", "duel arena", "pest control", "castle wars"
		};
		for (String kw : placeKeywords)
		{
			if (lower.contains(kw)) return n; // return the actual object name as the hint
		}

		return null;
	}

	private void promptAndSave(WorldPoint pos, BufferedImage image, List<String> autoHints)
	{
		String name = (String) JOptionPane.showInputDialog(
			null,
			"Enter a name for this location:",
			"Capture Location",
			JOptionPane.PLAIN_MESSAGE,
			null,
			null,
			pos.getX() + ", " + pos.getY()
		);

		if (name == null || name.isBlank())
		{
			return;
		}

		executor.submit(() ->
		{
			try
			{
				save(pos, image, name.trim(), autoHints);
			}
			catch (IOException e)
			{
				log.error("Failed to save capture", e);
			}
		});
	}

	private BufferedImage renderMinimap()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}

		SpritePixels sprite = client.drawInstanceMap(client.getTopLevelWorldView().getPlane());
		if (sprite == null)
		{
			log.debug("drawInstanceMap returned null");
			return null;
		}

		int w = sprite.getWidth();
		int h = sprite.getHeight();
		if (w < SCENE_SIZE || h < SCENE_SIZE)
		{
			log.debug("Sprite too small: {}x{}", w, h);
			return null;
		}

		// Compute padding dynamically — drawInstanceMap output size varies
		int padX = (w - SCENE_SIZE) / 2;
		int padY = (h - SCENE_SIZE) / 2;
		log.debug("drawInstanceMap: {}x{} sprite padX={} padY={} radius={}", w, h, padX, padY, CAPTURE_RADIUS);

		BufferedImage raw = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		raw.setRGB(0, 0, w, h, sprite.getPixels(), 0, w);

		// Copy to ARGB so icon transparency composites correctly over terrain
		BufferedImage scene = new BufferedImage(SCENE_SIZE, SCENE_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D sceneG = scene.createGraphics();
		sceneG.drawImage(raw.getSubimage(padX, padY, SCENE_SIZE, SCENE_SIZE), 0, 0, null);
		MinimapIconPainter.paint(sceneG, client, client.getTopLevelWorldView().getPlane());
		sceneG.dispose();

		// Crop centred on the player's actual position within the scene image
		LocalPoint lp = player.getLocalLocation();
		int sceneX = lp.getSceneX();
		int sceneY = lp.getSceneY();
		// Scene Y=0 is south; image row 0 is north — flip Y
		int playerPixelX = sceneX * PIXELS_PER_TILE + PIXELS_PER_TILE / 2;
		int playerPixelY = (SCENE_TILES - 1 - sceneY) * PIXELS_PER_TILE + PIXELS_PER_TILE / 2;

		int diameter = CAPTURE_RADIUS * 2;
		int cropX = Math.max(0, Math.min(playerPixelX - CAPTURE_RADIUS, SCENE_SIZE - diameter));
		int cropY = Math.max(0, Math.min(playerPixelY - CAPTURE_RADIUS, SCENE_SIZE - diameter));
		BufferedImage cropped = scene.getSubimage(cropX, cropY, diameter, diameter);
		return CircleMask.apply(cropped);
	}

	private void save(WorldPoint pos, BufferedImage image, String name, List<String> hints) throws IOException
	{
		CAPTURE_DIR.mkdirs();
		String filename = pos.getX() + "_" + pos.getY() + "_" + pos.getPlane() + ".png";
		ImageIO.write(image, "PNG", new File(CAPTURE_DIR, filename));

		GeoLocation entry = new GeoLocation();
		// Use coordinate-based ID to avoid collisions from normalised names
		entry.setId(pos.getX() + "_" + pos.getY() + "_" + pos.getPlane());
		entry.setName(name);
		entry.setX(pos.getX());
		entry.setY(pos.getY());
		entry.setPlane(pos.getPlane());
		entry.setImage(filename);
		entry.setHints(hints);
		entry.setTags(new ArrayList<>());

		appendCapture(entry);
		locationDatabase.addCapture(entry);
		log.info("Captured '{}' at {} → {}", name, pos, filename);
	}

	private void appendCapture(GeoLocation entry) throws IOException
	{
		File capturesFile = new File(CAPTURE_DIR, "captures.json");
		List<GeoLocation> entries = new ArrayList<>();

		if (capturesFile.exists())
		{
			try (InputStreamReader r = new InputStreamReader(new FileInputStream(capturesFile), StandardCharsets.UTF_8))
			{
				Type listType = new TypeToken<List<GeoLocation>>() {}.getType();
				List<GeoLocation> existing = gson.fromJson(r, listType);
				if (existing != null)
				{
					entries.addAll(existing);
				}
			}
		}

		// Duplicate check — same (x, y, plane)
		for (GeoLocation existing : entries)
		{
			if (existing.getX() == entry.getX()
				&& existing.getY() == entry.getY()
				&& existing.getPlane() == entry.getPlane())
			{
				log.info("Duplicate location at ({},{},{}), skipping", entry.getX(), entry.getY(), entry.getPlane());
				SwingUtilities.invokeLater(() -> javax.swing.JOptionPane.showMessageDialog(
					null,
					"A capture already exists at this exact tile (" + entry.getX() + ", " + entry.getY() + ").\nSkipping.",
					"Duplicate Skipped",
					javax.swing.JOptionPane.WARNING_MESSAGE));
				return;
			}
		}

		entries.add(entry);

		try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(capturesFile), StandardCharsets.UTF_8))
		{
			gson.toJson(entries, w);
		}
	}

	public void shutdown()
	{
		executor.shutdownNow();
	}
}
