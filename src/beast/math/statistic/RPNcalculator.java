package beast.math.statistic;



import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import beast.core.CalculationNode;
import beast.core.Description;
import beast.core.Function;
import beast.core.Input;
import beast.core.Loggable;
import beast.core.parameter.Parameter;


/**
 * A statistic based on evaluating simple expressions.
 * <p/>
 * The expressions are in RPN, so no parsing issues. whitespace separated. Variables (other statistics),
 * constants and operations. Currently just the basic four, but easy to extend.
 *
 * @author Joseph Heled in beast1, migrated to beast2 by Denise Kuehnert
 */
@Description("RPN calculator to evaluate simple expressions of parameters (Reverse Polish notation is a mathematical notation wherein every operator follows its operands)")
public class RPNcalculator extends CalculationNode implements Loggable, Function {


    final public Input<String> strExpressionInput = new Input<>("expression", "Expressions needed for the calculations", Input.Validate.REQUIRED);
    final public Input<List<Parameter<?>>> parametersInput = new Input<>("parameter", "Parameters needed for the calculations", new ArrayList<>());

    private RPNexpressionCalculator[] expressions;
    private List<String> names;

    private Map<String, Object[]> variables;
    RPNexpressionCalculator.GetVariable[] vars;
    int dim;

    @Override
	public void initAndValidate() throws Exception {

        names = new ArrayList<>();
        dim = parametersInput.get().get(0).getDimension();

        int pdim;

        for (final Parameter<?> p : parametersInput.get()) {

            pdim = p.getDimension();

            if (pdim != dim && dim != 1 && pdim != 1) {
                throw new IllegalArgumentException("error: all parameters have to have same length or be of dimension 1.");
            }
            if (pdim > dim) dim = pdim;

            expressions = new RPNexpressionCalculator[dim];
            names.add(p.toString());

            for (int i = 0; i < pdim; i++) {

                variables = new HashMap<>();

                variables.put(p.getID(), p.getValues());
            }
        }

        vars = new RPNexpressionCalculator.GetVariable[dim];

        for (int i = 0; i < dim; i++) {
            final int index = i;
            vars[i] = new RPNexpressionCalculator.GetVariable() {
                @Override
				public double get(final String name) {
                    final Object[] values = (variables.get(name));
                    if (values == null) {
                    	String ids = "";
                        for (final Parameter<?> p : parametersInput.get()) {
                    		ids += p.getID() +", ";
                    	}
                    	if (parametersInput.get().size() > 0) {
                    		ids = ids.substring(0, ids.length() - 2);
                    	}
                    	throw new RuntimeException("Something went wront with the RPNCalculator with id=" + getID() +".\n"
                    			+ "There might be a typo on the expression.\n" +
                    			"It should only contain these: " + ids +"\n"
                    					+ "but contains " + name);
                    }
                    if (values[0] instanceof Boolean)
                        return ((Boolean) values[values.length > 1 ? index : 0] ? 1. : 0.);
                    if (values[0] instanceof Integer)
                        return (Integer) values[values.length > 1 ? index : 0];
                    return (Double) values[values.length > 1 ? index : 0];
                }
            };
        }

        String err;
        for (int i = 0; i < dim; i++) {
            expressions[i] = new RPNexpressionCalculator(strExpressionInput.get());

            err = expressions[i].validate();
            if (err != null) {
                throw new RuntimeException("Error in expression: " + err);
            }
        }
    }

    private void updateValues() {
        for (Parameter<?> p : parametersInput.get()) {
            for (int i = 0; i < p.getDimension(); i++) {
                variables.put(p.getID(), p.getValues());
            }
        }
    }

    @Override
	public int getDimension() {
        return dim;
    }


    // todo: add dirty flag to avoid double calculation!!! 
    @Override
    public double getArrayValue() {
        return getStatisticValue(0);
    }

    @Override
    public double getArrayValue(final int i) {
        return getStatisticValue(i);
    }

//    public String getDimensionName(final int dim) {
//        return names.get(dim);
//    }

    /**
     * @return the value of the expression
     */
    public double getStatisticValue(final int i) {
        updateValues();
        return expressions[i].evaluate(vars[i]);
    }


    @Override
    public void init(final PrintStream out) {
        if (dim == 1)
            out.print(this.getID() + "\t");
        else
            for (int i = 0; i < dim; i++)
                out.print(this.getID() + "_" + i + "\t");
    }

    @Override
    public void log(final int sample, final PrintStream out) {
        for (int i = 0; i < dim; i++)
            out.print(getStatisticValue(i) + "\t");
    }

    @Override
    public void close(final PrintStream out) {
        // nothing to do
    }

    public List<String> getArguments() {
        final List<String> arguments = new ArrayList<>();
        for (final Parameter<?> par : parametersInput.get()) {
            arguments.add(par.getID());
        }
        return arguments;
    }


}