package beast.math.statistic;


import beast.core.CalculationNode;
import beast.core.Loggable;
import beast.core.Input;
import beast.core.Description;
import beast.core.parameter.Parameter;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.PrintStream;


/**
 *  A statistic based on evaluating simple expressions.
 *
 * The expressions are in RPN, so no parsing issues. whitspace separated. Variables (other statistics),
 * constants and operations. Currently just the basic four, but easy to extend.
 *
 * @author Joseph Heled in beast1, migrated to beast2 by Denise KŸhnert
 */
@Description("RPN calculator to evaluate simple expressions of parameters")
public class RPNcalculator extends CalculationNode implements Loggable {


    public Input<String> str_expression = new Input<String>("expression", "Expressions needed for the calculations", Input.Validate.REQUIRED);
    public Input<List<Parameter>> parameters = new Input<List<Parameter>>("parameter", "Parameters needed for the calculations", new ArrayList<Parameter>());

    private RPNexpressionCalculator expression;
    private List<String> names;

    private Map<String, Double> variables;
    RPNexpressionCalculator.GetVariable vars;

    public void initAndValidate(){

        variables = new HashMap<String, Double>();
        names = new ArrayList<String>();

        for (Parameter p : parameters.get()){

            names.add(p.toString());
            variables.put(p.getID(), Double.parseDouble(p.getValue().toString()));

        }

        vars = new RPNexpressionCalculator.GetVariable() {
            public double get(String name) {
                return variables.get(name);
            }
        };

        expression = new RPNexpressionCalculator(str_expression.get());

        String err = this.expression.validate();
        if( err != null ) {
            throw new RuntimeException("Error in expression: " + err);
        }
    }

    private void updateValues(){
        for (Parameter p : parameters.get()){
            variables.put(p.getID(), Double.parseDouble(p.getValue().toString()));
        }
    }

    public int getDimension() {
        return 1;
    }

    public String getDimensionName(int dim) {
        return names.get(dim);
    }

    /** @return the value of the expression */
	public double getStatisticValue() {
        updateValues();
        return expression.evaluate(vars);
	}


    @Override
    public void init(PrintStream out) throws Exception {
            out.print(this.getID() + "\t");
    }

    @Override
    public void log(int nSample, PrintStream out) {
            out.print(getStatisticValue() + "\t");
    }

    @Override
    public void close(PrintStream out){
        // nothing to do
    }


}