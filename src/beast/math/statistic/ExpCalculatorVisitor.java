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

import beast.core.parameter.Parameter;
import beast.math.statistic.expparser.ExpressionBaseVisitor;
import beast.math.statistic.expparser.ExpressionParser;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class ExpCalculatorVisitor extends ExpressionBaseVisitor<Double []>{

    int maxDim;
    Map<String, Parameter> parameterMap;
    
    /**
     * Create a new Expression AST visitor.
     * 
     * @param maxDim
     * @param parameters List of parameters.
     */
    public ExpCalculatorVisitor(int maxDim, List<Parameter> parameters) {
        this.maxDim = maxDim;
        
        // Assemble name->param map
        parameterMap = new HashMap<String, Parameter>();
        for (Parameter param : parameters)
            parameterMap.put(param.getID(), param);
    }
    
    /**
     * Return the chosen index of a parameter, modulo the total dimension
     * of the parameter.  This allows parameters of different dimensions
     * to be sensibly combined.  (R does a similar thing.)
     * 
     * @param param
     * @param index
     * @return 
     */
    private double getParameterValue(String paramName, int index) {
        
        if (!parameterMap.containsKey(paramName))
            throw new IllegalArgumentException("Paramter " + paramName
                    + " in expression was not found in list of provided"
                    + " parameters.");

        Parameter param = parameterMap.get(paramName);
        return param.getArrayValue(index % param.getDimension());
    }
    
    @Override
    public Double[] visitNumber(ExpressionParser.NumberContext ctx) {

        double num = Double.valueOf(ctx.val.getText());
        
        Double [] res = new Double[maxDim];
        for (int i=0; i<maxDim; i++)
            res[i] = num;
        
        return res;
    }

    @Override
    public Double[] visitVariable(ExpressionParser.VariableContext ctx) {

        String paramName = ctx.VARNAME().getText();
        int paramIdx = -1;
        if (ctx.i != null)
            paramIdx = Integer.valueOf(ctx.i.getText());

        
        Double [] res = new Double[maxDim];
        if (paramIdx<0) {
            for (int i=0; i<maxDim; i++)
                res[i] = getParameterValue(paramName, i);
        } else {
            for (int i=0; i<maxDim; i++)
                res[i] = getParameterValue(paramName, paramIdx);            
        }
        
        return res;
    }

    @Override
    public Double[] visitMulDiv(ExpressionParser.MulDivContext ctx) {
        Double [] left = visit(ctx.factor());
        Double [] right = visit(ctx.atom());
        
        Double [] res = new Double[maxDim];
        for (int i=0; i<maxDim; i++) {
            if (ctx.op.getType() == ExpressionParser.MUL)
                res[i] = left[i]*right[i];
            else
                res[i] = left[i]/right[i];
        }
        
        return res;
    }

    @Override
    public Double[] visitAddSub(ExpressionParser.AddSubContext ctx) {
        Double [] left = visit(ctx.expression());
        Double [] right = visit(ctx.factor());
        
        Double [] res = new Double[maxDim];
        for (int i=0; i<maxDim; i++) {
            if (ctx.op.getType() == ExpressionParser.ADD)
                res[i] = left[i]+right[i];
            else
                res[i] = left[i]-right[i];
        }
        
        return res;
    }

    @Override
    public Double[] visitBracketed(ExpressionParser.BracketedContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Double[] visitUnaryOp(ExpressionParser.UnaryOpContext ctx) {
        
        Double [] res = new Double[maxDim];
        Double [] arg = visit(ctx.expression());
        switch(ctx.op.getType()) {
            case ExpressionParser.EXP:
                for (int i=0; i<maxDim; i++)
                    res[i] = Math.exp(arg[i]);
                break;
                
            case ExpressionParser.LOG:
                for (int i=0; i<maxDim; i++)
                    res[i] = Math.log(arg[i]);
                break;
        }
        
        return res;
    }

    @Override
    public Double[] visitNegation(ExpressionParser.NegationContext ctx) {
 
        Double [] res = visit(ctx.atom());
        for (int i=0; i<maxDim; i++)
            res[i] = -res[i];
        
        return res;
    }
    
}
