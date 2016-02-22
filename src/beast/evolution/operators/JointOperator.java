/*
 * Copyright (C) 2013 Tim Vaughan <tgvaughan@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package beast.evolution.operators;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Operator;
import beast.core.StateNode;
import beast.core.util.Log;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
@Description("Operator which allows multiple operators to be chained together."
        + " This is only correct when each operator acts on a different part"
        + " of the state.")
public class JointOperator extends Operator {
    
    final public Input<List<Operator>> operatorsInput = new Input<>(
            "operator",
            "List of operators to combine into one operation.",
            new ArrayList<>());

    @Override
    public void initAndValidate() { }
    
    @Override
    public double proposal() {
        double logHR = 0;
       
        for (Operator op : operatorsInput.get()) {

            logHR += op.proposal();
            
            // Stop here if the move is going to be rejected anyway
            if (logHR == Double.NEGATIVE_INFINITY)
                break;
            
            // Update calculation nodes as subsequent operators may depend on
            // state nodes made dirty by this operation.
            try {
                if (!op.listStateNodes().isEmpty())
                    op.listStateNodes().get(0).getState().checkCalculationNodesDirtiness();
            } catch (Exception ex) {
                Log.err(ex.getMessage());
            }
        }
        
        return logHR;
    }
    
    @Override
    public List<StateNode> listStateNodes() {
        List<StateNode> stateNodeList = new ArrayList<>();
        
        for (Operator op : operatorsInput.get())
            stateNodeList.addAll(op.listStateNodes());
        
        return stateNodeList;
    }
}
