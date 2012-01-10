package beast.core;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import beast.util.Randomizer;

@Description("Specify operator selection and optimisation schedule")
public class OperatorSchedule extends Plugin {

	enum OptimisationTransform {none, log, sqrt};

	public Input<OptimisationTransform> transformInput = new Input<OperatorSchedule.OptimisationTransform>("transform",
			"transform optimisation schedeul (default none) This can be "
					+ Arrays.toString(OptimisationTransform.values()) + " (default 'none')",
			OptimisationTransform.none, OptimisationTransform.values());
	public Input<Boolean> autoOptimiseInput = new Input<Boolean>("autoOptimize", "whether to automatically optimise operator settings", true);
	public Input<Integer> autoOptimizeDelayInput = new Input<Integer>("autoOptimizeDelay", "number of samples to skip before auto optimisation kicks in", 10000);

	/** list of operators in the schedule **/
	List<Operator> operators = new ArrayList<Operator>();

	/** sum of weight of operators **/
	double totalWeight = 0;

	/** the relative weights add to unity **/
	double[] relativeOperatorWeigths;

	/** cumulative weights, with unity as max value **/
	double[] cumulativeProbs;

	/** name of the file to store operator related info **/
	String stateFileName;

	/**
	 * Don't start optimisation at the start of the chain, but wait till
	 * autoOptimizeDelay has been reached.
	 */
	protected int autoOptimizeDelay = 10000;
	protected int autoOptimizeDelayCount = 0;
	OptimisationTransform transform = OptimisationTransform.none;
	boolean autoOptimise = true;

	@Override
	public void initAndValidate() throws Exception {
		transform = transformInput.get();
		autoOptimise = autoOptimiseInput.get();
		autoOptimizeDelay = autoOptimizeDelayInput.get();
	}

	public void setStateFileName(String name) {
		this.stateFileName = name;
	}

	/** add operator to the schedule **/
	public void addOperator(Operator p) {
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
	 **/
	public Operator selectOperator() {
		int iOperator = Randomizer.randomChoice(cumulativeProbs);
		return operators.get(iOperator);
	}

	/** report operator statistics **/
	public void showOperatorRates(PrintStream out) {
		out.println("Operator                                                              Tuning\t#accept\t#reject\t#total\tacceptance rate");
		for (int i = 0; i < operators.size(); i++) {
			out.println(operators.get(i));
		}
	}

	/** store operator optimisation specific information to file **/
	public void storeToFile() throws Exception {
		// appends state of operator set to state file
		File aFile = new File(stateFileName);
		PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(aFile, true)));
		out.println("<!--\nID Weight Paramvalue #Accepted #Rejected #CorrectionAccepted #CorrectionRejected");
		for (int i = 0; i < operators.size(); i++) {
			Operator operator = operators.get(i);
			out.println(operator.getID() + " " + cumulativeProbs[i] + " " + operator.getCoercableParameterValue() + " "
					+ operator.m_nNrAccepted + " " + operator.m_nNrRejected + " " + operator.m_nNrAcceptedForCorrection
					+ " " + operator.m_nNrRejectedForCorrection);
		}
		out.println("-->");
		out.close();
	}

	/** restore operator optimisation specific information from file **/
	public void restoreFromFile() throws Exception {
		// reads state of operator set from state file
		String sXML = "";
		BufferedReader fin = new BufferedReader(new FileReader(stateFileName));
		while (fin.ready()) {
			sXML += fin.readLine() + "\n";
		}
		fin.close();
		sXML = sXML.substring(sXML.indexOf("</itsabeastystatewerein>") + 25);
		String[] sStrs = sXML.split("\n");
		for (int i = 0; i < operators.size() && i + 2 < sStrs.length; i++) {
			String[] sStrs2 = sStrs[i + 2].split(" ");
			Operator operator = operators.get(i);
			autoOptimizeDelayCount = 0;
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
				throw new Exception("Cannot resume: operator order or set changed from previous run");
			}
		}
		showOperatorRates(System.err);
	}

	/**
	 * Calculate change of coerceable parameter for operators that allow
	 * optimisation
	 * 
	 * @param logAlpha
	 *            difference in posterior between previous state & proposed
	 *            state + hasting ratio
	 * @return change of value of a parameter for MCMC chain optimisation
	 **/
	public double calcDelta(Operator operator, double logAlpha) {
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
		}

		final double deltaP = ((1.0 / count) * (Math.exp(Math.min(logAlpha, 0)) - target));

		if (deltaP > -Double.MAX_VALUE && deltaP < Double.MAX_VALUE) {
			return deltaP;
		}
		return 0;
	}

} // class OperatorSchedule
