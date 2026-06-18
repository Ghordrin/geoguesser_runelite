package com.geoguessrrs;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Keybind;

@ConfigGroup("geoguessr-rs")
public interface GeoguessrConfig extends Config
{
	@ConfigItem(
		keyName = "maxHints",
		name = "Max Hints",
		description = "Maximum hints available per round.",
		position = 0
	)
	@Range(min = 0, max = 3)
	default int maxHints()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "clanName",
		name = "Clan Name",
		description = "Your clan's unique name. Combined with the clan secret to identify your group.",
		position = 1
	)
	default String clanName()
	{
		return "";
	}

	@ConfigItem(
		keyName = "clanPasskey",
		name = "Clan Secret",
		description = "Private passkey for your clan. Only players with the same clan name AND secret share a leaderboard.",
		position = 2,
		secret = true
	)
	default String clanPasskey()
	{
		return "";
	}

	@ConfigItem(
		keyName = "showDistance",
		name = "Show Distance (Hunt)",
		description = "Show tile distance in the compass overlay.",
		position = 3
	)
	default boolean showDistance()
	{
		return true;
	}

	@ConfigItem(
		keyName = "captureMode",
		name = "Capture Mode",
		description = "Enable the developer minimap screenshot capture tool.",
		position = 10
	)
	default boolean captureMode()
	{
		return false;
	}

	@ConfigItem(
		keyName = "captureHotkey",
		name = "Capture Hotkey",
		description = "Hotkey to capture the current location screenshot.",
		position = 11
	)
	default Keybind captureHotkey()
	{
		return new Keybind(KeyEvent.VK_G, InputEvent.SHIFT_DOWN_MASK);
	}

	@ConfigItem(
		keyName = "captureViewport",
		name = "Capture Viewport",
		description = "Capture the full game viewport instead of the minimap. Use for creating in-game perspective screenshots.",
		position = 12
	)
	default boolean captureViewport()
	{
		return false;
	}
}
