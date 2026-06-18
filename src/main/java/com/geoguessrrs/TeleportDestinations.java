package com.geoguessrrs;

import net.runelite.api.coords.WorldPoint;

public final class TeleportDestinations
{
	private TeleportDestinations() {}

	private static final Object[][] TELEPORTS = {
		// Standard spellbook
		{"Varrock", 3210, 3424, 0},
		{"Lumbridge", 3222, 3218, 0},
		{"Falador", 2965, 3380, 0},
		{"Camelot", 2757, 3477, 0},
		{"Ardougne", 2661, 3305, 0},
		{"Watchtower", 2549, 3113, 0},
		{"Trollheim", 2889, 3678, 0},
		{"Ape Atoll", 2757, 2775, 0},
		{"Senntisten", 3323, 3336, 0},
		{"Yanille", 2544, 3095, 0},
		{"Edgeville (home)", 3087, 3494, 0},
		{"Al Kharid", 3293, 3174, 0},
		{"Draynor Village", 3093, 3244, 0},
		{"Duel Arena", 3316, 3235, 0},

		// Amulet of glory
		{"Edgeville (glory)", 3087, 3494, 0},
		{"Karamja (glory)", 2918, 3176, 0},
		{"Draynor (glory)", 3105, 3251, 0},
		{"Al Kharid (glory)", 3293, 3174, 0},

		// Skills necklace
		{"Fishing Guild", 2611, 3391, 0},
		{"Mining Guild", 3053, 9762, 0},
		{"Crafting Guild", 2934, 3295, 0},
		{"Cooking Guild", 3143, 3441, 0},
		{"Warriors' Guild", 2843, 3543, 0},
		{"Farming Guild", 1248, 3721, 0},

		// Combat bracelet
		{"Warriors' Guild (combat)", 2882, 3550, 0},
		{"Champions' Guild", 3191, 3368, 0},
		{"Monastery", 3048, 3500, 0},
		{"Ranging Guild", 2654, 3440, 0},

		// Games necklace
		{"Burthorpe", 2898, 3544, 0},
		{"Barbarian Outpost", 2521, 3571, 0},
		{"Corporeal Beast", 2965, 4383, 2},
		{"Tears of Guthix", 3245, 9517, 0},
		{"Wintertodt", 1630, 3948, 0},

		// Digsite pendant
		{"Digsite", 3341, 3445, 0},

		// Fairy rings (selected common ones)
		{"Zanaris (fairy ring hub)", 2412, 4434, 0},
		{"Mos Le'Harmless (AKP)", 3674, 2971, 0},
		{"Piscatoris (AKQ)", 2339, 3651, 0},
		{"Canifis (CKS)", 3476, 3431, 0},
		{"Slayer Tower (CKS alt)", 3422, 3537, 0},
		{"Zul-Andra (DLQ)", 2205, 3056, 0},
		{"Ourania Cave (BKP)", 2455, 3230, 0},
		{"Island of Stone (CIS)", 2451, 3815, 0},

		// Lodestones
		{"Lumbridge Lodestone", 3233, 3220, 0},
		{"Draynor Lodestone", 3080, 3249, 0},
		{"Port Sarim Lodestone", 3011, 3215, 0},
		{"Falador Lodestone", 2967, 3402, 0},
		{"Barbarian Village Lodestone", 3081, 3420, 0},
		{"Varrock Lodestone", 3214, 3432, 0},
		{"Grand Exchange Lodestone", 3164, 3478, 0},
		{"Edgeville Lodestone", 3067, 3508, 0},
		{"Al Kharid Lodestone", 3299, 3183, 0},
		{"Dwarven Mine Lodestone", 3011, 3354, 0},
		{"Goblin Village Lodestone", 2957, 3508, 0},
		{"Seers' Village Lodestone", 2760, 3483, 0},
		{"Ardougne Lodestone", 2634, 3348, 0},
		{"Catherby Lodestone", 2802, 3448, 0},
		{"Fishing Guild Lodestone", 2595, 3420, 0},
		{"Yanille Lodestone", 2530, 3092, 0},
		{"Ourania Lodestone", 2468, 3246, 0},
		{"Khazard Lodestone", 2576, 3167, 0},
		{"Lletya Lodestone", 2333, 3162, 0},
		{"Prifddinas Lodestone", 2208, 3368, 0},
		{"Burgh de Rott Lodestone", 3491, 3213, 0},
		{"Canifis Lodestone", 3492, 3490, 0},
		{"Mos Le'Harmless Lodestone", 3700, 2971, 0},
		{"Shilo Village Lodestone", 2862, 2979, 0},
		{"Tai Bwo Wannai Lodestone", 2791, 3064, 0},
		{"Karamja Lodestone", 2762, 3147, 0},
		{"Brimhaven Lodestone", 2758, 3178, 0},
		{"Corsair Cove Lodestone", 2576, 2862, 0},
		{"Hosidius Lodestone", 1740, 3519, 0},
		{"Shayzien Lodestone", 1493, 3626, 0},
		{"Lovakengj Lodestone", 1490, 3888, 0},
		{"Arceuus Lodestone", 1625, 3740, 0},
		{"Piscarilius Lodestone", 1803, 3784, 0},

		// Spirit trees
		{"Grand Exchange (spirit tree)", 3185, 3508, 0},
		{"Gnome Stronghold (spirit tree)", 2461, 3444, 0},
		{"Feldip Hills (spirit tree)", 2560, 2923, 0},
		{"Port Sarim (spirit tree)", 3058, 3259, 0},
		{"Poison Waste (spirit tree)", 2337, 3109, 0},
		{"Prifddinas (spirit tree)", 2264, 3319, 0},

		// Minigame teleports
		{"Pest Control", 2657, 2659, 0},
		{"Barbarian Assault", 2520, 3570, 0},
		{"Castle Wars", 2440, 3090, 0},
		{"Last Man Standing", 3140, 3639, 0},

		// Slayer masters
		{"Nieve/Steve (Stronghold)", 2432, 3423, 0},
		{"Duradel (Shilo Village)", 2869, 2980, 0},
		{"Konar (Karuulm)", 1312, 3825, 0},
	};

	/** Returns the name of the teleport destination closest to the given WorldPoint (2D distance). */
	public static String nearest(WorldPoint target)
	{
		String best = null;
		int bestDist = Integer.MAX_VALUE;

		for (Object[] row : TELEPORTS)
		{
			int dx = (int) row[1] - target.getX();
			int dy = (int) row[2] - target.getY();
			int dist = dx * dx + dy * dy;
			if (dist < bestDist)
			{
				bestDist = dist;
				best = (String) row[0];
			}
		}
		return best;
	}
}
