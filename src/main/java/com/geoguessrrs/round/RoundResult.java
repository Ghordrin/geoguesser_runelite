package com.geoguessrrs.round;

import lombok.Value;
import net.runelite.api.coords.WorldPoint;

@Value
public class RoundResult
{
	String locationName;
	WorldPoint guess;
	WorldPoint target;
	int distance;
	int hintsUsed;
	long elapsedSeconds;
	int score;
}
