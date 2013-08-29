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

import beast.core.Description;
import beast.core.Input;
import beast.core.Operator;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
@Description("Operator which allows multiple operators to be chained together."
        + " This is only correct when each operator acts on a different part"
        + " of the state.")
public class JointOperator extends Operator {
    
    public Input<List<Operator>> operatorsInput = new Input<List<Operator>>(
            "operator",
            "List of operators to combine into one operation.",
            new ArrayList<Operator>());

    @Override
    public void initAndValidate() { }
    
    @Override
    public double proposal() {
        double logHR = 0;
        
        for (Operator op : operatorsInput.get())
            logHR += op.proposal();
        
        return logHR;
    }
    
}
