package beast.inference;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Vector;

import beast.core.Description;
import beast.core.Input;
import beast.core.Logger;
import beast.core.MCMC;
import beast.evolution.tree.Node;
import beast.util.Randomizer;
import beast.util.TreeParser;
import beast.util.XMLParser;
import beast.util.XMLProducer;

@Description("Runs multiple MCMC chains in parallel and reports Rubin-Gelman statistic while running the chain " +
		"for each item in the first log file as well as the maximum difference in clade probability for every " +
		"pair of chains. " +
		"" +
		"Note that log file names should have $(seed) in their name so " +
		"that the first chain uses the actual seed in the file name and all subsequent chains add one to it." +
		"Furthermore, the log and tree log should have the same sample frequency.")
public class MultiMCMC extends MCMC {
	public Input<Integer> m_nrOfChains = new Input<Integer>("chains", " number of chains to run in parallel (default 2)", 2);
	
	/** plugins representing MCMC with model, loggers, etc **/
	MCMC [] m_chains;
	/** threads for running MCMC chains **/
	Thread [] m_threads;
	/** keep track of time taken between logs to estimate speed **/
    long m_nStartLogTime;
	/** tables of logs, one for each thread + one for the total**/
	List<Double[]>[] m_logTables;
	/** last line for which log is reported for all chains */
	int m_nLastReported = 0;
	/** pre-calculated sum of itmes and sum of itmes squared for all threads and all items */
	double [][] m_fSums;
	double [][] m_fSquaredSums;
	
	/** for each thread, counts the number of trees read from the log **/
	//int [] m_nTrees;
	/** maximum difference of clade probabilities for chain 1 & 2 **/
	Vector<Double> m_fMaxCladeProbDiffs;
	/** for each thread, keeps track of the frequency of clades **/
	HashMap<String, Integer> [] m_cladeMaps;

	/** index of log and tree log among the MCMC loggers**/
	int m_iTreeLog = 0;
	int m_iLog = 0;

	@Override
	public void initAndValidate() throws Exception {
		m_chains = new MCMC[m_nrOfChains.get()];

		// the difference between the various chains is
		// 1. it runs an MCMC, not a  MultiplMCMC
		// 2. remove chains attribute
		// 3. output logs change for every chain
		// 4. log to stdout is removed to prevent clutter on stdout
		String sXML = new XMLProducer().toXML(this);
		sXML = sXML.replaceAll("chains=[^ /]*", "");
		String sMultiMCMC = this.getClass().getName();
		while (sMultiMCMC.length() > 0) {
			sXML = sXML.replaceAll("\\b"+sMultiMCMC+"\\b", MCMC.class.getName());
			if (sMultiMCMC.indexOf('.') >= 0) {
				sMultiMCMC = sMultiMCMC.substring(sMultiMCMC.indexOf('.')+1);
			} else {
				sMultiMCMC = "";
			}
		}
		long nSeed = Randomizer.getSeed();
		
		// create new chains
		XMLParser parser = new XMLParser();
		for (int i = 0; i < m_chains.length; i++) {
			String sXML2 = sXML;
			sXML2 = sXML2.replaceAll("\\$\\(seed\\)", nSeed+i+"");
			if (sXML2.equals(sXML)) {
				// Uh oh, no seed in log name => logs will overwrite
				throw new Exception("Use $(seed) in log file name to guarantee log files do not overwrite");
			}
			m_chains[i] = (MCMC) parser.parseFragment(sXML2, true);
			// remove log to stdout, if any
			for (int iLogger = m_chains[i].m_loggers.get().size()-1; iLogger >= 0; iLogger--) {
				if (m_chains[i].m_loggers.get().get(iLogger).m_pFileName.get() == null) {
					m_chains[i].m_loggers.get().remove(iLogger);
				}
			}
		}
	
		// collect indices for tree log file names
		while (m_chains[0].m_loggers.get().get(m_iTreeLog).m_mode != Logger.TREE_LOGGER) {
			m_iTreeLog++;
		}
		while (m_chains[0].m_loggers.get().get(m_iLog).m_mode != Logger.COMPOUND_LOGGER) {
			m_iLog++;
		}
		int nEveryLog = m_chains[0].m_loggers.get().get(m_iLog).m_pEvery.get();
		int nEveryTree = m_chains[0].m_loggers.get().get(m_iTreeLog).m_pEvery.get();
		if (nEveryLog != nEveryTree) {
			throw new Exception("log frequencey and tree log frequencey should be the same.");
		}
	} // initAndValidate
	
	@SuppressWarnings("unchecked")
	@Override 
	public void run() throws Exception {
		// start threads with individual chains here.
		m_threads = new Thread[m_chains.length];
		int k = 0;
		for (final MCMC mcmc : m_chains) {
			mcmc.setStateFile(m_sStateFile + "." +k, m_bRestoreFromFile);
			// need this to keep regression testing time reasonable
			mcmc.m_oChainLength.setValue(m_oChainLength.get(), this);
			m_threads[k] = new Thread() {
				public void run() {
					try {
						mcmc.run();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			m_threads[k].start();
			k++;
		}
		// start a thread to tail all log files
		m_logTables = new List[m_threads.length + 1];
		for (int i = 0; i < m_threads.length+1; i++) {
			m_logTables[i] = new ArrayList<Double[]>();
		}
		//m_nTrees = new int[m_threads.length];
		m_fMaxCladeProbDiffs = new Vector<Double>();
		m_cladeMaps = new HashMap[m_threads.length];
		for (int i = 0; i < m_threads.length; i++) {
			m_cladeMaps[i] = new HashMap<String, Integer>();
		}
		new LogWatcherThread().start();
		// wait for the chains to finish
        m_nStartLogTime = System.currentTimeMillis();
		for (Thread thread : m_threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	} // run
	
	/** Represents class that tails all logs files and tree log files.
	 * When a new line is added, this is processed */ 
	class LogWatcherThread extends Thread {
		@Override
		public void run() {
			try {
				int nThreads = m_chains.length;
				/* file handle pairs; two for each thread, 
				 * even numbered files are for the log file, odd numbere for the tree file */
				BufferedReader [] fin = new BufferedReader[nThreads*2];
				int nFilesOpened = 0;

				// wait a seconds, the log file should be available
				sleep(1000);
				// open files
				while (nFilesOpened < nThreads*2) {
					for (int i = 0; i < nThreads*2; i++) {
						if (fin[i] == null) {
							String sFileName = m_chains[i/2].m_loggers.get().get(i%2==0?m_iLog:m_iTreeLog).m_pFileName.get(); 
							fin[i] = new BufferedReader(new FileReader(sFileName));
							if (fin[i] != null) {
								nFilesOpened++;
							}
						}
					}
					if (nFilesOpened < nThreads*2) {
						sleep(1000);
					}
				}

				// keep polling the tree logs file every second
				while (true) {
					int nLinesRead = 0;
					// grab a tree from every thread
					while (nLinesRead < nThreads*2) {
						boolean [] bDone = new boolean[nThreads*2];
						for (int i = 0; i < nThreads*2; i++) {
							if (!bDone[i]) {
								boolean bLineRead = (i%2==0?readLogLines(i/2, fin[i]) : readTreeLogLines(i/2, fin[i]));
								if (bLineRead) {
									nLinesRead++;
									bDone[i] = true;
								}
							}
						}
						if (nLinesRead< nThreads*2) {
							// wait a second before seeing if there is more
							sleep(1000);
						}
					}
					
					double fMaxCladeProbDiff = 0;
					for (int i = 0; i < nThreads; i++) {
						for (int k = 0; k < nThreads; k++) {
							fMaxCladeProbDiff = Math.max(fMaxCladeProbDiff, calcMaxCladeDifference(i, k));
						}
					}
					m_fMaxCladeProbDiffs.add(fMaxCladeProbDiff);
					calcGRStats(m_nLastReported);
					m_nLastReported++;				
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}	
	} // class LogWatcherThread
	
	/** read a line from the log, return true if successfull */
	boolean readLogLines(int iThread, BufferedReader fin) {
		try {
			String sStr = null;
			String [] sStrs = null;;
			do {
				sStr = fin.readLine();
				if (sStr == null) {
					return false;
				}
				sStrs = sStr.split("\\s+");
			} while (sStr.startsWith(("#")) || sStrs.length == 1); // ignore comment lines
			int nItems = sStrs.length;
			Double[] logLine = new Double[nItems];
			try {
				for (int i = 0; i < nItems; i++) {
					logLine[i] = Double.parseDouble(sStrs[i]);
				}
				processLogLine(iThread, logLine);
			} catch (Exception e) {
				//ignore, probably a parse errors
				if (iThread == 0) {
					System.out.println(sStr);
				}
				return false;
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	} // readLogLine

	/** Add log line to log table, and check whether other threads are up to date
	 * enough to report on a sample. 
	 */
	synchronized void processLogLine(int iThread, Double[] logLine) {
		m_logTables[iThread].add(logLine);

//		// can we calculate the Gelman Rubin statistic yet?
//		while (true) {
//			for (int iThread2 = 0; iThread2 < m_threads.length; iThread2++) {
//				if (m_logTables[iThread2].size() <= m_nLastReported) {
//					// not enough log lines processed yet
//					return;
//				}
//				if (m_fMaxCladeProbDiffs.size() <= m_nLastReported) {
////				if (m_nTrees[iThread2] < m_nLastReported) {
//					// not enough tree lines processed yet
//					return;
//				}
//			}
//			calcGRStats(m_nLastReported);
//			m_nLastReported++;
//		}
	}

	/** read a single tree from the tree log file, return true if successful **/
	boolean readTreeLogLines(int iThread, BufferedReader fin) {
		try {
			//while(true) {
				String sStr = null;
				do {
					sStr = fin.readLine();
					if (sStr == null) {
						return false;
					}
				} while (!sStr.matches("tree STATE.*")); // ignore non-tree lines
				sStr = sStr.substring(sStr.indexOf("("));
				Node tree = new TreeParser().parseNewick(sStr);
				List<String> sClades = new ArrayList<String>();
				traverse(tree, sClades);
				HashMap<String, Integer> cladeMap = m_cladeMaps[iThread];
				for (String sClade : sClades) {
					if (cladeMap.containsKey(sClade)) {
						cladeMap.put(sClade, cladeMap.get(sClade) + 1);
					} else {
						cladeMap.put(sClade, 1);
					}
				}
				//m_nTrees[iThread]++;
				return true;
			//}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	} // readTreeLogLine

	/** get clades from tree and store them in a list in String format **/
	int [] traverse(Node node, List<String> sClades) {
		int [] clade = null;
		if (node.isLeaf()) {
			clade = new int[1];
			clade[0] = node.getNr();
		} else {
			int [] leftClade = traverse(node.m_left, sClades);
			int [] rightClade = traverse(node.m_right, sClades);
			
			// merge clade with rightClade
			clade = new int[leftClade.length + rightClade.length];
			int i = 0, iLeft = 0, iRight = 0;
			while (i < clade.length) {
				if (leftClade[iLeft] < rightClade[iRight]) {
					clade[i] = leftClade[iLeft++];
					if (iLeft == leftClade.length) {
						while (iRight < rightClade.length) {
							clade[++i] = rightClade[iRight++];
						}
					}
				} else {
					clade[i] = rightClade[iRight++]; 
					if (iRight == rightClade.length) {
						while (iLeft < leftClade.length) {
							clade[++i] = leftClade[iLeft++];
						}
					}
				}
				i++;
			}
			String sClade = "";
			for (i = 0; i < clade.length; i++) {
				sClade += clade[i] + ",";
			}
			sClades.add(sClade);
		}
		return clade;
	}
	
	/** calculate maximum difference of clade probabilities 
	 * of 2 threads. It uses only the clades in thread1, so
	 * to it requires two calls to make sure no clades in thread2
	 * are missed, i.e. use max(calcMaxCladeDifference(iThread1, iThread2), calcMaxCladeDifference(iThread2, iThread2)) **/
	/** TODO: can be done incrementally?!? **/
	double calcMaxCladeDifference(int iThread1, int iThread2) {
		if (iThread1 == iThread2) {
			return 0;
		}
		int nTotal = 0;
		int nMax = 0;
		HashMap<String, Integer> map1 = m_cladeMaps[iThread1];
		HashMap<String, Integer> map2 = m_cladeMaps[iThread2];
		for (String sClade : map1.keySet()) {
			int i1 = map1.get(sClade);
			int i2 = 0;
			if (map2.containsKey(sClade)) {
				i2 = map2.get(sClade);
			}
			nTotal += i1;
			nMax = Math.max(nMax, Math.abs(i1 - i2));
		}
		return nMax / (double) nTotal;
	} // calcMaxCladeDifference

	

//	http://hosho.ees.hokudai.ac.jp/~kubo/Rdoc/library/coda/html/gelman.diag.html
//	Brooks, SP. and Gelman, A. (1997) 
//	General methods for monitoring convergence of iterative simulations. 
//	Journal of Computational and Graphical Statistics, 
//	7, 
//	434-455. 
//
//  m = # threads
//	n = # samples
//	B = variance within chain
//	W = variance among chains
//	R=(m+1/m)(W(n-1)/n + B/n + B/(mn))/W - (n-1)/nm 
//	=>
//	R=(m+1/m)((n-1)/n + B/Wn + B/(Wmn)) - (n-1)/nm
//	=>
//	R=(m+1/m)((n-1)/n + B/W(1/n + 1/mn) - (n-1)/nm
//	=>
//	R=(m+1/m)((n-1)/n + B/W((m+1)/nm)) - (n-1)/nm
	/** This calculates the Gelman Rubin statistic from scratch (using 10% burn in)
	 * and reports the log of the first chain, annotated with the R statistic.
	 * This number approaches 1 on convergence, so during the run of the chain
	 * you can check how well the chain converges.
	 *
	 * Exploit potential for efficiency by storing means and squared means
	 * NB: when the start of the chain changes, this needs to be taken in account.
	 */
	void calcGRStats(int nCurrentSample) {
		int nLogItems = m_logTables[0].get(0).length;
		int nThreads = m_chains.length;
		// calculate means and variance, use 10% burn in
		int nSamples = nCurrentSample - nCurrentSample/10;
		// the Gelman Rubin statistic for each log item 
		double [] fR = new double [nLogItems];
		if (nSamples > 5) {
			if (m_fSums == null) {
				m_fSums = new double[(nThreads+1)][nLogItems];
				m_fSquaredSums = new double[(nThreads+1)][nLogItems];
			}

			int nStartSample = nCurrentSample/10;
			int nOldStartSample = (nCurrentSample-1)/10;
			if (nStartSample != nOldStartSample) {
				// we need to remove log line from means
				// calc means and squared means
				int iSample = nOldStartSample;
				for (int iThread2 = 0; iThread2 < nThreads; iThread2++) {
					Double[] fLine = m_logTables[iThread2].get(iSample);
					for (int iItem = 1; iItem < nLogItems; iItem++) {
						m_fSums[iThread2][iItem] -= fLine[iItem];
						m_fSquaredSums[iThread2][iItem] -= fLine[iItem] * fLine[iItem];
					}
				}
				// sum to get totals
				for (int iItem = 1; iItem < nLogItems; iItem++) {
					double fMean = 0;
					for (int iThread2 = 0; iThread2 < nThreads; iThread2++) {
						fMean += m_logTables[iThread2].get(iSample)[iItem];
					}
					fMean /= nThreads;
					m_fSums[nThreads][iItem] -= fMean;
					m_fSquaredSums[nThreads][iItem] -= fMean * fMean;
				}
			}

			// calc means and squared means
			int iSample = nCurrentSample;
			for (int iThread2 = 0; iThread2 < nThreads; iThread2++) {
				Double[] fLine = m_logTables[iThread2].get(iSample);
				for (int iItem = 1; iItem < nLogItems; iItem++) {
					m_fSums[iThread2][iItem] += fLine[iItem];
					m_fSquaredSums[iThread2][iItem] += fLine[iItem] * fLine[iItem];
				}
			}
			// sum to get totals
			for (int iItem = 1; iItem < nLogItems; iItem++) {
				double fMean = 0;
				for (int iThread2 = 0; iThread2 < nThreads; iThread2++) {
					fMean += m_logTables[iThread2].get(iSample)[iItem];
				}
				fMean /= nThreads;
				m_fSums[nThreads][iItem] += fMean;
				m_fSquaredSums[nThreads][iItem] += fMean * fMean;
			}

			// calculate variances for all (including total counts)
			double [][] fVars = new double[(nThreads+1)][nLogItems];
			for (int iThread2 = 0; iThread2 < nThreads + 1; iThread2++) {
				for (int iItem = 1; iItem < nLogItems; iItem++) {
					double fMean = m_fSums[iThread2][iItem];
					double fMean2 = m_fSquaredSums[iThread2][iItem];
					fVars[iThread2][iItem] = (fMean2 - fMean * fMean);
				}
			}
			
			for (int iItem = 1; iItem < nLogItems; iItem++) {
				// average variance for this item
				double fW = 0;
				for (int i = 0 ; i < nThreads; i++ ){
					fW += fVars[i][iItem];
				}
				fW /= (nThreads*(nSamples -1));
				// variance for joint
				double fB = fVars[nThreads][iItem]/((nThreads-1) * nSamples);
				fR[iItem] = ((nThreads + 1.0)/nThreads) * ((nSamples-1.0) / nSamples + fB/fW * (nThreads+1)/(nSamples * nThreads)) - (nSamples-1.0)/(nSamples * nThreads); 
			}
		}

		// report means
		Double[] fLine = m_logTables[0].get(nCurrentSample);
		System.out.print(/*m_nLastReported + " " + */(int)(double)fLine[0] + "\t");
		for (int iItem = 1; iItem < nLogItems; iItem++) {
			String sStr = fLine[iItem] + "";
			if (sStr.length() > 10) {
				sStr = sStr.substring(0, 10);
			} else {
				sStr += "          ".substring(10 - sStr.length());
			}
			System.out.print(sStr);
			if (fR[iItem] > 0) {
				sStr = fR[iItem] + "";
				if (sStr.length() > 5) {
					sStr = sStr.substring(0, 5);
				}
				System.out.print("("+sStr+")" + "\t");
			} else {
				System.out.print("(-----)" + "\t");
			}
		}
        long nLogTime = System.currentTimeMillis();
        int nSecondsPerMSamples = (int) ((nLogTime - m_nStartLogTime) * 1000.0 / (fLine[0] + 1.0));
        String sTimePerMSamples =
                (nSecondsPerMSamples >= 3600 ? nSecondsPerMSamples / 3600 + "h" : "") +
                        (nSecondsPerMSamples >= 60 ? (nSecondsPerMSamples % 3600) / 60 + "m" : "") +
                        (nSecondsPerMSamples % 60 + "s");
        System.out.print(sTimePerMSamples + "/Msamples ");

		String sStr = m_fMaxCladeProbDiffs.get(m_nLastReported) + "";
		if (sStr.length() > 5) {
			sStr = sStr.substring(0, 5);
		}
		System.out.print(sStr);
        System.out.println();
		System.out.flush();
		
	} // processLogLine
	
} // class MultiMCMC



