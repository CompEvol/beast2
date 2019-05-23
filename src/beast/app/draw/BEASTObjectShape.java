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
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.UIManager;

import org.w3c.dom.Node;

import beast.core.BEASTInterface;
import beast.core.Input;
import beast.util.BEASTClassLoader;
import beast.util.Randomizer;





public class BEASTObjectShape extends Shape {
    static Font g_PluginFont = new Font("arial", Font.PLAIN, UIManager.getFont("Label.font").getSize() * 11 / 12);
    public beast.core.BEASTInterface m_beastObject;
    List<InputShape> m_inputs;


    public BEASTObjectShape() {
        super();
        m_fillcolor = new Color(Randomizer.nextInt(256), 128 + Randomizer.nextInt(128), Randomizer.nextInt(128));
    }

    public BEASTObjectShape(BEASTInterface beastObject, Document doc) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        super();
        m_beastObject = beastObject;
        m_fillcolor = new Color(Randomizer.nextInt(256), 128 + Randomizer.nextInt(128), Randomizer.nextInt(128));
        init(beastObject.getClass().getName(), doc);
    }

    public BEASTObjectShape(Node node, Document doc, boolean reconstructBEASTObjects) {
        parse(node, doc, reconstructBEASTObjects);
    }

    public void init(String className, Document doc) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
    	m_doc = doc;
        if (m_beastObject == null) {
            m_beastObject = (beast.core.BEASTInterface) BEASTClassLoader.forName(className).newInstance();
        }
        m_inputs = new ArrayList<>();
        if (m_beastObject.getID() == null) {
        	String id = m_beastObject.getClass().getName();
        	id = id.substring(id.lastIndexOf('.') + 1);
        	m_beastObject.setID(id);
        }
        //System.err.println("\n>>>>" + m_beastObject.getID());        
        List<Input<?>> inputs = m_beastObject.listInputs();
        for (Input<?> input_ : inputs) {
			String longInputName = m_beastObject.getClass().getName() + "." + input_.getName(); 
			//System.err.print(longInputName);
        	if (doc.showAllInputs() ||
        			!doc.tabulist.contains(longInputName) && 
        			input_.get() != null && (
        			(input_.get() instanceof List && ((List<?>)input_.get()).size()>0) ||  
        			!input_.get().equals(input_.defaultValue))) {
	            InputShape input = new InputShape(input_);
	            input.setPluginShape(this);
	            input.m_fillcolor = m_fillcolor;
	            input.m_w = 10;
	            doc.addNewShape(input);
	            m_inputs.add(input);
        		//System.err.println(" shown");
        	} else {
        		//System.err.println(" skipped");
        	}
        }
        m_h = Math.max(40, m_inputs.size() * 12);
        adjustInputs();
    } // setClassName

    // find input shape associated with input with name label
    InputShape getInputShape(String label) {
        for (InputShape shape : m_inputs) {
            String label2 = shape.getLabel();
            if (label2 != null) {
                if (label2.contains("=")) {
                    label2 = label2.substring(0, label2.indexOf('='));
                }
                if (label2.equals(label)) {
                    return shape;
                }
            }
        }
        return null;
    }

    /**
     * set coordinates of inputs based on location of this PluginShape
     */
    void adjustInputs() {
        if (m_beastObject != null) {
            try {
                //List<Input<?>> inputs = m_beastObject.listInputs();
                for (int i = 0; i < m_inputs.size(); i++) {
                    InputShape input = m_inputs.get(i);
                    //input.m_input = inputs.get(i);
                    int offset = i * m_h / (m_inputs.size()) + m_h / (2 * (m_inputs.size()));
                    input.m_x = m_x - input.m_w;
                    input.m_y = m_y + offset;
                    //input.m_w = 10;
                    input.m_h = 10;
                    input.m_fillcolor = m_fillcolor;
                    input.m_nPenWidth = 0;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void draw(Graphics2D g, JPanel panel) {
        if (m_bFilled) {
            GradientPaint m_gradientPaint = new GradientPaint(new Point(m_x, m_y), Color.WHITE, new Point(m_x + m_w, m_y + m_h), m_fillcolor);
            g.setPaint(m_gradientPaint);
            g.fillOval(m_x, m_y, m_w, m_h);
            g.fillRect(m_x, m_y, m_w / 2, m_h);
        } else {
            g.setColor(m_fillcolor);
            g.drawLine(m_x, m_y, m_x, m_y + m_h);
            g.drawLine(m_x, m_y, m_x + m_w / 2, m_y);
            g.drawLine(m_x, m_y + m_h, m_x + m_w / 2, m_y + m_h);
            g.drawArc(m_x, m_y, m_w, m_h, 0, 90);
            g.drawArc(m_x, m_y, m_w, m_h, 0, -90);
        }
        g.setStroke(new BasicStroke(m_nPenWidth));
        g.setColor(m_pencolor);
        g.setFont(g_PluginFont);
        drawLabel(g);
        adjustInputs();
    }

    @Override
    void parse(Node node, Document doc, boolean reconstructBEASTObjects) {
        super.parse(node, doc, reconstructBEASTObjects);
        if (reconstructBEASTObjects) {
            if (node.getAttributes().getNamedItem("class") != null) {
                String className = node.getAttributes().getNamedItem("class").getNodeValue();
                try {
                    m_beastObject = (beast.core.BEASTInterface) BEASTClassLoader.forName(className).newInstance();
                    m_beastObject.setID(m_sID);
                } catch (Exception e) {
                    // TODO: handle exception
                }
            }
            if (node.getAttributes().getNamedItem("inputids") != null) {
                String inputIDs = node.getAttributes().getNamedItem("inputids").getNodeValue();
                String[] inputID = inputIDs.split(" ");
                m_inputs = new ArrayList<>();
                try {
                    //List<Input<?>> inputs = m_beastObject.listInputs();
                    for (int i = 0; i < inputID.length; i++) {
                        InputShape ellipse = (InputShape) doc.findObjectWithID(inputID[i]);
                        m_inputs.add(ellipse);
                        ellipse.setPluginShape(this);
                        //ellipse.m_input = inputs.get(i);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public String getXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<" + Document.PLUGIN_SHAPE_ELEMENT);
        buf.append(" class='");
        buf.append(m_beastObject.getClass().getName());
        buf.append("'");
        buf.append(" inputids='");
        for (int i = 0; i < m_inputs.size(); i++) {
            buf.append(m_inputs.get(i).getID());
            buf.append(' ');
        }
        buf.append("'");

        buf.append(getAtts());
        buf.append(">\n");
        buf.append("</" + Document.PLUGIN_SHAPE_ELEMENT + ">");
        return buf.toString();
    }

    @Override
    void assignFrom(Shape other) {
        super.assignFrom(other);
        m_beastObject.setID(other.m_sID);
    }

    @Override
    boolean intersects(int x, int y) {
        return super.intersects(x, y);
    }

    @Override
    String getLabel() {
        return getID();
    }

    @Override
    String getID() {
        if (m_beastObject == null) {
            return null;
        }
        return m_beastObject.getID();
    }

    @Override
    void toSVG(PrintStream out) {
        out.println("<defs>");
        out.println("  <linearGradient id='grad" + getID() + "' x1='0%' y1='0%' x2='100%' y2='100%'>");
        out.println("    <stop offset='0%' style='stop-color:rgb(255,255,255);stop-opacity:1' />");
        out.println("    <stop offset='100%' style='stop-color:rgb(" + m_fillcolor.getRed() + "," + m_fillcolor.getGreen() + "," + m_fillcolor.getBlue() + ");stop-opacity:1' />");
        out.println("  </linearGradient>");
        out.println("</defs>");
        out.print("<path id='" + getID() + "' d='M " + m_x + " " + (m_y + m_h) + " l " + m_w / 2 + " 0 ");
        out.print(" a " + m_w / 2 + " " + (-m_h / 2) + " 0 0,0 0," + (-m_h) + " l " + (-m_w / 2) + " 0 z'");
        out.println(" fill='url(#grad" + getID() + ")' />");
        drawSVGString(out, g_PluginFont, m_pencolor, "middle");
    }
} // class Function
