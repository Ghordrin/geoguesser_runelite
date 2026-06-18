package com.geoguessrrs.capture;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.SpritePixels;
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
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
					null,
					"Capture failed — the minimap scene wasn't available.\nMake sure you're fully loaded in-game.",
					"Capture Failed",
					JOptionPane.WARNING_MESSAGE));
				return;
			}

			final WorldPoint finalPos = pos;
			final BufferedImage finalImage = image;
			SwingUtilities.invokeLater(() -> promptAndSave(finalPos, finalImage));
		});
	}

	private void promptAndSave(WorldPoint pos, BufferedImage image)
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
				save(pos, image, name.trim());
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

	private void save(WorldPoint pos, BufferedImage image, String name) throws IOException
	{
		CAPTURE_DIR.mkdirs();
		String filename = pos.getX() + "_" + pos.getY() + "_" + pos.getPlane() + ".png";
		ImageIO.write(image, "PNG", new File(CAPTURE_DIR, filename));

		GeoLocation entry = new GeoLocation();
		entry.setId(pos.getX() + "_" + pos.getY() + "_" + pos.getPlane());
		entry.setName(name);
		entry.setX(pos.getX());
		entry.setY(pos.getY());
		entry.setPlane(pos.getPlane());
		entry.setImage(filename);
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
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
					null,
					"A capture already exists at this exact tile (" + entry.getX() + ", " + entry.getY() + ").\nSkipping.",
					"Duplicate Skipped",
					JOptionPane.WARNING_MESSAGE));
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
