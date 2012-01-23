package beast.math.statistic;

import beast.core.Description;

import java.util.Stack;

/**
 * Simple RPN expression evaluator.
 * <p/>
 * Limitations:
 * - variables are statistics of 1 dimension.
 * - Four basic operations (easy to extend, though)
 *
 * @author Joseph Heled in beast1, migrated to beast2 by Denise Kuehnert
 *         Date: 10/05/2008
 */
@Description("RPN calculator to evaluate simple expressions of parameters - evaluating the expressions from RPNCalculator (Reverse Polish notation is a mathematical notation wherein every operator follows its operands)")
public class RPNexpressionCalculator {
    /**
     * Interface for variable access by name
     */
    public interface GetVariable {
        /**
         * @param name
         * @return variable value
         */
        double get(String name);
    }

    private enum OP {OP_ADD, OP_SUB, OP_MULT, OP_DIV, OP_LOG, OP_EXP, OP_CHS, OP_CONST, OP_REF}

    private class Eelement {
        OP op;
        String name;
        private double value;

        Eelement(OP op) {
            this.op = op;
            name = null;
        }

        Eelement(String name) {
            this.op = OP.OP_REF;
            this.name = name;
        }

        Eelement(double val) {
            this.op = OP.OP_CONST;
            this.value = val;
        }
    }

    Eelement[] expression;

    public RPNexpressionCalculator(String expressionString) {
        String[] tokens = expressionString.trim().split("\\s+");

        expression = new Eelement[tokens.length];

        for (int k = 0; k < tokens.length; ++k) {
            String tok = tokens[k];
            Eelement element;
            if (tok.equals("+")) {
                element = new Eelement(OP.OP_ADD);
            } else if (tok.equals("-")) {
                element = new Eelement(OP.OP_SUB);
            } else if (tok.equals("*")) {
                element = new Eelement(OP.OP_MULT);
            } else if (tok.equals("/")) {
                element = new Eelement(OP.OP_DIV);
            } else if (tok.equals("log")) {
                element = new Eelement(OP.OP_LOG);
            } else if (tok.equals("exp")) {
                element = new Eelement(OP.OP_EXP);
            } else if (tok.equals("chs")) {
                element = new Eelement(OP.OP_CHS);
            } else {
                try {
                    double val = Double.parseDouble(tok);
                    element = new Eelement(val);
                } catch (java.lang.NumberFormatException ex) {
                    element = new Eelement(tok);
                }
            }
            expression[k] = element;
        }
    }

    /**
     * @param variables
     * @return evaluate expression given context (i.e. variables)
     */
    public double evaluate(GetVariable variables) {
        Stack<Double> stack = new Stack<Double>();

        for (Eelement elem : expression) {
            switch (elem.op) {
                case OP_ADD: {
                    final Double y = stack.pop();
                    final Double x = stack.pop();
                    stack.push(x + y);
                    break;
                }
                case OP_SUB: {
                    final Double y = stack.pop();
                    final Double x = stack.pop();
                    stack.push(x - y);
                    break;
                }
                case OP_MULT: {
                    final Double y = stack.pop();
                    final Double x = stack.pop();
                    stack.push(x * y);
                    break;
                }
                case OP_DIV: {
                    final Double y = stack.pop();
                    final Double x = stack.pop();
                    stack.push(x / y);
                    break;
                }
                case OP_CHS: {
                    final Double x = stack.pop();
                    stack.push(-x);
                    break;
                }
                case OP_LOG: {
                    final Double x = stack.pop();
                    if (x <= 0.0) {
                        return Double.NaN;
                    }
                    stack.push(Math.log(x));
                    break;
                }
                case OP_EXP: {
                    final Double x = stack.pop();
                    stack.push(Math.exp(x));
                    break;
                }
                case OP_CONST: {
                    stack.push(elem.value);
                    break;
                }
                case OP_REF: {
                    stack.push(variables.get(elem.name));
                    break;
                }
            }
        }

        return stack.pop();
    }

    /**
     * @return null if all ok, error message otherwise
     */
    public String validate() {
        int stackSize = 0;

        for (Eelement elem : expression) {
            switch (elem.op) {
                case OP_ADD:
                case OP_SUB:
                case OP_MULT:
                case OP_DIV: {
                    if (stackSize < 2) {
                        return "Binary operator underflow";
                    }
                    stackSize -= 1;
                    break;
                }
                case OP_CHS:
                case OP_LOG:
                case OP_EXP: {
                    if (stackSize == 0) {
                        return "Unary operator underflow";
                    }
                    break;
                }
                case OP_CONST: {
                    stackSize += 1;
                    break;
                }
                case OP_REF: {
                    stackSize += 1;
                    break;
                }
            }
        }

        if (stackSize != 1) {
            return "Stack size " + stackSize + " ( != 1 ) at end of expression evaluation";
        }

        return null;
    }
}
