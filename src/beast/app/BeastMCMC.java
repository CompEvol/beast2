
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
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import beast.core.*;

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
	int m_nSeed = 127;
	/** name of SnAP specification file **/
	String m_sFileName = "";//"examples/testCoalescent.xml";
	/** MCMC object to execute **/
	RunnablePlugin m_runnable;


	/** parse command line arguments, and load file if specified
	 * @throws Exception **/
	void parseArgs(String[] args) throws Exception {
		int i = 0;
		try {
			while (i < args.length) {
				int iOld = i;
				if (i < args.length) {
					if (args[i].equals("")) {
						i += 1;
					} else if (args[i].equals("-seed")) {
						m_nSeed = Integer.parseInt(args[i + 1]);
						i += 2;
					} else if (args[i].equals("-threads")) {
						m_nThreads = Integer.parseInt(args[i + 1]);
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
		Randomizer.setSeed(m_nSeed);
		m_runnable = new XMLParser().parseFile(m_sFileName);
	} // parseArgs

	public static String getUsage() {
		return 	"Usage: BeastMCMC [options] <Beast.xml>\n" +
				"where <Beast.xml> the name of a file specifying a Beast run\n" +
				"and the following options are allowed:\n" +
				"-seed <int> : sets random number seed (default 127)\n" +
				"-threads <int> : sets number of threads (default 1)\n";
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
			System.err.println(e.getMessage());
			//e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(BeastMCMC.getUsage());
		}
	} // main

} // BeastMCMC
