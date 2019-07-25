/*
* File BitFlipOperator.java
*
* Copyright (C) 2010 Joseph Heled jheled@gmail.com
* This file is part of BEAST2.
* See the NOTICE file distributed with this work for additional
* information regarding copyright ownership and licensing.
*
* BEAST is free software; you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
*  BEAST is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with BEAST; if not, write to the
* Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
* Boston, MA  02110-1301  USA
*/

package beast.evolution.operators;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Logger;
import beast.core.Operator;
import beast.core.parameter.BooleanParameter;
import beast.core.util.Log;
import beast.util.Randomizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Alexei Drummond
 */

@Description("Move k 'on' bits in an array of boolean bits. The number of 'on' bits remains constant under this operation.")
public class BitMoveOperator extends Operator {
    final public Input<Integer> kInput = new Input<>("k", "the number of 'on' bits to shift", 1);

    final public Input<BooleanParameter> parameterInput = new Input<>("parameter", "the parameter to operate a bit move on.", Validate.REQUIRED);


    List<Integer> onPositions = new ArrayList<>();
    List<Integer> offPositions = new ArrayList<>();

    @Override
	public void initAndValidate() {}

    /**
     * A bit move picks a random 'on' bit and moves it to a new position that was previously 'off'. The original position becomes 'off'.
     * This is effectively two bit flips, one from 'on' to 'off' and one from 'off' to 'on'.
     */

    @Override
    public double proposal() {

        final BooleanParameter p = parameterInput.get(this);

        final int dim = p.getDimension();

        onPositions.clear();
        offPositions.clear();
        for (int i = 0; i < dim; i++) {
            if (p.getValue(i)) onPositions.add(i);
                else offPositions.add(i);
        }

        if (onPositions.size() == 0 || offPositions.size() == 0) {
            Log.warning("BitMoveOperator has no valid moves. Rejecting.");
            return Double.NEGATIVE_INFINITY;
        }

        for (int i = 0; i < kInput.get(); i++) {

            int onPos = Randomizer.nextInt(onPositions.size());
            Integer on = onPositions.get(onPos);

            int offPos = Randomizer.nextInt(offPositions.size());
            Integer off = offPositions.get(offPos);

            p.setValue(on,false);
            p.setValue(off,true);

            onPositions.remove(onPos);
            onPositions.add(off);
            offPositions.remove(offPos);
            offPositions.add(on);
        }

        return 0.0;
    }
}

