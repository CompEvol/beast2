package beast.evolution.operators;

import java.text.DecimalFormat;

import beast.core.Distribution;
import beast.core.Evaluator;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Operator;
import beast.core.parameter.RealParameter;
import beast.util.Randomizer;


@Description("A random walk operator that selects a random dimension of the real parameter and perturbs the value a " +
        "random amount within +/- windowSize.")
public class SliceOperator extends Operator {
    public Input<RealParameter> parameterInput =
            new Input<RealParameter>("parameter", "the parameter to operate a random walk on.", Validate.REQUIRED);
    public Input<Double> windowSizeInput =
            new Input<Double>("windowSize", "the size of the step for finding the slice boundaries", Input.Validate.REQUIRED);
    public Input<Distribution> sliceDensityInput =
        new Input<Distribution>("sliceDensity", "The density to sample from using slice sampling.", Input.Validate.REQUIRED);

    Double totalDelta;
    int totalNumber;
    int n_learning_iterations;
    double W;

    double windowSize = 1;
    Distribution g;
    public void initAndValidate() 
    {
	totalDelta = 0.0;
	totalNumber = 0;
	n_learning_iterations = 0;
	W=0.0;
	windowSize = windowSizeInput.get();
	g = sliceDensityInput.get();
    }

    boolean in_range(RealParameter X, double x) 
    {
	return (X.getLower() < x && x < X.getUpper());
    }
    
    boolean below_lower_bound(RealParameter X, double x)
    {
	return (x < X.getLower());
    }
    
    boolean above_upper_bound(RealParameter X, double x)
    {
	return (x > X.getUpper());
    }

    Double evaluate(RealParameter X, double x)
    {
	X.setValue(0,x);
	try {
	    return g.calculateLogP();
	}
	catch (Exception e)
	{
	    return  Double.NEGATIVE_INFINITY;
	}
    }

    Double evaluate()
    {
	try {
	    return g.calculateLogP();
	}
	catch (Exception e)
	{
	    return  Double.NEGATIVE_INFINITY;
	}

    }

    void find_slice_boundaries_stepping_out(RealParameter X, double logy, double w,int m, Double L, Double R)
    {
	double x0 = X.getValue(0);
	
	assert in_range(X,x0);
	
	double u = Randomizer.nextDouble()*w;
	L = x0 - u;
	R = x0 + (w-u);
	
	// Expand the interval until its ends are outside the slice, or until
	// the limit on steps is reached.
	
	if (m>1) {
	    int J = (int)Math.floor(Randomizer.nextDouble()*m);
	    int K = (m-1)-J;
	    
	    while (J>0 && (!below_lower_bound(X,L)) && evaluate(X,L)>logy) {
		L -= w;
		J--;
	    }
	    
	    while (K>0 && (!above_upper_bound(X,R)) && evaluate(X,R)>logy) {
		R += w;
		K--;
	    }
	}
	else {
	    while ((!below_lower_bound(X,L)) && evaluate(X,L)>logy)
		L -= w;
	    
	    while ((!above_upper_bound(X,R)) && evaluate(X,R)>logy)
		R += w;
	}
	
	// Shrink interval to lower and upper bounds.
	
	if (below_lower_bound(X,L)) L = X.getLower();
	if (above_upper_bound(X,R)) R = X.getUpper();
	
	assert L < R;
    }
    
    // Does this x0 really need to be the original point?
    // I think it just serves to let you know which way the interval gets shrunk...
    
    double search_interval(RealParameter X, Double L, Double R, double logy)
    {
	double x0 = X.getValue(0);

	//  assert(g(x0) > g(L) && g(x0) > g(R));
	assert evaluate(X,x0) >= logy;
	assert L < R;
	assert L <= x0 && x0 <= R;
	
	double L0 = L, R0 = R;
	
	for(int i=0;i<200;i++)
	{
	    double x1 = L + Randomizer.nextDouble()*(R-L);
	    
	    double gx1 = evaluate(X,x1);
	    
	    if (gx1 >= logy) return x1;
	    
	    if (x1 > x0) 
		R = x1;
	    else
		L = x1;
	}
	System.err.println("Warning!  Is size of the interval really ZERO?");
	double logy_x0 = evaluate(X,x0);  
	System.err.println("    L0 = " + L0 + "   x0 = " + x0 + "   R0 = " + R0);
	System.err.println("    L  = " + L  + "                    R  = " + R);
	System.err.println("    logy  = " + logy + "  logy_x0 = " + logy_x0 + "  logy_current = " + evaluate());
	
	return x0;
    }


    /**
     * override this for proposals,
     * returns log of hastingRatio, or Double.NEGATIVE_INFINITY if proposal should not be accepted *
     */
    @Override
    public double proposal() {

	int m = 100;

    	RealParameter X = parameterInput.get();

	// Find the density at the current point
	Double gx0 = evaluate();

	// Get the 1st element 
	Double x0 = X.getValue(0);

	// Determine the slice level, in log terms.
	double logy = gx0 - Randomizer.nextExponential(1);

	// Find the initial interval to sample from.
	Double L=X.getLower();
	Double R=X.getUpper();
	find_slice_boundaries_stepping_out(X, logy, windowSize, m, L, R);

	// Sample from the interval, shrinking it on each rejection
	double x_new = search_interval(X, L, R, logy);
	X.setValue(x_new);

	if (n_learning_iterations > 0)
	{
	    n_learning_iterations--;

	    totalDelta += Math.abs(x_new - x0);
	    totalNumber++;

	    double W_predicted = totalDelta/totalNumber*4.0;
	    if (totalNumber > 3)
	    {
		W = 0.95*W + 0.05*W_predicted;
	    }
	}

	return Double.POSITIVE_INFINITY;
    }

    
    
    @Override
    public double getCoercableParameterValue() {
        return windowSize;
    }
    
    @Override
    public void setCoercableParameterValue(double fValue) {
    	windowSize = fValue;
    }
    
    /**
     * called after every invocation of this operator to see whether
     * a parameter can be optimised for better acceptance hence faster
     * mixing
     *
     * @param logAlpha difference in posterior between previous state & proposed state + hasting ratio
     */
    @Override
    public void optimize(double logAlpha) {
        // must be overridden by operator implementation to have an effect
        double fDelta = calcDelta(logAlpha);
        
        fDelta += Math.log(windowSize);
        windowSize = Math.exp(fDelta);
    }

    @Override
    public final String getPerformanceSuggestion() 
    {
        // new scale factor
        double newWindowSize = totalDelta/totalNumber * 4;

	if (newWindowSize/windowSize < 0.8 || newWindowSize/windowSize > 1.2)
	{
	    DecimalFormat formatter = new DecimalFormat("#.###");
	    return "Try setting window size to about " + formatter.format(newWindowSize);
	}
	else
	    return "";
    }
} // class IntRandomWalkOperator