package com.geoguessrrs;

import com.google.inject.Provides;
import com.geoguessrrs.capture.CircleMask;
import com.geoguessrrs.capture.CaptureOverlay;
import com.geoguessrrs.capture.CaptureService;
import com.geoguessrrs.scores.PersonalBestStore;
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
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
	@Inject private PersonalBestStore personalBestStore;
	@Inject private DailyStore dailyStore;
	@Inject private RegionHintProvider regionHintProvider;
	@Inject private NpcHintProvider npcHintProvider;

	// TODO: make this a config option once the server is deployed publicly
	private static final String LEADERBOARD_URL = "http://localhost:3000";

	private GeoguessrState state = GeoguessrState.IDLE;
	private Round activeRound;
	private GameMode activeGameMode = GameMode.HUNT;
	private String lastPlayerName = "";

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
		dailyStore.loadOrReset();
		SwingUtilities.invokeLater(() ->
		{
			panel.updateDailyState(dailyStore.getAttemptsUsed(), dailyStore.isExhausted(), dailyStore.getAttempts());
			panel.updatePersonalBests(personalBestStore.getAll());
		});

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

		if (DevMode.isEnabled())
		{
			overlayManager.add(captureOverlay);
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
			log.info("GeoGuessr RS: capture mode available (dev build)");
		}

		wirePanelCallbacks();
		log.info("GeoGuessr RS started");
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(compassOverlay);
		overlayManager.remove(resultOverlay);
		overlayManager.remove(targetTileOverlay);
		overlayManager.remove(worldMapGuessOverlay);
		mouseManager.unregisterMouseListener(worldMapGuessOverlay.mouseListener);
		clientToolbar.removeNavigation(navButton);

		if (DevMode.isEnabled())
		{
			overlayManager.remove(captureOverlay);
			if (captureHotkeyListener != null)
			{
				keyManager.unregisterKeyListener(captureHotkeyListener);
			}
			captureService.shutdown();
		}
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
		if (DevMode.isEnabled() && config.captureMode())
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
			SwingUtilities.invokeLater(() ->
				panel.updateDailyState(dailyStore.getAttemptsUsed(), dailyStore.isExhausted(), dailyStore.getAttempts()));
		}
	}

	// -------------------------------------------------------------------------
	// Round flow
	// -------------------------------------------------------------------------

	private void startRound()
	{
		if (state != GeoguessrState.IDLE || dailyStore.isExhausted())
		{
			return;
		}

		worldMapGuessOverlay.setResult(null);
		clearResultPin();
		clearGuessPin();

		activeGameMode = GameMode.HUNT;

		GeoLocation location = dailyStore.getTodayLocation(locationDatabase.getAll(), dailyStore.getAttemptsUsed());
		if (location == null)
		{
			log.warn("No locations available");
			return;
		}

		// All rounds start with the same tight crop; hints progressively reveal more
		BufferedImage fullImage    = locationDatabase.loadClueImage(location);
		BufferedImage initialImage = cropToRadius(fullImage, 26); // ~30% of 88px or 176px source
		BufferedImage[] hintImages = buildHintImages(fullImage);

		Round round = new Round(location, initialImage, hintImages);
		setState(GeoguessrState.ACTIVE, round);
		panel.startRound(round, hintImages.length, true);
		log.debug("Daily attempt {}/{}: location {}",
			dailyStore.getAttemptsUsed() + 1, DailyStore.MAX_ATTEMPTS, location.getName());
	}

	private void revealHint(int index)
	{
		if (state != GeoguessrState.ACTIVE || activeRound == null)
		{
			return;
		}
		BufferedImage[] hintImages = activeRound.getHintImages();
		if (hintImages == null || index >= hintImages.length)
		{
			return;
		}
		activeRound.useHint();
		final BufferedImage img = hintImages[index];
		SwingUtilities.invokeLater(() -> panel.showHint(index, img));
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

		dailyStore.recordAttempt(loc.getName(), score, distance, activeRound.getElapsedSeconds());
		boolean isNewPb = personalBestStore.update(
			loc.getId(), loc.getName(), score, distance, activeRound.getElapsedSeconds());

		if (dailyStore.isExhausted())
		{
			submitScoreToLeaderboard();
		}

		worldMapGuessOverlay.setResult(result);
		resultOverlay.show(result);
		panel.showResult(result, isNewPb);
		SwingUtilities.invokeLater(() ->
		{
			panel.updateDailyState(dailyStore.getAttemptsUsed(), dailyStore.isExhausted(), dailyStore.getAttempts());
			panel.updatePersonalBests(personalBestStore.getAll());
		});
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
				lastPlayerName = player.getName() != null ? player.getName() : "";
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

		clearDebugTargetPin();
		if (DevMode.isEnabled() && newState == GeoguessrState.ACTIVE && round != null)
		{
			WorldPoint target = new WorldPoint(
				round.getLocation().getX(),
				round.getLocation().getY(),
				round.getLocation().getPlane()
			);
			debugTargetPin = new GeoguessrMapPoint(target, "[debug] " + round.getLocation().getName());
			worldMapPointManager.add(debugTargetPin);
		}

		if (newState == GeoguessrState.RESULT)
		{
			state = GeoguessrState.IDLE;
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
			this::submitHuntGuess,
			DevMode.isEnabled() ? this::debugResetDaily : null
		);
	}

	private void debugResetDaily()
	{
		dailyStore.reset();
		dailyStore.loadOrReset();
		setState(GeoguessrState.IDLE, null);
		SwingUtilities.invokeLater(() ->
			panel.updateDailyState(dailyStore.getAttemptsUsed(), dailyStore.isExhausted(), dailyStore.getAttempts()));
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

	private void submitScoreToLeaderboard()
	{
		List<DailyStore.DailyAttempt> attempts = dailyStore.getAttempts();
		int total = 0;
		for (DailyStore.DailyAttempt a : attempts) total += a.getScore();
		String date       = LocalDate.now().toString();
		String playerName = lastPlayerName;

		StringBuilder rounds = new StringBuilder("[");
		for (int i = 0; i < attempts.size(); i++)
		{
			DailyStore.DailyAttempt a = attempts.get(i);
			if (i > 0) rounds.append(',');
			rounds.append(String.format(
				"{\"locationName\":\"%s\",\"score\":%d,\"distance\":%d,\"elapsed\":%d}",
				a.getLocationName().replace("\\", "\\\\").replace("\"", "\\\""),
				a.getScore(), a.getDistance(), a.getElapsedSecs()
			));
		}
		rounds.append(']');

		String body = String.format(
			"{\"playerName\":\"%s\",\"date\":\"%s\",\"totalScore\":%d,\"rounds\":%s}",
			playerName.replace("\\", "\\\\").replace("\"", "\\\""),
			date, total, rounds
		);

		Thread t = new Thread(() ->
		{
			try
			{
				HttpClient http = HttpClient.newHttpClient();
				HttpRequest req = HttpRequest.newBuilder()
					.uri(URI.create(LEADERBOARD_URL + "/api/score"))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(body))
					.timeout(Duration.ofSeconds(5))
					.build();
				HttpResponse<Void> resp = http.send(req, HttpResponse.BodyHandlers.discarding());
				log.debug("Leaderboard submission: HTTP {}", resp.statusCode());
			}
			catch (Exception e)
			{
				log.debug("Leaderboard submission failed (server offline?): {}", e.getMessage());
			}
		}, "geoguessr-lb-submit");
		t.setDaemon(true);
		t.start();
	}

	private static BufferedImage cropToRadius(BufferedImage img, int radius)
	{
		if (img == null) return null;
		int diam = radius * 2;
		if (diam >= img.getWidth()) return CircleMask.apply(img);
		int offset = (img.getWidth() - diam) / 2;
		return CircleMask.apply(img.getSubimage(offset, offset, diam, diam));
	}

	/**
	 * Returns 3 hint images with dramatically increasing zoom-out steps.
	 * Radii are 50 %, 75 %, and 100 % of the image's max radius, with a floor
	 * so they're always wider than the initial r=26 crop.
	 *
	 * For 88px source  (maxR=44): r=30, 38, 44  →  60, 76, 88 px crops
	 * For 176px source (maxR=88): r=44, 66, 88  →  88, 132, 176 px crops (very dramatic)
	 */
	private static BufferedImage[] buildHintImages(BufferedImage fullImage)
	{
		if (fullImage == null) return new BufferedImage[3];
		int maxR = fullImage.getWidth() / 2;
		int r1   = Math.max(30, maxR / 2);
		int r2   = Math.max(38, maxR * 3 / 4);
		return new BufferedImage[]{
			cropToRadius(fullImage, r1),
			cropToRadius(fullImage, r2),
			cropToRadius(fullImage, maxR)
		};
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
