package com.geoguessrrs.locations;

import java.util.List;
import lombok.Data;

@Data
public class GeoLocation
{
	private String id;
	private String name;
	private int x;
	private int y;
	private int plane;
	private String difficulty;
	private String image;
	private List<String> tags;
	private List<String> hints;
}
