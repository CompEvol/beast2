/*
 * BeautiApp.java
 *
 * Copyright (C) 2002-2007 Alexei Drummond and Andrew Rambaut
 *
 * This file is part of BEAST.
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

package beast.app.beauti2;

import beast.app.beastapp.BeastVersion;
import beast.app.beauti.BeautiDoc;
import beast.app.beauti.BeautiDoc.ActionOnExit;
import beast.app.draw.PluginPanel;
import beast.app.util.Version;
import beast.util.AddOnManager;
import jam.framework.*;
import jam.mac.Utils;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: BeautiApp.java,v 1.18 2006/09/09 16:07:05 rambaut Exp $
 */
public class BeautiApp extends SingleDocApplication {
//public class BeautiApp extends MultiDocApplication {

    private static final boolean IS_MULTI_DOCUMENT = false;

    public BeautiApp(String nameString, String aboutString, Icon icon,
                     String websiteURLString, String helpURLString) {
        super(new BeautiMenuBarFactory(IS_MULTI_DOCUMENT), nameString, aboutString, icon, websiteURLString, helpURLString);
    }

    // Main entry point
    static public void main(String[] args) {
        try {
            AddOnManager.loadExternalJars();
            PluginPanel.init();
            BeautiDoc doc = new BeautiDoc();
            if (doc.parseArgs(args) == ActionOnExit.WRITE_XML) {
                return;
            }


// deal with arguments here...
//        if (args.length > 1) {
//
//            if (args.length != 3) {
//                System.err.println("Usage: beauti <input_file> <template_file> <output_file>");
//                return;
//            }
//
//            String inputFileName = args[0];
//            String templateFileName = args[1];
//            String outputFileName = args[2];
//
//            // new CommandLineBeauti(inputFileName, templateFileName, outputFileName);
//
//        } else {

            beast.app.util.Utils.loadUIManager();

/*        boolean lafLoaded = false;

        if (Utils.isMacOSX()) {
            System.setProperty("apple.awt.graphics.UseQuartz", "true");
            System.setProperty("apple.awt.antialiasing","true");
            System.setProperty("apple.awt.rendering","VALUE_RENDER_QUALITY");

            System.setProperty("apple.laf.useScreenMenuBar","true");
            System.setProperty("apple.awt.draggableWindowBackground","true");
            System.setProperty("apple.awt.showGrowBox","true");
            try {

                try {
                    // We need to do this using dynamic class loading to avoid other platforms
                    // having to link to this class. If the Quaqua library is not on the classpath
                    // it simply won't be used.
                    Class<?> qm = Class.forName("ch.randelshofer.quaqua.QuaquaManager");
                    Method method = qm.getMethod("setExcludedUIs", Set.class);

                    Set<String> excludes = new HashSet<String>();
                    excludes.add("Button");
                    excludes.add("ToolBar");
                    method.invoke(null, excludes);

                }
                catch (Throwable e) {
                }

                //set the Quaqua Look and Feel in the UIManager
                UIManager.setLookAndFeel(
                        "ch.randelshofer.quaqua.QuaquaLookAndFeel"
                );
                lafLoaded = true;

            } catch (Exception e) {

            }

            UIManager.put("SystemFont", new Font("Lucida Grande", Font.PLAIN, 13));
            UIManager.put("SmallSystemFont", new Font("Lucida Grande", Font.PLAIN, 11));
        }
*/

//            if (!lafLoaded) {
//                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//            }

            java.net.URL url = BeautiApp.class.getResource("images/beauti.png");
            Icon icon = null;

            if (url != null) {
                icon = new ImageIcon(url);
            }

            BeastVersion version = new BeastVersion();

            final String nameString = "BEAUti";
            final String versionString = version.getVersionString();
            String aboutString = "<html><div style=\"font-family:sans-serif;\"><center>" +
                    "<div style=\"font-size:12;\"><p>Bayesian Evolutionary Analysis Utility<br>" +
                    "Version " + versionString + ", " + version.getDateString() + "</p>" +
                    "<p>by Remco Bouckaert, Alexei J. Drummond, Andrew Rambaut and Marc A. Suchard</p></div>" +
                    "<hr><div style=\"font-size:10;\">Part of the BEAST package:" +
                    version.getHTMLCredits() +
                    "</div></center></div></html>";

            String websiteURLString = "http://beast.bio.ed.ac.uk/BEAUti";
            String helpURLString = "http://beast.bio.ed.ac.uk/BEAUti";

            System.setProperty("BEAST & BEAUTi Version", version.getVersion());

            BeautiApp app = new BeautiApp(nameString, aboutString, icon, websiteURLString, helpURLString);
            app.setDocumentFrame(new BeautiFrame(nameString, doc));

            // For multidoc apps...
//            app.setDocumentFrameFactory(new DocumentFrameFactory() {
//                public DocumentFrame createDocumentFrame(Application app, MenuBarFactory menuBarFactory) {
//                    return new BeautiFrame(nameString);
//                }
//            });
            app.initialize();

            // only required for the multidoc application:
//            app.doNew();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(new JFrame(), "Fatal exception: " + e,
                    "Please report this to the authors",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}
