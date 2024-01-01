package beast.base.evolution.operator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.inference.Operator;
import beast.base.inference.OperatorSchedule;
import beast.base.inference.StateNode;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.core.Log;
import beast.base.evolution.tree.Tree;
import beast.base.util.Randomizer;
import beast.base.evolution.tree.TreeMetric;
import beast.base.inference.parameter.CompoundRealParameter;

/**
 * 
 * @author Jordan Douglas
 */ 
@Description("An operator which selects samples from a series of other operators, with respect to their ability to explore one or more real/int parameters " +
"Training for each operator occurs following a burn-in period " +
"After a learning period, AdaptableOperatorSampler should pick the operator which is giving the best results n a particular data set")
@Citation(value="Douglas J, Zhang R, Bouckaert R. Adaptive dating and fast proposals: Revisiting the phylogenetic relaxed clock model. PLoS computational biology. 2021 Feb 2;17(2):e1008322.", 
	DOI="10.1371/journal.pcbi.1008322")
public class AdaptableOperatorSampler extends Operator {
	
	

    final public Input<List<Function>> paramInput = new Input<>("parameter", "list of parameters to compare before and after the proposal. If the tree heights"
    		+ " are a parameter then include the tree under 'tree'", new ArrayList<Function>());
    final public Input<List<Tree>> treeInput = new Input<>("tree", "tree(s) containing node heights to compare before and after the proposal (optional)", new ArrayList<>());
    final public Input<List<Operator>> operatorsInput = new Input<>("operator", "list of operators to select from", new ArrayList<Operator>());
    
    final public Input<Integer> burninInput = new Input<>("burnin", "number of operator calls until the learning process begins (default: 1000)", 1000);
    final public Input<Integer> learninInput = new Input<>("learnin", "number of operator calls after learning begins (ie. at burnin) but before this operator starts to use what it has learned"
    		+ " (default: 100 x number of operators)");
    
    
    final public Input<Double> uniformSampleProbInput = new Input<>("uniformp", "the probability that operators are sampled uniformly at random instead of using the trained parameters (default 0.1)", 0.1);
	
    
    final public Input<TreeMetric> treeMetricInput = new Input<>("metric", "A function for computing the distance between trees. If left empty, then tree distances will not be compared");
	
    final public Input<Double> maxRuntimeInput = new Input<>("maxRuntime", "The maximum amount of time (ms) to count towards the runtime of an operator. This should ensure that indefinite thread interruptions,"
    		+ "eg. on a cluster, do not unfairly penalise an operator for being slow", 1e6);
	
    
    final public Input<Boolean> setWeightFromDimension = new Input<>("dimensional", "Whether to set the weight of this operator as the dimension of its parameter", false);
    
    
    final boolean DEBUG = false;
    

    
    List<Function> parameters;
    List<Tree> trees;
    List<Operator> operators;
    double uniformSampleProb;
    int burnin;
    int learnin;
    int numParams;
    int numOps;
    
    TreeMetric treeMetric;
    

    // Number of times this meta-operator has been called
    long nProposals;
    
    // Learning begins after burnin
    boolean learningHasBegun;
    
    // Application of the learned terms to sample operators begins after learnin
    boolean teachingHasBegun;
    
    
    // The last operator which was called
    int lastOperator;
    
    // The parameter values from before the current proposal was made
    double[][] stateBefore;
    List<Tree> treesBefore = new ArrayList<>();
    
    // The start time of the previously called proposal
    long startTimeOfProposal;
    
    // Mean sum-of-squares of each parameter across before and after each operator was accepted
    double[][] mean_SS;
    
    
    // Cumulative mean sum (aka the mean) of each parameter -> for calculating mean
    double[] param_mean_sum = null;
    
    // Cumulative mean sum of squares of each parameter -> for calculating variance
    double[] param_mean_SS = null;
    
    // Number of times each operator has been called
    int[] numProposals = null;
    
    // Number of times each operator has been accepted
    int[] numAccepts = null;
    
    // Mean runtime of each operator (ms)
    double[] operator_mean_runtimes;

    

	@Override
	public void initAndValidate() {

		// Operators
		this.operators = new ArrayList<>();
		for (Operator operator: operatorsInput.get()) {
			if (operator.getWeight() > 0) {
				operators.add(operator);
			} else {
				Log.warning("Operator " + operator.getID() + " ignored by " + this.getClass().getSimpleName() + " because its weight is " + operator.getWeight());
			}
		}
		this.numOps = this.operators.size();
		
		// Parameters/tree
		this.trees = treeInput.get();
		if (trees.isEmpty()) trees = null;
		this.treesBefore = new ArrayList<>();
		this.parameters = paramInput.get();
		this.treeMetric = treeMetricInput.get();
		this.numParams = this.parameters.size() + (this.trees == null ? 0 : 1);
		
		// Burnin
		this.burnin = Math.max(0, burninInput.get());
		
		// Learnin
		if (learninInput.get() == null) {
			this.learnin = 100 * this.numOps;
		}else {
			this.learnin = Math.max(0, learninInput.get());
		}
		

		this.nProposals = 0;
		this.learningHasBegun = this.burnin == 0;
		this.teachingHasBegun = this.burnin + this.learnin == 0;
		this.lastOperator = -1;
		this.uniformSampleProb = uniformSampleProbInput.get();
		this.uniformSampleProb = Math.min(Math.max(this.uniformSampleProb, 0), 1);
		
		
		
		// Validate
		if (this.numOps < 2) {
			Log.warning("Warning: please provide at least two operators");
		}
		if (this.numParams == 0) {
			Log.warning("Warning: at least one sampled parameter or a tree should be provided to assist in measuring the efficiency of each operator.");
		}
		
		// If there is a tree metric, then ensure there is a tree and no parameters
		if (this.treeMetric != null) {
			if (this.trees == null) throw new IllegalArgumentException("Please provide a tree (or do not provide a tree metric)");
			if (!this.parameters.isEmpty()) throw new IllegalArgumentException("Please provide parameters or a tree metric but not both");
			this.numParams = 1;
		}
		
		
		// Check parameters
		for (Function p : this.parameters) {
			if (! (p instanceof RealParameter) && !(p instanceof IntegerParameter)) {
				//throw new IllegalArgumentException("Parameters must be Real or Integer parameters!");
			}
		}
		
		

		
		// Learned num proposals and accepts of all operators
		this.numProposals = new int[this.numOps];
		this.numAccepts = new int[this.numOps];
		this.operator_mean_runtimes = new double[this.numOps];
		
		for (int i = 0; i < this.numOps; i ++) {
			this.numProposals[i] = 0;
			this.numAccepts[i] = 0;
			this.operator_mean_runtimes[i] = 0.001; // Initialise it at 1ns to avoid division by 0
		}


		if (this.numParams > 0) {
			

			// Mean sum-of-squares of each difference before and after each proposal
			this.mean_SS = new double[this.numOps][]; 
			
			for (int i = 0; i < this.numOps; i ++) {
				
				this.mean_SS[i] = new double[this.numParams];
				
				for (int p = 0; p < this.numParams; p ++) {
					this.mean_SS[i][p] = 0;
				}
			}
			
			
			// Cumulative sum and SS of each parameter in each state
			this.param_mean_sum = new double[this.numParams];
			this.param_mean_SS = new double[this.numParams];
			for (int p = 0; p < this.numParams; p ++) {
				this.param_mean_sum[p] = 0;
				this.param_mean_SS[p] = 0;
			}
			
		}
		
		
		// Dimensional weighting
		if (setWeightFromDimension.get()) {
			
			
			double weight = 0;
			if (this.treeMetric != null) {
				
				// Topology: the dimension is number of nodes - 1
				for (Tree tree : trees) weight += tree.getNodeCount()-1;
				
			}else {
				
				// Get the total dimensionality
				for (int p = 0; p < this.numParams; p ++) {
					
					if (trees != null && p == this.numParams - 1 ) {
						
						// Number of nodes
						for (Tree tree : trees) weight += tree.getNodeCount(); // - tree.getLeafNodeCount();
						
					}else {
						// A RealParameter
						weight += this.parameters.get(p).getDimension();
					}
					
				}
			}
			
			Log.warning("Dimensional weighting: setting the weight of " + this.getID() + " to " + weight);
			this.m_pWeight.setValue(weight, this);
			
			
		}
		


		
	}

	@Override
	public double proposal() {
		

		
		// Get the values of each parameter before doing the proposal
		this.stateBefore = this.getAllParameterValues();
		if (this.treeMetric != null) {
			this.treesBefore.clear();
			for (Tree tree : this.trees) {
				this.treesBefore.add(tree.copy());
			}
		}

		
		// Update sum and SS of each parameter before making the proposal
		this.updateParamStats(this.stateBefore);
		

		// Sample an operator
		double[] operatorCumulativeProbs = this.getOperatorCumulativeProbs(false);
		this.lastOperator = Randomizer.binarySearchSampling(operatorCumulativeProbs);
		Operator operator = this.operators.get(this.lastOperator);
		
		// Increment the number of proposals
		this.nProposals ++;
		if (this.nProposals >= this.burnin && !this.learningHasBegun) {
			if (DEBUG) Log.warning("Burnin has been achieved. Beginning learning...");
			this.learningHasBegun = true;
		}
		if (this.nProposals >= this.burnin + this.learnin && !this.teachingHasBegun) {
			if (DEBUG) Log.warning("Learnn has been achieved. Applying the learning now...");
			this.teachingHasBegun = true;
		}
		if (this.learningHasBegun) this.numProposals[this.lastOperator] ++;

		
		// Write down the start time
		this.startTimeOfProposal = System.currentTimeMillis();
		
		// Do the proposal. If it gets accepted then the differences between the two states will be calculated afterwards
		return operator.proposal();

	}
	
	
	/**
	 * Update the mean and SS of each parameter from the current state
	 * @param thisState
	 */
	private void updateParamStats(double[][] thisState) {
		
		// If the parameter is the tree topology, then we do not compute the parameter mean/variance
		if (this.treeMetric != null) return;
		
		
		if (this.learningHasBegun && this.numParams > 0) {
			

			long n = this.nProposals - this.burnin;

			// Update the sum and the sum-of-squares each parameter
			for (int p = 0; p < this.numParams; p ++) {
				
				// If parameter is multidimensional, take the mean 
				double val = 0;
				for (int j = 0; j < thisState[p].length; j++) {
					val += thisState[p][j] / thisState[p].length;
				}

				// Mean -> sum -> mean
				double sum = param_mean_sum[p]*n + val;
				this.param_mean_sum[p] = sum / (n+1.0);
				
				
				// MeanSS -> sumSS -> meanSS
				double SSsum = param_mean_SS[p]*n + val*val;
				this.param_mean_SS[p] = SSsum / (n+1.0);
				

		
			}
		
		}
		
	}

	/**
	 * 
	 * @return A list of cumulative probabilities for sampling each operator
	 */
	public double[] getOperatorCumulativeProbs(boolean forceSampling){
		
		double[] operatorWeights = new double[this.numOps];
		boolean sampleUniformlyAtRandom = !forceSampling && (!this.teachingHasBegun || Randomizer.nextFloat() < this.uniformSampleProb);
		
		
		
		if (!sampleUniformlyAtRandom) {
			
			// If past burn-in, then sample from acceptance x squared-diff
			for (int i = 0; i < this.numOps; i ++) {
				
				//Operator op = this.operators.get(i);
				double acceptanceProb = 1.0 * this.numAccepts[i] / this.numProposals[i];
				double hScore = 0;
				
				// Calculate h for each parameter with respect to this operator
				for (int p = 0; p < this.numParams; p ++) {
					
					// p = numParams - 1 is the tree heights, all others are RealParameters
					double h = this.getZ(i, p) / this.numParams;
					if (Double.isNaN(h) || Double.isInfinite(h)) h = 0;
					hScore += h;
					
				}
				
				if (Double.isNaN(hScore) || Double.isInfinite(hScore)) hScore = 0;
				operatorWeights[i] = acceptanceProb * hScore;
				
				
				if (DEBUG) Log.warning("Operator " + i + " has acceptance prob of " + acceptanceProb + " and an hscore of " + hScore);
				
			}
			
			
		} else {
			
			// If still in burn-in, then sample uniformly at random
			for (int i = 0; i < this.operators.size(); i ++) {
				operatorWeights[i] = 1;
			}
			
		}
		
		
		// Weight sum
		double weightSum = 0;
		for (int i = 0; i < this.numOps; i ++) weightSum += operatorWeights[i];
		
		
		if (weightSum <= 0) {
			
			// If the weight sum is zero, then sample uniformly at random
			for (int i = 0; i < this.numOps; i ++) operatorWeights[i] = 1.0 / this.numOps;
		}else {
			
			// Otherwise normalise weights into probabilities
			for (int i = 0; i < this.numOps; i ++) operatorWeights[i] /= weightSum;
		}
		
		// Convert to cumulative probability array
		double cumProb = 0;
		for (int i = 0; i < this.numOps; i ++) {
			cumProb += operatorWeights[i];
			operatorWeights[i] = cumProb;
		}
		
		
		return operatorWeights;
	}
		

	
	/**
	 * Get all parameter values in the current state
	 * @return
	 */
	private double[][] getAllParameterValues() {
		
		
		if (this.numParams == 0) return null;
		
		double[][] vals = new double[this.numParams][];
		for (int p = 0; p < this.numParams; p ++) {
			
			// Get the values of this parameter
			double[] p_vals;
			if (trees != null && p == this.numParams - 1 ) {
				
				// The parameter is the tree heights
				int numNodes = 0;
				for (Tree tree : trees) numNodes += tree.getNodeCount();
				p_vals = new double[numNodes];
				int nodeNum = 0;
				for (Tree tree : trees) {
					for (int treeNodeNum = 0; treeNodeNum < tree.getNodeCount(); treeNodeNum++) {
						p_vals[nodeNum] = tree.getNode(treeNodeNum).getHeight();
						nodeNum ++;
					}
				}
				
				
			}else {
				
				// A RealParameter
				p_vals = this.parameters.get(p).getDoubleValues();
			}
			
			
			vals[p] = p_vals;
			
		}

		return vals;
		
		
	}
	
	
	@Override
	public void accept() {
		
		// Update trained terms from the accept
		if (learningHasBegun) {

			try {
			
				
				this.recordRuntime(this.startTimeOfProposal, System.currentTimeMillis(), this.lastOperator);
				
				if (this.numParams > 0) {
					
					long n = this.numAccepts[this.lastOperator];
				
					// Get the values of each parameter after doing the proposal
					double[][] stateAfter = this.getAllParameterValues();
		
					// Compute the average squared difference between the before and after states
					double[] squaredDiffs = this.computeSS(this.stateBefore, stateAfter, this.treesBefore, this.trees);
					
					// Update the sum of squared diffs for each parameter with respect to this operator
					for (int p = 0; p < this.numParams; p ++) {
						double SS = this.mean_SS[this.lastOperator][p]*n + squaredDiffs[p];
						this.mean_SS[this.lastOperator][p] = SS / (n+1.0);
					}
				}
				
			}catch (Exception e) {
				
			}
			
		}
		
		// Update the num accepts of this operator
		this.numAccepts[this.lastOperator] ++;	
		
		
		
		this.operators.get(this.lastOperator).accept();
		super.accept();
		
		
	
	}
	
	
	@Override
	public void reject(int reason) {
		if (learningHasBegun) this.recordRuntime(this.startTimeOfProposal, System.currentTimeMillis(), this.lastOperator);
		this.operators.get(this.lastOperator).reject(reason);
		super.reject(reason);
	}
	
	
	/**
	 * Updates the mean runtime of this operator in ms
	 * @param startTime
	 * @param stopTime
	 * @param operatorNum
	 */
	private void recordRuntime(long startTime, long stopTime, int operatorNum) {
		
		double time = stopTime - startTime;
		assert time >= 0;
		
		
		// If the operator is too fast the time will be zero. In this case, take the value of 1ns
		time = Math.max(time, 0.001);
		
		// Do not go over the maximum time, as this could be due to pauses
		time = Math.min(time, maxRuntimeInput.get());
		
		int n = this.numProposals[operatorNum];
		double sum = this.operator_mean_runtimes[operatorNum]*(n-1) + time;
		this.operator_mean_runtimes[operatorNum] = sum / n;

		
	}
	
	/**
	 * Return the sum of squares of the difference within each parameter before and after the proposal was accepted
	 * @param before
	 * @param after
	 */
	private double[] computeSS(double[][] before, double[][] after, List<Tree> beforeTrees, List<Tree> afterTrees) {
		
		
		double[] squaredDiff = new double[this.numParams];
		
		
		// Measure the distance between trees
		squaredDiff[0] = 0;
		if (this.treeMetric != null) {
			for (int i = 0; i < beforeTrees.size(); i ++) {
				Tree afterTree = afterTrees.get(i);
				squaredDiff[0] += Math.pow(this.treeMetric.distance(beforeTrees.get(i), afterTree), 2);
			}
		} 
		
		// Measure the distance between parameters
		else {
		
		
			for (int p = 0; p < this.numParams; p ++) {
				
				double[] p_before = before[p];
				double[] p_after = after[p];
				
				
				// Average the squared difference across all dimensions of this parameter
				double meanDelta2 = 0;
				for (int j = 0; j < p_before.length; j ++) {
					meanDelta2 += Math.pow(p_before[j] - p_after[j], 2);
				}
				
				squaredDiff[p] = meanDelta2;
					
			}
		
		}
		
		return squaredDiff;
		
	}


	
	
    @Override
    public void optimize(double logAlpha) {
    	this.operators.get(this.lastOperator).optimize(logAlpha);
    }
	


    
    
    /**
     * Returns a variance using a mean and a mean sum of squares
     * @param sum
     * @param SS
     * @return
     */
    public double getVar(double mean, double meanSS) {
    	return meanSS - mean*mean;
    }
    
    
    /**
     * Returns the normalised average squared-difference that this operator causes when applied to this parameter
     * This difference is normalised by dividing the mean squared-difference by the variance of the parameter 
     * This enables comparison between parameters which exist on different magnitudes
     * The value is also divided by the average runtime of the operator
     * @param opNum
     * @param paramNum
     * @return
     */
    public double getZ(int opNum, int paramNum) {
    	
    	// Get runtime
    	double runtime = this.operator_mean_runtimes[opNum];

    	// Contribution from the parameter (ie. 1/variance)
    	double parameterVariance = this.treeMetric != null ? 1 : this.getVar(this.param_mean_sum[paramNum], this.param_mean_SS[paramNum]);
    	
    	// Contribution from average squared-difference of applying this operator to this parameter
    	double opParTerm = this.mean_SS[opNum][paramNum];
    	
    	
    	return opParTerm / (runtime * parameterVariance);
    	
    }
    
    @Override
    public void setOperatorSchedule(final OperatorSchedule operatorSchedule) {
    	super.setOperatorSchedule(operatorSchedule);
    	for (int i = 0; i < this.numOps; i ++) this.operators.get(i).setOperatorSchedule(operatorSchedule);
    }
    
    
    @Override
    public List<StateNode> listStateNodes() {
    	
    	List<StateNode> stateNodes = new ArrayList<StateNode>(); //super.listStateNodes();
    	


    	for (int i = 0; i < this.numOps; i ++) {
    		
    		// Maintain a list of unique elements
    		for (StateNode node : this.operators.get(i).listStateNodes()) {
    			if (!stateNodes.contains(node)) stateNodes.add(node);
    		}

    	}
    	
    	
    	// Remove compound operators and add their components instead
    	boolean hasCompoundRealParameter = true;
	   	while (hasCompoundRealParameter) {
	   		hasCompoundRealParameter = false;
	       	for (int i = 0; i < stateNodes.size(); i++) {
	       		StateNode s = stateNodes.get(i);
	       		if (s instanceof CompoundRealParameter) {
	       			CompoundRealParameter c = (CompoundRealParameter) s;
	       			stateNodes.remove(i);
	       			
	       			for (StateNode node : c.parameterListInput.get()) {
	        			if (!stateNodes.contains(node)) stateNodes.add(node);
	        		}

	       			//stateNodes.addAll(c.parameterListInput.get());
	       			i--;
	       			hasCompoundRealParameter = true;
	       		}
	       	}
	   	}
    	
    	
    	return stateNodes;
    }
    

    
    
    
    @Override
    public void storeToFile(final PrintWriter out) {
    	

        
    	try {
	        JSONStringer json = new JSONStringer();
	        json.object();
	
	        if (getID() == null) setID("unknown");
	
	        // id
	        json.key("id").value(getID());
	        
	        
	        
	        // Store generic beast core properties by writing its json to a string and then parsing it back
	        StringWriter outStr = new StringWriter();
	        PrintWriter writer = new PrintWriter(outStr);
	        super.storeToFile(writer);
	        JSONObject obj = new JSONObject(outStr.toString());
	        for (String key : obj.keySet()) {
	        	if (key.equals("id")) continue;
	        	json.key(key).value(obj.get(key));
	        }
	        
	
	        // N proposals
	        json.key("nProposals").value(this.nProposals);
	        
	        // Store parameter sum/SS
	        json.key("param_mean_sum").value(Arrays.toString(this.param_mean_sum));
	        json.key("param_mean_SS").value(Arrays.toString(this.param_mean_SS));
	        
	        
	        // Store accepts/rejects/runtime of each operator
	        json.key("numAccepts").value(Arrays.toString(this.numAccepts));
	        json.key("numProposals").value(Arrays.toString(this.numProposals));
	        json.key("operator_mean_runtimes").value(Arrays.toString(this.operator_mean_runtimes));
	        
	        
	        // Store SS for each operator-parameter combination
	        json.key("mean_SS").value(Arrays.deepToString(this.mean_SS));
	        
	        
	        
	        
	        // For interest of the user (although not read in again later) also print the weight of each operator
	        double[] cumulativeProb = this.getOperatorCumulativeProbs(true);
	        double[] weights = new double[this.numOps];
	        for (int i = 0; i < this.numOps; i ++) {
	        	if (i == 0) weights[i] = cumulativeProb[i];
	        	else weights[i] = cumulativeProb[i] - cumulativeProb[i-1];
	        }
	        json.key("weights").value(Arrays.toString(weights));


	        // Store sub-operators in a list
	        JSONArray operatorListJson = new JSONArray();
	        for (int i = 0; i < this.numOps; i ++) {
	        	
	        	// Write the operator's json to a string
	        	outStr = new StringWriter();
		        writer = new PrintWriter(outStr);
	        	this.operators.get(i).storeToFile(writer);
	        	

	        	
	        	// Parse the json of the operator
	        	obj = new JSONObject(outStr.toString());
	        	operatorListJson.put(obj);

	        	//System.out.println(outStr.toString());

	        	
	        }
	        json.key("operators").value(operatorListJson);
	        
	        json.endObject();
	        out.print(json.toString());
	        

	        
    	} catch (JSONException e) {
    		// failed to log operator in state file
    		// report and continue
    		e.printStackTrace();
    	}
    }
    
    
    

    @Override
    public void restoreFromFile(JSONObject o) {

    	
    	super.restoreFromFile(o);
    	
    	
    	try {
    		
    		
    		this.nProposals = Long.parseLong(o.getString("nProposals"));
    		
    		
    		// Parameter sum and sum-of-squares
    		String[] param_sum_string = ((String) o.getString("param_mean_sum")).replace("[", "").replace("]", "").split(", ");
	        String[] param_SS_string = ((String) o.getString("param_mean_SS")).replace("[", "").replace("]", "").split(", ");
	        if (param_sum_string.length != this.numParams) {
	        	throw new IllegalArgumentException("Cannot resume because there are " + param_sum_string.length + " elements in param_mean_sum but " + this.numParams + " params");
	        }
	        if (param_SS_string.length != this.numParams) {
	        	throw new IllegalArgumentException("Cannot resume because there are " + param_SS_string.length + " elements in param_mean_SS but " + this.numParams + " params");
	        }
	        this.param_mean_sum = new double[this.numParams];
	        this.param_mean_SS = new double[this.numParams];
	        for (int p = 0; p < this.numParams; p++) {
	        	this.param_mean_sum[p] = Double.parseDouble(param_sum_string[p]);
	        	this.param_mean_SS[p] = Double.parseDouble(param_SS_string[p]);
	        }
	        
	        
	        // Operator runtime, proposals, and accepts post-burnin
	        String[] numAccepts_string = ((String) o.getString("numAccepts")).replace("[", "").replace("]", "").split(", ");
	        String[] numProposals_string = ((String) o.getString("numProposals")).replace("[", "").replace("]", "").split(", ");
	        String[] operator_mean_runtimes_string = ((String) o.getString("operator_mean_runtimes")).replace("[", "").replace("]", "").split(", ");
	        if (numAccepts_string.length != this.numOps) {
	        	throw new IllegalArgumentException("Cannot resume because there are " + numAccepts_string.length + " elements in numAccepts but " + this.numOps + " operators");
	        }
	        if (numProposals_string.length != this.numOps) {
	        	throw new IllegalArgumentException("Cannot resume because there are " + numProposals_string.length + " elements in numProposals but " + this.numOps + " operators");
	        }
	        if (operator_mean_runtimes_string.length != this.numOps) {
	        	throw new IllegalArgumentException("Cannot resume because there are " + operator_mean_runtimes_string.length + " elements in operator_mean_runtimes but " + this.numOps + " operators");
	        }
	        this.numAccepts = new int[this.numOps];
	        this.numProposals = new int[this.numOps];
	        this.operator_mean_runtimes = new double[this.numOps];
	        for (int i = 0; i < this.numOps; i++) {
	        	this.numAccepts[i] = Integer.parseInt(numAccepts_string[i]);
	        	this.numProposals[i] = Integer.parseInt(numProposals_string[i]);
	        	this.operator_mean_runtimes[i] = Double.parseDouble(operator_mean_runtimes_string[i]);
	        }
	        
	        
	        
	        // Sum of squares
	        String[] SS_string = ((String) o.getString("mean_SS")).replace("[", "").replace("]", "").split(", ");
	        if (SS_string.length != this.numOps * this.numParams) {
	        	throw new IllegalArgumentException("Cannot resume because there are " + SS_string.length + " elements in mean_SS but " + (this.numOps*this.numParams) + " operator-parameter combinations");
	        }
	        this.mean_SS = new double[this.numOps][];
	        for (int i = 0; i < this.numOps; i++) {
	        	this.mean_SS[i] = new double[this.numParams];
	        	for (int p = 0; p < this.numParams; p++) {
	        		double val = Double.parseDouble(SS_string[i*this.numParams + p]);
		        	this.mean_SS[i][p] = val;
		        }
	        	 
	        }
	        
	        
	        
	    	// Load sub-operators
	        JSONArray operatorArray = o.getJSONArray("operators");
	        if (operatorArray.length() != this.numOps) {
	        	throw new IllegalArgumentException("Cannot resume because there are " + operatorArray.length() + " elements in 'operators' but there should be " + this.numOps);
		 	     
	        }
	        for (int i = 0; i < this.numOps; i ++) {
	        	JSONObject jsonOp = operatorArray.getJSONObject(i);
	        	this.operators.get(i).restoreFromFile(jsonOp);
	        }
	        
    		
	        super.restoreFromFile(o);  	
    	} catch (JSONException e) {
    		// failed to restore from state file
    		// report and continue
    		e.printStackTrace();
    	}
    }

    
    
    

}







