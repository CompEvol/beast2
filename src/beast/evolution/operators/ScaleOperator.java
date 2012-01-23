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


import java.text.DecimalFormat;

import beast.core.Description;
import beast.core.Input;
import beast.core.Operator;
import beast.core.parameter.BooleanParameter;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;

@Description("Scales a parameter or a complete beast.tree (depending on which of the two is specified.")
public class ScaleOperator extends Operator {

    public Input<Tree> m_pTree = new Input<Tree>("tree", "if specified, all beast.tree branch length are scaled");

    public Input<RealParameter> m_pParameter = new Input<RealParameter>("parameter", "if specified, this parameter is scaled",
            Input.Validate.XOR, m_pTree);

    public Input<Double> m_pScaleFactor = new Input<Double>("scaleFactor", "scaling factor: larger means more bold proposals", 1.0);
    public Input<Boolean> m_pScaleAll =
            new Input<Boolean>("scaleAll", "if true, all elements of a parameter (not beast.tree) are scaled, otherwise one is randomly selected",
                    false);
    public Input<Boolean> m_pScaleAllIndependently =
            new Input<Boolean>("scaleAllIndependently", "if true, all elements of a parameter (not beast.tree) are scaled with " +
                    "a different factor, otherwise a single factor is used", false);

    public Input<Integer> m_pDegreesOfFreedom = new Input<Integer>("degreesOfFreedom", "Degrees of freedom used when " +
            "scaleAllIndependently=false and scaleAll=true to override default in calcualation of Hasting ratio. " +
            "Ignored when less than 0, default 1.", 1);
    public Input<BooleanParameter> m_indicator = new Input<BooleanParameter>("indicator", "indicates which of the dimension " +
            "of the parameters can be scaled. Only used when scaleAllIndependently=false and scaleAll=false. If not specified " +
            "it is assumed all dimensions are allowed to be scaled.");
    public Input<Boolean> m_pRootOnly = new Input<Boolean>("rootOnly", "scale root of a tree only, ignored if tree is not specified (default false)", false);
    public Input<Boolean> m_bOptimise = new Input<Boolean>("optimise", "flag to indicate that the scale factor is automatically changed in order to acheive a good acceptance rate (default true)", true);


    /**
     * shadows input *
     */
    double m_fScaleFactor;

    /**
     * flag to indicate this scales trees as opposed to scaling a parameter *
     */
    boolean m_bIsTreeScaler = true;

    @Override
    public void initAndValidate() throws Exception {
        m_fScaleFactor = m_pScaleFactor.get();
        m_bIsTreeScaler = (m_pTree.get() != null);

        final BooleanParameter indicators = m_indicator.get();
        if (indicators != null) {
            if (m_bIsTreeScaler) {
                throw new Exception("indicator is specified which has no effect for scaling a tree");
            }
            final int dataDim = m_pParameter.get().getDimension();
            final int indsDim = indicators.getDimension();
            if (!(indsDim == dataDim || indsDim + 1 == dataDim)) {
                throw new Exception("indicator dimension not compatible from parameter dimension");
            }
        }
    }


    private boolean outsideBounds(double value, RealParameter param) {
        final Double l = param.getLower();
        final Double h = param.getUpper();

        return (value < l || value > h);
        //return (l != null && value < l || h != null && value > h);
    }

    private double getScaler() {
        return (m_fScaleFactor + (Randomizer.nextDouble() * ((1.0 / m_fScaleFactor) - m_fScaleFactor)));
    }

    /**
     * override this for proposals,
     *
     * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal should not be accepted *
     */
    @Override
    public double proposal() {
        try {

            double hastingsRatio;
            final double scale = getScaler();

            if (m_bIsTreeScaler) {
                Tree tree = m_pTree.get(this);
                if (m_pRootOnly.get()) {
                    Node root = tree.getRoot();
                    double fNewHeight = root.getHeight() * scale;
                    if (fNewHeight < Math.max(root.m_left.getHeight(), root.m_right.getHeight())) {
                        return Double.NEGATIVE_INFINITY;
                    }
                    root.setHeight(fNewHeight);
                    return -Math.log(scale);
                } else {
                    // scale the beast.tree
                    final int nInternalNodes = tree.scale(scale);
                    return Math.log(scale) * (nInternalNodes - 2);
                }
            }

            // not a tree scaler, so scale a parameter
            final boolean bScaleAll = m_pScaleAll.get();
            final int nDegreesOfFreedom = m_pDegreesOfFreedom.get();
            final boolean bScaleAllIndependently = m_pScaleAllIndependently.get();

            final RealParameter param = m_pParameter.get(this);

            assert param.getLower() != null && param.getUpper() != null;

            final int dim = param.getDimension();

            if (bScaleAllIndependently) {
                // update all dimensions independently.
                hastingsRatio = 0;
                for (int i = 0; i < dim; i++) {

                    final double scaleOne = getScaler();
                    final double newValue = scaleOne * param.getValue(i);

                    hastingsRatio -= Math.log(scaleOne);

                    if (outsideBounds(newValue, param)) {
                        return Double.NEGATIVE_INFINITY;
                    }

                    param.setValue(i, newValue);
                }
            } else if (bScaleAll) {
                // update all dimensions
                // hasting ratio is dim-2 times of 1dim case. would be nice to have a reference here
                // for the proof. It is supposed to be somewhere in an Alexei/Nicholes article.
                final int df = (nDegreesOfFreedom > 0) ? -nDegreesOfFreedom : dim - 2;
                hastingsRatio = df * Math.log(scale);

                // all Values assumed independent!
                for (int i = 0; i < dim; i++) {
                    final double newValue = param.getValue(i) * scale;

                    if (outsideBounds(newValue, param)) {
                        return Double.NEGATIVE_INFINITY;
                    }
                    param.setValue(i, newValue);
                }
            } else {
                hastingsRatio = -Math.log(scale);

                // which position to scale
                int index;
                final BooleanParameter indicators = m_indicator.get();
                if (indicators != null) {
                    final int nDim = indicators.getDimension();
                    Boolean[] indicator = indicators.getValues();
                    final boolean impliedOne = nDim == (dim - 1);

                    // available bit locations. there can be hundreds of them. scan list only once.
                    int[] loc = new int[nDim + 1];
                    int nLoc = 0;

                    if (impliedOne) {
                        loc[nLoc] = 0;
                        ++nLoc;
                    }
                    for (int i = 0; i < nDim; i++) {
                        if (indicator[i]) {
                            loc[nLoc] = i + (impliedOne ? 1 : 0);
                            ++nLoc;
                        }
                    }

                    if (nLoc > 0) {
                        final int rand = Randomizer.nextInt(nLoc);
                        index = loc[rand];
                    } else {
                        return Double.NEGATIVE_INFINITY; // no active indicators
                    }

                } else {
                    // any is good
                    index = Randomizer.nextInt(dim);
                }

                final double oldValue = param.getValue(index);

                if (oldValue == 0) {
                    // Error: parameter has value 0 and cannot be scaled
                    return Double.NEGATIVE_INFINITY;
                }

                final double newValue = scale * oldValue;

                if (outsideBounds(newValue, param)) {
                    // reject out of bounds scales
                    return Double.NEGATIVE_INFINITY;
                }

                param.setValue(index, newValue);
                // provides a hook for subclasses
                //cleanupOperation(newValue, oldValue);
            }

            return hastingsRatio;

        } catch (Exception e) {
            // whatever went wrong, we want to abort this operation...
            return Double.NEGATIVE_INFINITY;
        }
    }


    /**
     * automatic parameter tuning *
     */
    @Override
    public void optimize(double logAlpha) {
        if (m_bOptimise.get()) {
            double fDelta = calcDelta(logAlpha);
            fDelta += Math.log(1.0 / m_fScaleFactor - 1.0);
            m_fScaleFactor = 1.0 / (Math.exp(fDelta) + 1.0);
        }
    }

    @Override
    public double getCoercableParameterValue() {
        return m_fScaleFactor;
    }

    @Override
    public void setCoercableParameterValue(double fValue) {
        m_fScaleFactor = fValue;
    }

    @Override
    public String getPerformanceSuggestion() {
        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        double sf = Math.pow(m_fScaleFactor, ratio);

        DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10) {
            return "Try setting scaleFactor to about " + formatter.format(sf);
        } else if (prob > 0.40) {
            return "Try setting scaleFactor to about " + formatter.format(sf);
        } else return "";
    }

} // class ScaleOperator
