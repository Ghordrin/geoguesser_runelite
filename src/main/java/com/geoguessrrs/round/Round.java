package com.geoguessrrs.round;

import com.geoguessrrs.locations.GeoLocation;
import java.awt.image.BufferedImage;
import java.time.Instant;
import lombok.Getter;

@Getter
public class Round
{
	private final GeoLocation location;
	private final Instant startTime;
	private final BufferedImage clueImage;
	private int hintsUsed;

	public Round(GeoLocation location, BufferedImage clueImage)
	{
		this.location = location;
		this.startTime = Instant.now();
		this.clueImage = clueImage;
	}

	public void useHint()
	{
		hintsUsed++;
	}

	public long getElapsedSeconds()
	{
		return Instant.now().getEpochSecond() - startTime.getEpochSecond();
	}
}
