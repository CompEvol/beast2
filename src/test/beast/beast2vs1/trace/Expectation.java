package test.beast.beast2vs1.trace;

import beast.core.BEASTObject;
import beast.core.Citation;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;


@Description("It is used by LogAnalyser. assertExpectation(TraceStatistics) sets TraceStatistics instance " +
        "passed from LogAnalyser.initAndValidate(), and determines whether expectation is significantly different " +
        "to statisctial mean considering stand error of mean. If true, then set isPassed = false, which makes JUnit " +
        "test assertion failed.")
@Citation("Created by Walter Xie")
public class Expectation extends BEASTObject {

    public Input<String> traceName = new Input<>("traceName", "The trace name of a loggable beastObject", Validate.REQUIRED);

    public Input<Double> expValue =
            new Input<>("expectedValue", "The expected value of the referred loggable beastObject", Validate.REQUIRED);

    public Input<Double> standErrorOfMean =
            new Input<>("stdError", "The expected standard error of mean. If not given, it will estimate error from log",
                    Validate.REQUIRED);

    private boolean isPassed = true; // assert result
    private boolean isValid = true; // assert ESS
    private TraceStatistics trace;

    // this constructor is used by Unit test
    public Expectation(String traceName, Double expValue, Double stdError) throws Exception {
        this.traceName.setValue(traceName, this);
        this.expValue.setValue(expValue, this);
        this.standErrorOfMean.setValue(stdError, this);
    }

    public boolean isPassed() {
        return isPassed;
    }

//    public void setFailed(boolean failed) {
//        isPassed = failed;
//    }

    public boolean isValid() {
        return isValid;
    }

    public boolean assertExpectation(TraceStatistics trace, boolean displayStatistics) {
        this.trace = trace;
        double mean = trace.getMean();
        double stderr = trace.getStdErrorOfMean();
        double ess = trace.getESS();

        double deltaMean = Math.abs(mean - expValue.get());
        double deltaStdErr = Math.abs(2 * stderr + 2 * standErrorOfMean.get());

        if (standErrorOfMean.get() == 0) {
            isPassed = mean == expValue.get(); // used to check whether fixed value is fixed.
        } else {
            isPassed = deltaMean <= deltaStdErr;
            isValid = ess > 100;
        }

        if (displayStatistics) {
            System.out.println(traceName.get() + " : " + mean + " +- " + stderr + ", ESS = " + ess + ", expectation is "
                    + expValue.get() + " +- " + standErrorOfMean.get());
        }
        return isPassed;
    }

    public TraceStatistics getTraceStatistics() {
        return trace;
    }

    public double getStdError() {
        return standErrorOfMean.get();
//        double stderr = trace.getStdErrorOfMean();
//        if (standErrorOfMean.get() != 0) {
//            stderr = standErrorOfMean.get();
////            System.out.println("User defines standard error of mean = " + stderr);
//        }
//        return stderr;
    }

    @Override
	public String toString() {
        return traceName.get() + " " + expValue.get();
    }

	@Override
	public void initAndValidate() {
		// TODO Auto-generated method stub
		
	}
}
