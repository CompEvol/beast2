/*
* File ScaleOperator.java
*
* Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
*
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

import beast.core.*;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;

@Description("Scales a parameter or a complete beast.tree (depending on which of the two is specified.")
public class ScaleOperator extends Operator {

    public Input<Tree> m_pTree = new Input<Tree>("tree", "if specified, all beast.tree branch length are scaled");
    public Input<Parameter> m_pParameter = new Input<Parameter>("parameter", "if specified, this parameter is scaled", Input.Validate.XOR, m_pTree);

    public Input<Double> m_pScaleFactor = new Input<Double>("scaleFactor", "scaling factor: larger means more bold proposals", new Double(1.0));
    // shadows input
    double m_fScaleFactor;
    public Input<Boolean> m_pScaleAll = new Input<Boolean>("scaleAll", "if true, all elements of a parameter (not beast.tree) are scaled, otherwise one is randomly selected", new Boolean(false));
    public Input<Boolean> m_pScaleAllIndependently = new Input<Boolean>("scaleAllIndependently", "if true, all elements of a parameter (not beast.tree) are scaled with a different factor, otherwise a single factor is used", new Boolean(false));
    public Input<Integer> m_pDegreesOfFreedom = new Input<Integer>("degreesOfFreedom", "Degrees of freedom used in ...", new Integer(1));

    @Override
    public void initAndValidate(State state) {
        // todo : implement this properly
        m_fScaleFactor = m_pScaleFactor.get();
    }


    double m_fLower = Double.NEGATIVE_INFINITY;
    double m_fUpper = Double.POSITIVE_INFINITY;

    double getLower() {
        return m_fLower;
    }

    double getUpper() {
        return m_fUpper;
    }

    public ScaleOperator() {
    }

    int m_iVar = -1;
    int m_nTreeID = -1;

    public double proposal(State state) throws Exception {
        //double fScaleFactor = m_pScaleFactor.get();

        double hastingsRatio = 1.0;
        double d = Randomizer.nextDouble();
        double scale = (m_fScaleFactor + (d * ((1.0 / m_fScaleFactor) - m_fScaleFactor)));

        if (m_pTree.get() != null) {
            if (m_nTreeID < 0) {
                m_nTreeID = state.getStateNodeIndex(m_pTree.get().getID());
            }
            // scale the beast.tree
            ((Tree) state.getStateNode(m_nTreeID)).getRoot().scale(scale);
            return Math.log(hastingsRatio);
        }
        boolean bScaleAll = m_pScaleAll.get();
        int nDegreesOfFreedom = m_pDegreesOfFreedom.get();
        boolean bScaleAllIndependently = m_pScaleAllIndependently.get();
        if (m_iVar < 0) {
            m_iVar = state.getStateNodeIndex(m_pParameter.get().getID());
        }

        Parameter param = (Parameter) state.getStateNode(m_iVar);
        int dim = param.getDimension();

        if (bScaleAllIndependently) {
            // update all dimensions independently.
            hastingsRatio = 0;
            for (int i = 0; i < dim; i++) {

                double scaleOne = (m_fScaleFactor + (Randomizer.nextDouble() * ((1.0 / m_fScaleFactor) - m_fScaleFactor)));
                double value = scaleOne * param.getValue(i);

                hastingsRatio -= Math.log(scaleOne);

                if (value < getLower() || value > getUpper()) {
                    throw new Exception("Error scaleOperator 101: proposed value outside boundaries");
                }

                param.setValue(i, value);
            }
        } else if (bScaleAll) {
            // update all dimensions
            // hasting ratio is dim-2 times of 1dim case. would be nice to have a reference here
            // for the proof. It is supposed to be somewhere in an Alexei/Nicholes article.
            if (nDegreesOfFreedom > 0)
                // For parameters with non-uniform prior on only one dimension
                hastingsRatio = -nDegreesOfFreedom * Math.log(scale);
            else
                hastingsRatio = (dim - 2) * Math.log(scale);

            // Must first set all parameters first and check for boundries later for the operator to work
            // correctly with dependent parameters such as beast.tree node heights.
            for (int i = 0; i < dim; i++) {
                param.setValue(i, param.getValue(i) * scale);
            }

            for (int i = 0; i < dim; i++) {
                if (param.getValue(i) < getLower() ||
                        param.getValue(i) > getUpper()) {
                    throw new Exception("Error scaleOperator 102: proposed value outside boundaries");
                }
            }
        } else {
            hastingsRatio = -Math.log(scale);

            // which bit to scale
            int index;
            /*
            if (indicator != null) {
                int idim = indicator.getDimension();
                bool impliedOne = idim == (dim - 1);
                // available bit locations
                int* loc = new int[idim + 1];
                for (int i = 0; i < idim + 1; i++) {
                	loc[i] = 0;
                }
                int nLoc = 0;
                // choose active or non active ones?
                bool takeOne = indicatorOnProb >= 1.0 || rand() < indicatorOnProb;

                if (impliedOne && takeOne) {
                    loc[nLoc] = 0;
                    ++nLoc;
                }
                for (int i = 0; i < idim; i++) {
                    double value = indicator.getStatisticValue(i);
                    if (takeOne == (value > 0)) {
                        loc[nLoc] = i + (impliedOne ? 1 : 0);
                        ++nLoc;
                    }
                }

                if (nLoc > 0) {
                    int rand = random_num(nLoc);
                    index = loc[rand];
                } else {
                    throw Exception("Error scaleOperator 103: no active indicators");
                }
                delete [] loc;
            } else {
            */
            // any is good
            index = Randomizer.nextInt(dim);
//            }

            double oldValue = param.getValue(index);

            if (oldValue == 0) {
                throw new Exception("Error scaleOperator 104: parameter has value 0 and cannot be scaled");
            }
            double newValue = scale * oldValue;

            if (newValue < getLower() || newValue > getUpper()) {
                throw new Exception("Error scaleOperator 105: proposed value outside boundaries");
            }

            param.setValue(index, newValue);
            // provides a hook for subclasses
            //cleanupOperation(newValue, oldValue);
        }
        hastingsRatio = Math.exp(hastingsRatio);
        return Math.log(hastingsRatio);
    }


    /**
     * automatic parameter tuning *
     */
    @Override
    public void optimize(double logAlpha) {
        Double fDelta = calcDelta(logAlpha);
        //double fScaleFactor = m_pScaleFactor.get();
        fDelta += Math.log(1.0 / m_fScaleFactor - 1.0);
        m_fScaleFactor = 1.0 / (Math.exp(fDelta) + 1.0);
    }

} // class ScaleOperator
