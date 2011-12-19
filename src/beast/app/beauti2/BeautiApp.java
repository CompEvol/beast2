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

import beast.app.util.Version;
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

    private final static Version version = new Version() {
        private static final String VERSION = "1.7.0";
        private static final String DATE_STRING = "2002-2011";
        private static final boolean IS_PRERELEASE = true;
        private static final String REVISION = "$Rev: 3910 $";

        @Override
        public String getVersion() { return VERSION; }

        @Override
        public String getVersionString() {  return "v" + VERSION + (IS_PRERELEASE ? " Prerelease " + getBuildString() : ""); }

        @Override
        public String getDateString() { return DATE_STRING; }

        @Override
        public String[] getCredits() {
            return new String[]{
                    "Designed and developed by",
                    "Alexei J. Drummond, Andrew Rambaut and Marc A. Suchard",
                    "",
                    "Department of Computer Science",
                    "University of Auckland",
                    "alexei@cs.auckland.ac.nz",
                    "",
                    "Institute of Evolutionary Biology",
                    "University of Edinburgh",
                    "a.rambaut@ed.ac.uk",
                    "",
                    "David Geffen School of Medicine",
                    "University of California, Los Angeles",
                    "msuchard@ucla.edu",
                    "",
                    "Downloads, Help & Resources:",

                    "\thttp://beast.bio.ed.ac.uk",
                    "",
                    "Source code distributed under the GNU Lesser General Public License:",
                    "\thttp://code.google.com/p/beast-mcmc",
                    "",
                    "BEAST developers:",
                    "\tAlex Alekseyenko, Erik Bloomquist, Joseph Heled, Sebastian Hoehna, ",
                    "\tPhilippe Lemey, Wai Lok Sibon Li, Gerton Lunter, Sidney Markowitz, ",
                    "\tVladimir Minin, Michael Defoin Platel, Oliver Pybus, Chieh-Hsi Wu, Walter Xie",
                    "",
                    "Thanks to:",
                    "\tRoald Forsberg, Beth Shapiro and Korbinian Strimmer"};
        }

        @Override
        public String getHTMLCredits() {
            return
                    "<p>Designed and developed by<br>" +
                            "Alexei J. Drummond, Andrew Rambaut and Marc A. Suchard</p>" +
                            "<p>Department of Computer Science, University of Auckland<br>" +
                            "<a href=\"mailto:alexei@cs.auckland.ac.nz\">alexei@cs.auckland.ac.nz</a></p>" +
                            "<p>Institute of Evolutionary Biology, University of Edinburgh<br>" +
                            "<a href=\"mailto:a.rambaut@ed.ac.uk\">a.rambaut@ed.ac.uk</a></p>" +
                            "<p>David Geffen School of Medicine, University of California, Los Angeles<br>" +
                            "<a href=\"mailto:msuchard@ucla.edu\">msuchard@ucla.edu</a></p>" +
                            "<p><a href=\"http://beast.bio.ed.ac.uk\">http://beast.bio.ed.ac.uk</a></p>" +
                            "<p>Source code distributed under the GNU LGPL:<br>" +
                            "<a href=\"http://code.google.com/p/beast-mcmc\">http://code.google.com/p/beast-mcmc</a></p>" +
                            "<p>BEAST developers:<br>" +
                            "Alex Alekseyenko, Erik Bloomquist, Joseph Heled, Sebastian Hoehna, Philippe Lemey,<br>" +
                            "Wai Lok Sibon Li, Gerton Lunter, Sidney Markowitz, Vladimir Minin,<br>" +
                            "Michael Defoin Platel, Oliver Pybus, Chieh-Hsi Wu, Walter Xie</p>" +
                            "<p>Thanks to Roald Forsberg, Beth Shapiro and Korbinian Strimmer</p>";
        }

        @Override
        public String getBuildString() {
            return "r" + REVISION.split(" ")[1];
        }
    };

    public BeautiApp(String nameString, String aboutString, Icon icon,
                     String websiteURLString, String helpURLString) {
        super(new BeautiMenuBarFactory(IS_MULTI_DOCUMENT), nameString, aboutString, icon, websiteURLString, helpURLString);
    }

    // Main entry point
    static public void main(String[] args) {

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

        boolean lafLoaded = false;

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

        try {

            if (!lafLoaded) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }

            java.net.URL url = BeautiApp.class.getResource("images/beauti.png");
            Icon icon = null;

            if (url != null) {
                icon = new ImageIcon(url);
            }

            final String nameString = "BEAUti";
            final String versionString = version.getVersionString();
            String aboutString = "<html><div style=\"font-family:sans-serif;\"><center>" +
                    "<div style=\"font-size:12;\"><p>Bayesian Evolutionary Analysis Utility<br>" +
                    "Version " + versionString + ", " + version.getDateString() + "</p>" +
                    "<p>by Alexei J. Drummond, Andrew Rambaut and Walter Xie</p></div>" +
                    "<hr><div style=\"font-size:10;\">Part of the BEAST package:" +
                    version.getHTMLCredits() +
                    "</div></center></div></html>";

            String websiteURLString = "http://beast.bio.ed.ac.uk/BEAUti";
            String helpURLString = "http://beast.bio.ed.ac.uk/BEAUti";

            System.setProperty("BEAST & BEAUTi Version", version.getVersion());

            BeautiApp app = new BeautiApp(nameString, aboutString, icon, websiteURLString, helpURLString);
            app.setDocumentFrame(new BeautiFrame(nameString));

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
