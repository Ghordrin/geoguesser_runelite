package com.geoguessrrs.capture;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import com.geoguessrrs.Difficulty;
import com.geoguessrrs.GeoguessrConfig;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.SpritePixels;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

@Slf4j
public class CaptureOverlay extends Overlay
{
	private static final int PREVIEW_SIZE   = 100;
	private static final int SCENE_TILES    = 104;
	private static final int PIXELS_PER_TILE = 4;
	private static final int SCENE_SIZE     = SCENE_TILES * PIXELS_PER_TILE; // 416

	// Tile-radius per difficulty (at 4 px/tile): easy=18 tiles, medium=11, hard=7
	private static final int RADIUS_EASY   = 44;
	private static final int RADIUS_MEDIUM = 36;
	private static final int RADIUS_HARD   = 26;

	@Inject
	private Client client;

	@Inject
	private GeoguessrConfig config;

	private static final int ICON_REFRESH_TICKS = 10;

	private BufferedImage cachedPreview;
	private BufferedImage cachedIconLayer;
	private int iconLayerTick = Integer.MIN_VALUE;


	@Inject
	CaptureOverlay(Client client, GeoguessrConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.TOP_RIGHT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(OverlayPriority.LOW);
	}

	/** Called from onGameTick to refresh the preview (not every frame). */
	public void refreshPreview()
	{
		SpritePixels sprite = client.drawInstanceMap(client.getTopLevelWorldView().getPlane());
		if (sprite == null)
		{
			cachedPreview = null;
			return;
		}

		int w = sprite.getWidth();
		int h = sprite.getHeight();
		if (w < SCENE_SIZE || h < SCENE_SIZE)
		{
			cachedPreview = null;
			return;
		}

		Player player = client.getLocalPlayer();
		if (player == null)
		{
			cachedPreview = null;
			return;
		}

		// Compute padding dynamically — drawInstanceMap output size varies
		int padX = (w - SCENE_SIZE) / 2;
		int padY = (h - SCENE_SIZE) / 2;

		LocalPoint lp = player.getLocalLocation();
		int sceneX = lp.getSceneX();
		int sceneY = lp.getSceneY();
		// Scene Y=0 is south; image row 0 is north — flip Y
		int playerPixelX = sceneX * PIXELS_PER_TILE + PIXELS_PER_TILE / 2;
		int playerPixelY = (SCENE_TILES - 1 - sceneY) * PIXELS_PER_TILE + PIXELS_PER_TILE / 2;

		int radius = radiusForDifficulty();
		log.debug("drawInstanceMap: {}x{} sprite padX={} padY={} sceneX={} sceneY={} px={} py={} radius={}",
			w, h, padX, padY, sceneX, sceneY, playerPixelX, playerPixelY, radius);

		BufferedImage raw = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		raw.setRGB(0, 0, w, h, sprite.getPixels(), 0, w);

		// Rebuild icon layer at most once every ICON_REFRESH_TICKS ticks
		int tick = client.getTickCount();
		if (cachedIconLayer == null || (tick - iconLayerTick) >= ICON_REFRESH_TICKS)
		{
			cachedIconLayer = new BufferedImage(SCENE_SIZE, SCENE_SIZE, BufferedImage.TYPE_INT_ARGB);
			Graphics2D iconG = cachedIconLayer.createGraphics();
			MinimapIconPainter.paint(iconG, client, client.getTopLevelWorldView().getPlane());
			iconG.dispose();
			iconLayerTick = tick;
		}

		// Composite: fresh terrain + cached icons
		BufferedImage scene = new BufferedImage(SCENE_SIZE, SCENE_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D sceneG = scene.createGraphics();
		sceneG.drawImage(raw.getSubimage(padX, padY, SCENE_SIZE, SCENE_SIZE), 0, 0, null);
		sceneG.drawImage(cachedIconLayer, 0, 0, null);
		sceneG.dispose();

		int diameter = radius * 2;
		int cropX = Math.max(0, Math.min(playerPixelX - radius, SCENE_SIZE - diameter));
		int cropY = Math.max(0, Math.min(playerPixelY - radius, SCENE_SIZE - diameter));
		BufferedImage cropped = scene.getSubimage(cropX, cropY, diameter, diameter);
		cachedPreview = CircleMask.apply(cropped);
	}

	private int radiusForDifficulty()
	{
		switch (config.difficulty())
		{
			case EASY:  return RADIUS_EASY;
			case HARD:  return RADIUS_HARD;
			default:    return RADIUS_MEDIUM;
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (cachedPreview == null)
		{
			return null;
		}

		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics.drawImage(cachedPreview, 0, 0, PREVIEW_SIZE, PREVIEW_SIZE, null);

		graphics.setColor(Color.YELLOW);
		graphics.drawOval(0, 0, PREVIEW_SIZE - 1, PREVIEW_SIZE - 1);
		graphics.setColor(Color.YELLOW);
		graphics.drawString("CAPTURE MODE", 2, PREVIEW_SIZE + 12);

		return new Dimension(PREVIEW_SIZE, PREVIEW_SIZE + 14);
	}
}
