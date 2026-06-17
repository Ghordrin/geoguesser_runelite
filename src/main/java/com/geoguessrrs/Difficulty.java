package com.geoguessrrs;

public enum Difficulty
{
	EASY("Easy"),
	MEDIUM("Medium"),
	HARD("Hard"),
	RANDOM("Random");

	private final String label;

	Difficulty(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
