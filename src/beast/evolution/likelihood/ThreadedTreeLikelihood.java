/*
* File TreeLikelihood.java
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



package beast.evolution.likelihood;





import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;

import beast.app.BeastMCMC;
import beast.app.beauti.Beauti;
import beast.core.BEASTInterface;
import beast.core.Description;
import beast.core.Input;
import beast.core.State;
import beast.core.util.Log;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.FilteredAlignment;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.substitutionmodel.SubstitutionModel;


@Description("Calculates the likelihood of sequence data on a beast.tree given a site and substitution model using " +
		"a variant of the 'peeling algorithm'. For details, see" +
		"Felsenstein, Joseph (1981). Evolutionary trees from DNA sequences: a maximum likelihood approach. J Mol Evol 17 (6): 368-376.")
public class ThreadedTreeLikelihood extends GenericTreeLikelihood {
    final public Input<Boolean> useAmbiguitiesInput = new Input<>("useAmbiguities", "flag to indicate leafs that sites containing ambigue states should be handled instead of ignored (the default)", false);
    
    final public Input<Integer> maxNrOfThreadsInput = new Input<>("threads","maximum number of threads to use, if less than 1 the number of threads in BeastMCMC is used (default -1)", -1);

    final public Input<String> proportionsInput = new Input<>("proportions", "specifies proportions of patterns used per thread as space "
    		+ "delimted string. This is useful when using a mixture of BEAGLE devices that run at different speeds, e.g GPU and CPU. "
    		+ "The string is duplicated if there are more threads than proportions specified. For example, "
    		+ "'1 2' as well as '33 66' with 2 threads specifies that the first thread gets a third of the patterns and the second "
    		+ "two thirds. With 3 threads, it is interpreted as '1 2 1' = 25%, 50%, 25% and with 7 threads it is "
    		+ "'1 2 1 2 1 2 1' = 10% 20% 10% 20% 10% 20% 10%. If not specified, all threads get the same proportion of patterns.");
    
    enum Scaling {none, always, _default};
    final public Input<Scaling> scalingInput = new Input<>("scaling", "type of scaling to use, one of " + Arrays.toString(Scaling.values()) + ". If not specified, the -beagle_scaling flag is used.", Scaling._default, Scaling.values());
    
    /** private list of likelihoods, to notify framework of TreeLikelihoods being created in initAndValidate() **/
    final private Input<List<TreeLikelihood>> likelihoodsInput = new Input<>("xxx","",new ArrayList<>());
    
    @Override
    public List<Input<?>> listInputs() {
    	List<Input<?>> list =  super.listInputs();
    	if (!Beauti.isInBeauti()) {
    		// do not expose internal likelihoods
    		list.add(likelihoodsInput);
    	}
    	return list;
    }
    
    /** calculation engine **/
    private TreeLikelihood [] treelikelihood;
    
    
    /** number of threads to use, changes when threading causes problems **/
    private int m_nThreads;
    private double [] logPByThread;
	
	
	// specified a set ranges of patterns assigned to each thread
	// first patternPoints contains 0, then one point for each thread
    private int [] patternPoints;
	
    @Override
    public void initAndValidate() {
		m_nThreads = BeastMCMC.m_nThreads;

		if (maxNrOfThreadsInput.get() > 0) {
			m_nThreads = Math.min(maxNrOfThreadsInput.get(), BeastMCMC.m_nThreads);
		}
		logPByThread = new double[m_nThreads];

    	// sanity check: alignment should have same #taxa as tree
    	if (dataInput.get().getTaxonCount() != treeInput.get().getLeafNodeCount()) {
    		throw new IllegalArgumentException("The number of nodes in the tree does not match the number of sequences");
    	}
    	
    	treelikelihood = new TreeLikelihood[m_nThreads];
    	
    	if (dataInput.get().isAscertained) {
    		Log.warning.println("Note, because the alignment is ascertained -- can only use single trhead per alignment");
    		m_nThreads = 1;
    	}
    	
    	if (m_nThreads == 1) {    		
    		treelikelihood[0] = new TreeLikelihood();
    		treelikelihood[0].setID(getID() + "0");
    		treelikelihood[0].initByName("data", dataInput.get(), 
    				"tree", treeInput.get(), 
    				"siteModel", siteModelInput.get(), 
    				"branchRateModel", branchRateModelInput.get(), 
    				"useAmbiguities", useAmbiguitiesInput.get(),
					"scaling" , scalingInput.get() + ""
    				);
    		treelikelihood[0].getOutputs().add(this);
    		likelihoodsInput.get().add(treelikelihood[0]);
    	} else {
    		
        	calcPatternPoints(dataInput.get().getSiteCount());
        	for (int i = 0; i < m_nThreads; i++) {
        		Alignment data = dataInput.get();
        		String filterSpec = (patternPoints[i] +1) + "-" + (patternPoints[i + 1]);
        		if (data.isAscertained) {
        			filterSpec += data.excludefromInput.get() + "-" + data.excludetoInput.get() + "," + filterSpec;
        		}
        		treelikelihood[i] = new TreeLikelihood();
        		treelikelihood[i].setID(getID() + i);
        		treelikelihood[i].getOutputs().add(this);
        		likelihoodsInput.get().add(treelikelihood[i]);

        		FilteredAlignment filter = new FilteredAlignment();
        		if (i == 0 && dataInput.get() instanceof FilteredAlignment && ((FilteredAlignment)dataInput.get()).constantSiteWeightsInput.get() != null) {
        			filter.initByName("data", dataInput.get()/*, "userDataType", m_data.get().getDataType()*/, 
        							"filter", filterSpec, 
        							"constantSiteWeights", ((FilteredAlignment)dataInput.get()).constantSiteWeightsInput.get()
        							);
        		} else {
        			filter.initByName("data", dataInput.get()/*, "userDataType", m_data.get().getDataType()*/, 
        							"filter", filterSpec
        							);
        		}
        		treelikelihood[i].initByName("data", filter, 
        				"tree", treeInput.get(), 
        				"siteModel", duplicate((BEASTInterface) siteModelInput.get(), i), 
        				"branchRateModel", duplicate(branchRateModelInput.get(), i), 
        				"useAmbiguities", useAmbiguitiesInput.get(),
						"scaling" , scalingInput.get() + ""
        				);
        	}
    	}
    }
    
    
    /** create new instance of src object, connecting all inputs from src object
     * Note if input is a SubstModel, it is duplicated as well.
     * @param src object to be copied
     * @param i index used to extend ID with.
     * @return copy of src object
     */
    private Object duplicate(BEASTInterface src, int i) {
    	if (src == null) { 
    		return null;
    	}
    	BEASTInterface copy;
		try {
			copy = src.getClass().newInstance();
        	copy.setID(src.getID() + "_" + i);
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			throw new RuntimeException("Programmer error: every object in the model should have a default constructor that is publicly accessible: " + src.getClass().getName());
		}
        for (Input<?> input : src.listInputs()) {
            if (input.get() != null) {
                if (input.get() instanceof List) {
                    // handle lists
                	//((List)copy.getInput(input.getName())).clear();
                    for (Object o : (List<?>) input.get()) {
                        if (o instanceof BEASTInterface) {
                        	// make sure it is not already in the list
                            copy.setInputValue(input.getName(), o);
                        }
                    }
                } else if (input.get() instanceof SubstitutionModel) {
                	// duplicate subst models
                	BEASTInterface substModel = (BEASTInterface) duplicate((BEASTInterface) input.get(), i);
            		copy.setInputValue(input.getName(), substModel);
            	} else {
                    // it is some other value
            		copy.setInputValue(input.getName(), input.get());
            	}
            }
        }
        copy.initAndValidate();
		return copy;
	}

	private void calcPatternPoints(int nPatterns) {
		patternPoints = new int[m_nThreads + 1];
		if (proportionsInput.get() == null) {
			int range = nPatterns / m_nThreads;
			for (int i = 0; i < m_nThreads - 1; i++) {
				patternPoints[i+1] = range * (i+1);
			}
			patternPoints[m_nThreads] = nPatterns;
		} else {
			String [] strs = proportionsInput.get().split("\\s+");
			double [] proportions = new double[m_nThreads];
			for (int i = 0; i < m_nThreads; i++) {
				proportions[i] = Double.parseDouble(strs[i % strs.length]);
			}
			// normalise
			double sum = 0;
			for (double d : proportions) {
				sum += d;
			}
			for (int i = 0; i < m_nThreads; i++) {
				proportions[i] /= sum;
			}
			// cummulative 
			for (int i = 1; i < m_nThreads; i++) {
				proportions[i] += proportions[i- 1];
			}
			// calc ranges
			for (int i = 0; i < m_nThreads; i++) {
				patternPoints[i+1] = (int) (proportions[i] * nPatterns + 0.5);
			}
		}
    }
        
    /**
     * This method samples the sequences based on the tree and site model.
     */
    @Override
	public void sample(State state, Random random) {
        throw new UnsupportedOperationException("Can't sample a fixed alignment!");
    }


    @Override
    public double calculateLogP() {
		logP =  calculateLogPByBeagle();
		return logP;
    }

    private CountDownLatch m_nCountDown;
	
	class BeagleCoreRunnable implements Runnable {
		int m_iThread;
		TreeLikelihood beagle;
		
		BeagleCoreRunnable(int iThread, TreeLikelihood beagle) {
			    m_iThread = iThread;
			    this.beagle = beagle;
		}

        @Override
		public void run() {
  		  	try {
	            logPByThread[m_iThread] = beagle.calculateLogP();
  		  	} catch (Exception e) {
  		  		System.err.println("Something went wrong ith thread " + m_iThread);
				e.printStackTrace();
				System.exit(0);
			}
  		    m_nCountDown.countDown();
        }

	} // CoreRunnable
	
    private double calculateLogPByBeagle() {
		try {
			if (m_nThreads > 1) {
				m_nCountDown = new CountDownLatch(m_nThreads);
		    	for (int iThread = 0; iThread < m_nThreads; iThread++) {
		    		BeagleCoreRunnable coreRunnable = new BeagleCoreRunnable(iThread, treelikelihood[iThread]);
		    		BeastMCMC.g_exec.execute(coreRunnable);
		    	}
				m_nCountDown.await();
		    	logP = 0;
		    	for (double f : logPByThread) {
		    		logP += f;
		    	}
			} else {
				logP = treelikelihood[0].calculateLogP();
			}
		} catch (RejectedExecutionException | InterruptedException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return logP;
	}
    
	
    /** CalculationNode methods **/

    /**
     * check state for changed variables and update temp results if necessary *
     */
    @Override
    protected boolean requiresRecalculation() {
		boolean requiresRecalculation = false;
		for (TreeLikelihood b : treelikelihood) {
			requiresRecalculation |= b.requiresRecalculation();
		}
		return requiresRecalculation;
    }

    @Override
    public void store() {
//		for (TreeLikelihood b : treelikelihood) {
//			b.store();
//		}
    	super.store();
    }

    @Override
    public void restore() {
//		for (TreeLikelihood b : treelikelihood) {
//			b.restore();
//		}
    	super.restore();
    }
        
    /**
     * @return a list of unique ids for the state nodes that form the argument
     */
    @Override
	public List<String> getArguments() {
        return Collections.singletonList(dataInput.get().getID());
    }

    /**
     * @return a list of unique ids for the state nodes that make up the conditions
     */
    @Override
	public List<String> getConditions() {
        return ((SiteModel.Base)siteModelInput.get()).getConditions();
    }
    
} // class ThreadedTreeLikelihood
