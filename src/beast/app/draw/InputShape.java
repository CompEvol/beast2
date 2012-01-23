/*
* File Ellipse.java
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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.io.PrintStream;
import java.util.List;

import javax.swing.JPanel;

import org.w3c.dom.Node;

import beast.core.Input;
import beast.core.Plugin;

public class InputShape extends Shape {
    Input<?> m_input;
    static Font g_InputFont = new Font("arial", Font.PLAIN, 8);

    public InputShape(Input<?> input) {
        super();
        m_input = input;
    }

    public InputShape(Node node, Document doc, boolean bReconstructPlugins) {
        parse(node, doc, bReconstructPlugins);
        //TODO: set inputName
    }

    public PluginShape m_pluginShape = null;

    PluginShape getPluginShape() {
        return m_pluginShape;
    }

    void setPluginShape(PluginShape function) {
        m_pluginShape = function;
    }

    Plugin getPlugin() {
        return m_pluginShape.m_plugin;
    }

    String getInputName() throws Exception {
        String sName = getLabel();
        if (sName.indexOf('=') >= 0) {
            sName = sName.substring(0, sName.indexOf('='));
        }
        return sName;
    }


    @Override
    public void draw(Graphics2D g, JPanel panel) {
        if (m_pluginShape == null || m_pluginShape.m_bNeedsDrawing) {
            if (m_bFilled) {
                g.setColor(m_fillcolor);
                g.fillOval(m_x, m_y, m_w, m_h);
            }
            g.setStroke(new BasicStroke(m_nPenWidth));
            g.setColor(m_pencolor);
            g.drawOval(m_x, m_y, m_w, m_h);
            g.setFont(g_InputFont);
            if (getLabel() != null) {
                FontMetrics fm = g.getFontMetrics(g.getFont());
                String sLabel = getLabel();
                int i = 0;
                g.drawString(sLabel, m_x + m_w / 2 - fm.stringWidth(sLabel), m_y + m_h / 2 + i * fm.getHeight());
            }
        }
    }

    @Override
    void parse(Node node, Document doc, boolean bReconstructPlugins) {
        super.parse(node, doc, bReconstructPlugins);
    }

    @Override
    public String getXML() {
        return "<" + Document.INPUT_SHAPE_ELEMENT + getAtts() + "/>";
    }

    @Override
    boolean intersects(int nX, int nY) {
        return (m_x + m_w / 2 - nX) * (m_x + m_w / 2 - nX) + (m_y + m_h / 2 - nY) * (m_y + m_h / 2 - nY) < m_w * m_w / 4 + m_h * m_h / 4;
    }

    @Override
    String getLabel() {
        if (m_input == null) {
            return "XXX";
        }
        String sLabel = m_input.getName();
        if (m_input.get() != null) {
            Object o = m_input.get();
            if (o instanceof String ||
                    o instanceof Integer ||
                    o instanceof Double ||
                    o instanceof Boolean) {
                sLabel += "=" + o.toString();
            }
        }
        return sLabel;
    }

    String toString(Object o) {
        if (o instanceof String ||
                o instanceof Integer ||
                o instanceof Double ||
                o instanceof Boolean) {
            return o.toString();
        } else if (o instanceof Plugin) {
            return ((Plugin) o).getID();
        }
        return "";
    }

    String getLongLabel() {
        String sLabel = m_input.getName();
        if (m_input.get() != null) {
            Object o = m_input.get();
            if (o instanceof String ||
                    o instanceof Integer ||
                    o instanceof Double ||
                    o instanceof Boolean) {
                sLabel += "=" + o.toString();
            } else if (o instanceof Plugin) {
                sLabel += "=" + ((Plugin) o).getID();
            } else if (o instanceof List<?>) {
                sLabel += "=[";
                boolean bNeedsComma = false;
                for (Object o2 : (List<?>) o) {
                    if (bNeedsComma) {
                        sLabel += ",";
                    }
                    sLabel += toString(o2);
                    bNeedsComma = true;
                }
                sLabel += "]";
            }
        }
        return sLabel;
    }

    @Override
    String getID() {
        if (m_pluginShape != null) {
            return m_pluginShape.m_plugin.getID() + "." + m_input.getName();
        } else {
            return m_sID;
        }
    }

    @Override
    void toSVG(PrintStream out) {
        out.print("<circle cx='" + (m_x + m_w / 2) + "' cy='" + (m_y + m_h / 2) + "' r='" + (m_w / 2) + "' ");
        out.println("fill='rgb(" + m_fillcolor.getRed() + "," + m_fillcolor.getGreen() + "," + m_fillcolor.getBlue() + ")'/>");
        drawSVGString(out, g_InputFont, m_pencolor, "end");
    }
} // class Ellipse
