/*
* File BeastMCMC.java
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
package beast.app;


import beagle.BeagleFlag;
import beast.app.beastapp.BeastDialog;
import beast.app.beastapp.BeastMain;
import beast.app.beauti.Beauti;
import beast.app.draw.ExtensionFileFilter;
import beast.app.util.Version;
import beast.core.Logger;
import beast.core.Runnable;
import beast.core.util.Log;
import beast.util.*;
import jam.util.IconUtils;
import org.json.JSONException;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main application for performing MCMC runs.
 * See getUsage() for command line options.
 */
public class BeastMCMC {
    final public static String VERSION = "2.0 Release candidate";
    final public static String DEVELOPERS = "Beast 2 development team";
    final public static String COPYRIGHT = "Beast 2 development team 2011";

    /**
     * number of threads used to run the likelihood beast.core *
     */
    static public int m_nThreads = 1;
    /**
     * thread pool *
     */
    public static ExecutorService g_exec = Executors.newFixedThreadPool(m_nThreads);
    /**
     * random number seed used to initialise Randomizer *
     */
    long m_nSeed = 127;

    /**
     * MCMC object to execute *
     */
    Runnable m_runnable;

    /**
     * parse command line arguments, and load file if specified
     * @throws IOException 
     * @throws JSONException 
     * @throws JSONParserException 
     */
    public void parseArgs(String[] args) throws IOException, XMLParserException, JSONException {
        int i = 0;
        boolean resume = false;
        boolean useStrictVersions = false;
        boolean sampleFromPrior = false;
        Map<String, String> parserDefinitions = new HashMap<>();

        File beastFile = null;

        try {
            while (i < args.length) {
                int old = i;
                if (i < args.length) {
                    if (args[i].equals("")) {
                        i += 1;
                    } else if (args[i].equals("-batch")) {
                        Logger.FILE_MODE = Logger.LogFileMode.only_new_or_exit;
                        i += 1;
                    } else if (args[i].equals("-resume")) {
                        resume = true;
                        Logger.FILE_MODE = Logger.LogFileMode.resume;
                        System.setProperty("beast.resume", "true");
                        System.setProperty("beast.debug", "false");
                        i += 1;
                    } else if (args[i].equals("-overwrite")) {
                        Logger.FILE_MODE = Logger.LogFileMode.overwrite;
                        i += 1;
                    } else if (args[i].equals("-seed")) {
                        if (args[i + 1].equals("random")) {
                            m_nSeed = Randomizer.getSeed();
                        } else {
                            m_nSeed = Long.parseLong(args[i + 1]);
                        }
                        i += 2;

                    } else if (args[i].equals("-threads")) {
                        m_nThreads = Integer.parseInt(args[i + 1]);
                        g_exec = Executors.newFixedThreadPool(m_nThreads);
                        i += 2;
// use BEAST environment variable to set Beast directories as colon separated list						
//					} else if (args[i].equals("-beastlib")) {
//						ClassDiscovery.setJarPath(args[i + 1]);
//						i += 2;
                    } else if (args[i].equals("-prefix")) {
                        System.setProperty("file.name.prefix", args[i + 1].trim());
                        i += 2;
                    } else if (args[i].equals("-D")) {
                        String [] strs = args[i + 1].split("=",-1);
                        for (int eqIdx = 0; eqIdx<strs.length-1; eqIdx++) {
                            int lastCommaIdx = strs[eqIdx].lastIndexOf(",");

                            if (lastCommaIdx != -1 && eqIdx == 0)
                                throw new IllegalArgumentException("Argument to -D is not well-formed: expecting comma-separated name=value pairs");

                            String name = strs[eqIdx].substring(lastCommaIdx+1);

                            lastCommaIdx = strs[eqIdx+1].lastIndexOf(",");
                            String value;
                            if (eqIdx+1 == strs.length-1) {
                                value = strs[eqIdx+1];
                            } else {
                                if (lastCommaIdx == -1)
                                    throw new IllegalArgumentException("Argument to -D is not well-formed: expecting comma-separated name=value pairs");

                                value = strs[eqIdx+1].substring(0, lastCommaIdx);
                            }
                            parserDefinitions.put(name, value);
            			}
                        i += 2;
                    } else if (args[i].equals("-strictversions")) {
                    	useStrictVersions = true;
                        i += 1;
                    } else if (args[i].equals("-sampleFromPrior")) {
                    	sampleFromPrior = true;
                        i += 1;
                    }
                    if (i == old) {
                        if (i == args.length - 1) {
                            beastFile = new File(args[i]);
                            i++;
                        } else {
                            throw new IllegalArgumentException("Wrong argument");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Error parsing command line arguments: " + Arrays.toString(args) + "\nArguments ignored\n\n" + getUsage());
        }

        if (beastFile == null) {
            // Not resuming so get starting options...

            List<String> MCMCargs = new ArrayList<>();
            Version version = new BEASTVersion2();
            String titleString = "<html><center><p>Bayesian Evolutionary Analysis Sampling Trees<br>" +
                    "Version " + version.getVersionString() + ", " + version.getDateString() + "</p></center></html>";
            javax.swing.Icon icon = IconUtils.getIcon(BeastMain.class, "images/beast.png");
            String nameString = "BEAST " + version.getVersionString();

            BeastDialog dialog = new BeastDialog(new JFrame(), titleString, icon);

            if (!dialog.showDialog(nameString, m_nSeed)) {
                return;
            }

            switch (dialog.getLogginMode()) {
                case 0:/* do not ovewrite */
                    break;
                case 1:
                    MCMCargs.add("-overwrite");
                    break;
                case 2:
                    MCMCargs.add("-resume");
                    break;
            }
            MCMCargs.add("-seed");
            MCMCargs.add(dialog.getSeed() + "");

            if (dialog.getThreadPoolSize() > 0) {
                MCMCargs.add("-threads");
                MCMCargs.add(dialog.getThreadPoolSize() + "");
            }

            boolean useBeagle = dialog.useBeagle();
            boolean beagleShowInfo = false;
            long beagleFlags = 0;
            if (useBeagle) {
                beagleShowInfo = dialog.showBeagleInfo();
                if (dialog.preferBeagleCPU()) {
                    beagleFlags |= BeagleFlag.PROCESSOR_CPU.getMask();
                }
                if (dialog.preferBeagleSSE()) {
                    beagleFlags |= BeagleFlag.VECTOR_SSE.getMask();
                }
                if (dialog.preferBeagleGPU()) {
                    beagleFlags |= BeagleFlag.PROCESSOR_GPU.getMask();
                }
                if (dialog.preferBeagleDouble()) {
                    beagleFlags |= BeagleFlag.PRECISION_DOUBLE.getMask();
                }
                if (dialog.preferBeagleSingle()) {
                    beagleFlags |= BeagleFlag.PRECISION_SINGLE.getMask();
                }
            }
            if (beagleFlags != 0) {
                System.setProperty("beagle.preferred.flags", Long.toString(beagleFlags));
            }
            if (!useBeagle) {
                System.setProperty("java.only", "true");
            }

            File inputFile = dialog.getInputFile();
            if (!beagleShowInfo && inputFile == null) {
                System.err.println("No input file specified");
                System.exit(1);
            }
            MCMCargs.add(inputFile.getAbsolutePath());

//			BeastStartDialog dlg = new BeastStartDialog();
//			if (dlg.m_bOK) {
//				parseArgs(dlg.getArgs());
//			}
            parseArgs(MCMCargs.toArray(new String[0]));
            return;
        }

        Log.warning.println("File: " + beastFile.getName() + " seed: " + m_nSeed + " threads: " + m_nThreads);
        if (resume) {
            Log.info.println("Resuming from file");
        }

        if (useStrictVersions) {
        	// grab "required" attribute from beast spec
            if (beastFile.getPath().toLowerCase().endsWith(".json")) {
            	throw new IllegalArgumentException("The -strictversions flag is not implemented for JSON files yet (only XML files are supported).");
            } else {
                BufferedReader fin = new BufferedReader(new FileReader(beastFile));
                StringBuffer buf = new StringBuffer();
                String str = null;
                int lineCount = 0;
                while (fin.ready() && lineCount < 100) {
                    str = fin.readLine();
                    buf.append(str);
                    buf.append(' ');
                }
                fin.close();
                str = buf.toString();
                int start = str.indexOf("required=");
                if (start < 0) {
                	throw new IllegalArgumentException("Could not find a 'required' attribute in the XML. Add the required attribute, or run without the -strictversions flag");
                }
                char c = str.charAt(start + 9);
                start += 10;
                int end = str.indexOf(c, start);
                String packages = str.substring(start, end);
                PackageManager.loadExternalJars(packages);
            }
        } else {
            PackageManager.loadExternalJars();
        }
        

        // parse xml
        Randomizer.setSeed(m_nSeed);
        if (beastFile.getPath().toLowerCase().endsWith(".json")) {
            m_runnable = new JSONParser(parserDefinitions).parseFile(beastFile, sampleFromPrior);
        } else {        	
        	try {
				m_runnable = new XMLParser(parserDefinitions).parseFile(beastFile, sampleFromPrior);
			} catch (SAXException | ParserConfigurationException e) {
				throw new IllegalArgumentException(e);
			}
        }
        m_runnable.setStateFile(beastFile.getName() + ".state", resume);
    } // parseArgs


    public static String getUsage() {
        return "Usage: BeastMCMC [options] <Beast.xml>\n" +
                "where <Beast.xml> the name of a file specifying a Beast run\n" +
                "and the following options are allowed:\n" +
                "-resume : read state that was stored at the end of the last run from file and append log file\n" +
                "-overwrite : overwrite existing log files (if any). By default, existing files will not be overwritten.\n" +
                "-seed [<int>|random] : sets random number seed (default 127), or picks a random seed\n" +
                "-threads <int> : sets number of threads (default 1)\n" +
                "-prefix <name> : use name as prefix for all log files\n" +
                "-beastlib <path> : Colon separated list of directories. All jar files in the path are loaded. (default 'beastlib')";
    } // getUsage

    /**
     * open file dialog for prompting the user to specify an xml script file to process *
     */
    String getFileNameByDialog() {
        JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
        fc.addChoosableFileFilter(new FileFilter() {
            @Override
			public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }
                String name = f.getName().toLowerCase();
                if (name.endsWith(".xml")) {
                    return true;
                }
                return false;
            }

            // The description of this filter
            @Override
			public String getDescription() {
                return "xml files";
            }
        });

        fc.setDialogTitle("Load xml file");
        int rval = fc.showOpenDialog(null);

        if (rval == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile().toString();
        }
        return null;
    } // getFileNameByDialog

    public void run() throws Exception {
        g_exec = Executors.newFixedThreadPool(m_nThreads);
        m_runnable.run();
        g_exec.shutdown();
        g_exec.shutdownNow();
    } // run


    /**
     * class for starting Beast with a dialog *
     */
    class BeastStartDialog extends JDialog {
        private static final long serialVersionUID = 1L;
        boolean m_bOK = false;
        JTextField m_fileEntry;
        JTextField m_seedEntry;
        JCheckBox m_bUseGPU;
        JComboBox<String> m_mode;

        public BeastStartDialog() {
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            setModalityType(DEFAULT_MODALITY_TYPE);
            init();
            setVisible(true);
        }

        String[] getArgs() {
            List<String> args = new ArrayList<>();
            args.add("-seed");
            args.add(m_seedEntry.getText());
            switch (m_mode.getSelectedIndex()) {
                case 0:
                    break;
                case 1:
                    args.add("-overwrite");
                    break;
                case 2:
                    args.add("-resume");
                    break;
            }
//			if (m_bUseGPU.isSelected()) {
//				args.add("-useGPU");
//			}
            args.add(m_fileEntry.getText());
            return args.toArray(new String[0]);
        }

        void init() {
            try {
                setTitle("Beast Start Dialog");
                Box box = Box.createVerticalBox();

                box.add(createHeader());
                box.add(Box.createVerticalStrut(10));
                box.add(createFileInput());
                box.add(Box.createVerticalStrut(10));
                box.add(Box.createVerticalBox());
                box.add(Box.createVerticalStrut(10));
                box.add(createSeedInput());
//		        box.add(Box.createVerticalStrut(10));
//		        box.add(createBeagleInput());
                box.add(Box.createVerticalStrut(10));
                box.add(createModeInput());

                box.add(Box.createVerticalGlue());
                box.add(createRunQuitButtons());
                add(box);
                int size = UIManager.getFont("Label.font").getSize();
                setSize(600 * size / 13, 500 * size / 13);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Could not create dialog: " + e.getMessage());
            }
        } // BeastStartDialog::init

        private Component createHeader() {
            Box box = Box.createHorizontalBox();

            String iconLocation = "beast/app/draw/icons/beast.png";
            ImageIcon icon = null;
            try {
                URL url = ClassLoader.getSystemResource(iconLocation);
                if (url == null) {
                    System.err.println("Cannot find icon " + iconLocation);
                    return null;
                }
                icon = new ImageIcon(url);
            } catch (Exception e) {
                System.err.println("Cannot load icon " + iconLocation + " " + e.getMessage());
                return null;
            }


            JLabel label = new JLabel(icon);
            label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            box.add(label, BorderLayout.WEST);
            label = new JLabel("<html><center>BEAST<br>Version: " + VERSION + "<br>Developers: " + DEVELOPERS + "<br>Copyright: " + COPYRIGHT + "</html>");
            label.setHorizontalAlignment(SwingConstants.CENTER);
            box.add(label);
            return box;
        } // BeastStartDialog::createHeader

        private Component createFileInput() {
            Box box = Box.createHorizontalBox();
            box.add(new JLabel("Beast XML File: "));
            m_fileEntry = new JTextField();
            int fontsize = m_fileEntry.getFont().getSize();
            Dimension size = new Dimension(300 * fontsize / 13, 20 * fontsize / 13);
            m_fileEntry.setMinimumSize(size);
            m_fileEntry.setPreferredSize(size);
            m_fileEntry.setSize(size);
            m_fileEntry.setToolTipText("Enter file name of Beast 2 XML file");
            m_fileEntry.setMaximumSize(new Dimension(1024 * fontsize / 13, 20 * fontsize / 13));
            box.add(m_fileEntry);
            //box.add(Box.createHorizontalGlue());

            JButton button = new JButton("Choose file");
            button.addActionListener(e -> {
                    JFileChooser fileChooser = new JFileChooser(Beauti.g_sDir);
                    File file = new File(m_fileEntry.getText());
                    if (file.exists())
                        fileChooser.setSelectedFile(file);
                    fileChooser.addChoosableFileFilter(new ExtensionFileFilter(".xml", "Beast xml file (*.xml)"));
                    fileChooser.setDialogTitle("Select Beast 2 XML file");
                    int rval = fileChooser.showOpenDialog(null);
                    if (rval == JFileChooser.APPROVE_OPTION) {
                        String fileName = fileChooser.getSelectedFile().toString();
                        if (fileName.lastIndexOf('/') > 0) {
                            Beauti.g_sDir = fileName.substring(0, fileName.lastIndexOf('/'));
                        }
                        m_fileEntry.setText(fileName);
                    }
                });
            box.add(button);

            return box;
        } // BeastStartDialog::createFileInput

        private Component createSeedInput() {
            Box box = Box.createHorizontalBox();
            box.add(new JLabel("Random number seed: "));
            m_seedEntry = new JTextField("127");
            m_seedEntry.setHorizontalAlignment(SwingConstants.RIGHT);
            Dimension size = new Dimension(100, 20);
            m_seedEntry.setMinimumSize(size);
            m_seedEntry.setPreferredSize(size);
            m_seedEntry.setSize(size);
            m_seedEntry.setToolTipText("Enter seed number used for initialising the random number generator");
            int fontsize = m_seedEntry.getFont().getSize();
            m_seedEntry.setMaximumSize(new Dimension(1024 * fontsize / 13, 20 * fontsize / 13));
            box.add(m_seedEntry);
            box.add(Box.createHorizontalGlue());
            return box;
        } // BeastStartDialog::createSeedInput

        private Component createModeInput() {
            Box box = Box.createHorizontalBox();
            box.add(new JLabel("Mode of running: "));
            m_mode = new JComboBox<>(new String[]{"default: only write new log files",
                    "overwrite: overwrite log files",
                    "resume: appends log to existing files (if any)"});
            Dimension size = new Dimension(350, 20);
            m_mode.setMinimumSize(size);
            m_mode.setPreferredSize(size);
            m_mode.setSize(size);
            m_mode.setMaximumSize(size);

            m_mode.setSelectedIndex(0);
            box.add(m_mode);
            box.add(Box.createHorizontalGlue());
            return box;
        } // BeastStartDialog::createModeInput

        Component createRunQuitButtons() {
            Box cancelOkBox = Box.createHorizontalBox();
            cancelOkBox.setBorder(new EtchedBorder());
            JButton okButton = new JButton("Run");
            okButton.addActionListener(e -> {
                    m_bOK = true;
                    dispose();
                });
            JButton cancelButton = new JButton("Quit");
            cancelButton.addActionListener(e -> {
                    dispose();
                    System.exit(0);
                });
            cancelOkBox.add(Box.createHorizontalGlue());
            cancelOkBox.add(cancelButton);
            cancelOkBox.add(Box.createHorizontalStrut(20));
            cancelOkBox.add(okButton);
            cancelOkBox.add(Box.createHorizontalStrut(20));
            return cancelOkBox;
        } // BeastStartDialog::createRunQuitButtons

    } // class BeastStartDialog


    public static void main(String[] args) {
        try {
            System.setProperty("beast.debug", "true");
            BeastMCMC app = new BeastMCMC();
            app.parseArgs(args);

            app.run();
        } catch (XMLParserException e) {
            System.out.println(e.getMessage());
            //e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(BeastMCMC.getUsage());
        }
        if (System.getProperty("beast.useWindow") == null) {
            // this indicates no window is open
            System.exit(0);
        }
    } // main

} // BeastMCMC
