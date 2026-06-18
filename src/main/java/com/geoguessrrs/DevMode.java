package com.geoguessrrs;

/** True only when the plugin runs via {@code ./gradlew run} (i.e. {@code -Dgeoguessrrs.devMode=true}). */
public final class DevMode
{
	private DevMode() {}

	public static boolean isEnabled()
	{
		return "true".equals(System.getProperty("geoguessrrs.devMode"));
	}
}
