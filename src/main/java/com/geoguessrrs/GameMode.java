package com.geoguessrrs;

public enum GameMode
{
	HUNT("Hunt");

	private final String label;

	GameMode(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
