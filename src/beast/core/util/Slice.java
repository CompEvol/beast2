package beast.core.util;

import beast.core.*;

import java.io.PrintStream;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
@Description("A Function representing a number of elements of another Function.")
public class Slice extends CalculationNode implements Function, Loggable {

    public Input<Function> functionInput = new Input<>("arg",
            "Argument to extract element from.", Input.Validate.REQUIRED);

    public Input<Integer> startIndexInput = new Input<>("index",
            "Index of first element to extract.", Input.Validate.REQUIRED);

    public Input<Integer> countInput = new Input<>("count",
            "Number of elements to extract.", 1);

    protected int indexStart, indexEnd, count;

    @Override
    public void initAndValidate() throws Exception {
        indexStart = startIndexInput.get();
        count = countInput.get();
        indexEnd = indexStart + count - 1;

        if (indexEnd >= functionInput.get().getDimension())
            throw new IllegalArgumentException("Index and count arguments to" +
                    " Slice are out of bounds.");
    }

    @Override
    public int getDimension() {
        return count;
    }

    @Override
    public double getArrayValue() {
        return functionInput.get().getArrayValue(0);
    }

    @Override
    public double getArrayValue(int iDim) {
        if (iDim < count)
            return functionInput.get().getArrayValue(indexStart + iDim);
        else
            return 0;
    }

    @Override
    public void init(PrintStream out) throws Exception {
        for (int i=0; i<count; i++)
            out.print(((BEASTObject)functionInput.get()).getID()
                    + "[" + (indexStart + i) + "]\t");
    }

    @Override
    public void log(int nSample, PrintStream out) {
        for (int i=0; i<count; i++)
            out.print(getArrayValue(i) + "\t");
    }

    @Override
    public void close(PrintStream out) {

    }
}
