/*
 * Copyright (C) 2014 Tim Vaughan <tgvaughan@gmail.com>.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package beast.math.statistic;

import beast.core.CalculationNode;
import beast.core.Description;
import beast.core.Function;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Loggable;
import beast.core.parameter.Parameter;
import beast.core.parameter.RealParameter;
import beast.math.statistic.expparser.ExpressionLexer;
import beast.math.statistic.expparser.ExpressionParser;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Simple expression calculator.  Takes simple arithmetic expressions
 * and returns the result by acting as a Loggable or and a Function.
 * 
 * Inspired by RPNcalculator by Joseph Heled (BEAST1, BEAST 2 port by 
 * Denise Kuehnert).  (This parser uses ANTLR, which is cheating.)
 * 
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
@Description("Evaluates simple expressions of parameters involving parameters,"
        + " including parameters of different lengths.  Individual elements of"
        + " parameters can be specified using [] notation.  The result has "
        + "the dimension of the longest parameter vector.")
public class ExpCalculator extends CalculationNode implements Loggable, Function {
    
    public Input<String> expressionInput = new Input<String>("expression",
            "Expression needed for calculations.", Validate.REQUIRED);
    
    public Input<List<Parameter>> parametersInput = new Input<List<Parameter>>(
            "parameter", "Parameters needed for the calculation",
            new ArrayList<Parameter>());

    
    ParseTree parseTree;
    ExpCalculatorVisitor visitor;
    int maxDim;
    
    Double [] res;

    public ExpCalculator() {
    }
    
    @Override
    public void initAndValidate() throws Exception {

        // Find maximum parameter dimension:
        for (Parameter param : parametersInput.get())
            maxDim = param.getDimension()>maxDim ? param.getDimension() : maxDim;
        
        // Build AST from expression string
        ANTLRInputStream input = new ANTLRInputStream(expressionInput.get());
        ExpressionLexer lexer = new ExpressionLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ExpressionParser parser = new ExpressionParser(tokens);
        parseTree = parser.expression();
        
        // Create new visitor for calculating expression values:
        visitor = new ExpCalculatorVisitor(maxDim, parametersInput.get());
    }

    private void update() {
        res = visitor.visit(parseTree);
    }
    
    @Override
    public void init(PrintStream out) throws Exception {
        if (maxDim == 1)
            out.print(this.getID() + "\t");
        else
            for (int i = 0; i < maxDim; i++)
                out.print(this.getID() + "_" + i + "\t");
    }

    @Override
    public void log(int nSample, PrintStream out) {
        update();
        for (int i = 0; i < maxDim; i++)
            out.print(res[i] + "\t");
    }

    @Override
    public void close(PrintStream out) { }

    @Override
    public int getDimension() {
        return maxDim;
    }

    @Override
    public double getArrayValue() {
        update();
        return res[0];
    }

    @Override
    public double getArrayValue(int i) {
        update();
        return res[i];
    }
    
    /**
     * Main method for debugging.
     * 
     * @param args
     * @throws Exception 
     */    
    public static void main(String [] args) throws Exception {
        

    }
}
