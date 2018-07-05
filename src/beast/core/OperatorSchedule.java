package beast.core;

import beast.core.util.Log;
import beast.util.Randomizer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

@Description("Specify operator selection and optimisation schedule")
public class OperatorSchedule extends BEASTObject {

    enum OptimisationTransform {none, log, sqrt}

    final public Input<OptimisationTransform> transformInput = new Input<>("transform",
            "transform optimisation schedule (default none) This can be "
                    + Arrays.toString(OptimisationTransform.values()) + " (default 'none')",
            OptimisationTransform.none, OptimisationTransform.values());
    final public Input<Boolean> autoOptimiseInput = new Input<>("autoOptimize", "whether to automatically optimise operator settings", true);

    final public Input<Boolean> detailedRejectionInput = new Input<>("detailedRejection", "true if detailed rejection statistics should be included. (default=false)", false);

    final public Input<Integer> autoOptimizeDelayInput = new Input<>("autoOptimizeDelay", "number of samples to skip before auto optimisation kicks in (default=10000)", 10000);

    // the following inputs are for to deal with schedules nested inside other schedules
    // this allows operators to be grouped, and a percentage of operator weights to be 
    // assigned to a group of operators.
    final public Input<List<Operator>> operatorsInput = new Input<>("operator", "operator that the schedule can choose from. Any operators "
    		+ "added by other classes (e.g. MCMC) will be added if there are no duplicates.", new ArrayList<>());
    final public Input<List<OperatorSchedule>> subschedulesInput = new Input<>("subschedule", "operator schedule representing a subset of"
    		+ "the weight of the operators it contains.", new ArrayList<>());
    final public Input<Double> weightInput = new Input<>("weight", "weight with which this operator schedule is selected. Only used when "
    		+ "this operator schedule is nested inside other schedules. This weight is relative to other operators and operator schedules "
    		+ "of the parent schedule.", 100.0);
    final public Input<Boolean> weightIsPercentageInput = new Input<>("weightIsPercentage", "indicates weight is a percentage of total weight instead of a relative weight", false);
    final public Input<String> operatorPatternInput = new Input<>("operatorPattern", "Regular expression matching operator IDs of operators of parent schedule");

    
    
    /**
     * list of operators in the schedule *
     */
    // temporary for play
    public List<Operator> operators = new ArrayList<>();

    /**
     * sum of weight of operators *
     */
    double totalWeight = 0;

    /**
     * cumulative weights, with unity as max value *
     */
    double[] cumulativeProbs;

    /**
     * The normalized weights of all operators. sums to 1.0 +/- numerical error
     */
    double[] normalizedWeights;

    /**
     * name of the file to store operator related info *
     */
    String stateFileName;

    /**
     * Don't start optimisation at the start of the chain, but wait till
     * autoOptimizeDelay has been reached.
     */
    protected int autoOptimizeDelay = 10000;
    protected int autoOptimizeDelayCount = 0;
    OptimisationTransform transform = OptimisationTransform.none;
    boolean autoOptimise = true;
    boolean detailedRejection = false;
    
    private boolean reweighted = false;

    @Override
    public void initAndValidate() {
        transform = transformInput.get();
        autoOptimise = autoOptimiseInput.get();
        autoOptimizeDelay = autoOptimizeDelayInput.get();
        detailedRejection = detailedRejectionInput.get();
        operators.addAll(operatorsInput.get());
        for (Operator o : operators) {
        	o.setOperatorSchedule(this);
        }
        
        // sanity check: make sure weight percentages add to less than 100%
        double sumPercentage = 0;
        for (OperatorSchedule o : subschedulesInput.get()) {
        	if (o.weightIsPercentageInput.get()) {
        		sumPercentage += o.weightInput.get();
        	}
        }
        if (sumPercentage > 100) {
        	throw new IllegalArgumentException("Sum of percentages of subschedules should not exceed 100%. Reduce the weight of subschedules.");
        }
        if (Math.abs(sumPercentage - 100) < 1e-6 && operators.size() > 0) {
        	throw new IllegalArgumentException("Sum of percentages of subschedules add to 100%, so operators in main schedule will be ignored. Reduce the weight of subschedules.");
        }
        
        // sanity check: warn if operators appear in multiple schedules
    	Set<Operator> allOperators = new LinkedHashSet<>();
    	allOperators.addAll(operators);
    	for (OperatorSchedule os : subschedulesInput.get()) {
    		for (Operator o : os.operators) {
    			if (allOperators.contains(o)) {
    				Log.warning("WARNING: Operator " + o.getID() + " is contained in multiple operator schedules.\n"
    						+ "Operator weighting may not work as expected.");
    			}
    			allOperators.add(o);
    		}
    	}

    }

    public void setStateFileName(final String name) {
        this.stateFileName = name;
    }

    /**
     * add operator to the schedule *
     * @param p
     */
    public void addOperator(final Operator p) {
    	// check for duplicates
    	for (Operator o : operators) {
    		if (o == p) {
    			// operator was already added earlier
    			return;
    		}
    	}
        operators.add(p);
        p.setOperatorSchedule(this);
        reweighted = false;
        totalWeight += p.getWeight();
    }

    /** used to add operators to subschedules matching a pattern **/ 
    protected void addOperators(Collection<Operator> ops) {
    	if (operatorPatternInput.get() == null || operatorPatternInput.get().trim().equals("")) {
    		return;
    	}
		String operatorPattern = operatorPatternInput.get();
    	boolean noMatch = true;
		for (Operator o : ops) {
			if (o.getID() != null && o.getID().matches(operatorPattern)) {
		    	for (Operator o2 : operators) {
		    		if (o2 == o) {
		    			// operator was already added earlier
		    			return;
		    		}
		    	}
				operators.add(o);
                noMatch = false;
			}
		}
    	reweighted = false;

		if (noMatch)
		   throw new IllegalArgumentException("Cannot find operator to match subschedule pattern: " + operatorPattern + " !\n");
    }
    
    /**
     * randomly select an operator with probability proportional to the weight
     * of the operator
     * @return
     */
    public Operator selectOperator() {
    	if (!reweighted) {
    		reweightOperators();
    		reweighted = true;
    	}
        final int operatorIndex = Randomizer.randomChoice(cumulativeProbs);
        return operators.get(operatorIndex);
    }

    private static final String TUNING = "Tuning";
    private static final String NUM_ACCEPT = "#accept";
    private static final String NUM_REJECT = "#reject";
    private static final String PR_M = "Pr(m)";
    private static final String PR_ACCEPT = "Pr(acc|m)";

    /**
     * report operator statistics *
     * @param out
     */
    public void showOperatorRates(final PrintStream out) {

        Formatter formatter = new Formatter(out);

        int longestName = 0;
        for (final Operator operator : operators) {
            if (operator.getName().length() > longestName) {
                longestName = operator.getName().length();
            }
        }

        formatter.format("%-" + longestName + "s", "Operator");

        int colWidth = 10;
        String headerFormat = " %" + colWidth + "s";

        formatter.format(headerFormat, TUNING);
        formatter.format(headerFormat, NUM_ACCEPT);
        formatter.format(headerFormat, NUM_REJECT);
        if (detailedRejection) {
            formatter.format(headerFormat, "rej.inv");
            formatter.format(headerFormat, "rej.op");
        }
        formatter.format(headerFormat, PR_M);
        formatter.format(headerFormat, PR_ACCEPT);
        out.println();
        int i = 0;
        for (final Operator operator : operators) {
            out.println(prettyPrintOperator(operator, longestName, colWidth, 5, normalizedWeights[i], detailedRejection));
            i += 1;
        }
        out.println();

        formatter.format(headerFormat,TUNING);
        out.println(": The value of the operator's tuning parameter, or '-' if the operator can't be optimized.");
        formatter.format(headerFormat, NUM_ACCEPT);
        out.println(": The total number of times a proposal by this operator has been accepted.");
        formatter.format(headerFormat, NUM_REJECT);
        out.println(": The total number of times a proposal by this operator has been rejected.");
        formatter.format(headerFormat, PR_M);
        out.println(": The probability this operator is chosen in a step of the MCMC (i.e. the normalized weight).");
        formatter.format(headerFormat, PR_ACCEPT);
        out.println(": The acceptance probability (" + NUM_ACCEPT + " as a fraction of the total proposals for this operator).");
        out.println();
        
        // closing the formatter somehow closes PrintStream out, so better not close this here
        //formatter.close();
    }

    protected static String prettyPrintOperator(
            Operator op,
            int nameColWidth,
            int colWidth,
            int dp,
            // weight of this operator (p(m))
            double normalizedWeight,
            boolean detailedRejection) {

        double tuning = op.getCoercableParameterValue();
        double accRate = (double) op.m_nNrAccepted / (double) (op.m_nNrAccepted + op.m_nNrRejected);

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        String intFormat = " %" + colWidth + "d";
        String doubleFormat = " %" + colWidth + "." + dp + "f";

        formatter.format("%-" + nameColWidth + "s", op.getName());
        if (!Double.isNaN(tuning)) {
            formatter.format(doubleFormat, tuning);
        } else {
            formatter.format(" %" + colWidth + "s", "-");
        }

        formatter.format(intFormat, op.m_nNrAccepted);
        formatter.format(intFormat, op.m_nNrRejected);
        if (detailedRejection) {
            formatter.format(doubleFormat, (double) op.m_nNrRejectedInvalid / (double) op.m_nNrRejected);
            formatter.format(doubleFormat, (double) op.m_nNrRejectedOperator / (double) op.m_nNrRejected);
        }
        formatter.format(doubleFormat, normalizedWeight);
        formatter.format(doubleFormat, accRate);

        sb.append(" " + op.getPerformanceSuggestion());

        formatter.close();
   
        return sb.toString();
    }

    /**
     * store operator optimisation specific information to file *
     * @throws IOException
     */
    public void storeToFile() throws IOException {
        // appends state of operator set to state file
        File file = new File(stateFileName);
        PrintWriter out = new PrintWriter(new FileWriter(file, true));

        out.println("<!--");
        out.println("{\"operators\":[");
        int k = 0;
        for (Operator operator: operators) {
            operator.storeToFile(out);
            if (k++ < operators.size() - 1) {
            	out.println(",");
            }
        }
        out.println("\n]}");
        out.println("-->");
        out.flush();
        out.close();
    }

    /**
     * restore operator optimisation specific information from file *
     * @throws IOException
     */
    public void restoreFromFile() throws IOException {
        // reads state of operator set from state file
        String xml = "";
        final BufferedReader fin = new BufferedReader(new FileReader(stateFileName));
        while (fin.ready()) {
            xml += fin.readLine() + "\n";
        }
        fin.close();
        int start = xml.indexOf("</itsabeastystatewerein>") + 25 + 5;
        if (start >= xml.length() - 4) {
        	return;
        }
        xml = xml.substring(xml.indexOf("</itsabeastystatewerein>") + 25 + 5, xml.length() - 4);
        try {
	        JSONObject o = new JSONObject(xml);
	        JSONArray operatorlist = o.getJSONArray("operators");
	        autoOptimizeDelayCount = 0;
	        for (int i = 0; i < operatorlist.length(); i++) {
	            JSONObject item = operatorlist.getJSONObject(i);
	            String id = item.getString("id");
	    		boolean found = false;
	            if (!id.equals("null")) {
	            	for (Operator operator: operators) {
	            		if (id.equals(operator.getID())) {
	                    	operator.restoreFromFile(item);
	                        autoOptimizeDelayCount += operator.m_nNrAccepted + operator.m_nNrRejected;
	                        found = true;
	            			break;
	            		}
	            	}
	            }
	        	if (!found) {
	        		Log.warning.println("Operator (" + id + ") found in state file that is not in operator list any more");
	        	}
	        }
	    	for (Operator operator: operators) {
	    		if (operator.getID() == null) {
	        		Log.warning.println("Operator (" + operator.getClass() + ") found in BEAST file that could not be restored because it has not ID");
	    		}
	    	}    
        } catch (JSONException e) {
        	// it is not a JSON file -- probably a version 2.0.X state file
	        String[] strs = xml.split("\n");
            autoOptimizeDelayCount = 0;
	        for (int i = 0; i < operators.size() && i + 2 < strs.length; i++) {
	            String[] strs2 = strs[i + 1].split(" ");
	            Operator operator = operators.get(i);
	            if ((operator.getID() == null && strs2[0].equals("null")) || operator.getID().equals(strs2[0])) {
	                cumulativeProbs[i] = Double.parseDouble(strs2[1]);
	                if (!strs2[2].equals("NaN")) {
	                    operator.setCoercableParameterValue(Double.parseDouble(strs2[2]));
	                }
	                operator.m_nNrAccepted = Integer.parseInt(strs2[3]);
	                operator.m_nNrRejected = Integer.parseInt(strs2[4]);
	                autoOptimizeDelayCount += operator.m_nNrAccepted + operator.m_nNrRejected;
	                operator.m_nNrAcceptedForCorrection = Integer.parseInt(strs2[5]);
	                operator.m_nNrRejectedForCorrection = Integer.parseInt(strs2[6]);
	            } else {
	                throw new RuntimeException("Cannot resume: operator order or set changed from previous run");
	            }
	        }
	    }
	    // resuming from state file needs to init normalizedWeights[]
        reweightOperators();
        showOperatorRates(System.err);
    }

    /**
     * Calculate change of coerceable parameter for operators that allow
     * optimisation
     *
     * @param operator
     * @param logAlpha difference in posterior between previous state & proposed
     *                 state + hasting ratio
     * @return change of value of a parameter for MCMC chain optimisation
     */
    public double calcDelta(final Operator operator, final double logAlpha) {
        // do no optimisation for the first N optimisable operations
        if (autoOptimizeDelayCount < autoOptimizeDelay || !autoOptimise) {
            autoOptimizeDelayCount++;
            return 0;
        }
        final double target = operator.getTargetAcceptanceProbability();

        double count = (operator.m_nNrRejectedForCorrection + operator.m_nNrAcceptedForCorrection + 1.0);
        switch (transform) {
            case log:
                count = Math.log(count + 1.0);
                break;
            case sqrt:
                count = Math.sqrt(count);
                break;
            case none:
            	break;
            default:
            	break;
        }

        final double deltaP = ((1.0 / count) * (Math.exp(Math.min(logAlpha, 0)) - target));

        if (deltaP > -Double.MAX_VALUE && deltaP < Double.MAX_VALUE) {
            return deltaP;
        }
        return 0;
    }


    /** 
     * collect all operators (both local and from sub schedules) and calculate weight for each of them 
     * **/
    private void reweightOperators() {
    	Set<Operator> allOperators = new LinkedHashSet<>();
    	Set<Operator> subOperators = new LinkedHashSet<>();
    	allOperators.addAll(operators);
    	for (OperatorSchedule os : subschedulesInput.get()) {
    		allOperators.addAll(os.operators);
    	}
    	for (OperatorSchedule os : subschedulesInput.get()) {
    		os.addOperators(allOperators);
    		subOperators.addAll(os.operators);
    	}
    	allOperators.addAll(subOperators);

    	Set<Operator> localOperators = new LinkedHashSet<>();
    	localOperators.addAll(allOperators);
    	localOperators.removeAll(subOperators);
    	
    	// set up operators list and raw weights
    	operators.clear();
    	for (Operator o : localOperators) {
    		operators.add(o);
    	}
    	for (OperatorSchedule os : subschedulesInput.get()) {
    		for (Operator o : os.operators) {
        		operators.add(o);
    		}
    	}
    	// operatorCount can double count operators that appear in multiple operator schedules
    	int operatorCount = operators.size();

        normalizedWeights = new double[operatorCount];
    	int i = 0;
    	for (Operator o : localOperators) {
            normalizedWeights[i++] = o.getWeight();
    	}
    	for (OperatorSchedule os : subschedulesInput.get()) {
    		for (Operator o : os.operators) {
                normalizedWeights[i++] = o.getWeight();
    		}
    	}
    	
    	// calculate weights per OperatorSchedule
    	double localWeight = 0;
    	for (Operator o : localOperators) {
    		localWeight += o.getWeight();
    	}
    	
    	double totalSubSchedulePercentage = 0;
    	double totalSubScheduleWeight = 0;
    	for (OperatorSchedule os : subschedulesInput.get()) {
    		if (os.weightIsPercentageInput.get()) {
    			totalSubSchedulePercentage += os.weightInput.get();
    		} else {
    			totalSubScheduleWeight += os.weightInput.get();
    		}
    	}
    	double totalWeight = totalSubSchedulePercentage >= 100 ? 100 :
    			(localWeight + totalSubScheduleWeight) * 100 / (100-totalSubSchedulePercentage);
    	
    	// reweight local operators
    	double localFactor = (1/totalWeight);    	
    	i = 0;
    	for (Operator o : localOperators) {
            normalizedWeights[i++] *= localFactor;
    	}

    	// reweight operators of sub OperatorSchedules
    	for (OperatorSchedule os : subschedulesInput.get()) {
	    	localWeight = 0;
	    	for (Operator o : os.operators) {
	    		localWeight += o.getWeight();
	    	}
	    	double factor;
    		if (!os.weightIsPercentageInput.get()) {
    			factor = (os.weightInput.get() / localWeight) * (1/totalWeight);
    		} else {
    			factor = (os.weightInput.get() / 100) * 1.0/localWeight;
    		}
	    	for (Operator o : os.operators) {
                normalizedWeights[i++] *= factor;
	    	}
    	}

    	
    	// calc cumulative probabilities
        cumulativeProbs = new double[normalizedWeights.length];
        cumulativeProbs[0] = normalizedWeights[0];
        for (i = 1; i < operators.size(); i++) {
            cumulativeProbs[i] = normalizedWeights[i] + cumulativeProbs[i - 1];
        }

        // log results
    	//Log.debug("operator weight cumulativeProbs");
        //for (i = 0; i < operatorCount; i++) {
        //	Log.debug(operators.get(i).getID() + " " + weights[i] + " " + cumulativeProbs[i]);
        //}
    }

    /** handy for unit tests **/
    public double [] getCummulativeProbs() {
    	return cumulativeProbs.clone();
    }

    /**
     * @param operator
     * @return the probability of selecting this operator
     */
    public double getNormalizedWeight(Operator operator) {
        int i = operators.indexOf(operator);
        if (i != -1 && normalizedWeights != null) {
            return normalizedWeights[i];
        } else return 0.0;
    }




} // class OperatorSchedule
