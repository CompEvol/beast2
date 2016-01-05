package beast.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import beast.core.util.Log;
import beast.util.Randomizer;

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

    @Override
    public void initAndValidate() throws Exception {
        transform = transformInput.get();
        autoOptimise = autoOptimiseInput.get();
        autoOptimizeDelay = autoOptimizeDelayInput.get();
        detailedRejection = detailedRejectionInput.get();
    }

    public void setStateFileName(final String name) {
        this.stateFileName = name;
    }

    /**
     * add operator to the schedule *
     * @param p
     */
    public void addOperator(final Operator p) {
        operators.add(p);
        p.setOperatorSchedule(this);
        totalWeight += p.getWeight();
        cumulativeProbs = new double[operators.size()];
        cumulativeProbs[0] = operators.get(0).getWeight() / totalWeight;
        for (int i = 1; i < operators.size(); i++) {
            cumulativeProbs[i] = operators.get(i).getWeight() / totalWeight + cumulativeProbs[i - 1];
        }
    }

    /**
     * randomly select an operator with probability proportional to the weight
     * of the operator
     * @return
     */
    public Operator selectOperator() {
        final int iOperator = Randomizer.randomChoice(cumulativeProbs);
        return operators.get(iOperator);
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
        for (final Operator operator : operators) {
            out.println(prettyPrintOperator(operator, longestName, colWidth, 4, totalWeight, detailedRejection));
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
        
        formatter.close();
    }

    protected static String prettyPrintOperator(
            Operator op,
            int nameColWidth,
            int colWidth,
            int dp,
            double totalWeight,
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
        if (totalWeight > 0.0) {
            formatter.format(doubleFormat, op.getWeight() / totalWeight);
        }
        formatter.format(doubleFormat, accRate);

        sb.append(" " + op.getPerformanceSuggestion());

        formatter.close();
   
        return sb.toString();
    }

    /**
     * store operator optimisation specific information to file *
     * @throws Exception
     */
    public void storeToFile() throws Exception {
        // appends state of operator set to state file
        File aFile = new File(stateFileName);
        PrintWriter out = new PrintWriter(new FileWriter(aFile, true));

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
     * @throws Exception
     */
    public void restoreFromFile() throws Exception {
        // reads state of operator set from state file
        String sXML = "";
        final BufferedReader fin = new BufferedReader(new FileReader(stateFileName));
        while (fin.ready()) {
            sXML += fin.readLine() + "\n";
        }
        fin.close();
        int start = sXML.indexOf("</itsabeastystatewerein>") + 25 + 5;
        if (start >= sXML.length() - 4) {
        	return;
        }
        sXML = sXML.substring(sXML.indexOf("</itsabeastystatewerein>") + 25 + 5, sXML.length() - 4);
        try {
	        JSONObject o = new JSONObject(sXML);
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
	        String[] sStrs = sXML.split("\n");
            autoOptimizeDelayCount = 0;
	        for (int i = 0; i < operators.size() && i + 2 < sStrs.length; i++) {
	            String[] sStrs2 = sStrs[i + 1].split(" ");
	            Operator operator = operators.get(i);
	            if ((operator.getID() == null && sStrs2[0].equals("null")) || operator.getID().equals(sStrs2[0])) {
	                cumulativeProbs[i] = Double.parseDouble(sStrs2[1]);
	                if (!sStrs2[2].equals("NaN")) {
	                    operator.setCoercableParameterValue(Double.parseDouble(sStrs2[2]));
	                }
	                operator.m_nNrAccepted = Integer.parseInt(sStrs2[3]);
	                operator.m_nNrRejected = Integer.parseInt(sStrs2[4]);
	                autoOptimizeDelayCount += operator.m_nNrAccepted + operator.m_nNrRejected;
	                operator.m_nNrAcceptedForCorrection = Integer.parseInt(sStrs2[5]);
	                operator.m_nNrRejectedForCorrection = Integer.parseInt(sStrs2[6]);
	            } else {
	                throw new RuntimeException("Cannot resume: operator order or set changed from previous run");
	            }
	        }
	    }
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

} // class OperatorSchedule
