package com.geoguessrrs.capture;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

public class CircleMask
{
	public static BufferedImage apply(BufferedImage src)
	{
		int size = src.getWidth();
		BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setClip(new Ellipse2D.Float(0, 0, size, size));
		g.drawImage(src, 0, 0, null);
		g.dispose();
		return out;
	}
}
