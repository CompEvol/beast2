/*
 * Transform.java
 *
 * Copyright (c) 2002-2016 Alexei Drummond, Andrew Rambaut and Marc Suchard
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

package beast.base.inference.operator.kernel;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import beast.base.core.BEASTInterface;
import beast.base.core.BEASTObject;
import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.inference.parameter.RealParameter;
import beast.base.math.matrixalgebra.Matrix;
import beast.base.util.Randomizer;

/**
 * interface for the one-to-one transform of a continuous variable.
 * A static member Transform.LOG provides an instance of LogTransform
 *
 * @author Andrew Rambaut
 * @author Guy Baele
 * @author Marc Suchard
 * @version $Id: Transform.java,v 1.5 2005/05/24 20:26:01 rambaut Exp $
 */
public interface Transform {
    /**
     * @param value evaluation point
     * @return the transformed value
     */
    double transform(double value);
    
    
    
    /**
     * Get the minumum number of dimensions this transform can handle
     * @return
     */
    int getMinDimensions(); 
    

    /**
     * overloaded transformation that takes and returns an array of doubles
     * @param values evaluation points
     * @param from start transformation at this index
     * @param to end transformation at this index
     * @return the transformed values
     */
    double[] transform(double[] values, int from, int to);

    /**
     * @param value evaluation point
     * @return the inverse transformed value
     */
    double inverse(double value);

    /**
     * overloaded transformation that takes and returns an array of doubles
     * @param values evaluation points
     * @param from start transformation at this index
     * @param to end transformation at this index
     * @return the transformed values
     */
    double[] inverse(double[] values, int from, int to);

    /**
     * overloaded transformation that takes and returns an array of doubles
     * @param values evaluation points
     * @param from start transformation at this index
     * @param to end transformation at this index
     * @param sum fixed sum of values that needs to be enforced
     * @return the transformed values
     */
    double[] inverse(double[] values, int from, int to, double sum);

    double updateGradientLogDensity(double gradient, double value);

    double[] updateGradientLogDensity(double[] gradient, double[] value, int from, int to);

    double updateDiagonalHessianLogDensity(double diagonalHessian, double gradient, double value);

    double[] updateDiagonalHessianLogDensity(double[] diagonalHessian, double[] gradient, double[] value, int from, int to);

    double[][] updateHessianLogDensity(double[][] hessian, double[][] transformationHessian, double[] gradient, double[] value, int from, int to);

    double updateOffdiagonalHessianLogDensity(double offdiagonalHessian, double transformationHessian, double gradientI, double gradientJ, double valueI, double valueJ);

    double updateGradientInverseUnWeightedLogDensity(double gradient, double value);

    double[] updateGradientInverseUnWeightedLogDensity(double[] gradient, double[] value, int from, int to);

    double updateGradientUnWeightedLogDensity(double gradient, double value);

    double[] updateGradientUnWeightedLogDensity(double[] gradient, double[] value, int from, int to);

    double gradient(double value);

    double[] gradient(double[] values, int from, int to);

    double gradientInverse(double value);

    double[] gradientInverse(double[] values, int from, int to);
    
    List<Function> getF();

    /**
     * @return the transform's name
     */
    String getTransformName();

    /**
     * @param value evaluation point
     * @return the log of the transform's jacobian
     */
    double getLogJacobian(double value);

    /**
     * @param values evaluation points
     * @param from start calculation at this index
     * @param to end calculation at this index
     * @return the log of the transform's jacobian
     */
    double getLogJacobian(double[] values, int from, int to);

    /**
     * @return true if the transform is multivatiate (i.e. components not independents)
     */
    boolean isMultivariate();
    
    @Description(value="Transforms parameter of dimension 1", isInheritable = false)
    abstract class UnivariableTransform extends BEASTObject implements Transform {
    	final public Input<List<Function>> functionInput = new Input<>("f", "parameter to be transformed", new ArrayList<>());
    	
    	List<Function> parameter;
    	
 	   /* Utility for testing purposes
 	    * The arguments are alternating input names and values,
 	    * and values are assigned to the input with the particular name.
 	    * For example myInitByName("kappa", 2.0, "lambda", true)
 	    * assigns 2 to input kappa and true to input lambda.
 	    * After assigning inputs, initAndValidate() is called.
 	    */
 	  public Object myInitByName(final Object... objects) {
 	      if (objects.length % 2 == 1) {
 	          throw new RuntimeException("Expected even number of arguments, name-value pairs");
 	      }
 	      for (int i = 0; i < objects.length; i += 2) {
 	          if (objects[i] instanceof String) {
 	              final String name = (String) objects[i];
 	              setInputValue(name, objects[i + 1]);
 	          } else {
 	              throw new RuntimeException("Expected a String in " + i + "th argument ");
 	          }
 	      }
 	      try {
 	          validateInputs();
 	      } catch (IllegalArgumentException ex) {
 	          ex.printStackTrace();
 	          throw new RuntimeException("validateInputs() failed! " + ex.getMessage());
 	      }
 	      try {
 	          initAndValidate();
 	      } catch (Exception e) {
 	          e.printStackTrace();
 	          throw new RuntimeException("initAndValidate() failed! " + e.getMessage());
 	      }
 	      return this;
 	  } // myInitByName
 	  
        public List<Function> getF() {
			return parameter;
		}

		public void setParameter(Function parameter) {
			if (parameter == null) {
				this.parameter = new ArrayList<>();
			}
			if (parameter != null) {
				this.parameter.add(parameter);
			}
		}

		public void setParameter(List<Function> parameter) {
			if (parameter == null) {
				this.parameter = new ArrayList<>();
			}
			if (parameter != null) {
				this.parameter.addAll(parameter);
			}
		}
		
		public UnivariableTransform() {			
		}
		
		public UnivariableTransform(List<Function> parameter) {
			this.parameter = parameter;
		}
		
		@Override
		public void initAndValidate() {
			parameter = functionInput.get();			
		}
		
		public abstract double transform(double value);

        public double[] transform(double[] values, int from, int to) {
            double[] transformedValues = new double[to - from];
            int counter = 0;
            for (int i = from; i < to; i++) {
                transformedValues[counter] = transform(values[i]);
                counter++;
            }
            return transformedValues;
//            double[] result = values.clone();
//            for (int i = from; i < to; ++i) {
//                result[i] = transform(values[i]);
//            }
//            return result;
        }

        public abstract double inverse(double value);

        public double[] inverse(double[] values, int from, int to) {
            double[] inverse = new double[to - from];
            int counter = 0;
            for (int i = from; i < to; i++) {
            	inverse[counter] = inverse(values[i]);
                counter++;
            }
            return inverse;
//            double[] result = values.clone();
//            for (int i = from; i < to; ++i) {
//                result[i] = inverse(values[i]);
//            }
//            return result;
        }

        public double[] inverse(double[] values, int from, int to, double sum) {
            throw new RuntimeException("Fixed sum cannot be enforced for a univariate transformation.");
        }

        public abstract double gradientInverse(double value);

        public double[] gradientInverse(double[] values, int from, int to) {
            double[] result = new double[to - from];
            int counter = 0;
            for (int i = from; i < to; i++) {
            	result[counter] = gradientInverse(values[i]);
                counter++;
            }
            return result;
//            double[] result = values.clone();
//            for (int i = from; i < to; ++i) {
//                result[i] = gradientInverse(values[i]);
//            }
//            return result;
        }

        public double updateGradientLogDensity(double gradient, double value) {
            // value : untransformed. TODO:use updateGradientUnWeightedLogDensity()
            return updateGradientInverseUnWeightedLogDensity(gradient, transform(value)) + getGradientLogJacobianInverse(transform(value));
        }

        public double[] updateGradientLogDensity(double[] gradient, double[] value , int from, int to) {
            double[] result = new double[to - from];
            int counter = 0;
            for (int i = from; i < to; i++) {
            	result[counter] = updateGradientLogDensity(gradient[i], value[i]);
                counter++;
            }
            return result;
//            double[] result = value.clone();
//            for (int i = from; i < to; ++i) {
//                result[i] = updateGradientLogDensity(gradient[i], value[i]);
//            }
//            return result;
        }

        public double[] updateDiagonalHessianLogDensity(double[] diagonalHessian, double[] gradient, double[] value, int from, int to) {
            double[] result = new double[to - from];
            int counter = 0;
            for (int i = from; i < to; i++) {
            	result[counter] = updateDiagonalHessianLogDensity(diagonalHessian[i], gradient[i], value[i]);
                counter++;
            }
            return result;
//            double[] result = value.clone();
//            for (int i = from; i < to; ++i) {
//                result[i] = updateDiagonalHessianLogDensity(diagonalHessian[i], gradient[i], value[i]);
//            }
//            return result;
        }

        public double updateGradientInverseUnWeightedLogDensity(double gradient, double value) {
            // value is transformed
            return gradient * gradientInverse(value);
        }

        public double updateGradientUnWeightedLogDensity(double gradient, double value) {
            // value is unTransformed
            return gradient * gradient(value);
        }

        public double[] updateGradientInverseUnWeightedLogDensity(double[] gradient, double[] value, int from, int to) {
            double[] result = new double[to - from];
            int counter = 0;
            for (int i = from; i < to; i++) {
            	result[counter] = updateGradientInverseUnWeightedLogDensity(gradient[i], value[i]);
                counter++;
            }
            return result;
//            double[] result = value.clone();
//            for (int i = from; i < to; ++i) {
//                result[i] = updateGradientInverseUnWeightedLogDensity(gradient[i], value[i]);
//            }
//            return result;
        }

        public double[] updateGradientUnWeightedLogDensity(double[] gradient, double[] value, int from, int to) {
            double[] result = new double[to - from];
            int counter = 0;
            for (int i = from; i < to; i++) {
            	result[counter] = updateGradientUnWeightedLogDensity(gradient[i], value[i]);
                counter++;
            }
            return result;
//            double[] result = value.clone();
//            for (int i = from; i < to; ++i) {
//                result[i] = updateGradientUnWeightedLogDensity(gradient[i], value[i]);
//            }
//            return result;
        }

        public double[][] updateHessianLogDensity(double[][] hessian, double[][] transformationHessian, double[] gradient, double[] value, int from, int to) {

            final int dim = to - from;
            double[][] updatedHessian = new double[dim][dim];

            for (int i = 0; i < dim; i++) {
                for (int j = 0; j < dim; j++) {
                    if (i == j) updatedHessian[i][j] = updateDiagonalHessianLogDensity(hessian[i][j], gradient[i], value[i]);
                    else updatedHessian[i][j] = updateOffdiagonalHessianLogDensity(hessian[i][j], transformationHessian[i][j], gradient[i], gradient[j], value[i], value[j]);
                }
            }
            return updatedHessian;
        }

        protected abstract double getGradientLogJacobianInverse(double value); // takes transformed value

        public abstract double gradient(double value);

        @Override
        public double[] gradient(double[] values, int from, int to) {
            double[] result = new double[to - from];
            int counter = 0;
            for (int i = from; i < to; i++) {
            	result[counter] = gradient(values[i]);
                counter++;
            }
            return result;
//            double[] result = values.clone();
//            for (int i = from; i < to; ++i) {
//                result[i] = gradient(values[i]);
//            }
//            return result;
        }

        public abstract double getLogJacobian(double value);

        public double getLogJacobian(double[] values, int from, int to) {
            double sum = 0.0;
            for (int i = from; i < to; i++) {
                sum += getLogJacobian(values[i]);
            }
            return sum;
//            double sum = 0.0;
//            for (int i = from; i < to; ++i) {
//                sum += getLogJacobian(values[i]);
//            }
//            return sum;
        }

        public boolean isMultivariate() { return false;}
    }

    @Description("Transforms multiple parameters or trees")
    abstract class MultivariableTransform extends BEASTObject implements Transform {
    	final public Input<List<Function>> functionInput = new Input<>("f", "parameter to be transformed", new ArrayList<>());    	
    	
		@Override
		public void initAndValidate() {
			parameter = functionInput.get();			
		}

		   /* Utility for testing purposes
		    * The arguments are alternating input names and values,
		    * and values are assigned to the input with the particular name.
		    * For example myInitByName("kappa", 2.0, "lambda", true)
		    * assigns 2 to input kappa and true to input lambda.
		    * After assigning inputs, initAndValidate() is called.
		    */
		  public Object myInitByName(final Object... objects) {
		      if (objects.length % 2 == 1) {
		          throw new RuntimeException("Expected even number of arguments, name-value pairs");
		      }
		      for (int i = 0; i < objects.length; i += 2) {
		          if (objects[i] instanceof String) {
		              final String name = (String) objects[i];
		              setInputValue(name, objects[i + 1]);
		          } else {
		              throw new RuntimeException("Expected a String in " + i + "th argument ");
		          }
		      }
		      try {
		          validateInputs();
		      } catch (IllegalArgumentException ex) {
		          ex.printStackTrace();
		          throw new RuntimeException("validateInputs() failed! " + ex.getMessage());
		      }
		      try {
		          initAndValidate();
		      } catch (Exception e) {
		          e.printStackTrace();
		          throw new RuntimeException("initAndValidate() failed! " + e.getMessage());
		      }
		      return this;
		  } // myInitByName
		  
    	List<Function> parameter;
    	
        public List<Function> getF() {
			return parameter;
		}

		public void setF(Function parameter) {
			if (this.parameter == null) {
				this.parameter = new ArrayList<>();
			}
			if (parameter != null) {
				this.parameter.add(parameter);
			}
		}

		public void setParameter(List<Function> parameters) {
			if (this.parameter == null) {
				this.parameter = new ArrayList<>();
			}
			if (parameter != null) {
				this.parameter.addAll(parameters);
			}
		}

		public MultivariableTransform() {}
		
		MultivariableTransform(List<Function> parameter) {
    		setParameter(parameter);
    	}
    	
        public double transform(double value) {
            throw new RuntimeException("Transformation not permitted for this type of parameter, exiting ...");
        }

        public double inverse(double value) {
            throw new RuntimeException("Transformation not permitted for this type of parameter, exiting ...");
        }

        public double updateGradientLogDensity(double gradient, double value) {
            throw new RuntimeException("Transformation not permitted for this type of parameter, exiting ...");
        }

        public double updateDiagonalHessianLogDensity(double diagonalHessian, double gradient, double value) {
            throw new RuntimeException("Transformation not permitted for this type of parameter, exiting ...");
        }

        public double updateOffdiagonalHessianLogDensity(double offdiagonalHessian, double transformationHessian, double gradientI, double gradientJ, double valueI, double valueJ) {
            throw new RuntimeException("Transformation not permitted for this type of parameter, exiting ...");
        }

        public double updateGradientInverseUnWeightedLogDensity(double gradient, double value) {
            throw new RuntimeException("Transformation not permitted for this type of parameter, exiting ...");
        }

        public double updateGradientUnWeightedLogDensity(double gradient, double value) {
            throw new RuntimeException("Transformation not permitted for this type of parameter, exiting ...");
        }

        protected double getGradientLogJacobianInverse(double value) {
            throw new RuntimeException("Transformation not permitted for this type of parameter, exiting ...");
        }

        public double gradientInverse(double value) {
             throw new RuntimeException("Transformation not permitted for this type of parameter, exiting ...");
         }

        public double gradient(double value) {
            throw new RuntimeException("Transformation not permitted for this type of parameter, exiting ...");
        }

        public double getLogJacobian(double value) {
            throw new RuntimeException("Transformation not permitted for this type of parameter, exiting ...");
        }
    }

    @Description(value="Transforms multiple parameters or trees", isInheritable=false)
    abstract class MultivariableTransformWithParameter extends MultivariableTransform {
    	public MultivariableTransformWithParameter(List<Function> parameter) {
    		super(parameter);    		
    	}
        abstract public List<Function> getF();
    }

    @Description(value="Transforms multiple parameters or trees", isInheritable=false)
    abstract class MultivariateTransform extends MultivariableTransform {
        // A class for a multivariate transform

    	public MultivariateTransform() {
    		super(new ArrayList<>());
    	}
    	public MultivariateTransform(List<Function> parameter) {
    		super(parameter);    		
    	}
    	
        public double[] transform(double[] values) {
            return transform(values, 0, values.length);
        }

        public double[] inverse(double[] values) {
            return inverse(values, 0, values.length);
        }

        public double getLogJacobian(double[] values) {
            return getLogJacobian(values, 0, values.length);
        }

        public double[] updateGradientLogDensity(double[] gradient, double[] value, int from, int to) {
            // values = untransformed (R)
            double[] transformedValues = transform(value, 0, value.length);
            // Transform Inverse
            double[] updatedGradient = updateGradientInverseUnWeightedLogDensity(gradient, transformedValues, from, to);
            // gradient of log jacobian of the inverse
            double[] gradientLogJacobianInverse = getGradientLogJacobianInverse(transformedValues);
            // Add gradient log jacobian
            for (int i = 0; i < gradient.length; i++) {
                updatedGradient[i] += gradientLogJacobianInverse[i];
            }
            return updatedGradient;
        }

        public double[] updateDiagonalHessianLogDensity(double[] diagonalHessian, double[] gradient, double[] value, int from, int to) {
            throw new RuntimeException("Not yet implemented");
        }

        public double[][] updateHessianLogDensity(double[][] hessian, double[][] transformationHessian, double[] gradient, double[] value, int from, int to) {
            throw new RuntimeException("Not yet implemented");
        }

        public double[] updateGradientInverseUnWeightedLogDensity(double[] gradient, double[] value, int from, int to) {
            // takes transformed values
            // Jacobian of inverse (transpose)
            double[][] jacobianInverse = computeJacobianMatrixInverse(value);
            return updateGradientJacobian(gradient, jacobianInverse);
        }

        double[] updateGradientJacobian(double[] gradient, double[][] jacobianInverse) {
            // Matrix multiplication
            double[] updatedGradient = new double[gradient.length];
            for (int i = 0; i < gradient.length; i++) {
                for (int j = 0; j < gradient.length; j++) {
                    updatedGradient[i] += jacobianInverse[i][j] * gradient[j];
                }
            }
            return updatedGradient;
        }

        @Override
        public double[] updateGradientUnWeightedLogDensity(double[] gradient, double[] value, int from, int to) {
            // takes untransformed value TODO: more efficient way ?
            return updateGradientInverseUnWeightedLogDensity(gradient, transform(value, from, to), from, to);
        }

        abstract protected double[] getGradientLogJacobianInverse(double[] values); // transformed value

        abstract public double[][] computeJacobianMatrixInverse(double[] values); // transformed values

        public boolean isMultivariate() { return true;}
    }

    @Description(value="Log transform for univariables")
    public class LogTransform extends UnivariableTransform {
    	
    	public LogTransform() {    		
    		super(new ArrayList<>());
    	}
    	
    	public LogTransform(Function parameter) {
    		myInitByName("f", parameter);
    	}

    	@Override
    	public void initAndValidate() {
    		super.initAndValidate();
    		for (Function f : parameter) {
    			if (f instanceof RealParameter) {
    				RealParameter p  = (RealParameter) f;
    				if (p.getLower() < 0) {
    					Log.warning("\n\nWarning: parameter " + p.getID() + " has lower bound < 0, which is not appropriate for a LogTransform\n");
    				}
    			}
    			for (int i = 0; i < f.getDimension(); i++) {
    				if (f.getArrayValue(i) <= 0) {
    					Log.warning("\n\nWarning:" + f.getClass().getSimpleName() + " "
							+ (f instanceof BEASTInterface ? ((BEASTInterface)f).getID() : "") + 
							" has initial value <= 0, which is not appropriate for a LogTransform\n");
    					break;
    				}
    			}
    		}
    	}
    	
    	public LogTransform(List<Function> parameter) {
    		super(parameter);
    	}

    	public double transform(double value) {
            return Math.log(value);
        }

        public double inverse(double value) {
            return Math.exp(value);
        }

        public double gradientInverse(double value) { return Math.exp(value); }

        public double updateGradientLogDensity(double gradient, double value) {
            // gradient == gradient of inverse()
            // value == gradient of inverse() (value is untransformed)
            // 1.0 == gradient of log Jacobian of inverse()
            return gradient * value + 1.0;
        }

        protected double getGradientLogJacobianInverse(double value) {
            return 1.0;
        }

        @Override
        public double updateDiagonalHessianLogDensity(double diagonalHessian, double gradient, double value) {
            // value == inverse()
            // diagonalHessian == hessian of inverse()
            // gradient == gradient of inverse()
            return value * (gradient + value * diagonalHessian);
        }

        @Override
        public double updateOffdiagonalHessianLogDensity(double offdiagonalHessian, double transfomationHessian, double gradientI, double gradientJ, double valueI, double valueJ) {
            return offdiagonalHessian * valueI * valueJ + gradientJ * transfomationHessian;
        }

        @Override
        public double gradient(double value) {
            return value;
        }

        public String getTransformName() { return "log"; }

        public double getLogJacobian(double value) { return -Math.log(value); }

		@Override
		public int getMinDimensions() {
			return 1;
		}
		
    }

    @Description(value="Transform on parameters that sum to a fixed value (e.g. nucleotide frequencies)")
    public class LogConstrainedSumTransform extends MultivariableTransform {
    	public Input<Double> sumInput = new Input<>("sum", "sum of the items", 1.0);
    	public Input<Double[]> weightsInput = new Input<>("weights", "weights of individual items");
        private double fixedSum;
        private double [] weights;
        
//        public LogConstrainedSumTransform(@Param(name="parameter",description="parameter to be transformed") List<Function> parameter) {
//    		super(parameter);
//    		fixedSum = 1;
//        }
        public LogConstrainedSumTransform() {
    		super(new ArrayList<>());
    		myInitByName("sum", 1.0);
        }
        
//		public LogConstrainedSumTransform(@Param(name="f",description="parameter to be transformed") List<Function> parameter,
//        		@Param(name="sum",description="total sum of parameter values the parameter is constrained to", defaultValue="1.0") double fixedSum,
//        		@Param(name="weights",description="weight used for each dimension") double [] weights) {
		public LogConstrainedSumTransform(List<Function> parameter,
        		double fixedSum,
        		double [] weights) {
    		super(parameter);
            this.fixedSum = fixedSum;
            setWeights(weights);
        }

		public LogConstrainedSumTransform(Function parameter,
        		double fixedSum) {
			myInitByName("f", parameter, "sum", fixedSum);
        }
		
//		public LogConstrainedSumTransform(@Param(name="f",description="parameter to be transformed") List<Function> parameter,
//        		@Param(name="sum",description="total sum of parameter values the parameter is constrained to", defaultValue="1.0") double fixedSum) {
		public LogConstrainedSumTransform(List<Function> parameter,
        		 double fixedSum) {
    		super(parameter);
            this.fixedSum = fixedSum;
            setWeights(null);
        }

		@Override
		public void initAndValidate() {
			super.initAndValidate();
			fixedSum = sumInput.get();
			if (weightsInput.get() == null) {
				setWeights(null);
			} else {
				setWeights(Stream.of(weightsInput.get()).mapToDouble(Double::doubleValue).toArray());
			}
    		for (Function f : parameter) {
    			if (f instanceof RealParameter) {
    				RealParameter p  = (RealParameter) f;
    				if (p.getLower() < 0) {
    					Log.warning("\n\nWarning: parameter " + p.getID() + " has lower bound < 0, which is not appropriate for a LogConstrainedTransform\n");
    				}
    			}
    			for (int i = 0; i < f.getDimension(); i++) {
    				if (f.getArrayValue(i) <= 0) {
    					Log.warning("\n\nWarning:" + f.getClass().getSimpleName() + " "
							+ (f instanceof BEASTInterface ? ((BEASTInterface)f).getID() : "") + 
							" has initial value <= 0, which is not appropriate for a LogConstrainedTransform\n");
    					break;
    				}
    			}
	    	}		
	    }
		
		
        public double[] getWeights() {
			return weights;
		}

		public void setWeights(double[] weights) {
            if (weights == null || weights.length == 0 || weights[0] < 0) {
            	int dim = 0;
            	for (Function p : parameter) {
            		try {
            			// make sure the paramter is initialised
            			p.getArrayValue();
            		} catch (NullPointerException e) {
            			((BEASTObject)p).initAndValidate();
            		}
            		dim += p.getDimension();
            	}
            	this.weights = new double[dim];
            	Arrays.fill(this.weights, 1.0);
            } else {
            	this.weights = new double[weights.length];
            	double sum = 0;
            	for (double d : weights) {
            		sum += d;
            	}
            	int dim = weights.length;
            	for (int i = 0; i < dim; i++) {
            		this.weights[i] = dim * weights[i] / sum;
            	}
            }
        }


        public double getSum() {
            return this.fixedSum;
        }
        public void setSum(double sum) {
            this.fixedSum = sum;
        }        
        
        public double getConstrainedSum() {
            return this.fixedSum;
        }

        public double[] transform(double[] values, int from, int to) {
            double[] transformedValues = new double[to - from + 1];
            int counter = 0;
            for (int i = from; i <= to; i++) {
                transformedValues[counter] = Math.log(values[i]);
                counter++;
            }
            return transformedValues;
        }

        //inverse transformation assumes a sum of elements equal to the number of elements
        public double[] inverse(double[] values, int from, int to) {
            double sum = (double)(to - from + 1);
            double[] transformedValues = new double[to - from + 1];
            int counter = 0;
            double newSum = 0.0;
            for (int i = from; i <= to; i++) {
                transformedValues[counter] = Math.exp(values[i]);
                newSum += transformedValues[counter] * weights[i];
                counter++;
            }
            /*for (int i = 0; i < sum; i++) {
                transformedValues[i] = (transformedValues[i] / newSum) * sum;
            }*/
            for (int i = 0; i < transformedValues.length; i++) {
                transformedValues[i] = (transformedValues[i] / newSum) * sum;
            }
            return transformedValues;
        }

        //inverse transformation assumes a given sum provided as an argument
        public double[] inverse(double[] values, int from, int to, double sum) {
            //double sum = (double)(to - from + 1);
            double[] transformedValues = new double[to - from + 1];
            int counter = 0;
            double newSum = 0.0;
            for (int i = from; i <= to; i++) {
                transformedValues[counter] = Math.exp(values[i]);
                newSum += transformedValues[counter];
                counter++;
            }
            /*for (int i = 0; i < sum; i++) {
                transformedValues[i] = (transformedValues[i] / newSum) * sum;
            }*/
            for (int i = 0; i < transformedValues.length; i++) {
                transformedValues[i] = (transformedValues[i] / newSum) * sum;
            }
            return transformedValues;
        }

        public String getTransformName() {
            return "logConstrainedSum";
        }

        public double[] updateGradientLogDensity(double[] gradient, double[] value, int from, int to) {
            throw new RuntimeException("Not yet implemented");
        }

        public double[] updateGradientInverseUnWeightedLogDensity(double[] gradient, double[] value, int from, int to) {
            throw new RuntimeException("Not yet implemented");
        }

        public double[] updateGradientUnWeightedLogDensity(double[] gradient, double[] value, int from, int to) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public double[] updateDiagonalHessianLogDensity(double[] diagonalHessian, double[] gradient, double[] value, int from, int to) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public double[][] updateHessianLogDensity(double[][] hessian, double[][] transformationHessian, double[] gradient, double[] value, int from, int to) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public double updateOffdiagonalHessianLogDensity(double offdiagonalHessian, double transformationHessian, double gradientI, double gradientJ, double valueI, double valueJ) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public double gradient(double value) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public double[] gradient(double[] values, int from, int to) {
            throw new RuntimeException("Not yet implemented");
        }

        public double[] gradientInverse(double[] values, int from, int to) {
            throw new RuntimeException("Not yet implemented");
        }

        public double getLogJacobian(double[] values, int from, int to) {
            double sum = 0.0;
            for (int i = from; i <= to; i++) {
                sum -= Math.log(values[i]);
            }
            return sum;
        }

        public boolean isMultivariate() { return true;}

        public static void main(String[] args) {

            //specify starting values
            double[] startValues = {1.5, 0.6, 0.9};
            System.err.print("Starting values: ");
            double startSum = 0.0;
            for (double startValue : startValues) {
                System.err.print(startValue + " ");
                startSum += startValue;
            }
            System.err.println("\nSum = " + startSum);

            //perform transformation
            double[] transformedValues = LOG_CONSTRAINED_SUM.transform(startValues, 0, startValues.length-1);
            System.err.print("Transformed values: ");
            for (double transformedValue : transformedValues) {
                System.err.print(transformedValue + " ");
            }
            System.err.println();

            //add draw for normal distribution to transformed elements
            for (int i = 0; i < transformedValues.length; i++) {
                transformedValues[i] += 0.20 * Randomizer.nextDouble();
            }

            //perform inverse transformation
            transformedValues = LOG_CONSTRAINED_SUM.inverse(transformedValues, 0, transformedValues.length-1);
            System.err.print("New values: ");
            double endSum = 0.0;
            for (double transformedValue : transformedValues) {
                System.err.print(transformedValue + " ");
                endSum += transformedValue;
            }
            System.err.println("\nSum = " + endSum);

            if (startSum != endSum) {
                System.err.println("Starting and ending constraints differ!");
            }

        }

		@Override
		public int getMinDimensions() {
			return 2;
		}

    }

    @Description(value="Logit transform for univariables")
    public class LogitTransform extends UnivariableTransform {

    	public LogitTransform() {
    		super(new ArrayList<>());
    	}

        public LogitTransform(List<Function> parameter) {
    		super(parameter);
            //range = 1.0;
            //lower = 0.0;
        }

        public double transform(double value) {
            return Math.log(value / (1.0 - value));
        }

        public double inverse(double value) {
            return 1.0 / (1.0 + Math.exp(-value));
        }

        public double gradientInverse(double value) {
            throw new RuntimeException("Not yet implemented");
        }

        public double updateGradientLogDensity(double gradient, double value) {
            throw new RuntimeException("Not yet implemented");
        }

        protected double getGradientLogJacobianInverse(double value) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public double updateDiagonalHessianLogDensity(double diagonalHessian, double gradient, double value) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public double updateOffdiagonalHessianLogDensity(double offdiagonalHessian, double transformationHessian, double gradientI, double gradientJ, double valueI, double valueJ) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public double gradient(double value) {
            throw new RuntimeException("Not yet implemented");
        }

        public String getTransformName() {
            return "logit";
        }

        public double getLogJacobian(double value) {
            return -Math.log(1.0 - value) - Math.log(value);
        }

		@Override
		public int getMinDimensions() {
			return 1;
		}

//        private final double range;
//        private final double lower;
    }

    @Description(value="Fisher Z-transform for univariables")
    public class FisherZTransform extends UnivariableTransform {

    	public FisherZTransform() {
    		super(new ArrayList<>());
    	}
    	
    	public FisherZTransform(List<Function> parameter) {
    		super(parameter);
    	}
    	
        public double transform(double value) {
            return 0.5 * (Math.log(1.0 + value) - Math.log(1.0 - value));
        }

        public double inverse(double value) {
            return (Math.exp(2 * value) - 1) / (Math.exp(2 * value) + 1);
        }

        public double gradientInverse(double value) {
            return 1.0 - Math.pow(inverse(value), 2);
        }

        public double updateGradientLogDensity(double gradient, double value) {
            // 1 - value^2 : gradient of inverse (value is untransformed)
            // - 2*value : gradient of log jacobian of inverse
            return (1.0 - value * value) * gradient  - 2 * value;
        }

        protected double getGradientLogJacobianInverse(double value) {
            // - 2*value : gradient of log jacobian of inverse (value is transformed)
            return  - 2 * inverse(value);
        }

        @Override
        public double updateDiagonalHessianLogDensity(double diagonalHessian, double gradient, double value) {
            return (1.0 - value * value) * (diagonalHessian * (1.0 - value * value) - 2.0 * gradient * value - 2.0);
        }

        @Override
        public double updateOffdiagonalHessianLogDensity(double offdiagonalHessian, double transformationHessian, double gradientI, double gradientJ, double valueI, double valueJ) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public double gradient(double value) {
            return 1.0 - Math.pow(value, 2);
        }

        public String getTransformName() {
            return "fisherz";
        }

        public double getLogJacobian(double value) {
            return -Math.log(1 - value) - Math.log(1 + value);
        }

		@Override
		public int getMinDimensions() {
			return 1;
		}
    }

    @Description(value="Negate transform for univariables")
    public class NegateTransform extends UnivariableTransform {

    	public NegateTransform() {
    		super(new ArrayList<>());
    	}
    	
    	public NegateTransform(List<Function> parameter) {
    		super(parameter);
    	}
    	
        public double transform(double value) {
            return -value;
        }

        public double inverse(double value) {
            return -value;
        }

        public double updateGradientLogDensity(double gradient, double value) {
            // -1 == gradient of inverse()
            // 0.0 == gradient of log Jacobian of inverse()
            return -gradient;
        }

        protected double getGradientLogJacobianInverse(double value) {
            return 0.0;
        }

        @Override
        public double updateDiagonalHessianLogDensity(double diagonalHessian, double gradient, double value) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public double updateOffdiagonalHessianLogDensity(double offdiagonalHessian, double transformationHessian, double gradientI, double gradientJ, double valueI, double valueJ) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public double gradient(double value) {
            return -1.0;
        }

        public double gradientInverse(double value) { return -1.0; }

        public String getTransformName() {
            return "negate";
        }

        public double getLogJacobian(double value) {
            return 0.0;
        }

		@Override
		public int getMinDimensions() {
			return 1;
		}
    }

    @Description(value="Power transform for univariables")
    public class PowerTransform extends UnivariableTransform{
        private double power;

    	public PowerTransform() {
    		super(new ArrayList<>());
    	}

    	PowerTransform(List<Function> parameter) {
    		super(parameter);
            this.power = 2;
        }

//        PowerTransform(double power){
//            this.power = power;
//        }

        @Override
        public String getTransformName() {
            return "Power Transform";
        }

        @Override
        public double transform(double value) {
            return Math.pow(value, power);
        }

        @Override
        public double inverse(double value) {
            return Math.pow(value, 1 / power);
        }

        @Override
        public double gradientInverse(double value) {
            throw new RuntimeException("not implemented yet");
//            return 0;
        }

        @Override
        public double updateGradientLogDensity(double gradient, double value) {
            throw new RuntimeException("not implemented yet");
        }

        @Override
        public double updateDiagonalHessianLogDensity(double diagonalHessian, double gradient, double value) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public double updateOffdiagonalHessianLogDensity(double offdiagonalHessian, double transformationHessian, double gradientI, double gradientJ, double valueI, double valueJ) {
            throw new RuntimeException("Not yet implemented");
        }

        public double updateGradientInverseUnWeightedLogDensity(double gradient, double value) {
            throw new RuntimeException("not implemented yet");
        }

        @Override
        protected double getGradientLogJacobianInverse(double value) {
            throw new RuntimeException("not implemented yet");
        }

        @Override
        public double gradient(double value) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public double getLogJacobian(double value) {
            throw new RuntimeException("not implemented yet");
        }

		@Override
		public int getMinDimensions() {
			return 1;
		}
    }

    @Description(value="No transform for univariables = transform that leaves the variable unchanged")
    public class NoTransform extends UnivariableTransform {

    	public NoTransform() {
    		super(new ArrayList<>());
    	}

    	public NoTransform(List<Function> parameter) {
    		super(parameter);
    	}
    	
        public double transform(double value) {
            return value;
        }

        public double inverse(double value) {
            return value;
        }
        
//        @Override
//        public double[] inverse(double[] values, int from, int to, double sum) {
//        	return values;
//        }
        
        public double updateGradientLogDensity(double gradient, double value) {
            return gradient;
        }

        protected double getGradientLogJacobianInverse(double value) {
            return 0.0;
        }

        @Override
        public double updateDiagonalHessianLogDensity(double diagonalHessian, double gradient, double value) {
            return diagonalHessian;
        }

        @Override
        public double updateOffdiagonalHessianLogDensity(double offdiagonalHessian, double transformationHessian, double gradientI, double gradientJ, double valueI, double valueJ) {
            return offdiagonalHessian;
        }

        @Override
        public double gradient(double value) {
            return 1.0;
        }

        public double gradientInverse(double value) { return 1.0; }

        public String getTransformName() {
            return "none";
        }

        public double getLogJacobian(double value) {
            return 0.0;
        }

		@Override
		public int getMinDimensions() {
			return 1;
		}
    }

    @Description("Transform that leaves multi parameter the same")
    public class NoTransformMultivariable extends MultivariableTransform {

    	public NoTransformMultivariable() {
    		super(new ArrayList<>());
    	}

    	public NoTransformMultivariable(List<Function> parameter) {
    		super(parameter);    		
    	}
    	
        @Override
        public String getTransformName() {
            return "NoTransformMultivariate";
        }

        @Override
        public double[] transform(double[] values, int from, int to) {
            return subArray(values, from, to);
        }

        private double[] subArray(double[] values, int from, int to) {
            int length = to - from;
            if (length == values.length) return values;
            double[] result = new double[length];
            System.arraycopy(values, to, result, 0, length);
            return result;
        }

        @Override
        public double[] inverse(double[] values, int from, int to) {
            return subArray(values, from, to);
        }

        @Override
        public double[] updateGradientLogDensity(double[] gradient, double[] value, int from, int to) {
            return subArray(gradient, from, to);
        }

        @Override
        public double[] updateDiagonalHessianLogDensity(double[] diagonalHessian, double[] gradient, double[] value, int from, int to) {
            throw new RuntimeException("Not yet implemented.");
        }

        @Override
        public double[][] updateHessianLogDensity(double[][] hessian, double[][] transformationHessian, double[] gradient, double[] value, int from, int to) {
            throw new RuntimeException("Not yet implemented.");
        }

        @Override
        public double[] updateGradientInverseUnWeightedLogDensity(double[] gradient, double[] value, int from, int to) {
            throw new RuntimeException("Not yet implemented.");
        }

        @Override
        public double[] updateGradientUnWeightedLogDensity(double[] gradient, double[] value, int from, int to) {
            return subArray(gradient, from, to);
        }

        @Override
        public double[] inverse(double[] values, int from, int to, double sum) {
        	return values;
            //throw new RuntimeException("Not implemented.");
        }

        @Override
        public double[] gradient(double[] values, int from, int to) {
            return arrayValue(1.0, from, to);
        }

        private double[] arrayValue(double value, int from, int to) {
            int length = to - from;
            double[] result = new double[length];
            for (int i = 0; i < length; i++) {
                result[i] = value;
            }
            return result;
        }

        @Override
        public double[] gradientInverse(double[] values, int from, int to) {
            return arrayValue(1.0, from, to);
        }

        @Override
        public double getLogJacobian(double[] values, int from, int to) {
            return 0.0;
        }

        public boolean isMultivariate() { return false;}

		@Override
		public int getMinDimensions() {
			return 2;
		}
    }

    @Description(value="Composable transform: apply inner transforms first, then outer transform on the result")
    public class Compose extends UnivariableTransform  {

    	public Compose(UnivariableTransform outer, UnivariableTransform inner) {
        	super(null);
            this.outer = outer;
            this.inner = inner;
        }

        @Override
        public String getTransformName() {
            return "compose." + outer.getTransformName() + "." + inner.getTransformName();
        }

        @Override
        public double transform(double value) {
            final double outerValue = inner.transform(value);
            final double outerTransform = outer.transform(outerValue);

//            System.err.println(value + " " + outerValue + " " + outerTransform);
//            System.exit(-1);

            return outerTransform;
//            return outer.transform(inner.transform(value));
        }

        @Override
        public double inverse(double value) {
            return inner.inverse(outer.inverse(value));
        }

        @Override
        public double gradientInverse(double value) {
            return inner.gradientInverse(value) * outer.gradientInverse(inner.transform(value));
        }

        @Override
        public double updateGradientLogDensity(double gradient, double value) {
//            final double innerGradient = inner.updateGradientLogDensity(gradient, value);
//            final double outerValue = inner.transform(value);
//            final double outerGradient = outer.updateGradientLogDensity(innerGradient, outerValue);
//            return outerGradient;

            return outer.updateGradientLogDensity(inner.updateGradientLogDensity(gradient, value), inner.transform(value));
        }

        @Override
        public double updateDiagonalHessianLogDensity(double diagonalHessian, double gradient, double value) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public double updateOffdiagonalHessianLogDensity(double offdiagonalHessian, double transformationHessian, double gradientI, double gradientJ, double valueI, double valueJ) {
            throw new RuntimeException("Not yet implemented");
        }

        protected double getGradientLogJacobianInverse(double value) {
            throw new RuntimeException("not implemented yet");
        }

        @Override
        public double gradient(double value) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public double getLogJacobian(double value) {
            return inner.getLogJacobian(value) + outer.getLogJacobian(inner.transform(value));
        }

        private final UnivariableTransform outer;
        private final UnivariableTransform inner;
		@Override
		public int getMinDimensions() {
			return 1;
		}
    }

    @Description(value="Composes transform by applying outer transform to inner transform")
    public class ComposeMultivariable extends MultivariableTransform {

        public ComposeMultivariable(MultivariableTransform outer, MultivariableTransform inner) {
        	super(null);
            this.outer = outer;
            this.inner = inner;
        }

        @Override
        public String getTransformName() {
            return "compose." + outer.getTransformName() + "." + inner.getTransformName();
        }

        @Override
        public double[] transform(double[] values, int from, int to) {
            return outer.transform(inner.transform(values, from, to), from, to);
        }

        @Override
        public double[] inverse(double[] values, int from, int to) {
            return inner.inverse(outer.inverse(values, from, to), from, to);
        }

        @Override
        public double[] inverse(double[] values, int from, int to, double sum) {
            throw new RuntimeException("Not yet implemented.");
        }

        @Override
        public double[] updateGradientLogDensity(double[] gradient, double[] value, int from, int to) {
            return outer.updateGradientLogDensity(
                    inner.updateGradientLogDensity(gradient, value, from, to),
                    inner.transform(value, from, to),
                    from, to);
        }

        @Override
        public double[] updateDiagonalHessianLogDensity(double[] diagonalHessian, double[] gradient, double[] value, int from, int to) {

            return outer.updateDiagonalHessianLogDensity(
                    inner.updateDiagonalHessianLogDensity(diagonalHessian, gradient, value,from, to),
                    inner.updateGradientLogDensity(gradient, value, from, to),
                    inner.transform(value, from, to),
                    from, to);
        }

        @Override
        public double[][] updateHessianLogDensity(double[][] hessian, double[][] transformationHessian, double[] gradient, double[] value, int from, int to) {
            throw new RuntimeException("not implemented yet");
        }

        @Override
        public double[] updateGradientInverseUnWeightedLogDensity(double gradient[], double[] value, int from, int to) {
            return outer.updateGradientInverseUnWeightedLogDensity(
                    inner.updateGradientInverseUnWeightedLogDensity(gradient, outer.inverse(value, from, to), from, to),
                    value, from, to);
        }

        @Override
        public double[] updateGradientUnWeightedLogDensity(double[] gradient, double[] value, int from, int to) {
            return outer.updateGradientUnWeightedLogDensity(
                    inner.updateGradientUnWeightedLogDensity(gradient, value, from, to),
                    inner.transform(value, from, to), from, to);
        }

        @Override
        public double[] gradient(double[] values, int from, int to) {
            throw new RuntimeException("Not yet implemented.");
        }

        @Override
        public double[] gradientInverse(double[] values, int from, int to) {
            throw new RuntimeException("Not yet implemented.");
        }

        @Override
        public double getLogJacobian(double[] values, int from, int to) {
            return inner.getLogJacobian(values, from, to)
                    + outer.getLogJacobian(inner.transform(values, from, to), from, to);
        }

        public boolean isMultivariate() { return outer.isMultivariate() || inner.isMultivariate();}

        private final MultivariableTransform outer;
        private final MultivariableTransform inner;
		@Override
		public int getMinDimensions() {
			return 2;
		}
    }

    @Description(value="Inverse transform for univariables")
    public class Inverse extends UnivariableTransform {

        public Inverse(UnivariableTransform inner, List<Function> parameter) {
    		super(parameter);    	
            this.inner = inner;
        }

        @Override
        public String getTransformName() {
            return "inverse." + inner.getTransformName();
        }

        @Override
        public double transform(double value) {
            return inner.inverse(value);  // Purposefully switched

        }

        @Override
        public double updateGradientLogDensity(double gradient, double value) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public double updateDiagonalHessianLogDensity(double diagonalHessian, double gradient, double value) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public double updateOffdiagonalHessianLogDensity(double offdiagonalHessian, double transformationHessian, double gradientI, double gradientJ, double valueI, double valueJ) {
            throw new RuntimeException("Not yet implemented");
        }

        protected double getGradientLogJacobianInverse(double value) {
            throw new RuntimeException("not implemented yet");
        }

        @Override
        public double gradient(double value) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public double inverse(double value) {
            return inner.transform(value); // Purposefully switched
        }

        @Override
        public double gradientInverse(double value) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public double getLogJacobian(double value) {
            return -inner.getLogJacobian(inner.inverse(value));
        }

        private final UnivariableTransform inner;

		@Override
		public int getMinDimensions() {
			return 1;
		}
    }

    @Description(value="Inverse transforms multiple parameters or trees")
    public class InverseMultivariate extends MultivariateTransform {

        public InverseMultivariate(MultivariateTransform inner, List<Function> parameter) {
    		super(parameter);    	
            this.inner = inner;
        }

        @Override
        public String getTransformName() {
            return "inverse." + inner.getTransformName();
        }

        @Override
        public double[] transform(double[] values, int from, int to) {
            return inner.inverse(values, from, to); // Purposefully switched
        }

        @Override
        public double[] inverse(double[] values, int from, int to) {
            return inner.transform(values, from, to); // Purposefully switched
        }

        @Override
        public double[] updateDiagonalHessianLogDensity(double[] diagonalHessian, double[] gradient, double[] value, int from, int to) {
            throw new RuntimeException("not implemented yet.");
        }

        @Override
        public double[][] updateHessianLogDensity(double[][] hessian, double[][] transformationHessian, double[] gradient, double[] value, int from, int to) {
            throw new RuntimeException("not implemented yet");
        }

        @Override
        public double[] updateGradientInverseUnWeightedLogDensity(double[] gradient, double[] value, int from, int to) {
            throw new RuntimeException("not implemented yet");
        }

        public double[] inverse(double[] values, int from, int to, double sum) {
            throw new RuntimeException("Not relevant.");
        }

        @Override
        public double[] gradient(double[] values, int from, int to) {
            return inner.gradientInverse(values, from, to);
        }

        @Override
        public double[] gradientInverse(double[] values, int from, int to) {
            return inner.gradient(values, from, to);
        }

        @Override
        public double getLogJacobian(double[] values, int from, int to) {
            return -inner.getLogJacobian(inner.inverse(values, from, to), from, to);
        }

//        @Override
//        public double[] updateGradientLogDensity(double[] gradient, double[] value, int from, int to) {
//            throw new RuntimeException("Not yet implemented.");
//        }

//        @Override
//        public double[] updateGradientInverse(double[] gradient, double[] value, int from, int to) {
//            throw new RuntimeException("not implemented yet");
//        }

        @Override
        public double[] updateGradientLogDensity(double[] gradient, double[] transformedValues, int from, int to) {
            // transformedValues = transformed
            assert from == 0 && to == transformedValues.length : "The transform function can only be applied to the whole array of values.";
            // gradient of log jacobian of the inverse
            double[] gradientLogJacobianInverse = inner.getGradientLogJacobianInverse(transformedValues);
            // Add gradient log jacobian
            double[] updatedGradient = new double[gradient.length];
            for (int i = 0; i < gradient.length; i++) {
                updatedGradient[i] = gradient[i] - gradientLogJacobianInverse[i];
            }
            // Jacobian
            double[][] jacobian = computeJacobianMatrix(transformedValues);
            // Matrix Multiplication
            return updateGradientJacobian(updatedGradient, jacobian);
        }

        private double[][] computeJacobianMatrix(double[] transformedValues) {
            Matrix jacobianInverse = new Matrix(inner.computeJacobianMatrixInverse(transformedValues));
            return jacobianInverse.inverse().transpose().toComponents();
        }

        @Override
        public double[][] computeJacobianMatrixInverse(double[] values) {
            // values : untransformed
            Matrix jacobianInverse = new Matrix(inner.computeJacobianMatrixInverse(inner.transform(values)));
            return jacobianInverse.inverse().transpose().toComponents();
        }

        @Override
        protected double[] getGradientLogJacobianInverse(double[] transformedValues) {
            double[] gradient = inner.getGradientLogJacobianInverse(transformedValues);
            for (int i = 0; i < gradient.length; i++) {
                gradient[i] = - gradient[i];
            }
            return gradient;
        }

        private final MultivariateTransform inner;

		@Override
		public int getMinDimensions() {
			return 2;
		}
    }

    @Description(value="Log transform on difference between consecutive entries. Entries must be increasing in order.")
    public class PositiveOrdered extends MultivariateTransform {
        	
    	public PositiveOrdered(List<Function> parameter) {
    		super(parameter); 
		}
    	
    	@Override
        // x (positive ordered) -> y (unconstrained)
        public double[] transform(double[] values, int from, int to) {
            int dim = values.length;
            assert from == 0 && to == dim : "The transform function can only be applied to the whole array of values.";

            double[] result = new double[dim];
            result[0] = Math.log(values[0]);
            for (int i = 1; i < dim; i++) {
                result[i] = Math.log(values[i] - values[i-1]);
            }
            return result;
        }

        @Override
        public double[] updateGradientUnWeightedLogDensity(double[] gradient, double[] value, int from, int to) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public double[] inverse(double[] values, int from, int to) {
            int dim = values.length;
            assert from == 0 && to == dim : "The transform function can only be applied to the whole array of values.";

            double[] result = new double[dim];
            result[0] = Math.exp(values[0]);
            for (int i = 1; i < dim; i++) {
                result[i] = result[i-1] + Math.exp(values[i]);
            }
            return result;
        }

        @Override
        public double[] inverse(double[] values, int from, int to, double sum) {
            throw new RuntimeException("Not relevant.");
        }

        public String getTransformName() {
            return "PositiveOrdered";
        }

        @Override
        public double[] gradient(double[] values, int from, int to) {
            throw new RuntimeException("Not yet implemented.");
        }

        @Override
        public double[] gradientInverse(double[] values, int from, int to) {
            throw new RuntimeException("Not yet implemented.");
        }

        @Override
        public double getLogJacobian(double[] values, int from, int to) {
            int dim = values.length;
            assert from == 0 && to == dim : "The transform function can only be applied to the whole array of values.";

            double result = Math.log(values[0]);
            for (int i = 1; i < dim; i++) {
                result += Math.log(values[i] - values[i-1]);
            }
            return -result;
        }

        @Override
        public double[] getGradientLogJacobianInverse(double[] values) {
            int dim = values.length;
            double[] result = new double[dim];
            for (int i = 0; i < dim; i++) {
                result[i] = 1.0;
            }
            return result;
        }

        @Override
        // jacobian[j][i] = d x_i / d y_j
        public double[][] computeJacobianMatrixInverse(double[] values) {
            int dim = values.length;
            double[][] jacobian = new double[dim][dim];
            for (int i = 0; i < dim; i++) {
                for (int j = i; j < dim; j++) {
                    jacobian[j][i] = Math.exp(values[j]);
                }
            }
            return jacobian;
        }
		@Override
		public int getMinDimensions() {
			return 1;
		}


    }


    @Description(value="Applies list of transforms to individual dimensions of a parameter")
    public class Array extends MultivariableTransformWithParameter {

          private final List<Transform> array;
          //private final Function parameter;

          public Array(List<Transform> array, List<Function> parameter) {
      		  super(parameter); 
              //this.parameter = parameter;
              this.array = array;

//              if (parameter.getDimension() != array.size()) {
//                  throw new IllegalArgumentException("Dimension mismatch");
//              }
          }

          public List<Function> getF() { return parameter; }

          @Override
          public double[] transform(double[] values, int from, int to) {

              final double[] result = values.clone();

              for (int i = from; i < to; ++i) {
                  result[i] = array.get(i).transform(values[i]);
              }
              return result;
          }

          @Override
          public double[] inverse(double[] values, int from, int to) {

              final double[] result = values.clone();

              for (int i = from; i < to; ++i) {
                  result[i] = array.get(i).inverse(values[i]);
              }
              return result;
          }

          @Override
          public double[] inverse(double[] values, int from, int to, double sum) {
              throw new RuntimeException("Not yet implemented.");
          }

          @Override
          public double[] gradientInverse(double[] values, int from, int to) {

              final double[] result = values.clone();

              for (int i = from; i < to; ++i) {
                  result[i] = array.get(i).gradientInverse(values[i]);
              }
              return result;
          }

          @Override
          public double[] updateGradientLogDensity(double[] gradient, double[] values, int from, int to) {

              final double[] result = values.clone();

              for (int i = from; i < to; ++i) {
                  result[i] = array.get(i).updateGradientLogDensity(gradient[i], values[i]);
              }
              return result;
          }

        @Override
        public double[] updateDiagonalHessianLogDensity(double[] diagonalHessian, double[] gradient, double[] values, int from, int to) {
            final double[] result = values.clone();

            for (int i = from; i < to; ++i) {
                result[i] = array.get(i).updateDiagonalHessianLogDensity(diagonalHessian[i], gradient[i], values[i]);
            }
            return result;
        }

        public double[] updateGradientInverseUnWeightedLogDensity(double[] gradient, double[] values, int from, int to) {

            final double[] result = values.clone();

            for (int i = from; i < to; ++i) {
                result[i] = array.get(i).updateGradientInverseUnWeightedLogDensity(gradient[i], values[i]);
            }
            return result;
        }

        public double[] updateGradientUnWeightedLogDensity(double[] gradient, double[] values, int from, int to) {

            final double[] result = values.clone();

            for (int i = from; i < to; ++i) {
                result[i] = array.get(i).updateGradientUnWeightedLogDensity(gradient[i], values[i]);
            }
            return result;
        }

        @Override
        public double[][] updateHessianLogDensity(double[][] hessian, double[][] transformationHessian, double[] gradient, double[] value, int from, int to) {

            final int dim = to - from;
            double[][] updatedHessian = new double[dim][dim];

            for (int i = 0; i < dim; i++) {
                for (int j = 0; j < dim; j++) {
                    if (i == j) updatedHessian[i][j] = array.get(i).updateDiagonalHessianLogDensity(hessian[i][j], gradient[i], value[i]);
                    else {
                        assert(array.get(i).getClass().equals(array.get(j).getClass()));  // TODO: more generic implementation
                        updatedHessian[i][j] = array.get(i).updateOffdiagonalHessianLogDensity(hessian[i][j], transformationHessian[i][j], gradient[i], gradient[j], value[i], value[j]);
                    }
                }
            }
            return updatedHessian;
        }

        @Override
        public double[] gradient(double[] values, int from, int to) {

            final double[] result = values.clone();

            for (int i = from; i < to; ++i) {
                result[i] = array.get(i).gradient(values[i]);
            }
            return result;
        }

        @Override
          public String getTransformName() {
              return "array";
          }

          @Override
          public double getLogJacobian(double[] values, int from, int to) {

              double sum = 0.0;

              for (int i = from; i < to; ++i) {
                  sum += array.get(i).getLogJacobian(values[i]);
              }
              return sum;
          }

        public boolean isMultivariate() { return false;}

		@Override
		public int getMinDimensions() {
			return 1;
		}
    }
    
    @Description(value="Applies list of parsed transforms to segments (=contiguous subsets of dimensions?) of a parameter")
    public class Collection extends MultivariableTransformWithParameter {

        private final List<ParsedTransform> segments;
        //private final Function parameter;

        public Collection(List<ParsedTransform> segments, List<Function> parameter) {
        	super(parameter);
            // this.parameter = parameter;
            this.segments = ensureContiguous(segments);
        }

        public List<Function> getF() { return parameter; }

        private List<ParsedTransform> ensureContiguous(List<ParsedTransform> segments) {

            final List<ParsedTransform> contiguous = new ArrayList<ParsedTransform>();

            int current = 0;
            for (ParsedTransform segment : segments) {
                if (current < segment.start) {
                    contiguous.add(new ParsedTransform(NONE, current, segment.start));
                }
                contiguous.add(segment);
                current = segment.end;
            }
            
            int dim = 0;
            for (Function p : parameter) {
            	dim += p.getDimension();
            }            
            if (current < dim) {
                contiguous.add(new ParsedTransform(NONE, current, dim));
            }

//            System.err.println("Segments:");
//            for (ParsedTransform transform : contiguous) {
//                System.err.println(transform.transform.getTransformName() + " " + transform.start + " " + transform.end);
//            }
//            System.exit(-1);

            return contiguous;
        }

        @Override
        public double[] transform(double[] values, int from, int to) {

            final double[] result = values.clone();

            for (ParsedTransform segment : segments) {
                if (from < segment.end && to >= segment.start) {
                    final int begin = Math.max(segment.start, from);
                    final int end = Math.min(segment.end, to);
                    for (int i = begin; i < end; ++i) {
                        result[i] = segment.transform.transform(values[i]);
                    }
                }
            }
            return result;
        }

        @Override
        public double[] inverse(double[] values, int from, int to) {

            final double[] result = values.clone();

            for (ParsedTransform segment : segments) {
                if (from < segment.end && to >= segment.start) {
                    final int begin = Math.max(segment.start, from);
                    final int end = Math.min(segment.end, to);
                    for (int i = begin; i < end; ++i) {
                        result[i] = segment.transform.inverse(values[i]);
                    }
                }
            }
            return result;
        }

        @Override
        public double[] inverse(double[] values, int from, int to, double sum) {
            throw new RuntimeException("Not yet implemented.");
        }

        @Override
        public double[] gradientInverse(double[] values, int from, int to) {

            final double[] result = values.clone();

            for (ParsedTransform segment : segments) {
                if (from < segment.end && to >= segment.start) {
                    final int begin = Math.max(segment.start, from);
                    final int end = Math.min(segment.end, to);
                    for (int i = begin; i < end; ++i) {
                        result[i] = segment.transform.gradientInverse(values[i]);
                    }
                }
            }
            return result;
        }

        @Override
        public double[] updateGradientLogDensity(double[] gradient, double[] values, int from, int to) {

            final double[] result = values.clone();

            for (ParsedTransform segment : segments) {
                if (from < segment.end && to >= segment.start) {
                    final int begin = Math.max(segment.start, from);
                    final int end = Math.min(segment.end, to);
                    for (int i = begin; i < end; ++i) {
                        result[i] = segment.transform.updateGradientLogDensity(gradient[i], values[i]);
                    }
                }
            }
            return result;
        }

        @Override
        public double[] updateDiagonalHessianLogDensity(double[] diagonalHessian, double[] gradient, double[] values, int from, int to) {

            final double[] result = values.clone();

            for (ParsedTransform segment : segments) {
                if (from < segment.end && to >= segment.start) {
                    final int begin = Math.max(segment.start, from);
                    final int end = Math.min(segment.end, to);
                    for (int i = begin; i < end; ++i) {
                        result[i] = segment.transform.updateDiagonalHessianLogDensity(diagonalHessian[i], gradient[i], values[i]);
                    }
                }
            }
            return result;
        }

        public double[] updateGradientInverseUnWeightedLogDensity(double[] gradient, double[] values, int from, int to) {

            final double[] result = values.clone();

            for (ParsedTransform segment : segments) {
                if (from < segment.end && to >= segment.start) {
                    final int begin = Math.max(segment.start, from);
                    final int end = Math.min(segment.end, to);
                    for (int i = begin; i < end; ++i) {
                        result[i] = segment.transform.updateGradientInverseUnWeightedLogDensity(gradient[i], values[i]);
                    }
                }
            }
            return result;
        }

        @Override
        public double[] updateGradientUnWeightedLogDensity(double[] gradient, double[] values, int from, int to) {

            final double[] result = values.clone();

            for (ParsedTransform segment : segments) {
                if (from < segment.end && to >= segment.start) {
                    final int begin = Math.max(segment.start, from);
                    final int end = Math.min(segment.end, to);
                    for (int i = begin; i < end; ++i) {
                        result[i] = segment.transform.updateGradientUnWeightedLogDensity(gradient[i], values[i]);
                    }
                }
            }
            return result;
        }

        @Override
        public double[][] updateHessianLogDensity(double[][] hessian, double[][] transformationHessian, double[] gradient, double[] value, int from, int to) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public double[] gradient(double[] values, int from, int to) {

            final double[] result = values.clone();

            for (ParsedTransform segment : segments) {
                if (from < segment.end && to >= segment.start) {
                    final int begin = Math.max(segment.start, from);
                    final int end = Math.min(segment.end, to);
                    for (int i = begin; i < end; ++i) {
                        result[i] = segment.transform.gradient(values[i]);
                    }
                }
            }
            return result;
        }


        @Override
        public String getTransformName() {
            return "collection";
        }

        @Override
        public double getLogJacobian(double[] values, int from, int to) {

            double sum = 0.0;

            for (ParsedTransform segment : segments) {
                if (from < segment.end && to >= segment.start) {
                    final int begin = Math.max(segment.start, from);
                    final int end = Math.min(segment.end, to);
                    for (int i = begin; i < end; ++i) {
                        sum += segment.transform.getLogJacobian(values[i]);
                    }
                }
            }
//            System.err.println("Log: " + sum + " " + segments.size());
            return sum;
        }

        public boolean isMultivariate() { return false;}

		@Override
		public int getMinDimensions() {
			return 1;
		}

//        class Segment {
//
//            public Segment(Transform transform, int start, int end) {
//                this.transform = transform;
//                this.start = start;
//                this.end = end;
//            }
//            public Transform transform;
//            public int start;
//            public int end;
//        }
    }

    @Description("Helper class for Transform$Collection")
    public class ParsedTransform {
        public Transform transform;
        public int start; // zero-indexed
        public int end; // zero-indexed, i.e, i = start; i < end; ++i
        public int every = 1;
        public double fixedSum = 0.0;
        public List<Function> parameters = null;

        public ParsedTransform() {
            
        }

        public ParsedTransform(Transform transform, int start, int end) {
            this.transform = transform;
            this.start = start;
            this.end = end;
        }

        public ParsedTransform clone() {
            ParsedTransform clone = new ParsedTransform();
            clone.transform = transform;
            clone.start = start;
            clone.end = end;
            clone.every = every;
            clone.fixedSum = fixedSum;
            clone.parameters = parameters;
            return clone;
        }

        public boolean equivalent(ParsedTransform other) {
            if (start == other.start && end == other.end && every == other.every && parameters == other.parameters) {
                return true;
            } else {
                return false;
            }
        }
    }

    public class Util {
        public static Transform[] getListOfNoTransforms(int size) {
            Transform[] transforms = new Transform[size];
            for (int i = 0; i < size; ++i) {
                transforms[i] = NONE;
            }
            return transforms;
        }

//        public static Transform parseTransform(XMLObject xo) {
//            final Transform transform = (Transform) xo.getChild(Transform.class);
//            final Transform.ParsedTransform parsedTransform
//                    = (Transform.ParsedTransform) xo.getChild(Transform.ParsedTransform.class);
//            if (transform == null && parsedTransform != null) return parsedTransform.transform;
//            return transform;
//        }
    }

    NoTransform NONE = new NoTransform(null);
    LogTransform LOG = new LogTransform();
    NegateTransform NEGATE = new NegateTransform(null);
    Compose LOG_NEGATE = new Compose(new LogTransform(), new NegateTransform(null));
    LogConstrainedSumTransform LOG_CONSTRAINED_SUM = new LogConstrainedSumTransform();
    LogitTransform LOGIT = new LogitTransform(null);
    FisherZTransform FISHER_Z = new FisherZTransform(null);
    PositiveOrdered POSITIVE_ORDERED = new PositiveOrdered(null);

    enum Type {
        NONE("none", new NoTransform(null)),
        LOG("log", new LogTransform()),
        NEGATE("negate", new NegateTransform(null)),
        LOG_NEGATE("log-negate", new Compose(new LogTransform(), new NegateTransform(null))),
        LOG_CONSTRAINED_SUM("logConstrainedSum", new LogConstrainedSumTransform()),
        LOGIT("logit", new LogitTransform(null)),
        FISHER_Z("fisherZ",new FisherZTransform(null)),
        POWER("power", new PowerTransform(null)),
        POSITIVE_ORDERED("positiveOrdered",new PositiveOrdered(null));

        Type(String name, Transform transform) {
            this.name = name;
            this.transform = transform;
        }

        public Transform getTransform() {
            return transform;
        }

        public String getName() {
            return name;
        }

        private Transform transform;
        private String name;
    }
//    String TRANSFORM = "transform";
//    String TYPE = "type";
//    String START = "start";
//    String END = "end";
//    String EVERY = "every";
//    String INVERSE = "inverse";

}
