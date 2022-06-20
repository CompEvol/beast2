package beast.base.evolution.tree.coalescent;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.inference.parameter.RealParameter;


/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: ConstantPopulation.java,v 1.9 2005/05/24 20:25:55 rambaut Exp $
 */
@Description("Coalescent intervals for a exponentially growing population.")
public class ExponentialGrowth extends PopulationFunction.Abstract {
    final public Input<Function> popSizeParameterInput = new Input<>("popSize",
            "present-day population size (defaults to 1.0). ");
    final public Input<Function> growthRateParameterInput = new Input<>("growthRate",
            "Growth rate is the exponent of the exponential growth. " +
            "A value of zero represents a constant population size, negative values represent " +
            "decline towards the present, positive numbers represents exponential growth towards " +
            "the present.");

    //
    // Public stuff
    //

    @Override
	public void initAndValidate() {
        if (popSizeParameterInput.get() != null && popSizeParameterInput.get() instanceof RealParameter) {
            RealParameter param = (RealParameter) popSizeParameterInput.get();
            param.setBounds( Math.max(0.0, param.getLower()), param.getUpper());
        }
//        if (growthRateParameter.get() != null) {
//            growthRateParameter.get().setBounds(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
//        }
    }

    /**
     * @return initial population size.
     */
    public double getN0() {
        return popSizeParameterInput.get().getArrayValue();
    }

    /**
     * sets initial population size.
     *
     * @param N0 new size
     */
//    public void setN0(double N0) {
//        this.N0 = N0;
//    }

    /**
     * @return growth rate.
     */
    public final double getGrowthRate() {
        return growthRateParameterInput.get().getArrayValue();
    }

    /**
     * sets growth rate to r.
     *
     * @param r
     */
//    public void setGrowthRate(double r) {
//        this.r = r;
//    }

    /**
     * An alternative parameterization of this model. This
     * function sets growth rate for a given doubling time.
     *
     * @param doublingTime
     */
//    public void setDoublingTime(double doublingTime) {
//        setGrowthRate(Math.log(2) / doublingTime);
//    }

    // Implementation of abstract methods
    @Override
	public double getPopSize(double t) {

        double r = getGrowthRate();
        if (r == 0) {
            return getN0();
        } else {
            return getN0() * Math.exp(-t * r);
        }
    }

    /**
     * Calculates the integral 1/N(x) dx between start and finish.
     */
    @Override
    public double getIntegral(double start, double finish) {
        double r = getGrowthRate();
        if (r == 0.0) {
            return (finish - start) / getN0();
        } else {
            return (Math.exp(finish * r) - Math.exp(start * r)) / getN0() / r;
        }
    }

    @Override
	public double getIntensity(double t) {
        double r = getGrowthRate();
        if (r == 0.0) {
            return t / getN0();
        } else {
            return (Math.exp(t * r) - 1.0) / getN0() / r;
        }
    }

    @Override
	public double getInverseIntensity(double x) {

        double r = getGrowthRate();
        if (r == 0.0) {
            return getN0() * x;
        } else {
            return Math.log(1.0 + getN0() * x * r) / r;
        }
    }


    // Implementation of abstract methods

    @Override
	public List<String> getParameterIds() {
    	List<String> ids = new ArrayList<>();
    	if (popSizeParameterInput.get() instanceof BEASTInterface)
            ids.add(((BEASTInterface)popSizeParameterInput.get()).getID());
        return ids;
    }

//    public int getNumArguments() {
//        return 2;
//    }
//
//    public String getArgumentName(int n) {
//        if (n == 0) return "N0";
//        else return "r";
//    }
//
//    public double getArgument(int n) {
//        if (n == 0) return getN0();
//        else return r;
//    }
//
//    public void setArgument(int n, double value) {
//        if (n == 0) setN0(value);
//        else r = value;
//    }
//
//    public double getLowerBound(int n) {
//        if (n == 0) return 0.0;
//        else return Double.NEGATIVE_INFINITY;
//    }
//
//    public double getUpperBound(int n) {
//        return Double.POSITIVE_INFINITY;
//    }
//
//    public PopulationFunction getCopy() {
//        ExponentialGrowth eg = new ExponentialGrowth();
//        eg.setN0(N0);
//        eg.r = r;
//        return eg;
//    }

//    public void prepare(State state) {
//        if (popSizeParameter.get() != null) {
//            N0 = popSizeParameter.get().getValue();//state.getParameter(popSizeParameter).getValue();
//        }
//        if (growthRateParameter.get() != null) {
//            r = growthRateParameter.get().getValue();//state.getParameter(growthRateParameter).getValue();
//        }
//    }

    //
    // private stuff
    //

    /**
     * The current day population size
     */
    //private double N0 = 1.0;

    /**
     * The exponential growth rate
     */
    //private double r = 0.01;
}