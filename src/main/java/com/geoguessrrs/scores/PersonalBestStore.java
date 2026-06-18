package com.geoguessrrs.scores;

import com.geoguessrrs.locations.GeoLocation;
import com.geoguessrrs.locations.LocationDatabase;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

@Slf4j
@Singleton
public class PersonalBestStore
{
	private static final String CONFIG_GROUP = "geoguessrrs";
	private static final String KEY_PREFIX = "pb.";

	@Inject
	private ConfigManager configManager;

	@Inject
	private LocationDatabase locationDatabase;

	/**
	 * Updates the personal best for a location if the new score is higher.
	 * Returns true if a new personal best was set.
	 */
	public boolean update(String locationId, String locationName, int score, int distance, long elapsedSecs)
	{
		PersonalBestEntry current = getBest(locationId);
		if (current != null && current.getBestScore() >= score)
		{
			return false;
		}
		String value = score + "|" + distance + "|" + elapsedSecs + "|" + locationName;
		configManager.setConfiguration(CONFIG_GROUP, KEY_PREFIX + locationId, value);
		log.debug("New PB for {}: {} pts", locationName, score);
		return true;
	}

	public PersonalBestEntry getBest(String locationId)
	{
		String raw = configManager.getConfiguration(CONFIG_GROUP, KEY_PREFIX + locationId);
		return parse(locationId, raw);
	}

	public List<PersonalBestEntry> getAll()
	{
		List<PersonalBestEntry> results = new ArrayList<>();
		for (GeoLocation loc : locationDatabase.getAll())
		{
			PersonalBestEntry entry = getBest(loc.getId());
			if (entry != null)
			{
				results.add(entry);
			}
		}
		results.sort(Comparator.comparingInt(PersonalBestEntry::getBestScore).reversed());
		return results;
	}

	private static PersonalBestEntry parse(String locationId, String raw)
	{
		if (raw == null || raw.isBlank())
		{
			return null;
		}
		String[] parts = raw.split("\\|", 4);
		if (parts.length < 4)
		{
			return null;
		}
		try
		{
			int score       = Integer.parseInt(parts[0]);
			int distance    = Integer.parseInt(parts[1]);
			long elapsed    = Long.parseLong(parts[2]);
			String name     = parts[3];
			return new PersonalBestEntry(locationId, name, score, distance, elapsed);
		}
		catch (NumberFormatException e)
		{
			log.warn("Corrupt PB entry for {}: {}", locationId, raw);
			return null;
		}
	}
}
