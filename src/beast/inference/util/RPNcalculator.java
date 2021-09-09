package beast.inference.util;




import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import beast.base.BEASTInterface;
import beast.base.Description;
import beast.base.Function;
import beast.base.Input;
import beast.base.Loggable;
import beast.inference.CalculationNode;


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
    final public Input<List<Function>> parametersInput = new Input<>("parameter", "Parameters needed for the calculations", new ArrayList<>());
    public Input<String> argNamesInput = new Input<>("argnames", "names of arguments used in expression (comma delimited)," +
            " order as given by XML");

    private RPNexpressionCalculator[] expressions;
    private List<String> names;

    private Map<String, Object[]> variables;
    RPNexpressionCalculator.GetVariable[] vars;
    int dim;

    @Override
	public void initAndValidate() {

        names = new ArrayList<>();
        dim = parametersInput.get().get(0).getDimension();

        int pdim;

        if (argNamesInput.get() != null) {
        	String [] names_ = argNamesInput.get().split(",");
        	if (names_.length != parametersInput.get().size()) {
        		throw new IllegalArgumentException("number of argnames does not match number of parameters");
        	}
        	
            variables = new HashMap<>();
        	int k = 0;
        	for (String name : names_) {
        		names.add(name);
        		Function p = parametersInput.get().get(k);
                expressions = new RPNexpressionCalculator[dim];
	            pdim = p.getDimension();
	        	
	            if (pdim != dim && dim != 1 && pdim != 1) {
	                throw new IllegalArgumentException("error: all parameters have to have same length or be of dimension 1.");
	            }
	            if (pdim > dim) dim = pdim;
	
	            expressions = new RPNexpressionCalculator[dim];
	            names.add(p.toString());

	            variables.put(name, toObjectArray(p.getDoubleValues()));
	
	            k++;
        	}
        } else {
	        for (final Function p : parametersInput.get()) {
	
	            pdim = p.getDimension();
	
	            if (pdim != dim && dim != 1 && pdim != 1) {
	                throw new IllegalArgumentException("error: all parameters have to have same length or be of dimension 1.");
	            }
	            if (pdim > dim) dim = pdim;
	
	            expressions = new RPNexpressionCalculator[dim];
	            names.add(p.toString());
	
	            for (int i = 0; i < pdim; i++) {
	                variables = new HashMap<>();
	                variables.put(((BEASTInterface) p).getID(), toObjectArray(p.getDoubleValues()));
	            }
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
                        for (final Function p : parametersInput.get()) {
                    		ids += ((BEASTInterface) p).getID() +", ";
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

    private Object[] toObjectArray(double[] doubleValues) {
		Double [] o = new Double[doubleValues.length];
		for (int i = 0; i < doubleValues.length; i++) {
			o[i] = doubleValues[i];
		}
		return o;
	}

	private void updateValues() {
        for (Function p : parametersInput.get()) {
            for (int i = 0; i < p.getDimension(); i++) {
                variables.put(((BEASTInterface) p).getID(), toObjectArray(p.getDoubleValues()));
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
                out.print(this.getID() + "_" + (i + 1) + "\t");
    }

    @Override
    public void log(final long sample, final PrintStream out) {
        for (int i = 0; i < dim; i++)
            out.print(getStatisticValue(i) + "\t");
    }

    @Override
    public void close(final PrintStream out) {
        // nothing to do
    }

    public List<String> getArguments() {
        final List<String> arguments = new ArrayList<>();
        for (final Function p : parametersInput.get()) {
            arguments.add(((BEASTInterface) p).getID());
        }
        return arguments;
    }


}