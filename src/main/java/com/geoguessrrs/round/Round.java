package com.geoguessrrs.round;

import com.geoguessrrs.locations.GeoLocation;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

@Getter
public class Round
{
	private final GeoLocation location;
	private final Instant startTime;
	private final BufferedImage clueImage;
	private final BufferedImage[] hintImages; // progressive zoom-out images, length 3
	private int hintsUsed;

	private final List<WorldPoint> path = new ArrayList<>();
	private WorldPoint lastRecordedPoint = null;

	public Round(GeoLocation location, BufferedImage clueImage, BufferedImage[] hintImages)
	{
		this.location   = location;
		this.startTime  = Instant.now();
		this.clueImage  = clueImage;
		this.hintImages = hintImages;
	}

	public void useHint()
	{
		hintsUsed++;
	}

	public long getElapsedSeconds()
	{
		return Duration.between(startTime, Instant.now()).toSeconds();
	}

	/** Called each game tick from the client thread. Only records when the player actually moves. */
	public void recordPosition(WorldPoint point)
	{
		if (lastRecordedPoint == null || !lastRecordedPoint.equals(point))
		{
			path.add(point);
			lastRecordedPoint = point;
		}
	}

	public List<WorldPoint> getPath()
	{
		return Collections.unmodifiableList(path);
	}
}
