package com.geoguessrrs.hint;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

public class NpcHintProvider
{
	private static final int HINT_RADIUS = 15;

	@Inject
	private Client client;

	/** Returns an NPC hint for an explicit position, or null if no NPCs are nearby. */
	public String getHintForLocation(WorldPoint pos)
	{
		List<String> nearbyNames = client.getNpcs().stream()
			.filter(npc -> npc.getWorldLocation().distanceTo2D(pos) <= HINT_RADIUS)
			.map(NPC::getName)
			.filter(name -> name != null && !name.isEmpty())
			.distinct()
			.limit(4)
			.collect(Collectors.toList());
		return nearbyNames.isEmpty() ? null : "Nearby: " + String.join(", ", nearbyNames) + ".";
	}
}
