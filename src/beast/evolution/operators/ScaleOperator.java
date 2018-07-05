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

    public final Input<Tree> treeInput = new Input<>("tree", "if specified, all beast.tree divergence times are scaled");

    public final Input<RealParameter> parameterInput = new Input<>("parameter", "if specified, this parameter is scaled",
            Input.Validate.XOR, treeInput);

    public final Input<Double> scaleFactorInput = new Input<>("scaleFactor", "scaling factor: larger means more bold proposals", 1.0);
    public final Input<Boolean> scaleAllInput =
            new Input<>("scaleAll", "if true, all elements of a parameter (not beast.tree) are scaled, otherwise one is randomly selected",
                    false);
    public final Input<Boolean> scaleAllIndependentlyInput =
            new Input<>("scaleAllIndependently", "if true, all elements of a parameter (not beast.tree) are scaled with " +
                    "a different factor, otherwise a single factor is used", false);

    final public Input<Integer> degreesOfFreedomInput = new Input<>("degreesOfFreedom", "Degrees of freedom used when " +
            "scaleAllIndependently=false and scaleAll=true to override default in calculation of Hasting ratio. " +
            "Ignored when less than 1, default 0.", 0);
    final public Input<BooleanParameter> indicatorInput = new Input<>("indicator", "indicates which of the dimension " +
            "of the parameters can be scaled. Only used when scaleAllIndependently=false and scaleAll=false. If not specified " +
            "it is assumed all dimensions are allowed to be scaled.");
    final public Input<Boolean> rootOnlyInput = new Input<>("rootOnly", "scale root of a tree only, ignored if tree is not specified (default false)", false);
    final public Input<Boolean> optimiseInput = new Input<>("optimise", "flag to indicate that the scale factor is automatically changed in order to achieve a good acceptance rate (default true)", true);

    final public Input<Double> scaleUpperLimit = new Input<>("upper", "Upper Limit of scale factor", 1.0 - 1e-8);
    final public Input<Double> scaleLowerLimit = new Input<>("lower", "Lower limit of scale factor", 1e-8);

    /**
     * shadows input *
     */
    private double m_fScaleFactor;

    private double upper, lower;
    /**
     * flag to indicate this scales trees as opposed to scaling a parameter *
     */
    boolean m_bIsTreeScaler = true;

    @Override
    public void initAndValidate() {
        m_fScaleFactor = scaleFactorInput.get();
        m_bIsTreeScaler = (treeInput.get() != null);
        upper = scaleUpperLimit.get();
        lower = scaleLowerLimit.get();

        final BooleanParameter indicators = indicatorInput.get();
        if (indicators != null) {
            if (m_bIsTreeScaler) {
                throw new IllegalArgumentException("indicator is specified which has no effect for scaling a tree");
            }
            final int dataDim = parameterInput.get().getDimension();
            final int indsDim = indicators.getDimension();
            if (!(indsDim == dataDim || indsDim + 1 == dataDim)) {
                throw new IllegalArgumentException("indicator dimension not compatible from parameter dimension");
            }
        }
    }


    protected boolean outsideBounds(final double value, final RealParameter param) {
        final Double l = param.getLower();
        final Double h = param.getUpper();

        return (value < l || value > h);
        //return (l != null && value < l || h != null && value > h);
    }

    protected double getScaler() {
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

                final Tree tree = treeInput.get(this);
                if (rootOnlyInput.get()) {
                    final Node root = tree.getRoot();
                    final double newHeight = root.getHeight() * scale;

                    if (newHeight < Math.max(root.getLeft().getHeight(), root.getRight().getHeight())) {
                        return Double.NEGATIVE_INFINITY;
                    }
                    root.setHeight(newHeight);
                    return -Math.log(scale);
                } else {
                    // scale the beast.tree
                    final int internalNodes = tree.scale(scale);
                    return Math.log(scale) * (internalNodes - 2);
                }
            }

            // not a tree scaler, so scale a parameter
            final boolean scaleAll = scaleAllInput.get();
            final int specifiedDoF = degreesOfFreedomInput.get();
            final boolean scaleAllIndependently = scaleAllIndependentlyInput.get();

            final RealParameter param = parameterInput.get(this);

            assert param.getLower() != null && param.getUpper() != null;

            final int dim = param.getDimension();

            if (scaleAllIndependently) {
                // update all dimensions independently.
                hastingsRatio = 0;
                final BooleanParameter indicators = indicatorInput.get();
                if (indicators != null) {
                    final int dimCount = indicators.getDimension();
                    final Boolean[] indicator = indicators.getValues();
                    final boolean impliedOne = dimCount == (dim - 1);
                    for (int i = 0; i < dim; i++) {
                        if( (impliedOne && (i == 0 || indicator[i-1])) || (!impliedOne && indicator[i]) )  {
                            final double scaleOne = getScaler();
                            final double newValue = scaleOne * param.getValue(i);

                            hastingsRatio -= Math.log(scaleOne);

                            if (outsideBounds(newValue, param)) {
                                return Double.NEGATIVE_INFINITY;
                            }

                            param.setValue(i, newValue);
                        }
                    }
                }  else {

                    for (int i = 0; i < dim; i++) {

                        final double scaleOne = getScaler();
                        final double newValue = scaleOne * param.getValue(i);

                        hastingsRatio -= Math.log(scaleOne);

                        if( outsideBounds(newValue, param) ) {
                            return Double.NEGATIVE_INFINITY;
                        }

                        param.setValue(i, newValue);
                    }
                }
            } else if (scaleAll) {
                // update all dimensions
                // hasting ratio is dim-2 times of 1dim case. would be nice to have a reference here
                // for the proof. It is supposed to be somewhere in an Alexei/Nicholes article.

                // all Values assumed independent!
                final int computedDoF = param.scale(scale);
                final int usedDoF = (specifiedDoF > 0) ? specifiedDoF : computedDoF ;
                hastingsRatio = (usedDoF - 2) * Math.log(scale);
            } else {
                hastingsRatio = -Math.log(scale);

                // which position to scale
                final int index;
                final BooleanParameter indicators = indicatorInput.get();
                if (indicators != null) {
                    final int dimCount = indicators.getDimension();
                    final Boolean[] indicator = indicators.getValues();
                    final boolean impliedOne = dimCount == (dim - 1);

                    // available bit locations. there can be hundreds of them. scan list only once.
                    final int[] loc = new int[dimCount + 1];
                    int locIndex = 0;

                    if (impliedOne) {
                        loc[locIndex] = 0;
                        ++locIndex;
                    }
                    for (int i = 0; i < dimCount; i++) {
                        if (indicator[i]) {
                            loc[locIndex] = i + (impliedOne ? 1 : 0);
                            ++locIndex;
                        }
                    }

                    if (locIndex > 0) {
                        final int rand = Randomizer.nextInt(locIndex);
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
    public void optimize(final double logAlpha) {
        if (optimiseInput.get()) {
            double delta = calcDelta(logAlpha);
            delta += Math.log(1.0 / m_fScaleFactor - 1.0);
            setCoercableParameterValue(1.0 / (Math.exp(delta) + 1.0));
        }
    }

    @Override
    public double getCoercableParameterValue() {
        return m_fScaleFactor;
    }

    @Override
    public void setCoercableParameterValue(final double value) {
        m_fScaleFactor = Math.max(Math.min(value, upper), lower);
    }

    @Override
    public String getPerformanceSuggestion() {
        final double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        final double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        final double sf = Math.pow(m_fScaleFactor, ratio);

        final DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10) {
            return "Try setting scaleFactor to about " + formatter.format(sf);
        } else if (prob > 0.40) {
            return "Try setting scaleFactor to about " + formatter.format(sf);
        } else return "";
    }

} // class ScaleOperator
