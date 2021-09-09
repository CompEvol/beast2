/*
* File ModelBuilder.java
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
package beast.app.tools;



import java.awt.BorderLayout;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

import beast.pkgmgmt.*;
import beast.util.Randomizer;


/**
 * Program for drawing BEAST 2.0 models.
 * This is a bit of a clutch... but potentially useful.
 * <p/>
 * *
 */

public class ModelBuilder extends beast.app.draw.ModelBuilder {
    /**
     * for serialisation
     */
    static final long serialVersionUID = 1L;

    public void init() {
        m_Selection.setDocument(m_doc);
        int size = UIManager.getFont("Label.font").getSize();
        setSize(2048 * size / 13, 2048 * size / 13);
        g_panel = new DrawPanel();
        m_jScrollPane = new JScrollPane(g_panel);
        makeToolbar();
        makeMenuBar();
        addComponentListener(this);
        this.setLayout(new BorderLayout());
        this.add(m_jScrollPane, BorderLayout.CENTER);
        g_panel.setPreferredSize(getSize());
    }
    
    public static void main(String args[]) {
        Randomizer.setSeed(127);
        try {
            PackageManager.loadExternalJars();
        } catch (Exception e) {
            e.printStackTrace();// ignore
        }
        JFrame f = new JFrame("Model Builder");
        beast.app.draw.ModelBuilder drawTest = new beast.app.draw.ModelBuilder();
        drawTest.init();
        JMenuBar menuBar = drawTest.makeMenuBar();
        f.setJMenuBar(menuBar);

        f.add(drawTest.m_jTbTools, BorderLayout.NORTH);
        f.add(drawTest.g_panel, BorderLayout.CENTER);

        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        java.net.URL tempURL = ClassLoader.getSystemResource(beast.app.draw.ModelBuilder.ICONPATH + "/GenerationD.png");
        try {
            URL url = BEASTClassLoader.classLoader.getResource(beast.app.draw.ModelBuilder.ICONPATH + "/GenerationD.png");
            ImageIcon icon = new ImageIcon(url);
            f.setIconImage(icon.getImage());
        } catch (Exception e) {
            System.err.println("error loading icon");
            e.printStackTrace();
            // ignore
        }
        //drawTest.m_doc.loadFile("G:\\eclipse\\workspace\\var\\test2.xdl");
        if (args.length > 0) {
            drawTest.m_doc.loadFile(args[0]);
            drawTest.setDrawingFlag();
        }
        int size = UIManager.getFont("Label.font").getSize();
        f.setSize(600 * size / 13, 800 * size / 13);
        f.setVisible(true);
    } // main
}
