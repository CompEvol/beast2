
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


import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import beast.core.Logger;
import beast.core.Runnable;

import beast.util.ClassloaderUtil;
import beast.util.Randomizer;
import beast.util.XMLParser;
import beast.util.XMLParserException;

/** Main application for performing MCMC runs.
 * See getUsage() for command line options.
 */
public class BeastMCMC {
	/** number of threads used to run the likelihood beast.core **/
	static public int m_nThreads = 1;
	/** thread pool **/
	public static ExecutorService g_exec = null;
	/** random number seed used to initialise Randomizer **/
	long m_nSeed = 127;
	/** name of SnAP specification file **/
	String m_sFileName = "";//"examples/testCoalescent.xml";
	/** MCMC object to execute **/
	Runnable m_runnable;
	/** External jar loader path. This takes the form of directories separated by colons. **/
	static String m_sJarPath = (System.getenv("beastlib") != null ? System.getenv("beastlib") : "beastlib");

	/** parse command line arguments, and load file if specified
	 * @throws Exception **/
	void parseArgs(String[] args) throws Exception {
		int i = 0;
		boolean bResume = false;
		try {
			while (i < args.length) {
				int iOld = i;
				if (i < args.length) {
					if (args[i].equals("")) {
						i += 1;
					} else if (args[i].equals("-batch")) {
						Logger.FILE_MODE = Logger.FILE_ONLY_NEW_OR_EXIT;
						i += 1;
					} else if (args[i].equals("-resume")) {
						bResume = true;
						Logger.FILE_MODE = Logger.FILE_APPEND;
						i += 1;
					} else if (args[i].equals("-overwrite")) {
						Logger.FILE_MODE = Logger.FILE_OVERWRITE;
						i += 1;
					} else if (args[i].equals("-seed")) {
                        if(args[i+1].equals("random")){
                            m_nSeed = Randomizer.getSeed();
                        }else{
						    m_nSeed = Integer.parseInt(args[i + 1]);
                        }
						i += 2;
					
                    } else if (args[i].equals("-threads")) {
						m_nThreads = Integer.parseInt(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-beastlib")) {
						m_sJarPath = args[i + 1];
						i += 2;
					}
					if (i == iOld) {
						if (i == args.length-1) {
							m_sFileName = args[i];
							i++;
						} else {
							throw new Exception("Wrong argument");
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Error parsing command line arguments: " + Arrays.toString(args) + "\nArguments ignored\n\n" + getUsage());
		}
		System.err.println("File: " + m_sFileName + " seed: " + m_nSeed + " threads: " + m_nThreads);
		if (bResume) {
			System.out.println("Resuming from file");
		}
		loadExternalJars();
		// parse xml
		Randomizer.setSeed(m_nSeed);
		m_runnable = new XMLParser().parseFile(m_sFileName);
		m_runnable.setStateFile(m_sFileName+".state", bResume);
	} // parseArgs

	// load external jars first
	public static void loadExternalJars() throws Exception {
		String [] sJarDirs = m_sJarPath.split(":");
		for (String sJarDir : sJarDirs) {
			File jarDir = new File(sJarDir);
			if (jarDir.isDirectory()) {
				for (String sFile : jarDir.list()) {
					if (sFile.endsWith(".jar")) {
						@SuppressWarnings("deprecation")
						URL url = new File(jarDir.getAbsolutePath() + "/" + sFile).toURL();
						ClassloaderUtil.addURL(url);
					}
				}
			}
		}
	} //loadExternalJars
	
	public static String getUsage() {
		return 	"Usage: BeastMCMC [options] <Beast.xml>\n" +
				"where <Beast.xml> the name of a file specifying a Beast run\n" +
				"and the following options are allowed:\n" +
				"-resume : read state that was stored at the end of the last run from file and append log file\n" +
				"-overwrite : overwrite existing log files (if any). By default, existing files will not be overwritten.\n" +
				"-seed [<int>|random] : sets random number seed (default 127), or picks a random seed\n" +
				"-threads <int> : sets number of threads (default 1)\n" +
				"-beastlib <path> : Colon separated list of directories. All jar files in the path are loaded. (default 'beastlib')";
	} // getUsage

	/** open file dialog for prompting the user to specify an xml script file to process **/
	String getFileNameByDialog() {
		JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
		fc.addChoosableFileFilter(new FileFilter() {
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
		System.exit(0);
	} // run

	public static void main(String [] args) {
		try {
//            for (String packageName : PluginLoader.getAvailablePackages()) {
//                List<Plugin> plugins = PluginLoader.loadPlugins(packageName);
//                for (Plugin plugin : plugins) {
//                    System.out.println("Plugin loaded: " + plugin.getDescription());
//                }
//            }

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
	} // main

} // BeastMCMC
