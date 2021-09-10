/*
 * AdaptableVarianceMultivariateNormalOperator.java
 *
 * Copyright (c) 2002-2018 Alexei Drummond, Andrew Rambaut and Marc Suchard
 *
 * This file is part of BEAST.
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

package beast.base.evolution.operator.kernel;



import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.evolution.tree.Tree;
import beast.base.inference.StateNode;
import beast.base.inference.operator.kernel.KernelOperator;
import beast.base.inference.operator.kernel.Transform;
import beast.base.inference.operator.kernel.Transform.*;
import beast.base.inference.parameter.RealParameter;
import beast.base.math.matrixalgebra.*;

/**
 * Adapted from BEAST 1
 * @author Guy Baele
 * @author Marc A. Suchard
 */
@Description("Operator that moves many parameters (possibly, after transformation to make them "
		+ "more normally distributed). It learns the correlation structure among these parameters "
		+ "during the MCMC run and updates parameters accordingly. doi:10.1093/bioinformatics/btx088")
public class AdaptableVarianceMultivariateNormalOperator extends KernelOperator {
	final public Input<List<Transform>> transformationsInput = new Input<>("transformations", 
			"one or more transformed parameters to be moved.\n"
			+ "For scale parameters use LogTransform (where e.g. scale operators were used).\n"
			+ "For location parameter use NoTransform (where e.g. random walk operators were used).\n"
			+ "For parameters that sum to a constant use LogConstrainedSumTransform  (where e.g. delta-exchange operators were used).", new ArrayList<>()); 
	final public Input<Double> scaleFactorInput = new Input<>("scaleFactor", "start scaling factor, larger values give bolder moves (this is tuned during the run)", 1.0); 
	final public Input<Double> coefficientInput = new Input<>("coefficient", "determines diagonal correlation for variance matrix", 1.0);
	final public Input<Double> betaInput = new Input<>("beta", "fraction of proposal determined by non-covariance matrix");
	final public Input<Integer> initialInput = new Input<>("initial", "Number of proposals before covariance matrix is considered in proposal. "
			+ "Must be larger than burnin, if specified. "
			+ "If not specified (or < 0), the operator uses 200 * parameter dimension", -1); 
	final public Input<Integer> burninInput = new Input<>("burnin", "Number of proposals that are ignored before covariance matrix is being updated. "
			+ "If initial is not specified, uses half the default initial value (which equals 100 * parameter dimension)", 0); 
	final public Input<Integer> everyInput = new Input<>("every", "update interval for covariance matrix, default 1 (that is, every step)", 1); 
    final public Input<Boolean> optimiseInput = new Input<>("optimise", "flag to indicate that the scale factor is automatically changed in order to achieve a good acceptance rate (default true)", true);
    final public Input<Boolean> storeInput = new Input<>("store", "flag to indicate if covariance and mean should be stored to the state file (default true)", true);

    final public Input<Boolean> allowNonsenseInput = new Input<>("allowNonsense", "flag to indicate if transforms may accept nonsensical inputs eg. 0 parameters (default false)", false);


    public static final boolean DEBUG = false;
    public static final boolean PRINT_FULL_MATRIX = false;

    private double scaleFactor;
    private double beta;
    private int initial, burnin, every;
    private CompoundParameterHelper parameter;
    private Transform[] transformations;
    private int[] transformationSizes;
    private double[] transformationSums;
    private int dim;
    private boolean allowNonsense;

    private double[] oldMeans, newMeans;

    private double[][] matrix;
    private double[][] empirical;
    private double[][] cholesky;

    // temporary storage, allocated once.
    private double[] epsilon;
    private double[][] proposal;

    
    

    @Override
	public void initAndValidate() {
        // setMode(modeInput.get());
        this.scaleFactor = scaleFactorInput.get();
        List<Function> parameterList = new ArrayList<>();
        this.allowNonsense = allowNonsenseInput.get();
        List<Transform> transforms = new ArrayList<Transform>();
        transforms.addAll(transformationsInput.get());
        List<Integer> toRemove = new ArrayList<>();
        for (int i = 0; i < transforms.size(); i ++) {
        	
        	Transform t = transforms.get(i);
        	
        	// Skip nonsensical transforms
        	if (this.allowNonsense) {
        		
        		int fDim = 0;
        		for (Function f : t.getF()) {
        			fDim += f.getDimension();
        		}
        		
        		if (fDim < t.getMinDimensions()) {
        			Log.warning("Warning: removing transform " + t.getClass().toString() + " because it should have at least " + 
        						t.getMinDimensions() + " dimensions but it has " + t.getF().size());
        			toRemove.add(i);
        			continue;
        		}
        	}
        	
			for (Function f : t.getF()) {
				parameterList.add(f);
			}
        }
        
        
        if (this.allowNonsense && parameterList.isEmpty()) {
        	dim = 0;
        	return;
        }
        
        // Remove nonsense transforms
        for (int i = toRemove.size() - 1; i >=0; i --) {
        	int indexToRemove = toRemove.get(i);
        	transforms.remove(indexToRemove);
        }

        
        this.parameter = new CompoundParameterHelper(parameterList);
        this.transformations = transforms.toArray(new Transform[]{});
        this.beta = betaInput.get();
        
//        this.iterations = 0;
//        this.updates = 0;
        //this.m_pWeight.setValue(1.0, this);
        
        dim = parameter.getDimension();
        
        matrix = new double[dim][dim];
        for (int i = 0; i < dim; i++) {
        	matrix[i][i] = Math.pow(coefficientInput.get(), 2) / ((double) dim);            	
        }        
        
        
        // constantFactor = Math.pow(2.38, 2) / ((double) dim); // not necessary because scaleFactor is auto-tuned
        this.initial = initialInput.get();
        
        if (this.initial < 0) {
        	// options set according to recommendations in AVMVN paper
        	this.initial = 200 * dim;
        	this.burnin = this.initial / 2;
        } else {
            this.burnin = burninInput.get();
        }

        
        this.every = everyInput.get();
        this.empirical = new double[dim][dim];
        this.oldMeans = new double[dim];
        this.newMeans = new double[dim];

        this.epsilon = new double[dim];
        this.proposal = new double[dim][dim];

    	
    	
        if (burnin > initial || burnin < 0) {
            throw new IllegalArgumentException("Burn-in must be smaller than the initial period.");
        }


        if (every <= 0) {
            throw new IllegalArgumentException("Covariance matrix needs to be updated at least every single iteration.");
        }

        if (scaleFactor <= 0.0) {
            throw new IllegalArgumentException("ScaleFactor must be greater than zero.");
        }

        dim = parameter.getDimension();
        
        int paramCount = 0;
        for(Transform t : transformations) {
        	if (t instanceof MultivariableTransform) {
        		paramCount++;
        	} else {
        		paramCount += t.getF().size();
        	}
        }
        transformationSizes = new int[paramCount];
        transformationSums = new double[paramCount];
        Transform [] ts = new Transform[paramCount];
        int k = 0;
        for (Transform t : transformations) {
        	if (t instanceof MultivariableTransform) {
        		for (Function p : t.getF()) {
        			if (p instanceof RealParameter) {
        				transformationSizes[k] += p.getDimension();
        			} else {
        				throw new IllegalArgumentException("Don't know how to handle MultivariableTransform of " + p.getClass().getSimpleName());
        			}
        		}
        		if (t instanceof LogConstrainedSumTransform) {
        			transformationSums[k] = ((LogConstrainedSumTransform)t).getSum();
        		}
        		ts[k] = t;
        		k++;
        	} else {
        		for (Function p : t.getF()) {
        			if (p instanceof RealParameter) {
        				transformationSizes[k] = p.getDimension();
        			} else if (p instanceof Tree) {
        				transformationSizes[k] = 1;
        			} else {
        				throw new IllegalArgumentException("Don't know how to handle " + p.getClass().getSimpleName());
        			}
            		ts[k] = t;
            		k++;
        		}
        	}
        }
        transformations = ts;
        
        
        try {
            cholesky = (new CholeskyDecomposition(matrix)).getL();
        } catch (IllegalDimension illegalDimension) {
            throw new RuntimeException("Unable to decompose matrix in AdaptableVarianceMultivariateNormalOperator");
        }	
        
        super.initAndValidate();
	}
    

	@Override
	public List<StateNode> listStateNodes() {
		List<StateNode> nodes = new ArrayList<>();
		if (this.allowNonsense && dim == 0) return nodes;
		
		for (Transform t : transformations) {
			
			// Include the transform?
			if (this.allowNonsense) {
				int fDim = 0;
				for (Function f : t.getF()) fDim += f.getDimension();
				if (fDim < t.getMinDimensions()) continue;
			}
			
			for (Function f : t.getF()) {
				if (f instanceof StateNode) {
					nodes.add((StateNode) f);
				}
			}
		}
		return nodes;
	}

    //act as if population mean is known
    private double calculateCovariance(int number, double currentMatrixEntry, double[] values, int firstIndex, int secondIndex) {
        // number will always be > 1 here
        /*double result = currentMatrixEntry * (number - 1);
        result += (values[firstIndex] * values[secondIndex]);
        result += ((number - 1) * oldMeans[firstIndex] * oldMeans[secondIndex] - number * newMeans[firstIndex] * newMeans[secondIndex]);
        result /= ((double) number);*/

    	// Welford-style update
    	// https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Welford's_online_algorithm
        double result = currentMatrixEntry * (number - 2);
        result += (values[firstIndex] * values[secondIndex]);
        result += ((number - 1) * oldMeans[firstIndex] * oldMeans[secondIndex] - number * newMeans[firstIndex] * newMeans[secondIndex]);
        result /= ((double)(number - 1));
        
//      result += (values[firstIndex] - newMeans[firstIndex]) * (values[secondIndex]- newMeans[secondIndex]);
//      result /= ((double)(number - 1));

        return result;

    }

	@Override
	public double proposal() {
		return doOperation();
	}

	public double doOperation() {
		
		
		if (this.allowNonsense && dim == 0) return 0;
		
		
        int iterations=m_nNrAccepted+m_nNrRejected;


        if (DEBUG) {
            System.err.println("\nAVMVN Iteration: " + iterations);
            System.err.println("Using AdaptableVarianceMultivariateNormalOperator: " + iterations + " for " + "parameter.getID()");
            System.err.println("Old parameter values:");
            for (int i = 0; i < dim; i++) {
                System.err.println(parameter.getValue(i));
            }
        }

        // double[] x = parameter.getDoubleValues();
        double[] x = new double[dim];
        for (int i = 0; i < dim; i++) {
        	x[i] = parameter.getValue(i);
        }

        //transform to the appropriate scale
        double[] transformedX = new double[dim];
        /*for (int i = 0; i < dim; i++) {
            transformedX[i] = transformations[i].transform(x[i]);
        }*/
        //iterate over transformation sizes rather than number of parameters
        //as a transformation might impact multiple parameters
        int currentIndex = 0;
        for (int i = 0; i < transformationSizes.length; i++) {
            if (DEBUG) {
                System.err.println("currentIndex = " + currentIndex);
                System.err.println("transformationSizes[i] = " + transformationSizes[i]);
            }
            if (transformationSizes[i] > 1) {
            	if (transformationSums[i] != 0) {
            		final double [] t = transformations[i].transform(x, currentIndex, currentIndex + transformationSizes[i] - 1);
            		System.arraycopy(t,0,transformedX,currentIndex,transformationSizes[i]);
            	} else {
            		final double [] t = transformations[i].transform(x, currentIndex, currentIndex + transformationSizes[i]);
            		System.arraycopy(t,0,transformedX,currentIndex,transformationSizes[i]);            		
            	}
            } else {
                transformedX[currentIndex] = transformations[i].transform(x[currentIndex]);
                if (DEBUG) {
                    System.err.println("x[" + currentIndex + "] = " + x[currentIndex] + " -> " + transformedX[currentIndex]);
                }
            }
            currentIndex += transformationSizes[i];
        }
        

        if (DEBUG) {
            System.err.println("Old transformed parameter values:");
            for (int i = 0; i < dim; i++) {
                System.err.println(transformedX[i]);
            }
        }

        //store MH-ratio in logq
        double logJacobian = 0.0;

        //change this: make a rule for when iterations == burnin
        if (iterations > 1 && iterations > burnin) {

            if (DEBUG) {
                System.err.println("  AVMVN iterations > burnin");
            }

            // TODO Beginning of adaptable covariance
            if (iterations > (burnin+1)) {

                if (iterations % every == 0) {

                    int updates=iterations-burnin;

                    if (DEBUG) {
                        System.err.println("updates = " + updates);
                    }
                    //first recalculate the means using recursion
                    for (int i = 0; i < dim; i++) {
                        newMeans[i] = ((oldMeans[i] * (updates - 1)) + transformedX[i]) / updates;
                    }

                    if (updates > 1) {
                        //here we can simply use the double[][] matrix
                        for (int i = 0; i < dim; i++) {
                            for (int j = i; j < dim; j++) {
                                empirical[i][j] = calculateCovariance(updates, empirical[i][j], transformedX, i, j);
                                empirical[j][i] = empirical[i][j];
                            }
                        }
                    }


                    if (DEBUG) {
                        System.err.println("Old means:");
                        for (int i = 0; i < dim; i++) {
                            System.err.println(oldMeans[i]);
                        }
                        System.err.println("New means:");
                        for (int i = 0; i < dim; i++) {
                            System.err.println(newMeans[i]);
                        }
                        System.err.println("Empirical covariance matrix:");
                        for (int i = 0; i < dim; i++) {
                            for (int j = 0; j < dim; j++) {
                                System.err.print(empirical[i][j] + " ");
                            }
                            System.err.println();
                        }
                    }

                }

                /*if (iterations == 17) {
                    System.exit(0);
                }*/

            } else if (iterations == (burnin+1)) {

                //updates++;

                //i.e. iterations == burnin+1, i.e. first sample for C_t
                //this will not be reached when burnin is set to 0
                for (int i = 0; i < dim; i++) {
                    //oldMeans[i] = transformedX[i];
                    //newMeans[i] = transformedX[i];
                    oldMeans[i] = 0.0;
                    newMeans[i] = 0.0;
                }

                for (int i = 0; i < dim; i++) {
                    for (int j = 0; j < dim; j++) {
                        empirical[i][j] = 0.0;
                    }
                }

            }

            // TODO End of adaptable covariance -- move into separate class

        } else if (iterations == 1) {

            if (DEBUG) {
                System.err.println("\niterations == 1");
            }

            //iterations == 1
            for (int i = 0; i < dim; i++) {
                //oldMeans[i] = transformedX[i];
                //newMeans[i] = transformedX[i];
                oldMeans[i] = 0.0;
                newMeans[i] = 0.0;
            }

            for (int i = 0; i < dim; i++) {
                for (int j = 0; j < dim; j++) {
                    empirical[i][j] = 0.0;
                    proposal[i][j] = matrix[i][j];
                }
            }

        }

        for (int i = 0; i < dim; i++) {
            //epsilon[i] = scaleFactor * Randomizer.nextGaussian();
            epsilon[i] = kernelDistribution.getRandomDelta(i, epsilon[i], scaleFactor);
        }

        if (iterations > initial) {

            if (DEBUG) {
                System.err.println("  iterations > initial");
            }

            if (iterations % every == 0) {
                // TODO: For speed, it may not be necessary to update decomposition each and every iteration

                for (int i = 0; i < dim; i++) {
                    for (int j = i; j < dim; j++) { // symmetric matrix
                        proposal[j][i] = (1 - beta) * // constantFactor *  /* auto-tuning using scaleFactor */
                                empirical[j][i] + beta * matrix[j][i];
                        proposal[i][j] = proposal[j][i] ;
                    }
                }

                // not necessary for first test phase, but will need to be performed when covariance matrix is being updated
                try {
                    cholesky = (new CholeskyDecomposition(proposal)).getL();
                } catch (IllegalDimension illegalDimension) {
                    throw new RuntimeException("Unable to decompose matrix in AdaptableVarianceMultivariateNormalOperator");
                }
            }

        }

        if (DEBUG) {
            System.err.println("  Drawing new values");
        }

        for (int i = 0; i < dim; i++) {
            for (int j = i; j < dim; j++) {
                transformedX[i] += cholesky[j][i] * epsilon[j];
                // caution: decomposition returns lower triangular
            }
        }

        if (DEBUG) {
            System.err.println("\nTransformed X values:");
            for (int i = 0; i < dim; i++) {
                System.err.println(transformedX[i]);
            }
            System.err.println();
        }

        //iterate over transformation sizes rather than number of parameters
        //as a transformation might impact multiple parameters
        currentIndex = 0;
        for (int i = 0; i < transformationSizes.length; i++) {
            if (DEBUG) {
                System.err.println("currentIndex = " + currentIndex);
                System.err.println("transformationSizes[i] = " + transformationSizes[i]);
            }
            if (MULTI) {
                if (transformationSizes[i] > 1) {
                	if (transformationSums[i] != 0) {
                		double[] temp = transformations[i].inverse(transformedX, currentIndex, currentIndex + transformationSizes[i] - 1, transformationSums[i]);
                		for (int k = 0; k < temp.length; k++) {
                			parameter.setValue(currentIndex + k, temp[k]);
                		}
                		logJacobian += transformations[i].getLogJacobian(x, currentIndex, currentIndex + transformationSizes[i] - 1) - transformations[i].getLogJacobian(temp, 0, transformationSizes[i] - 1);
                	} else {
                		double[] temp = transformations[i].inverse(transformedX, currentIndex, currentIndex + transformationSizes[i]);
                		for (int k = 0; k < temp.length; k++) {
                			parameter.setValue(currentIndex + k, temp[k]);
                		}
                		logJacobian += transformations[i].getLogJacobian(x, currentIndex, currentIndex + transformationSizes[i]) - transformations[i].getLogJacobian(temp, 0, transformationSizes[i]);
                	}
                } else {
                    int k = parameter.setValue(currentIndex, transformations[i].inverse(transformedX[currentIndex]));
                    logJacobian += k * (transformations[i].getLogJacobian(x[currentIndex]) - transformations[i].getLogJacobian(parameter.getValue(currentIndex)));
                }
                if (DEBUG) {
                    System.err.println("Current logJacobian = " + logJacobian);
                }
            } else {
                if (transformationSizes[i] > 1) {
                    //TODO: figure out if this is really a problem ...
                    throw new RuntimeException("Transformations on more than 1 parameter value should be set quietly");
                } else {
                    parameter.setValue(currentIndex, transformations[i].inverse(transformedX[currentIndex]));
                    logJacobian += transformations[i].getLogJacobian(x[currentIndex]) - transformations[i].getLogJacobian(parameter.getValue(currentIndex));
                }
                if (DEBUG) {
                    System.err.println("Current logJacobian = " + logJacobian);
                }
            }
            currentIndex += transformationSizes[i];
        }

        if (DEBUG) {
            System.err.println("Proposed parameter values:");
            for (int i = 0; i < dim; i++) {
                System.err.println(x[i] + " -> " + parameter.getValue(i));
            }
            System.err.println("LogJacobian: " + logJacobian);
        }

        if (MULTI) {
            //  parameter.fireParameterChangedEvent(); // Signal once.
        }

        if (iterations % every == 0) {
            if (DEBUG) {
                System.err.println("  Copying means");
            }
            //copy new means to old means for next update iteration
            //System.arraycopy(newMeans, 0, oldMeans, 0, dim);
            double[] tmp = oldMeans;
            oldMeans = newMeans;
            newMeans = tmp; // faster to swap pointers
        }

        return logJacobian;

    }

    public String toString() {
        return this.getClass().getSimpleName() + "(" + "parameter.getID()" + ")";
    }

    public static final boolean MULTI = true;

    public void provideSamples(ArrayList<ArrayList<Double>> parameterSamples) {
        if (DEBUG) {
            System.err.println("AVMVN operator parameter length: " + parameter.getDimension());
            System.err.println("provideSamples argument length: " + parameterSamples.size());
        }
        if (parameter.getDimension() != parameterSamples.size()) {
            throw new RuntimeException("Dimension mismatch in AVMVN Operator: inconsistent parameter dimensions");
        } else {
            int lowestNumberOfSamples = parameterSamples.get(0).size();
            for (int i = 0; i < parameterSamples.size(); i++) {
                if (parameterSamples.get(i).size() < lowestNumberOfSamples) {
                    lowestNumberOfSamples = parameterSamples.get(i).size();
                }
            }
            if (DEBUG) {
                System.err.println("lowest number of samples: " + lowestNumberOfSamples);
            }
            //set number of iterations of AVMVN operator
//            this.iterations = lowestNumberOfSamples;
//            this.updates = lowestNumberOfSamples;
            this.beta = 0.0;
            //set means based on provided samples, but take into account transformation(s)
            for (int i = 0; i < parameterSamples.size(); i++) {
                for (int j = 0; j < lowestNumberOfSamples; j++) {
                    newMeans[i] += transformations[i].transform(parameterSamples.get(i).get(j));
                    //parameterSamples.get(i).get(j);
                }
                newMeans[i] /= (double)lowestNumberOfSamples;
            }
            if (DEBUG) {
                System.err.println();
                for (int i = 0; i < parameterSamples.size(); i++) {
                    System.err.println("Mean " + i + ": " + newMeans[i]);
                }
            }
            //set covariance matrix based on provided samples, but take into account transformation(s)
            for (int i = 0; i < dim; i++) {
                for (int j = i; j < dim; j++) {
                    for (int k = 0; k < lowestNumberOfSamples; k++) {
                        empirical[i][j] += transformations[i].transform(parameterSamples.get(i).get(k))*transformations[i].transform(parameterSamples.get(j).get(k));
                    }
                    empirical[i][j] /= (double)lowestNumberOfSamples;
                    empirical[i][j] -= newMeans[i]*newMeans[j];
                    empirical[j][i] = empirical[i][j];
                }
            }
            if (DEBUG) {
                System.err.println();
                for (int i = 0; i < dim; i++) {
                    for (int j = 0; j < dim; j++) {
                        System.err.print(empirical[i][j] + "  ");
                    }
                    System.err.println();
                }
            }
        }
    }

    //MCMCOperator INTERFACE
    public final String getOperatorName() {
        String output = "adaptableVarianceMultivariateNormal(" + "parameter.getID()" + ")";
        if (PRINT_FULL_MATRIX) {
            output += "\nMeans:\n";
            for (int i = 0; i < dim; i++) {
                output += newMeans[i] + " ";
            }
            output += "\nVariance-covariance matrix:\n";
            for (int i = 0; i < dim; i++) {
                for (int j = 0; j < dim; j++) {
                    output += empirical[i][j] + " ";
                }
                output += "\n";
            }
        }
        return output;
    }
    
    public class CompoundParameterHelper {
        protected int[] parameterIndex1; // index to select parameter
        protected int[] parameterIndex2; // index to select dimension inside parameter

        final List<Function> parameterList;

        public CompoundParameterHelper(final List<Function> parameterList) {
            this.parameterList = parameterList;

            if (parameterList == null || parameterList.size() < 1) {
            	if (allowNonsense) return;
                throw new IllegalArgumentException("There is no parameter inputted into CompoundParameter !");
            }

            int dim = 0;
            for (final Function para : parameterList) {
            	if (para instanceof RealParameter) {
            		dim += para.getDimension();
            	} else if (para instanceof Tree) {
            		dim += 1;
            	}
            }

            parameterIndex1 = new int[dim];
            parameterIndex2 = new int[dim];

            int k = 0;
            for (int y = 0; y < parameterList.size(); y++) {
                final Function para = parameterList.get(y);
            	if (para instanceof RealParameter) {
	                for (int d = 0; d < para.getDimension(); d++) {
	                    parameterIndex1[k] = y;
	                    parameterIndex2[k] = d;
	                    k++;
	                }
            	} else {
                    parameterIndex1[k] = y;
                    parameterIndex2[k] = 0;
                    k++;            		
            	}
            }
        }

        public int getDimension() {
            return parameterIndex1.length;
        }

        public int setValue(final int param, final double value) {
            final Function para = parameterList.get(getY(param));
            if (para instanceof RealParameter) {
            	RealParameter p = (RealParameter) para;
            	if (value > p.getUpper()) {
            		p.setValue(getX(param), p.getUpper());
            	} else if (value < p.getLower()) {
            		p.setValue(getX(param), p.getLower());
            	} else {
            		p.setValue(getX(param), value);
            	}
            	return 1;
            } else if (para instanceof Tree) {
            	double old = para.getArrayValue();
            	double scale = value / old;
            	((Tree) para).scale(scale);
            	return ((Tree) para).getInternalNodeCount();
            }
            return 0;
        }

        public double getValue(final int param) {
        	Function f = parameterList.get(getY(param));
        	if (f instanceof RealParameter) {
        		return f.getArrayValue(getX(param));
        	}
        	return ((Tree) f).getRoot().getHeight();
        }

//        public double getLower(final int param) {
//            return parameterList.get(getY(param)).getLower();
//        }
//
//        public double getUpper(final int param) {
//            return parameterList.get(getY(param)).getUpper();
//        }

        // the index inside a parameter
        protected int getX(final int param) {
            return parameterIndex2[param];
        }

        // the index of parameter list
        protected int getY(final int param) {
            return parameterIndex1[param];
        }

    }

    @Override
    public void setCoercableParameterValue(double scaleFactor) {
    	this.scaleFactor = scaleFactor;
    }

    
    @Override
    public double getCoercableParameterValue() {
    	return scaleFactor;
    }
    
    @Override
    public void optimize(double logAlpha) {
    	if (optimiseInput.get()) {
    		final double i = calcDelta(logAlpha);
    		if (scaleFactor + i > 0) {
    			scaleFactor += i;
    		}
    	}
   }
    
    final static double DEFAULT_ADAPTATION_TARGET = 0.234;
    final static double MINIMUM_ACCEPTANCE_LEVEL = 0.1;
    final static double MAXIMUM_ACCEPTANCE_LEVEL = 0.4;
    final static double MINIMUM_GOOD_ACCEPTANCE_LEVEL = 0.2;
    final static double MAXIMUM_GOOD_ACCEPTANCE_LEVEL = 0.3;
    
    public final double[] getMeans() {
    	double[] means = new double[oldMeans.length];
    	System.arraycopy(oldMeans, 0, means, 0, oldMeans.length);
    	return means;
    } 
    
    public final void setMeans(double[] means) {
    	System.arraycopy(means, 0, oldMeans, 0, means.length);
    }
    
    
    public final double[][] getCovariance(){
    	double[][] covariance = new double[empirical.length][empirical[0].length];
    	for (int i = 0; i < empirical.length;i++)
    		System.arraycopy(empirical[i], 0, covariance[i], 0, empirical[i].length);
    	
    	return covariance;
    }
    
    public final void setCovariance(double[][] covariance) {
    	for (int i = 0; i < covariance.length;i++)
    		System.arraycopy(covariance[i], 0, empirical[i], 0, covariance[i].length);
    }
    
    @Override
    public final double getTargetAcceptanceProbability() {
        return DEFAULT_ADAPTATION_TARGET;
    }

    @Override
    public final String getPerformanceSuggestion() {
        final double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        final double targetProb = getTargetAcceptanceProbability();

        double delta = getCoercableParameterValue();
        if (delta <= 0.0) {
            throw new IllegalArgumentException("random walk window size cannot be negative: " + delta);
        }

        double ratio = prob / targetProb;

        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        double newDelta = delta * ratio;

        String adaptationParameterName = "scalefactor";
        final DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < MINIMUM_ACCEPTANCE_LEVEL) {
            return "Try setting " + adaptationParameterName + " to about " + formatter.format(newDelta);
        } else if (prob > MAXIMUM_ACCEPTANCE_LEVEL) {
            return "Try setting " + adaptationParameterName + " to about " + formatter.format(newDelta);
        } else return "";
    }
    
//    public int gete() {return iterations;}
    
    /**
     * Since this operator additionally uses the covariances for proposal, they have to be stored to a file as well
     */
    @Override
    public void storeToFile(final PrintWriter out) {
    	try {
	        JSONStringer json = new JSONStringer();
	        json.object();
	
	        if (getID()==null)
	           setID("unknown");
	
	        json.key("id").value(getID());
	
	        double p = getCoercableParameterValue();
	        if (Double.isNaN(p)) {
	            json.key("p").value("NaN");
	        } else if (Double.isInfinite(p)) {
	        	if (p > 0) {
	        		json.key("p").value("Infinity");
	        	} else {
	        		json.key("p").value("-Infinity");
	        	}
	        } else {
	            json.key("p").value(p);
	        }
	        
	        if (storeInput.get()) {
	        	// make the covariance matrix into an array	        
		        int cov_length = empirical.length;
		        int c = 0;
		        double[] flat_cov = new double[(cov_length*cov_length-cov_length)/2+cov_length];
		        for (int a = 0; a < empirical.length; a++) {
		        	for (int b = a; b < empirical.length; b++) {
		        		flat_cov[c] = empirical[a][b];
		        		c++;
		        	}
		        } 
		        json.key("means").value(Arrays.toString(oldMeans));
		        json.key("covariance").value(Arrays.toString(flat_cov));	        
	        }
	        json.key("accept").value(m_nNrAccepted);
	        json.key("reject").value(m_nNrRejected);
	        json.key("acceptFC").value(m_nNrAcceptedForCorrection);
	        json.key("rejectFC").value(m_nNrRejectedForCorrection);
	        json.key("rejectIv").value(m_nNrRejectedInvalid);
	        json.key("rejectOp").value(m_nNrRejectedOperator);
	        json.endObject();
	        out.print(json.toString());
    	} catch (JSONException e) {
    		// failed to log operator in state file
    		// report and continue
    		e.printStackTrace();
    	}
    }

    @Override
    public void restoreFromFile(JSONObject o) {

    	try {
    		if (storeInput.get()) {
	    		String[] means_string = ((String) o.getString("means")).replace("[", "").replace("]", "").split(", ");
		        String[] cov_string = ((String) o.getString("covariance")).replace("[", "").replace("]", "").split(", ");
		        
		        
		        oldMeans = new double[means_string.length];
		        for (int a = 0; a < oldMeans.length; a++)
		        	oldMeans[a] = Double.parseDouble(means_string[a]);
		        
		        empirical = new double[oldMeans.length][oldMeans.length];
		        int c = 0;
		        for (int a = 0; a < empirical.length; a++) {
		        	for (int b = a; b < empirical.length; b++) {
		        		empirical[a][b] = Double.parseDouble(cov_string[c]);
		        		empirical[b][a] = Double.parseDouble(cov_string[c]);
		        		c++;
		        	}	        	
		    	}    	
    		}else {
    	        this.empirical = new double[dim][dim];
    	        this.oldMeans = new double[dim];
    	        this.newMeans = new double[dim];

    			
                for (int i = 0; i < dim; i++) {
                    //oldMeans[i] = transformedX[i];
                    //newMeans[i] = transformedX[i];
                    oldMeans[i] = 0.0;
                    newMeans[i] = 0.0;
                }

                for (int i = 0; i < dim; i++) {
                    for (int j = 0; j < dim; j++) {
                        empirical[i][j] = 0.0;
                    }
                }
    		}
	        super.restoreFromFile(o);  	
    	} catch (JSONException e) {
    		// failed to restore from state file
    		// report and continue
    		e.printStackTrace();
    	}
    }

    
}
