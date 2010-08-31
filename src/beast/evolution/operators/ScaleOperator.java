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

import beast.core.Description;
import beast.core.Input;
import beast.core.Operator;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;

@Description("Scales a parameter or a complete beast.tree (depending on which of the two is specified.")
public class ScaleOperator extends Operator {

    public Input<Tree> m_pTree = new Input<Tree>("tree", "if specified, all beast.tree branch length are scaled");

    public Input<RealParameter> m_pParameter = new Input<RealParameter>("parameter", "if specified, this parameter is scaled",
            Input.Validate.XOR, m_pTree);

    public Input<Double> m_pScaleFactor = new Input<Double>("scaleFactor", "scaling factor: larger means more bold proposals", 1.0);
    // shadows input
    double m_fScaleFactor;
    public Input<Boolean> m_pScaleAll =
            new Input<Boolean>("scaleAll",
                    "if true, all elements of a parameter (not beast.tree) are scaled, otherwise one is randomly selected",
                    false);
    public Input<Boolean> m_pScaleAllIndependently =
            new Input<Boolean>("scaleAllIndependently", "if true, all elements of a parameter (not beast.tree) are scaled with " +
                    "a different factor, otherwise a single factor is used", false);

    public Input<Integer> m_pDegreesOfFreedom = new Input<Integer>("degreesOfFreedom", "Degrees of freedom used in ...", 1);

    /** flag to indicate this scales trees as opposed to scaling a parameter **/
    boolean m_bIsTreeScaler = true;

    @Override
    public void initAndValidate() {
        m_fScaleFactor = m_pScaleFactor.get();
        m_bIsTreeScaler = (m_pTree.get() != null);
    }


    /** override this for proposals,
	 * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal should not be accepted **/
    @Override
    public double proposal() {
    	try {
        //double fScaleFactor = m_pScaleFactor.get();

        double hastingsRatio = 1.0;
        final double d = Randomizer.nextDouble();
        final double scale = (m_fScaleFactor + (d * ((1.0 / m_fScaleFactor) - m_fScaleFactor)));
        
        if (m_bIsTreeScaler) {
        	final Tree tree = m_pTree.get(this);
            // scale the beast.tree
        	tree.getRoot().scale(scale);
            return Math.log(hastingsRatio);
        }
        final boolean bScaleAll = m_pScaleAll.get();
        final int nDegreesOfFreedom = m_pDegreesOfFreedom.get();
        final boolean bScaleAllIndependently = m_pScaleAllIndependently.get();

        final RealParameter param = m_pParameter.get(this);
        final int dim = param.getDimension();

        if (bScaleAllIndependently) {
            // update all dimensions independently.
            hastingsRatio = 0;
            for (int i = 0; i < dim; i++) {

                final double scaleOne = (m_fScaleFactor + (Randomizer.nextDouble() * ((1.0 / m_fScaleFactor) - m_fScaleFactor)));
                final double value = scaleOne * param.getValue(i);

                hastingsRatio -= Math.log(scaleOne);

                if (value < param.getLower() || value > param.getUpper()) {
                    return Double.NEGATIVE_INFINITY;
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

            // Must first set all parameters first and check for boundaries later for the operator to work
            // correctly with dependent parameters such as beast.tree node heights.
            for (int i = 0; i < dim; i++) {
                param.setValue(i, param.getValue(i) * scale);
            }

            for (int i = 0; i < dim; i++) {
                if (param.getValue(i) < param.getLower() || param.getValue(i) > param.getUpper()) {
                    return Double.NEGATIVE_INFINITY;
                    //throw new Exception("Error scaleOperator 102: proposed value outside boundaries");
                }
            }
        } else {
            hastingsRatio = -Math.log(scale);

            // which bit to scale
            final int index;
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

            final double oldValue = param.getValue(index);

            if (oldValue == 0) {
                return Double.NEGATIVE_INFINITY;
                //throw new Exception("Error scaleOperator 104: parameter has value 0 and cannot be scaled");
            }
            final double newValue = scale * oldValue;

            if (param.getLower() != null && newValue < param.getLower() || param.getUpper() != null && newValue > param.getUpper()) {
                // reject out of bounds scales
                return Double.NEGATIVE_INFINITY;
                //throw new Exception("Error scaleOperator 105: proposed value outside boundaries");
            }

            param.setValue(index, newValue);
            // provides a hook for subclasses
            //cleanupOperation(newValue, oldValue);
        }
        hastingsRatio = Math.exp(hastingsRatio);
        return Math.log(hastingsRatio);
    	} catch (Exception e) {
    		// whatever went wrong, we want to abort this operation...
            return Double.NEGATIVE_INFINITY;
		}
    }

    /**
     * automatic parameter tuning *
     */
    @Override
    public void optimize(final double logAlpha) {
        double fDelta = calcDelta(logAlpha);
        //double fScaleFactor = m_pScaleFactor.get();
        fDelta += Math.log(1.0 / m_fScaleFactor - 1.0);
        m_fScaleFactor = 1.0 / (Math.exp(fDelta) + 1.0);
    }

} // class ScaleOperator
