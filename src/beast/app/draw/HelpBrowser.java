/*
* File HTMLHelp.java
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

import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import beast.app.DocMaker;
import beast.core.util.Log;



/**
 * Dialog for showing HTML help, with hyperlinks and some browser
 * functionality to navigate around the BEASTObject help facilities.
 */
public class HelpBrowser extends JDialog implements HyperlinkListener {
    /**
     * serialisation *
     */
    private static final long serialVersionUID = 1L;
    /**
     * generates HTML pages *
     */
    static DocMaker m_docMaker;
    /**
     * browser stack *
     */
    List<String> m_sPlugins = new ArrayList<>();
    int m_iCurrentPlugin = 0;

    /**
     * GUI components *
     */
    JEditorPane m_editorPane;
    JButton m_forwardButton;
    JButton m_backwardButton;


    public HelpBrowser(String beastObjectName) {
        if (m_docMaker == null) {
            m_docMaker = new DocMaker();
        }

        // initialise JEditorPane
        m_editorPane = new JEditorPane();
        m_editorPane.setEditable(false);
        m_editorPane.setContentType("text/html");
        m_editorPane.addHyperlinkListener(this);
        setModal(true);

        JScrollPane scroller = new JScrollPane(m_editorPane);

        // add the navigation buttons at the top
        Box buttonBox = Box.createHorizontalBox();
        buttonBox.setAlignmentX(LEFT_ALIGNMENT);
        m_forwardButton = new JButton(">");
        m_forwardButton.setToolTipText("Browse forward");
        m_forwardButton.addActionListener(e -> {
                browseForward();
            });

        m_backwardButton = new JButton("<");
        m_backwardButton.setToolTipText("Browse backward");
        m_backwardButton.addActionListener(e -> {
                browseBackward();
            });


        JButton closeButton = new JButton("x");
        closeButton.setToolTipText("Close Help");
        closeButton.addActionListener(e -> {
                dispose();
            });
//		m_backwardButton.setMnemonic(KeyEvent.VK_RIGHT);
//		m_forwardButton.setMnemonic(KeyEvent.VK_LEFT);

        buttonBox.add(m_backwardButton);
        buttonBox.add(m_forwardButton);
        buttonBox.add(closeButton);

        Box box = Box.createVerticalBox();
        box.add(buttonBox);
        box.add(scroller);


        m_sPlugins.add(beastObjectName);
        updateState();
        this.add(box);
    } // c'tor

    void browseForward() {
        if (m_iCurrentPlugin < m_sPlugins.size() - 1) {
            m_iCurrentPlugin++;
        }
        updateState();
    }

    void browseBackward() {
        if (m_iCurrentPlugin > 0) {
            m_iCurrentPlugin--;
        }
        updateState();
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent link) {
        try {
            HyperlinkEvent.EventType type = link.getEventType();
            if (type == HyperlinkEvent.EventType.ACTIVATED) {
                String beastObjectName = link.getDescription();
                beastObjectName = beastObjectName.replaceAll(".html", "");
                // update browser stack
                m_iCurrentPlugin++;
                while (m_iCurrentPlugin < m_sPlugins.size()) {
                    m_sPlugins.remove(m_iCurrentPlugin);
                }
                m_sPlugins.add(beastObjectName);
                updateState();
            }
        } catch (Exception e) {
            // ignore
            Log.err.println(e.getMessage());
        }
    } // hyperlinkUpdate


    /**
     * change html text and enable/disable buttons (where appropriate) *
     */
    void updateState() {
        String beastObjectName = m_sPlugins.get(m_iCurrentPlugin);
        try {
            String hTML = m_docMaker.getHTML(beastObjectName, false);
            m_editorPane.setText(hTML);
        } catch (Exception e) {
            // ignore
            Log.err.println("HelpBrowser: Something is wrong: " + e.getClass().getName() + " " + e.getMessage());
        }
        m_backwardButton.setEnabled(m_iCurrentPlugin > 0);
        m_forwardButton.setEnabled(m_iCurrentPlugin < m_sPlugins.size() - 1);
    } // updateState


    /**
     * test *
     */
    public static void main(String[] args) {
        try {
            HelpBrowser b = new HelpBrowser("beast.core.MCMC");
            b.setSize(800, 800);
            b.setVisible(true);
            b.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    } // main

} // HelpBrowser
