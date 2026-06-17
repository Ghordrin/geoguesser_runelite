package com.geoguessrrs.scores;

import lombok.Value;

@Value
public class PersonalBestEntry
{
	String locationId;
	String locationName;
	int bestScore;
	int bestDistance;
	long bestElapsedSecs;
}
