
/*
 * File PluginShape.java
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


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.List;
import java.util.ArrayList;

import javax.swing.JPanel;

import org.w3c.dom.Node;

import beast.util.Randomizer;

import beast.core.Input;
import beast.core.Plugin;


public class PluginShape extends Rect {
	public beast.core.Plugin m_function;
	List<InputShape> m_inputs;


	public PluginShape() {
		super();
		m_fillcolor = new Color(Randomizer.nextInt(256), 128+Randomizer.nextInt(128), Randomizer.nextInt(128));
	}
	public PluginShape(Plugin plugin, Document doc) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		super();
		m_function = plugin;
		m_fillcolor = new Color(Randomizer.nextInt(256), 128+Randomizer.nextInt(128), Randomizer.nextInt(128));
		setClassName(plugin.getClass().getName(), doc);
	}
	public PluginShape(Node node, Document doc) {
		parse(node, doc);
	}
	void setLabel(String sLabel) {
		super.setLabel(sLabel);
		(m_function).setID(sLabel);
	}
	public void setClassName(String sClassName, Document doc) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		try {
			if (m_function == null) {
				m_function = (beast.core.Plugin) Class.forName(sClassName).newInstance();
				setLabel(sClassName.substring(sClassName.lastIndexOf('.')+1));
			}
		m_inputs = new ArrayList<InputShape>();
		List<Input<?>> sInputs = m_function.listInputs();
		for (Input<?> input_ : sInputs) {
			//int nOffset = i*m_w/(sInputs.length) + m_w/(2*(sInputs.length));
			InputShape input = new InputShape();
			input.setFunction(this);
			input.m_fillcolor = m_fillcolor;
			input.m_w = 10;
			String sInputLabel = input_.getName();
			input.setLabel(sInputLabel);
			doc.addNewShape(input);
			m_inputs.add(input);
		}
		m_h = Math.max(40, sInputs.size()*12);
		adjustInputs();
		} catch (Exception e) {
			System.err.println("Could not process inputs: " + e.getMessage());
			m_inputs = new ArrayList<InputShape>();
			// TODO: handle exception
		}
	} // setClassName

	Shape getInput(String sLabel) {
		// find relevant input
		for (Shape shape: m_inputs) {
			String sLabel2 = shape.getLabel();
			if (sLabel2 != null) {
				if (sLabel2.contains("=")) {
					sLabel2 = sLabel2.substring(0, sLabel2.indexOf('='));
				}
				if (sLabel2.equals(sLabel)) {
					return shape;
				}
			}
		}
		return null;
	}
	int getNrInputs() {
		return m_inputs.size();
	}
	public boolean connect(Shape tail, String sInputID, Document doc) throws Exception {
		// find relevant input
		int iInput = 0;
		while (!m_inputs.get(iInput).m_id.equals(sInputID)) {
			iInput++;
		}
		String sInput = m_inputs.get(iInput).getLabel();
		Shape inputShape = doc.getID(tail.m_id);
		if (inputShape instanceof PluginShape) {
			beast.core.Plugin input = ((PluginShape) inputShape).m_function;
			return setInput(m_function, sInput, input);
		}
		return false;
	}

	boolean setInput(beast.core.Plugin plugin, String sName, beast.core.Plugin plugin2) throws Exception {
		plugin.setInputValue(sName, plugin2);
		return true;
	}

	void adjustInputs() {
		if (m_function != null) {
			for (int i = 0; i < m_inputs.size(); i++) {
				InputShape input = m_inputs.get(i);
				int nOffset = i*m_h/(m_inputs.size()) + m_h/(2*(m_inputs.size()));
				input.m_x = m_x - input.m_w;
				input.m_y = m_y + nOffset;
				//input.m_w = 10;
				input.m_h = 10;
				input.m_fillcolor = m_fillcolor;
				input.m_nPenWidth = 0;
			}
		}
	}


	public void draw(Graphics2D g, JPanel panel) {
		if (m_bFilled) {
			GradientPaint m_gradientPaint = new GradientPaint(new Point(m_x, m_y), Color.WHITE, new Point(m_x + m_w, m_y + m_h), m_fillcolor);
			g.setPaint(m_gradientPaint);
			//g.setColor(m_fillcolor);
			g.fillOval(m_x, m_y, m_w, m_h);
			g.fillRect(m_x, m_y, m_w/2, m_h);
		} else {
			g.setColor(m_fillcolor);
			g.drawLine(m_x, m_y, m_x, m_y+m_h);
			g.drawLine(m_x, m_y, m_x+m_w/2, m_y);
			g.drawLine(m_x, m_y+m_h, m_x+m_w/2, m_y+m_h);
			g.drawArc(m_x, m_y, m_w, m_h, 0, 90);
			g.drawArc(m_x, m_y, m_w, m_h, 0, -90);
		}
		g.setStroke(new BasicStroke(m_nPenWidth));
		g.setColor(m_pencolor);
		//g.drawOval(m_x, m_y, m_w, m_h);
		//g.drawRect(m_x, m_y, m_w, m_h);
		drawLabel(g);
		//g.drawString(m_sOutput,m_x+m_w/2-5, m_y+m_h+10);
		adjustInputs();
	}

	void parse(Node node, Document doc) {
		super.parse(node, doc);
		if (node.getAttributes().getNamedItem("class") != null) {
			String sClassName = node.getAttributes().getNamedItem("class").getNodeValue();
			try {
			m_function = (beast.core.Plugin) Class.forName(sClassName).newInstance();
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		if (node.getAttributes().getNamedItem("inputids") != null) {
			String sInputIDs = node.getAttributes().getNamedItem("inputids").getNodeValue();
			String [] sInputID = sInputIDs.split(" ");
			m_inputs = new ArrayList<InputShape>();
			for (int i = 0;i < sInputID.length; i++) {
				InputShape ellipse = (InputShape) doc.findObjectWithID(sInputID[i]);
				m_inputs.add(ellipse);
				ellipse.setFunction(this);
			}
		}
//		m_function = doc.getConstant(node);
		//updateOutput();
	}
	public String getXML() {
		StringBuffer buf = new StringBuffer();
		buf.append("<gdx:function");
		buf.append(" class='"); buf.append(m_function.getClass().getName());buf.append("'");
		buf.append(" inputids='");
		for (int i = 0; i < m_inputs.size(); i++) {
			buf.append(m_inputs.get(i).m_id);
			buf.append(' ');
		}
		buf.append("'");

		buf.append(getAtts());
		buf.append(">\n");
//		buf.append(m_function.toXML());
		buf.append("</gdx:function>");
		return buf.toString();
	}
	boolean intersects(int nX, int nY) {
		return super.intersects(nX, nY);
		//return (m_x+m_w/2-nX)*(m_x+m_w/2-nX)+ (m_y+m_h/2-nY)*(m_y+m_h/2-nY) < m_w*m_w/4+m_h*m_h/4;
	}
	
	@Override
	String getLabel() {
		return m_function.getID();
	}
} // class Function
