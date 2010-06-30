
/*
 * File Picture.java
 *
 * Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
 *
 * This file is part of BEAST2.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */
package beast.app.draw;

import java.awt.Graphics2D;
import javax.swing.JPanel;

import org.w3c.dom.Node;

class Picture extends Rect {
	public Picture(Node node, Document doc) {
		parse(node, doc);
	}
	public void draw(Graphics2D g, JPanel panel) {
		g.drawImage(m_icon, m_x, m_y, m_w, m_h, panel);
	}
	void parse(Node node, Document doc) {
		super.parse(node, doc);
	}
	String getAtts() {
		return
		" src='" + XMLnormalizeAtt(m_src) + "'"
		+ super.getAtts();
   }
	public String getXML() {
		return "<picture" + getAtts() + "/>";
	}
} // class Picture
