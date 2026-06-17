package com.geoguessrrs.hint;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

public class RegionHintProvider implements HintProvider
{
	// regionId = (x >> 6) * 256 + (y >> 6)
	private static final Map<Integer, String> REGION_NAMES = new HashMap<>();

	static
	{
		// --- Misthalin ---
		REGION_NAMES.put(12850, "Lumbridge");
		REGION_NAMES.put(12849, "Lumbridge");
		REGION_NAMES.put(12593, "Lumbridge Swamp");
		REGION_NAMES.put(12848, "Lumbridge Swamp");
		REGION_NAMES.put(12338, "Draynor Village");
		REGION_NAMES.put(12594, "Draynor Village");
		REGION_NAMES.put(12595, "Draynor Village");
		REGION_NAMES.put(12337, "Wizard's Tower");
		REGION_NAMES.put(12340, "Draynor Manor");
		REGION_NAMES.put(12082, "Port Sarim");
		REGION_NAMES.put(12083, "Port Sarim");
		REGION_NAMES.put(11826, "Rimmington");
		REGION_NAMES.put(11827, "Rimmington");
		REGION_NAMES.put(11570, "Rimmington");

		// --- Asgarnia ---
		REGION_NAMES.put(11828, "Falador");
		REGION_NAMES.put(11572, "Falador");
		REGION_NAMES.put(12084, "Falador");
		REGION_NAMES.put(12085, "Falador");
		REGION_NAMES.put(11829, "Falador");
		REGION_NAMES.put(11573, "Taverley");
		REGION_NAMES.put(11574, "Taverley");
		REGION_NAMES.put(11575, "Burthorpe");
		REGION_NAMES.put(11315, "Burthorpe");
		REGION_NAMES.put(11062, "Camelot / Taverley area");

		// --- Edgeville / Barbarian Village / Wilderness border ---
		REGION_NAMES.put(12342, "Edgeville");
		REGION_NAMES.put(12341, "Barbarian Village");
		REGION_NAMES.put(12605, "Edgeville / Wilderness border");
		REGION_NAMES.put(12604, "Wilderness / Edgeville");

		// --- Varrock ---
		REGION_NAMES.put(12853, "Varrock");
		REGION_NAMES.put(12852, "Varrock");
		REGION_NAMES.put(12854, "Varrock");
		REGION_NAMES.put(13109, "Varrock east");
		REGION_NAMES.put(13108, "Varrock east / Digsite area");
		REGION_NAMES.put(13107, "Digsite");
		REGION_NAMES.put(12597, "Varrock west / Grand Exchange");
		REGION_NAMES.put(12598, "Grand Exchange");
		REGION_NAMES.put(12599, "Grand Exchange");

		// --- Al Kharid / Eastern Misthalin ---
		REGION_NAMES.put(13105, "Al Kharid");
		REGION_NAMES.put(13106, "Duel Arena / Al Kharid");
		REGION_NAMES.put(13104, "Shantay Pass");
		REGION_NAMES.put(13361, "Al Kharid south");

		// --- Wilderness ---
		REGION_NAMES.put(12859, "the Wilderness");
		REGION_NAMES.put(12860, "the Wilderness");
		REGION_NAMES.put(12861, "the Wilderness");
		REGION_NAMES.put(13116, "the Wilderness");
		REGION_NAMES.put(13117, "the Wilderness");
		REGION_NAMES.put(13118, "the Wilderness");
		REGION_NAMES.put(13372, "the Wilderness");
		REGION_NAMES.put(13373, "the Wilderness");
		REGION_NAMES.put(13374, "the Wilderness");
		REGION_NAMES.put(13628, "the Wilderness");
		REGION_NAMES.put(13629, "the Wilderness");
		REGION_NAMES.put(13630, "the Wilderness");

		// --- Morytania ---
		REGION_NAMES.put(13621, "Morytania / Canifis road");
		REGION_NAMES.put(13877, "Canifis");
		REGION_NAMES.put(13878, "Canifis");
		REGION_NAMES.put(13879, "Canifis");
		REGION_NAMES.put(13622, "Slayer Tower");
		REGION_NAMES.put(13623, "Slayer Tower");
		REGION_NAMES.put(14131, "Barrows");
		REGION_NAMES.put(14130, "Barrows");
		REGION_NAMES.put(14132, "Barrows");
		REGION_NAMES.put(14646, "Port Phasmatys");
		REGION_NAMES.put(14391, "Mort'ton");
		REGION_NAMES.put(14390, "Mort'ton");
		REGION_NAMES.put(13874, "Burgh de Rott");
		REGION_NAMES.put(13875, "Burgh de Rott");
		REGION_NAMES.put(13870, "Morytania swamp");
		REGION_NAMES.put(14386, "Morytania swamp");
		REGION_NAMES.put(13382, "Paterdomus / River Salve");

		// --- Kandarin: Seers / Camelot ---
		REGION_NAMES.put(10806, "Seers' Village");
		REGION_NAMES.put(10807, "Seers' Village");
		REGION_NAMES.put(10803, "Seers' Village area");
		REGION_NAMES.put(10291, "Seers' Village area");

		// --- Kandarin: Catherby ---
		REGION_NAMES.put(11061, "Catherby");
		REGION_NAMES.put(11317, "Catherby");
		REGION_NAMES.put(11316, "Catherby / Camelot area");

		// --- Kandarin: Ardougne ---
		REGION_NAMES.put(10035, "West Ardougne");
		REGION_NAMES.put(10547, "East Ardougne");
		REGION_NAMES.put(9516,  "Ardougne");
		REGION_NAMES.put(9517,  "Ardougne");
		REGION_NAMES.put(9518,  "Ardougne");

		// --- Kandarin: Fishing Guild / Yanille ---
		REGION_NAMES.put(10034, "Fishing Guild");
		REGION_NAMES.put(10293, "Fishing Guild");
		REGION_NAMES.put(10288, "Yanille");
		REGION_NAMES.put(10032, "Yanille");
		REGION_NAMES.put(10033, "Yanille");

		// --- Kandarin: Gnome Stronghold / Tree Gnome Village ---
		REGION_NAMES.put(9781, "Tree Gnome Village");
		REGION_NAMES.put(9782, "Grand Tree / Gnome Stronghold");
		REGION_NAMES.put(9526, "Gnome Stronghold");
		REGION_NAMES.put(9527, "Gnome Stronghold");

		// --- Kandarin: Khazard / Legends / Barbarian Outpost ---
		REGION_NAMES.put(10290, "Legends' Guild area");
		REGION_NAMES.put(10545, "Port Khazard");
		REGION_NAMES.put(10546, "Khazard area");
		REGION_NAMES.put(10550, "Barbarian Outpost");

		// --- Fremennik Province ---
		REGION_NAMES.put(10042, "Rellekka");
		REGION_NAMES.put(10041, "Rellekka");
		REGION_NAMES.put(10298, "Rellekka");
		REGION_NAMES.put(10554, "Mountain Camp");
		REGION_NAMES.put(10553, "Rellekka mountain area");
		REGION_NAMES.put(10297, "Waterbirth Island");
		REGION_NAMES.put(10314, "Miscellania");
		REGION_NAMES.put(10315, "Miscellania / Etceteria");
		REGION_NAMES.put(10571, "Etceteria");

		// --- Fremennik Isles (Neitiznot / Jatizso) ---
		REGION_NAMES.put(9272, "Neitiznot");
		REGION_NAMES.put(9528, "Jatizso");

		// --- Lunar Isle ---
		REGION_NAMES.put(10794, "Lunar Isle");
		REGION_NAMES.put(10795, "Lunar Isle");

		// --- Karamja ---
		REGION_NAMES.put(11053, "Karamja / Musa Point");
		REGION_NAMES.put(11054, "Karamja");
		REGION_NAMES.put(11311, "Brimhaven");
		REGION_NAMES.put(11310, "Brimhaven");
		REGION_NAMES.put(11055, "Tai Bwo Wannai");
		REGION_NAMES.put(10800, "Shilo Village");
		REGION_NAMES.put(10801, "Shilo Village");
		REGION_NAMES.put(11823, "Brimhaven Agility Arena");

		// --- Tirannwn / Elven Lands ---
		REGION_NAMES.put(9012, "Lletya");
		REGION_NAMES.put(8752, "Lletya");
		REGION_NAMES.put(8753, "Isafdar");
		REGION_NAMES.put(8509, "Zul-Andra");

		// --- Prifddinas (high-Y coordinates, ~y=6040-6120) ---
		REGION_NAMES.put(12894, "Prifddinas");
		REGION_NAMES.put(12895, "Prifddinas");
		REGION_NAMES.put(13150, "Prifddinas");
		REGION_NAMES.put(13151, "Prifddinas");

		// --- Kharidian Desert ---
		REGION_NAMES.put(13358, "Pollnivneach");
		REGION_NAMES.put(13613, "Nardah");
		REGION_NAMES.put(13099, "Sophanem");
		REGION_NAMES.put(13100, "Sophanem");
		REGION_NAMES.put(13356, "Agility Pyramid");
		REGION_NAMES.put(13357, "Agility Pyramid");
		REGION_NAMES.put(12592, "Shantay Pass");
		REGION_NAMES.put(13103, "Desert");
		REGION_NAMES.put(13359, "Desert");
		REGION_NAMES.put(13360, "Desert");
		REGION_NAMES.put(13615, "Desert");

		// --- Zeah / Great Kourend ---
		REGION_NAMES.put(6967, "Hosidius");
		REGION_NAMES.put(6966, "Hosidius");
		REGION_NAMES.put(6968, "Hosidius");
		REGION_NAMES.put(7223, "Shayzien");
		REGION_NAMES.put(7224, "Shayzien");
		REGION_NAMES.put(6711, "Kourend Castle");
		REGION_NAMES.put(6712, "Kourend Castle");
		REGION_NAMES.put(6455, "Lovakengj");
		REGION_NAMES.put(6456, "Lovakengj");
		REGION_NAMES.put(6969, "Piscarillius");
		REGION_NAMES.put(6970, "Piscarillius");
		REGION_NAMES.put(6479, "Arceuus");
		REGION_NAMES.put(6480, "Arceuus");
		REGION_NAMES.put(7479, "Farming Guild");

		// --- Feldip Hills ---
		REGION_NAMES.put(9775, "Feldip Hills");
		REGION_NAMES.put(9776, "Feldip Hills");
		REGION_NAMES.put(9520, "Feldip Hills");

		// --- Ape Atoll ---
		REGION_NAMES.put(9275, "Ape Atoll");
		REGION_NAMES.put(9276, "Ape Atoll");

		// --- Entrana ---
		REGION_NAMES.put(11069, "Entrana");
		REGION_NAMES.put(11068, "Entrana");
	}

	@Inject
	private Client client;

	public String getHintForLocation(WorldPoint pos)
	{
		String name = REGION_NAMES.get(pos.getRegionID());
		if (name != null)
		{
			return "You are near " + name + ".";
		}
		// Coordinate-range fallback for areas not in the region map
		String area = getAreaFromCoordinates(pos.getX(), pos.getY(), pos.getPlane());
		if (area != null)
		{
			return "You are in " + area + ".";
		}
		return null;
	}

	/** Broad coordinate-range detection covering the whole OSRS world. */
	private static String getAreaFromCoordinates(int x, int y, int plane)
	{
		// Prifddinas — high Y surface tiles
		if (y >= 6000 && x >= 3200 && x <= 3300)
			return "Prifddinas";

		// Player-owned houses / instances sit well above normal surface Y
		if (y > 6400)
			return "a player-owned or instanced area";

		// Multi-storey buildings use plane 1/2 but normal surface X/Y
		if (plane > 0)
			return "inside a building or dungeon";

		// Zeah / Great Kourend
		if (x >= 1152 && x <= 1920 && y >= 3520 && y <= 4160)
		{
			if (x >= 1280 && x <= 1500 && y >= 3680) return "Arceuus (Zeah)";
			if (x <= 1600 && y >= 3600)              return "Lovakengj (Zeah)";
			if (x >= 1750 && y >= 3680)              return "Arceuus (Zeah)";
			if (y >= 3680)                            return "Hosidius (Zeah)";
			return "Great Kourend (Zeah)";
		}

		// Wilderness
		if (x >= 2944 && x <= 3392 && y >= 3524)
		{
			if (y >= 3960) return "the deep Wilderness";
			if (y >= 3760) return "the mid Wilderness";
			return "the Wilderness";
		}

		// Morytania — east of River Salve
		if (x >= 3456 && y >= 3136 && y <= 3744)
		{
			if (y <= 3300) return "southern Morytania";
			return "Morytania";
		}

		// Fremennik Province
		if (x >= 2432 && x <= 2880 && y >= 3648 && y <= 4032)
		{
			if (x >= 2620 && x <= 2720) return "Rellekka";
			return "the Fremennik Province";
		}

		// Fremennik Isles (Neitiznot / Jatizso)
		if (x >= 2304 && x <= 2496 && y >= 3776 && y <= 3904)
			return "the Fremennik Isles";

		// Lunar Isle
		if (x >= 2048 && x <= 2176 && y >= 3840 && y <= 3968)
			return "Lunar Isle";

		// Tirannwn / Elven Lands
		if (x >= 2112 && x <= 2432 && y >= 2816 && y <= 3392)
			return "Tirannwn";

		// Kandarin
		if (x >= 2368 && x <= 2944 && y >= 2944 && y <= 3648)
		{
			if (x >= 2800 && y >= 3392) return "the Camelot / Seers' Village area";
			if (x >= 2560 && x <= 2800 && y >= 3200) return "Ardougne";
			if (x >= 2560 && y <= 3200) return "southern Kandarin";
			return "Kandarin";
		}

		// Karamja
		if (x >= 2688 && x <= 2944 && y >= 2816 && y <= 3200)
			return "Karamja";

		// Feldip Hills / Ogres
		if (x >= 2368 && x <= 2688 && y >= 2816 && y <= 3072)
			return "the Feldip Hills";

		// Kharidian Desert
		if (x >= 2944 && x <= 3520 && y >= 2688 && y <= 3200)
		{
			if (y <= 2944) return "the deep Kharidian Desert";
			return "the Kharidian Desert";
		}

		// Asgarnia (Falador, Taverley, Burthorpe, Port Sarim area)
		if (x >= 2816 && x <= 3072 && y >= 3136 && y <= 3648)
			return "Asgarnia";

		// Central Misthalin (Varrock, Lumbridge, Draynor, Al Kharid)
		if (x >= 3072 && x <= 3456 && y >= 3136 && y <= 3648)
			return "Misthalin";

		return null;
	}

	@Override
	public String getHint()
	{
		Player player = client.getLocalPlayer();
		if (player == null) return null;
		return getHintForLocation(player.getWorldLocation());
	}
}
