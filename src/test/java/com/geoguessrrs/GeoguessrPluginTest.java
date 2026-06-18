package com.geoguessrrs;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GeoguessrPluginTest
{
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GeoguessrPlugin.class);
		RuneLite.main(args);
	}
}
