package com.geoguessrrs.scoring;

public class ScoreCalculator
{
	private static final int BASE_SCORE = 5000;

	// Exponential decay constant: score = BASE * e^(-distance / DECAY_SCALE)
	// At DECAY_SCALE tiles away you score ~36.8 % of max.
	// Reference points with DECAY_SCALE = 150:
	//   50 tiles  (same city area)   → ~3585 pts
	//  150 tiles  (different region) → ~1839 pts
	//  300 tiles  (different continent) → ~677 pts
	//  800 tiles  (Zeah / far wrong) → ~24 pts
	private static final double DECAY_SCALE = 150.0;

	private static final int HINT_PENALTY = 500;
	private static final int FREE_TIME_SECONDS = 60;
	private static final int TIME_PENALTY_PER_SECOND = 5;

	public int calculate(int distanceTiles, int hintsUsed, long elapsedSeconds)
	{
		int distanceScore = (int) Math.round(BASE_SCORE * Math.exp(-distanceTiles / DECAY_SCALE));
		int hintPenalty = hintsUsed * HINT_PENALTY;
		int timePenalty = (int) Math.max(0, (elapsedSeconds - FREE_TIME_SECONDS) * TIME_PENALTY_PER_SECOND);
		return Math.max(0, distanceScore - hintPenalty - timePenalty);
	}
}
