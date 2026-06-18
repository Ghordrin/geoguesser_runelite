package com.geoguessrrs;

import com.geoguessrrs.locations.GeoLocation;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

@Slf4j
@Singleton
public class DailyStore
{
	public static final int MAX_ATTEMPTS = 4;

	private static final String CONFIG_GROUP  = "geoguessrrs";
	private static final String KEY_DATE      = "daily.date";
	private static final String KEY_ATTEMPTS  = "daily.attempts";

	@Value
	public static class DailyAttempt
	{
		String locationName;
		int score;
		int distance;
		long elapsedSecs;
	}

	@Inject
	private ConfigManager configManager;

	private final List<DailyAttempt> attempts = new ArrayList<>();

	/** Call on plugin startup. Wipes stored state if the calendar day has changed. */
	public void loadOrReset()
	{
		String today  = LocalDate.now().toString();
		String stored = configManager.getConfiguration(CONFIG_GROUP, KEY_DATE);

		if (!today.equals(stored))
		{
			configManager.setConfiguration(CONFIG_GROUP, KEY_DATE, today);
			configManager.unsetConfiguration(CONFIG_GROUP, KEY_ATTEMPTS);
			attempts.clear();
			log.debug("Daily reset for {}", today);
		}
		else
		{
			attempts.clear();
			String raw = configManager.getConfiguration(CONFIG_GROUP, KEY_ATTEMPTS);
			if (raw != null && !raw.isBlank())
			{
				for (String chunk : raw.split(";"))
				{
					String[] f = chunk.split("\\|", 4);
					try
					{
						if (f.length == 4)
						{
							// current format: locationName|score|distance|elapsed
							attempts.add(new DailyAttempt(f[0],
								Integer.parseInt(f[1]),
								Integer.parseInt(f[2]),
								Long.parseLong(f[3])));
						}
						else if (f.length == 3)
						{
							// legacy format without locationName
							attempts.add(new DailyAttempt("Unknown",
								Integer.parseInt(f[0]),
								Integer.parseInt(f[1]),
								Long.parseLong(f[2])));
						}
					}
					catch (NumberFormatException e)
					{
						log.warn("Corrupt daily attempt entry: {}", chunk);
					}
				}
			}
			log.debug("Loaded {} daily attempt(s) for {}", attempts.size(), today);
		}
	}

	/**
	 * Returns the location for a given round index (0-based) on today's date.
	 * Shuffles the location list with a deterministic day seed so every player
	 * gets the same 4 locations in the same order, with no duplicates.
	 */
	public GeoLocation getTodayLocation(List<GeoLocation> locations, int roundIndex)
	{
		if (locations.isEmpty()) return null;
		List<Integer> indices = new ArrayList<>();
		for (int i = 0; i < locations.size(); i++) indices.add(i);
		Collections.shuffle(indices, new Random(LocalDate.now().toEpochDay()));
		return locations.get(indices.get(roundIndex % locations.size()));
	}

	/** Records an attempt. Returns false and does nothing if the daily is already exhausted. */
	public boolean recordAttempt(String locationName, int score, int distance, long elapsedSecs)
	{
		if (isExhausted())
		{
			return false;
		}
		attempts.add(new DailyAttempt(locationName, score, distance, elapsedSecs));
		persist();
		return true;
	}

	public List<DailyAttempt>  getAttempts()          { return Collections.unmodifiableList(attempts); }
	public int                 getAttemptsUsed()       { return attempts.size(); }
	public int                 getAttemptsRemaining()  { return Math.max(0, MAX_ATTEMPTS - attempts.size()); }
	public boolean             isExhausted()           { return attempts.size() >= MAX_ATTEMPTS; }

	/** Clears all stored daily state. Next loadOrReset() starts fresh for today. */
	public void reset()
	{
		attempts.clear();
		configManager.unsetConfiguration(CONFIG_GROUP, KEY_DATE);
		configManager.unsetConfiguration(CONFIG_GROUP, KEY_ATTEMPTS);
		log.debug("Daily state reset for debugging");
	}

	private void persist()
	{
		if (attempts.isEmpty())
		{
			configManager.unsetConfiguration(CONFIG_GROUP, KEY_ATTEMPTS);
			return;
		}
		StringBuilder sb = new StringBuilder();
		for (DailyAttempt a : attempts)
		{
			if (sb.length() > 0) sb.append(';');
			sb.append(a.locationName).append('|')
			  .append(a.score).append('|')
			  .append(a.distance).append('|')
			  .append(a.elapsedSecs);
		}
		configManager.setConfiguration(CONFIG_GROUP, KEY_ATTEMPTS, sb.toString());
	}
}
