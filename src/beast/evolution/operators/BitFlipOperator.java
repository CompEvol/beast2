package beast.evolution.operators;

import beast.core.Operator;
import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.BooleanParameter;
import beast.util.Randomizer;

/**
 * @author Joseph Heled
 *
 */

@Description("bit flip operator bla bla")
public class BitFlipOperator extends Operator {
    public Input<Boolean> uniform = new Input<Boolean>("uniform", "when on, total probability of combinations with k" +
            " 'on' bits is equal. Otherwise uniform on all combinations");

    public Input<BooleanParameter> parameter = new Input<BooleanParameter>("parameter", "the parameter to operate a flip on.");

    private boolean usesPriorOnSum = true;

    public void initAndValidate() {
        Boolean b =  uniform.get();
        if( b != null ) {
            usesPriorOnSum = b;
        }
    }

    /**
     * Change the parameter and return the hastings ratio.
     * Flip (Switch a 0 to 1 or 1 to 0) for a random bit in a bit vector.
     * Return the hastings ratio which makes all subsets of vectors with the same number of 1 bits
     * equiprobable, unless usesPriorOnSum = false then all configurations are equiprobable
     */

    @Override
    public double proposal() throws Exception {

        final BooleanParameter p = parameter.get(this);

        final int dim = p.getDimension();

        double sum = 0.0;
        if( usesPriorOnSum ) {
            for(int i = 0; i < dim; i++) {
                if( p.getValue(i) ) sum += 1;
            }
        }

        final int pos = Randomizer.nextInt(dim);

        final boolean value = p.getValue(pos);

        double logq = 0.0;
        if ( ! value ) {
            p.setValue(pos, true);

            if( usesPriorOnSum ) {
                logq = -Math.log((dim - sum) / (sum + 1));
            }

        } else {
            assert value;

            p.setValue(pos, false);
            if( usesPriorOnSum ) {
                logq = -Math.log(sum / (dim - sum + 1));
            }
        }
        return logq;
    }
}

