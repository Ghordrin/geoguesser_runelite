package com.geoguessrrs.capture;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages a list of coordinates loaded from batch_coords.txt for mass-producing location captures.
 * Format (one per line, lines starting with # are comments):
 *   name,x,y[,plane]
 *   Grand Exchange,3165,3487,0
 */
@Slf4j
@Singleton
public class BatchCaptureManager
{
	public static final File BATCH_FILE = new File(CaptureService.CAPTURE_DIR, "batch_coords.txt");

	@Value
	public static class BatchTarget
	{
		String name;
		int x;
		int y;
		int plane;
	}

	private final List<BatchTarget> targets = new ArrayList<>();

	@Getter
	private int currentIndex = 0;

	public void load()
	{
		targets.clear();
		currentIndex = 0;
		if (!BATCH_FILE.exists())
		{
			log.debug("No batch file at {} — batch capture disabled", BATCH_FILE);
			return;
		}
		try (BufferedReader reader = new BufferedReader(new FileReader(BATCH_FILE)))
		{
			String line;
			int lineNum = 0;
			while ((line = reader.readLine()) != null)
			{
				lineNum++;
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) continue;
				String[] parts = line.split(",", 4);
				if (parts.length < 3)
				{
					log.warn("batch_coords.txt line {}: expected name,x,y[,plane]", lineNum);
					continue;
				}
				try
				{
					String name = parts[0].trim();
					int x       = Integer.parseInt(parts[1].trim());
					int y       = Integer.parseInt(parts[2].trim());
					int plane   = parts.length >= 4 ? Integer.parseInt(parts[3].trim()) : 0;
					targets.add(new BatchTarget(name, x, y, plane));
				}
				catch (NumberFormatException e)
				{
					log.warn("batch_coords.txt line {}: bad coordinates, skipping", lineNum);
				}
			}
			log.info("Loaded {} batch targets from {}", targets.size(), BATCH_FILE);
		}
		catch (IOException e)
		{
			log.error("Failed to read batch_coords.txt", e);
		}
	}

	public boolean     isActive()     { return !targets.isEmpty() && currentIndex < targets.size(); }
	public int         getTotal()     { return targets.size(); }
	public BatchTarget getCurrent()   { return isActive() ? targets.get(currentIndex) : null; }

	public void advance()
	{
		if (currentIndex < targets.size()) currentIndex++;
		log.debug("Batch advanced to {}/{}", currentIndex, targets.size());
	}

	public void reset()
	{
		currentIndex = 0;
		log.debug("Batch reset to beginning");
	}

	/**
	 * Appends a scouted coordinate to batch_coords.txt and the in-memory list.
	 * Does not reset the current batch index so an in-progress batch run is unaffected.
	 */
	public void appendScout(String name, int x, int y, int plane)
	{
		CaptureService.CAPTURE_DIR.mkdirs();
		try (FileWriter fw = new FileWriter(BATCH_FILE, true))
		{
			fw.write(name + "," + x + "," + y + "," + plane + System.lineSeparator());
		}
		catch (IOException e)
		{
			log.error("Failed to write scout entry to {}", BATCH_FILE, e);
			return;
		}
		targets.add(new BatchTarget(name, x, y, plane));
		log.info("Scouted '{}' at ({},{},{}) — batch total now {}", name, x, y, plane, targets.size());
	}
}
