package com.geoguessrrs.capture;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.geoguessrrs.GeoguessrConfig;
import com.geoguessrrs.locations.GeoLocation;
import com.geoguessrrs.locations.LocationDatabase;
import java.awt.AWTException;
import java.awt.Canvas;
import java.awt.Point;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
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

	public static final int CAPTURE_RADIUS = 44;

	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private LocationDatabase locationDatabase;
	@Inject private GeoguessrConfig config;
	@Inject private BatchCaptureManager batchManager;
	@Inject private Gson gson;

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	/**
	 * Entry point from the hotkey listener (EDT).
	 * Dispatches to either minimap or viewport capture based on config,
	 * then routes to batch auto-save or the name prompt.
	 */
	public void captureAndPrompt()
	{
		if (config.captureViewport())
		{
			// Capture the viewport immediately while the current frame is on screen.
			// Robot.createScreenCapture is safe to call from EDT.
			final BufferedImage viewportImg = captureViewport();
			if (viewportImg == null)
			{
				showCaptureError("Viewport capture failed — make sure the game window is visible.");
				return;
			}
			// Get world position from client thread, then dispatch
			clientThread.invoke(() ->
			{
				WorldPoint pos = client.getLocalPlayer() != null
					? client.getLocalPlayer().getWorldLocation()
					: null;
				final WorldPoint finalPos = pos;
				SwingUtilities.invokeLater(() -> dispatchCapture(finalPos, viewportImg));
			});
		}
		else
		{
			clientThread.invoke(() ->
			{
				WorldPoint pos = client.getLocalPlayer() != null
					? client.getLocalPlayer().getWorldLocation()
					: null;
				BufferedImage image = renderMinimap();
				if (pos == null || image == null)
				{
					log.warn("Minimap capture failed: pos={} image={}", pos, image);
					SwingUtilities.invokeLater(() -> showCaptureError(
						"Capture failed — the minimap wasn't available.\nMake sure you're fully loaded in-game."));
					return;
				}
				final WorldPoint finalPos  = pos;
				final BufferedImage finalImage = image;
				SwingUtilities.invokeLater(() -> dispatchCapture(finalPos, finalImage));
			});
		}
	}

	/**
	 * Routes the captured image either to the batch auto-save flow (if a batch file is loaded)
	 * or to the interactive name prompt. Always called on EDT.
	 */
	private void dispatchCapture(WorldPoint playerPos, BufferedImage image)
	{
		BatchCaptureManager.BatchTarget bt = batchManager.getCurrent();
		if (bt != null)
		{
			// Batch mode: save using the preset name and target coordinates, no dialog
			WorldPoint savePos = new WorldPoint(bt.getX(), bt.getY(), bt.getPlane());
			executor.submit(() ->
			{
				try
				{
					save(savePos, image, bt.getName());
					batchManager.advance();
				}
				catch (IOException e)
				{
					log.error("Batch save failed for '{}'", bt.getName(), e);
				}
			});
		}
		else
		{
			// Manual mode: ask the user for a name
			if (playerPos == null)
			{
				showCaptureError("Capture failed — could not determine player position.");
				return;
			}
			promptAndSave(playerPos, image);
		}
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
		if (name == null || name.isBlank()) return;

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

	// ── Capture implementations ───────────────────────────────────────────────

	/** Renders the tile-map minimap centred on the player. Returns null if unavailable. */
	private BufferedImage renderMinimap()
	{
		Player player = client.getLocalPlayer();
		if (player == null) return null;

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

		int padX = (w - SCENE_SIZE) / 2;
		int padY = (h - SCENE_SIZE) / 2;
		log.debug("drawInstanceMap: {}x{} padX={} padY={} radius={}", w, h, padX, padY, CAPTURE_RADIUS);

		BufferedImage raw = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		raw.setRGB(0, 0, w, h, sprite.getPixels(), 0, w);

		BufferedImage scene = new BufferedImage(SCENE_SIZE, SCENE_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D sceneG = scene.createGraphics();
		sceneG.drawImage(raw.getSubimage(padX, padY, SCENE_SIZE, SCENE_SIZE), 0, 0, null);
		MinimapIconPainter.paint(sceneG, client, client.getTopLevelWorldView().getPlane());
		sceneG.dispose();

		LocalPoint lp = player.getLocalLocation();
		int playerPixelX = lp.getSceneX() * PIXELS_PER_TILE + PIXELS_PER_TILE / 2;
		int playerPixelY = (SCENE_TILES - 1 - lp.getSceneY()) * PIXELS_PER_TILE + PIXELS_PER_TILE / 2;

		int diameter = CAPTURE_RADIUS * 2;
		int cropX = Math.max(0, Math.min(playerPixelX - CAPTURE_RADIUS, SCENE_SIZE - diameter));
		int cropY = Math.max(0, Math.min(playerPixelY - CAPTURE_RADIUS, SCENE_SIZE - diameter));
		return CircleMask.apply(scene.getSubimage(cropX, cropY, diameter, diameter));
	}

	/** Captures the full game viewport using AWT Robot. Returns null on failure. */
	private BufferedImage captureViewport()
	{
		try
		{
			Canvas canvas = client.getCanvas();
			Point loc = canvas.getLocationOnScreen();
			int w = client.getCanvasWidth();
			int h = client.getCanvasHeight();
			if (w <= 0 || h <= 0)
			{
				log.warn("Canvas has zero size: {}x{}", w, h);
				return null;
			}
			return new Robot().createScreenCapture(new Rectangle(loc.x, loc.y, w, h));
		}
		catch (AWTException e)
		{
			log.error("Robot viewport capture failed", e);
			return null;
		}
	}

	// ── Persistence ───────────────────────────────────────────────────────────

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
				if (existing != null) entries.addAll(existing);
			}
		}

		for (GeoLocation existing : entries)
		{
			if (existing.getX() == entry.getX()
				&& existing.getY() == entry.getY()
				&& existing.getPlane() == entry.getPlane())
			{
				log.info("Duplicate at ({},{},{}), skipping", entry.getX(), entry.getY(), entry.getPlane());
				if (!batchManager.isActive())
				{
					SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
						null,
						"A capture already exists at this tile (" + entry.getX() + ", " + entry.getY() + ").\nSkipping.",
						"Duplicate Skipped",
						JOptionPane.WARNING_MESSAGE));
				}
				return;
			}
		}

		entries.add(entry);
		try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(capturesFile), StandardCharsets.UTF_8))
		{
			gson.toJson(entries, w);
		}
	}

	/**
	 * Entry point for the scout hotkey (EDT).
	 * Asks for a name and appends the current tile to batch_coords.txt — no image captured.
	 */
	public void scoutPosition()
	{
		clientThread.invoke(() ->
		{
			WorldPoint pos = client.getLocalPlayer() != null
				? client.getLocalPlayer().getWorldLocation()
				: null;
			if (pos == null) return;
			final WorldPoint finalPos = pos;
			SwingUtilities.invokeLater(() ->
			{
				String name = (String) JOptionPane.showInputDialog(
					null,
					"Name this location (will be added to batch_coords.txt):",
					"Scout Location",
					JOptionPane.PLAIN_MESSAGE,
					null,
					null,
					finalPos.getX() + ", " + finalPos.getY()
				);
				if (name == null || name.isBlank()) return;
				batchManager.appendScout(name.trim(), finalPos.getX(), finalPos.getY(), finalPos.getPlane());
			});
		});
	}

	public void shutdown()
	{
		executor.shutdownNow();
	}

	private static void showCaptureError(String message)
	{
		JOptionPane.showMessageDialog(null, message, "Capture Failed", JOptionPane.WARNING_MESSAGE);
	}
}
