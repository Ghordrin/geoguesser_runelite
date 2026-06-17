package com.geoguessrrs;

import com.google.inject.Provides;
import com.geoguessrrs.capture.CircleMask;
import com.geoguessrrs.capture.CaptureOverlay;
import com.geoguessrrs.capture.CaptureService;
import com.geoguessrrs.hint.NpcHintProvider;
import com.geoguessrrs.hint.RegionHintProvider;
import com.geoguessrrs.locations.GeoLocation;
import com.geoguessrrs.locations.LocationDatabase;
import com.geoguessrrs.overlay.CompassOverlay;
import com.geoguessrrs.overlay.GeoguessrMapPoint;
import com.geoguessrrs.overlay.ResultOverlay;
import com.geoguessrrs.overlay.TargetTileOverlay;
import com.geoguessrrs.overlay.WorldMapGuessOverlay;
import com.geoguessrrs.round.Round;
import com.geoguessrrs.round.RoundResult;
import com.geoguessrrs.scoring.ScoreCalculator;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.HotkeyListener;

@Slf4j
@PluginDescriptor(
	name = "GeoGuessr RS",
	description = "GeoGuessr-style OSRS location guessing game",
	tags = {"game", "minigame", "geoguessr", "location"}
)
public class GeoguessrPlugin extends Plugin
{
@Inject private Client client;
	@Inject private GeoguessrConfig config;
	@Inject private GeoguessrPanel panel;
	@Inject private OverlayManager overlayManager;
	@Inject private ClientToolbar clientToolbar;
	@Inject private KeyManager keyManager;
	@Inject private MouseManager mouseManager;
	@Inject private WorldMapPointManager worldMapPointManager;
	@Inject private net.runelite.client.callback.ClientThread clientThread;
	@Inject private CaptureService captureService;
	@Inject private CaptureOverlay captureOverlay;
	@Inject private CompassOverlay compassOverlay;
	@Inject private ResultOverlay resultOverlay;
	@Inject private TargetTileOverlay targetTileOverlay;
	@Inject private WorldMapGuessOverlay worldMapGuessOverlay;
	@Inject private LocationDatabase locationDatabase;
	@Inject private ScoreCalculator scoreCalculator;
	@Inject private RegionHintProvider regionHintProvider;
	@Inject private NpcHintProvider npcHintProvider;

	private GeoguessrState state = GeoguessrState.IDLE;
	private Round activeRound;
	private GameMode activeGameMode = GameMode.HUNT;

	public GameMode getActiveGameMode() { return activeGameMode; }

	private NavigationButton navButton;
	private HotkeyListener captureHotkeyListener;
	private WorldMapPoint resultPin;
	private WorldMapPoint guessPin;
	private WorldMapPoint debugTargetPin;

	@Override
	protected void startUp()
	{
		locationDatabase.load();

		overlayManager.add(captureOverlay);
		overlayManager.add(compassOverlay);
		overlayManager.add(resultOverlay);
		overlayManager.add(targetTileOverlay);
		overlayManager.add(worldMapGuessOverlay);
		mouseManager.registerMouseListener(worldMapGuessOverlay.mouseListener);

		navButton = NavigationButton.builder()
			.tooltip("GeoGuessr RS")
			.icon(buildNavIcon())
			.priority(5)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		captureHotkeyListener = new HotkeyListener(() -> config.captureHotkey())
		{
			@Override
			public void hotkeyPressed()
			{
				if (config.captureMode())
				{
					captureService.captureAndPrompt();
				}
			}
		};
		keyManager.registerKeyListener(captureHotkeyListener);

		wirePanelCallbacks();
		panel.setIdle();
		log.info("GeoGuessr RS started");
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(captureOverlay);
		overlayManager.remove(compassOverlay);
		overlayManager.remove(resultOverlay);
		overlayManager.remove(targetTileOverlay);
		overlayManager.remove(worldMapGuessOverlay);
		mouseManager.unregisterMouseListener(worldMapGuessOverlay.mouseListener);
		clientToolbar.removeNavigation(navButton);
		keyManager.unregisterKeyListener(captureHotkeyListener);
		captureService.shutdown();
		clearResultPin();
		clearGuessPin();
		clearDebugTargetPin();
		setState(GeoguessrState.IDLE, null);
		log.info("GeoGuessr RS stopped");
	}

	// -------------------------------------------------------------------------
	// Events
	// -------------------------------------------------------------------------

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (config.captureMode())
		{
			captureOverlay.refreshPreview();
		}

	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN && state == GeoguessrState.ACTIVE)
		{
			setState(GeoguessrState.IDLE, null);
			panel.setIdle();
		}
	}

	// -------------------------------------------------------------------------
	// Round flow
	// -------------------------------------------------------------------------

	private void startRound()
	{
		if (state != GeoguessrState.IDLE)
		{
			return;
		}

		worldMapGuessOverlay.setResult(null);
		clearResultPin();
		clearGuessPin();
		Difficulty difficulty  = panel.getSelectedDifficulty();
		activeGameMode        = panel.getSelectedGameMode();

		GeoLocation location = locationDatabase.pickRandom();
		if (location == null)
		{
			log.warn("No locations available for difficulty {}", difficulty);
			return;
		}

		BufferedImage clueImage = cropForDifficulty(locationDatabase.loadClueImage(location), difficulty);
		Round round = new Round(location, clueImage);
		setState(GeoguessrState.ACTIVE, round);
		panel.startRound(round, config.maxHints(), activeGameMode == GameMode.HUNT);
		log.debug("Started round: {} at ({},{},{})", location.getName(), location.getX(), location.getY(), location.getPlane());
	}

	private void revealHint(int index)
	{
		if (state != GeoguessrState.ACTIVE || activeRound == null)
		{
			return;
		}

		List<String> staticHints = activeRound.getLocation().getHints();
		if (staticHints != null && index < staticHints.size())
		{
			// Static hint from JSON — safe to show directly on EDT
			String text = staticHints.get(index);
			activeRound.useHint();
			panel.showHint(index, text);
		}
		else
		{
			// Dynamic fallback calls getWorldLocation() — must run on client thread
			activeRound.useHint();
			clientThread.invoke(() ->
			{
				String text;
				switch (index % 2)
				{
					case 0: text = regionHintProvider.getHint(); break;
					default: text = npcHintProvider.getHint(); break;
				}
				final String finalText = text;
				SwingUtilities.invokeLater(() -> panel.showHint(index, finalText));
			});
		}
	}

	private void endRound(WorldPoint playerPos)
	{
		if (activeRound == null)
		{
			return;
		}

		GeoLocation loc = activeRound.getLocation();
		WorldPoint target = new WorldPoint(loc.getX(), loc.getY(), loc.getPlane());
		int distance = playerPos.distanceTo2D(target);
		int score = scoreCalculator.calculate(distance, activeRound.getHintsUsed(), activeRound.getElapsedSeconds());

		RoundResult result = new RoundResult(
			loc.getName(),
			playerPos,
			target,
			distance,
			activeRound.getHintsUsed(),
			activeRound.getElapsedSeconds(),
			score
		);

		// Pin both locations on the world map
		clearResultPin();
		clearGuessPin();
		resultPin = new WorldMapPoint(target, buildTargetPinIcon());
		resultPin.setName(loc.getName());
		worldMapPointManager.add(resultPin);
		guessPin = new WorldMapPoint(playerPos, buildGuessPinIcon());
		guessPin.setName("Your guess");
		worldMapPointManager.add(guessPin);

		worldMapGuessOverlay.setResult(result);
		resultOverlay.show(result);
		panel.showResult(result);
		setState(GeoguessrState.RESULT, null);

		log.debug("Round ended: {} pts, {} tiles, {}s", score, distance, result.getElapsedSeconds());
	}

	/** Called from the Guess Here! button (EDT) — dispatches to client thread for world location. */
	private void submitHuntGuess()
	{
		if (state != GeoguessrState.ACTIVE || activeRound == null)
		{
			return;
		}
		clientThread.invoke(() ->
		{
			Player player = client.getLocalPlayer();
			if (player != null)
			{
				endRound(player.getWorldLocation());
			}
		});
	}

	/** Called from Classic Mode world-map click. */
	private void submitGuess(WorldPoint guess)
	{
		if (state != GeoguessrState.ACTIVE || activeRound == null)
		{
			return;
		}
		endRound(guess);
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private void setState(GeoguessrState newState, Round round)
	{
		state = newState;
		activeRound = round;
		compassOverlay.setState(newState, round);
		targetTileOverlay.setState(newState, round);
		worldMapGuessOverlay.setState(newState, newState == GeoguessrState.ACTIVE ? this::submitGuess : null);

		if (newState == GeoguessrState.ACTIVE && round != null)
		{
			clearDebugTargetPin();
			WorldPoint target = new WorldPoint(
				round.getLocation().getX(),
				round.getLocation().getY(),
				round.getLocation().getPlane()
			);
			debugTargetPin = new GeoguessrMapPoint(target, "[debug] " + round.getLocation().getName());
			worldMapPointManager.add(debugTargetPin);
		}
		else
		{
			clearDebugTargetPin();
			if (newState == GeoguessrState.RESULT)
			{
				state = GeoguessrState.IDLE;
			}
		}
	}

	private void wirePanelCallbacks()
	{
		panel.setCallbacks(
			this::startRound,
			Arrays.asList(
				() -> revealHint(0),
				() -> revealHint(1),
				() -> revealHint(2)
			),
			this::submitHuntGuess
		);
	}

	private void clearResultPin()
	{
		if (resultPin != null)
		{
			worldMapPointManager.remove(resultPin);
			resultPin = null;
		}
	}

	private void clearGuessPin()
	{
		if (guessPin != null)
		{
			worldMapPointManager.remove(guessPin);
			guessPin = null;
		}
	}

	private void clearDebugTargetPin()
	{
		if (debugTargetPin != null)
		{
			worldMapPointManager.remove(debugTargetPin);
			debugTargetPin = null;
		}
	}

	private static BufferedImage cropForDifficulty(BufferedImage img, Difficulty difficulty)
	{
		if (img == null) return null;
		int cropRadius;
		switch (difficulty)
		{
			case MEDIUM: cropRadius = 36; break;
			case HARD:   cropRadius = 26; break;
			default:     return img;
		}
		int cropDiam = cropRadius * 2;
		int offset = (img.getWidth() - cropDiam) / 2;
		if (offset <= 0) return img;
		return CircleMask.apply(img.getSubimage(offset, offset, cropDiam, cropDiam));
	}

	private static BufferedImage buildNavIcon()
	{
		int size = 16;
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(0x5C9DF5));
		g.fillOval(1, 1, size - 2, size - 2);
		g.setColor(Color.WHITE);
		g.setFont(g.getFont().deriveFont(Font.BOLD, 9f));
		g.drawString("G", 5, 11);
		g.dispose();
		return img;
	}

	private static BufferedImage buildTargetPinIcon()
	{
		return buildPinIcon(Color.RED);
	}

	private static BufferedImage buildGuessPinIcon()
	{
		return buildPinIcon(new Color(0x22BB44));
	}

	private static BufferedImage buildPinIcon(Color color)
	{
		int size = 12;
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(color);
		g.fillOval(1, 1, size - 2, size - 2);
		g.setColor(Color.WHITE);
		g.fillOval(4, 4, 4, 4);
		g.dispose();
		return img;
	}

	@Provides
	GeoguessrConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GeoguessrConfig.class);
	}
}
