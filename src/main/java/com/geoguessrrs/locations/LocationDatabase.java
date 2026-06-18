package com.geoguessrrs.locations;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.geoguessrrs.capture.CaptureService;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocationDatabase
{
	private static final String LOCATIONS_RESOURCE = "/locations.json";
	private static final String IMAGE_RESOURCE_PREFIX = "/locations/";
	private static final int PLACEHOLDER_SIZE = 200;

	@Inject
	private Gson gson;

	private List<GeoLocation> allLocations = new ArrayList<>();

	public void load()
	{
		loadBundled();
		loadCaptures();
		log.info("Location pool: {} total", allLocations.size());
	}

	private void loadBundled()
	{
		try (InputStream is = getClass().getResourceAsStream(LOCATIONS_RESOURCE))
		{
			if (is == null)
			{
				log.warn("locations.json not found in resources");
				return;
			}
			Type listType = new TypeToken<List<GeoLocation>>() {}.getType();
			List<GeoLocation> loaded = gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), listType);
			if (loaded != null)
			{
				allLocations = loaded;
			}
			log.info("Loaded {} bundled locations", allLocations.size());
		}
		catch (IOException e)
		{
			log.error("Failed to load bundled locations", e);
		}
	}

	private void loadCaptures()
	{
		File capturesFile = new File(CaptureService.CAPTURE_DIR, "captures.json");
		if (!capturesFile.exists())
		{
			return;
		}
		try (InputStreamReader r = new InputStreamReader(new FileInputStream(capturesFile), StandardCharsets.UTF_8))
		{
			Type listType = new TypeToken<List<GeoLocation>>() {}.getType();
			List<GeoLocation> captured = gson.fromJson(r, listType);
			if (captured != null && !captured.isEmpty())
			{
				allLocations.addAll(captured);
				log.info("Loaded {} captured locations from disk", captured.size());
			}
		}
		catch (IOException e)
		{
			log.error("Failed to load captured locations", e);
		}
	}

	/** Loads the PNG for a location — checks capture dir on disk first, then bundled JAR resources. */
	public BufferedImage loadClueImage(GeoLocation location)
	{
		if (location.getImage() != null && !location.getImage().isBlank())
		{
			// Disk capture directory (populated by capture tool)
			File diskFile = new File(CaptureService.CAPTURE_DIR, location.getImage());
			if (diskFile.exists())
			{
				try
				{
					return ImageIO.read(diskFile);
				}
				catch (IOException e)
				{
					log.warn("Could not load disk image {}", diskFile, e);
				}
			}
			// Bundled JAR resource
			String resource = IMAGE_RESOURCE_PREFIX + location.getImage();
			try (InputStream is = getClass().getResourceAsStream(resource))
			{
				if (is != null)
				{
					return ImageIO.read(is);
				}
			}
			catch (IOException e)
			{
				log.warn("Could not load image {}", location.getImage(), e);
			}
		}
		return makePlaceholder(location.getName());
	}

	private BufferedImage makePlaceholder(String name)
	{
		BufferedImage img = new BufferedImage(PLACEHOLDER_SIZE, PLACEHOLDER_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setColor(new Color(40, 40, 40));
		g.fillOval(0, 0, PLACEHOLDER_SIZE, PLACEHOLDER_SIZE);
		g.setColor(Color.GRAY);
		g.drawOval(0, 0, PLACEHOLDER_SIZE - 1, PLACEHOLDER_SIZE - 1);
		g.setColor(Color.WHITE);
		g.setFont(new Font("Arial", Font.PLAIN, 11));
		String label = "No preview";
		int tw = g.getFontMetrics().stringWidth(label);
		g.drawString(label, (PLACEHOLDER_SIZE - tw) / 2, PLACEHOLDER_SIZE / 2 - 4);
		if (name != null)
		{
			int nw = g.getFontMetrics().stringWidth(name);
			g.drawString(name, (PLACEHOLDER_SIZE - nw) / 2, PLACEHOLDER_SIZE / 2 + 12);
		}
		g.dispose();
		return img;
	}

	/** Adds a freshly-captured location to the in-memory pool without reloading from disk. */
	public void addCapture(GeoLocation location)
	{
		boolean exists = allLocations.stream().anyMatch(l ->
			l.getX() == location.getX()
			&& l.getY() == location.getY()
			&& l.getPlane() == location.getPlane());
		if (!exists)
		{
			allLocations.add(location);
			log.info("Added capture '{}' to pool (pool size now {})", location.getName(), allLocations.size());
		}
	}

	public List<GeoLocation> getAll()
	{
		return Collections.unmodifiableList(allLocations);
	}
}
