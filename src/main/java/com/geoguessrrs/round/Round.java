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
	private final BufferedImage[] hintImages; // progressive zoom-out images, length 3
	private int hintsUsed;

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
		return Instant.now().getEpochSecond() - startTime.getEpochSecond();
	}
}
