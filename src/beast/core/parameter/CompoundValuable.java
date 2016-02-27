package beast.core.parameter;

import java.util.ArrayList;
import java.util.List;

import beast.core.BEASTObject;
import beast.core.CalculationNode;
import beast.core.Description;
import beast.core.Function;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.StateNode;



@Description("Summarizes a set of valuables so that for example a rate matrix can be " +
        "specified that uses a parameter in various places in the matrix.")
public class CompoundValuable extends CalculationNode implements Function {
    final public Input<List<BEASTObject>> m_values = new Input<>("var", "reference to a valuable",
            new ArrayList<>(), Validate.REQUIRED, Function.class);

    boolean m_bRecompute = true;
    /**
     * contains values of the inputs *
     */
    double[] m_fValues;

    @Override
    public void initAndValidate() {
        // determine dimension
        int dimension = 0;
        for (BEASTObject beastObject : m_values.get()) {
            if (!(beastObject instanceof Function)) {
                throw new IllegalArgumentException("Input does not implement Valuable");
            }
            dimension += ((Function) beastObject).getDimension();
        }
        m_fValues = new double[dimension];
    }

    /**
     * Valuable implementation follows *
     */
    @Override
    public int getDimension() {
        return m_fValues.length;
    }

    @Override
    public double getArrayValue() {
        if (m_bRecompute) {
            recompute();
        }
        return m_fValues[0];
    }

    @Override
    public double getArrayValue(int dim) {
        if (m_bRecompute) {
            recompute();
        }
        return m_fValues[dim];
    }

    /**
     * collect values of the compounds into an array *
     */
    private void recompute() {
        int k = 0;
        for (BEASTObject beastObject : m_values.get()) {
            Function valuable = (Function) beastObject;
            if (beastObject instanceof StateNode) {
                valuable = ((StateNode) beastObject).getCurrent();
            }
            int dimension = valuable.getDimension();
            for (int i = 0; i < dimension; i++) {
                m_fValues[k++] = valuable.getArrayValue(i);
            }
        }
        m_bRecompute = false;
    }

    /**
     * CalculationNode methods *
     */
    @Override
    public void store() {
        m_bRecompute = true;
        super.store();
    }

    @Override
    public void restore() {
        m_bRecompute = true;
        super.restore();
    }

    @Override
    public boolean requiresRecalculation() {
        m_bRecompute = true;
        return true;
    }

}
