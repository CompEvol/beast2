package beast.base.inference.operator.kernel;

import beast.base.core.BEASTObject;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.util.Randomizer;

public interface KernelDistribution {
	public static Input<Integer> defaultInitialInput = new Input<>("defaultInitial", "Number of proposals skipped before learning about the val" , 500);
	public static Input<Integer> defaultBurninInput = new Input<>("defaultBurnin", "Number of proposals skipped before any learned information is applied" , 500);

	/**
	 * @param m determines shape of Bactrian distribution. m=0.95 is recommended
	 * @param scaleFactor determines range of scale values, larger is bigger random scale changes
	 * @return random scale factor for scaling parameters
	 */
	public double getScaler(int dim, double value, double scaleFactor);
	default public double getScaler(int dim, double windowSize) {
		return getScaler(dim, Double.NaN, windowSize);
	}

	/**
	 * @param dim number of dimensions
	 * @param windowSize determines range of random delta values, larger is bigger random updates
	 * @return random delta value for random walks
	 */
	public double getRandomDelta(int dim, double value, double windowSize);
	
	/**
	 * Do not use this method if there is learning process eg. mirror kernel
	 * Instead use getRandomDelta(dim, value, windowSize)
	 * @param dim number of dimensions
	 * @param windowSize
	 * @return
	 */
	default public double getRandomDelta(int dim, double windowSize) {
		return getRandomDelta(dim, Double.NaN, windowSize);
	}
		
	static KernelDistribution newDefaultKernelDistribution() {
		Bactrian kdist = new Bactrian();
		return kdist;
//		Mirror kdist = new Mirror();
//		kdist.initAndValidate();
//		return kdist;
	}
	

	@Description("Kernel distribution with two modes, so called Bactrian distribution")
	public class Bactrian extends BEASTObject implements KernelDistribution {
		public enum mode {
			uniform, 
			normal,
			laplace,
			t4, // t-distribution with 4 degrees of freedom
			cauchy,
			bactrian_normal, 
			bactrian_laplace,
			bactrian_triangle, 
			bactrian_uniform, 
			bactrian_t4, 
			bactrian_cauchy,
			bactrian_box, 
			bactrian_airplane, 
			bactrian_strawhat 
			};
		final public Input<mode> modeInput = new Input<>("mode", "selects the shape of the distribution", mode.bactrian_normal, mode.values());
		
	    final public Input<Double> windowSizeInput = new Input<>("m", "standard deviation for Bactrian distribution. "
	    		+ "Larger values give more peaked distributions. "
	    		+ "The default 0.95 is claimed to be a good choice (Yang 2014, book p.224).", 0.95);
	    
	    final public Input<Double> parameterAInput = new Input<>("a", "parameter for box, airplane and strawhat kernels, ignored otherwise", 0.0);
	    

	    
	    double m = 0.95;
	    double a = 0.0;
	    double b = 0.0;
	    
	    public mode kernelmode = mode.bactrian_normal;
	    
	    public Bactrian() {}  
	    public Bactrian(mode mode) {
	    	initByName("mode", mode);
	    }
	    public Bactrian(mode mode, double a) {
	    	initByName("mode", mode, "a", a);
	    }
	    public Bactrian(double m, mode mode) {
	    	initByName("mode", mode, "m", m);
	    }
	    
		@Override
		public void initAndValidate() {
			m = windowSizeInput.get();
	        if (m <=0 || m >= 1) {
	        	throw new IllegalArgumentException("m should be withing the (0,1) range");
	        }
	        a = parameterAInput.get();
	        kernelmode = modeInput.get();

	        switch (kernelmode) {
	        case bactrian_box:
				b = 0.5 * (Math.sqrt(12-3 * a * a) - a);
				break;
	        case bactrian_airplane: {
	        	// b is root of 4b^3−12b+6a−a^3=0
	        	// which according to https://www.wolframalpha.com/input/?i=4x%5E3%E2%88%9212x%2B6a%E2%88%92a%5E3%3D0
	        	// is below (the other two roots are imaginary)
	        	double a2 = a * a;
	        	double a3 = a2 * a;
	        	double a4 = a2 * a2;
	        	double a6 = a4 * a2;
	        	b = 0.5 * Math.pow(a3 + Math.sqrt(a6 - 12 * a4 + 36 * a2 - 64) - 6 * a,1.0/3.0) + 
					2/Math.pow(a3 + Math.sqrt(a6 - 12 * a4 + 36 * a2 - 64) - 6 * a,1.0/3.0);
	        } break;
	        case bactrian_strawhat: {
	        	// b is root of 5x^3−15x+10a−2a^3
	        	// which according to https://www.wolframalpha.com/input/?i=5x%5E3%E2%88%9215x%2B10a%E2%88%922a%5E3
	        	// is 
	        	// x = (a^3 + sqrt(a^6 - 10 a^4 + 25 a^2 - 25) - 5 a)^(1/3)/5^(1/3) + 5^(1/3)/(a^3 + sqrt(a^6 - 10 a^4 + 25 a^2 - 25) - 5 a)^(1/3)
	        	// again, the other two roots are imaginary
	        	double a2 = a * a;
	        	double a3 = a2 * a;
	        	double a4 = a2 * a2;
	        	double a6 = a4 * a2;
	        	double c = Math.pow(5.0, 1.0/3.0);
	        	b = Math.pow(a3 + Math.sqrt(a6 - 10 * a4 + 25 * a2 - 25) - 5 * a,1.0/3.0)/c + 
	        			c/Math.pow(a3 + Math.sqrt(a6 - 10 * a4 + 25 * a2 - 25) - 5 * a, 1.0/3.0);
	        	b = 1-a;
	        } break;
	        default:
	        	b = 0;
	        }
		}

		private double getRandomNumber() {
			switch (kernelmode) {
			case normal:
				return Randomizer.nextGaussian();
			case uniform:
				return Math.sqrt(12) * (Randomizer.nextDouble() - 0.5);
			case laplace:
			{
				double u = Randomizer.nextDouble() - 0.5;
				return Math.signum(u) * Math.log(1.0 - Math.abs(u * 2.0)) / Math.sqrt(2);
			}
			case t4:
				return (1.0/2.0)*(Randomizer.nextGaussian() + Randomizer.nextGaussian() + Randomizer.nextGaussian() + Randomizer.nextGaussian());
			case cauchy:
				return Math.tan(Math.PI * (Randomizer.nextDouble() - .5));
			case bactrian_box:
				double y = a + Randomizer.nextDouble() * (b-a);
				if (Randomizer.nextBoolean()) {
					return y;
				} else {
					return -y;
				}
//			case bactrian_airplane:
//				double u1 = Randomizer.nextDouble();
//				if (u1 < a / (2*b - a)) {
//					return a * Math.sqrt(Randomizer.nextDouble());
//				} else {
//					if (Randomizer.nextBoolean()) {
//						return a + Randomizer.nextDouble() * (b-a);
//					} else {
//						return -a - Randomizer.nextDouble() * (b-a);
//					}
//				}
//			case bactrian_strawhat:
//				double u2 = Randomizer.nextDouble();
//				if (u2 < a / (3*b - 2*a)) {
//					return a * Math.pow(Randomizer.nextDouble(), 1.0/3.0);
//				} else {
//					if (Randomizer.nextBoolean()) {
//						return a + Randomizer.nextDouble() * (b-a);
//					} else {
//						return -a - Randomizer.nextDouble() * (b-a);
//					}
//				}
			case bactrian_airplane:
				double u1 = Randomizer.nextDouble();
				if (u1 < a ) {
					if (Randomizer.nextBoolean()) {
						return a * Math.sqrt(Randomizer.nextDouble());
					} else {
						return -a * Math.sqrt(Randomizer.nextDouble());
					}
				} else {
					if (Randomizer.nextBoolean()) {
						return a + Randomizer.nextDouble() * (1-a);
					} else {
						return -a - Randomizer.nextDouble() * (1-a);
					}
				}
			case bactrian_strawhat:
				double u2 = Randomizer.nextDouble();
				if (u2 < a / (3*b - 2*a)) {
					if (Randomizer.nextBoolean()) {
						return a * Math.pow(Randomizer.nextDouble(), 1.0/3.0);
					} else {
						return -a * Math.pow(Randomizer.nextDouble(), 1.0/3.0);
					}
				} else {
					if (Randomizer.nextBoolean()) {
						return a + Randomizer.nextDouble() * (b-a);
					} else {
						return -a - Randomizer.nextDouble() * (b-a);
					}
				}
			default:
				// it is one of the Bactrian distributions
				// i.e. two-modal, symmetric with mean 0
		        if (Randomizer.nextBoolean()) {
		        	return m + getBactrianRandomNumber() * Math.sqrt(1-m*m);
		        } else {
		        	return -m + getBactrianRandomNumber() * Math.sqrt(1-m*m);
		        }
			}
		}
		
		private double getBactrianRandomNumber() {
			switch (kernelmode) {
			case bactrian_normal:
				return Randomizer.nextGaussian();
			case bactrian_laplace:
			{
				double u = Randomizer.nextDouble() - 0.5;
				return Math.signum(u) * Math.log(1.0 - Math.abs(u * 2.0)) / Math.sqrt(2);
			}
			case bactrian_triangle:
			{
				double u = Randomizer.nextDouble();
				if (u < 0.5) {
					return (-Math.sqrt(6) + 2 * Math.sqrt(3 * u));
				} else {
					return (Math.sqrt(6) - 2 * Math.sqrt(3 * (1-u)));
				}
			}
			case bactrian_uniform:
				return (Randomizer.nextDouble() - 0.5) * Math.sqrt(12);
			case bactrian_t4:
				return (1.0/2.0)*(Randomizer.nextGaussian() + Randomizer.nextGaussian() + Randomizer.nextGaussian() + Randomizer.nextGaussian());
			case bactrian_cauchy:
				return Math.tan(Math.PI * (Randomizer.nextDouble() - .5));
			default:
				return Double.NaN;
			}
		}
		
		@Override
		public double getScaler(int dim, double oldValue, double scaleFactor) {
	        double scale = 0;
	        scale = scaleFactor * getRandomNumber();
	        scale = Math.exp(scale);
			return scale;
		}
		
		@Override
		public double getRandomDelta(int dim, double oldValue, double windowSize) {
	        double value;
	        value = windowSize * getRandomNumber();
	        return value;
		}	
		
	}
	
	@Description("Distribution that learns mean m and variance s from the values provided."
			+ "Uses Bactrian kernel while in the process of learning")
	public class Mirror extends Bactrian {
		final public Input<Boolean> onePerDimensionInput = new Input<>("onePerDimension", 
				"whether to keep track which dimension of the parameter is affected for learning the m and s of its distribtion", false); 
		
		final public Input<Integer> initialInput = new Input<>("initial", "Number of proposals before m and s are considered in proposal. "
				+ "Must be larger than burnin, if specified. "
				+ "If not specified (or < 0), the operator uses " + defaultInitialInput.get(), -1); 
		final public Input<Integer> burninInput = new Input<>("burnin", "Number of proposals that are ignored before m and s are being updated. "
				+ "If initial is not specified, uses half the default initial value (which equals " + defaultBurninInput.get() + ")", 0);
		
		int callcount;
		int initial, burnin;
		double estimatedMean, estimatedSD;
		boolean onePerDimension;
		double [] estimatedMeans;
		double [] estimatedSDs;
		int [] callcounts;

		public Mirror() {}
		public Mirror(mode mode) {
	    	initByName("mode", mode);
	    }
	    
		@Override
		public void initAndValidate() {
			onePerDimension = onePerDimensionInput.get();
			if (onePerDimension) {
				estimatedMeans = new double[1];
				estimatedSDs = new double[1];
				callcounts = new int[1];
			}
			callcount = 0;
			initial = initialInput.get();
			if (initial < 0) {
				initial = defaultInitialInput.get();
			}
			burnin = burninInput.get();
			if (burnin <= 0) {
				burnin = defaultBurninInput.get();
			}
			estimatedMean = 0; estimatedSD = 0;	
		}
				
		@Override
		public double getScaler(int dim, double value, double scaleFactor) {
			double logValue = Math.log(value);
			if (onePerDimension) {
				if (dim >= callcounts.length) {
					int [] tmp = new int[dim+1];
					System.arraycopy(callcounts, 0, tmp, 0, callcounts.length);
					callcounts = tmp;
					double [] tmp2 = new double[dim+1];
					System.arraycopy(estimatedMeans, 0, tmp2, 0, estimatedMeans.length);
					estimatedMeans = tmp2;
					tmp2 = new double[dim+1];
					System.arraycopy(estimatedSDs, 0, tmp2, 0, estimatedSDs.length);
					estimatedSDs = tmp2;
				}
				callcounts[dim]++;
				if (callcounts[dim] > initial) {
					double prevMean = estimatedMeans[dim];
					double n = callcounts[dim] - initial;
					estimatedMeans[dim] = logValue / n + prevMean * (n - 1.0) / n;
					
				    double ssq = estimatedSDs[dim] * estimatedSDs[dim];
				    estimatedSDs[dim] = Math.sqrt(ssq + (((logValue - prevMean) * (logValue - estimatedMeans[dim])) - ssq) / n);
				    estimatedMean = estimatedMeans[dim];
				    estimatedSD = estimatedSDs[dim];
				    callcount = callcounts[dim];
				}
			} else {
				callcount++;
				if (callcount > initial) {
					double prevMean = estimatedMean;
					double n = callcount - initial;
					estimatedMean = logValue / n + prevMean * (n - 1.0) / n;
					
				    double ssq = estimatedSD * estimatedSD;
				    estimatedSD = Math.sqrt(ssq + (((logValue - prevMean) * (logValue - estimatedMean)) - ssq) / n);
				}
			}
			if (Double.isNaN(value) || callcount < initial + burnin) {
				return super.getScaler(dim, value, scaleFactor);
			}
			if (callcount == initial + burnin) {
				Log.warning("kick in ");
			}
			
			double delta = scaleFactor * Randomizer.nextGaussian() * estimatedSD;
			
			double mean = 2 * estimatedMean - value;
			double newValue = mean + delta;
			// double mean2 = 2 * estimatedMean - newValue;
			double scale = -logValue + newValue;
//			logHR = //- logDensity(mean, scaleFactor * estimatedSD, newValue) // these terms cancel each other 
//					//+ logDensity(mean2, scaleFactor * estimatedSD, value)
//					+ scale;
			
			return Math.exp(scale);
		}
		
		@Override
		public double getRandomDelta(int dim, double value, double windowSize) {
			if (onePerDimension) {
				if (dim >= callcounts.length) {
					int [] tmp = new int[dim+1];
					System.arraycopy(callcounts, 0, tmp, 0, callcounts.length);
					callcounts = tmp;
					double [] tmp2 = new double[dim+1];
					System.arraycopy(estimatedMeans, 0, tmp2, 0, estimatedMeans.length);
					estimatedMeans = tmp2;
					tmp2 = new double[dim+1];
					System.arraycopy(estimatedSDs, 0, tmp2, 0, estimatedSDs.length);
					estimatedSDs = tmp2;
				}
				callcounts[dim]++;
				if (callcounts[dim] > initial) {
					double prevMean = estimatedMeans[dim];
					double n = callcounts[dim] - initial;
					estimatedMeans[dim] = value / n + prevMean * (n - 1.0) / n;
					
				    double ssq = estimatedSDs[dim] * estimatedSDs[dim];
				    estimatedSDs[dim] = Math.sqrt(ssq + ((value - prevMean) * (value - estimatedMeans[dim]) - ssq) / n);

				    estimatedMean = estimatedMeans[dim];
				    estimatedSD = estimatedSDs[dim];
				    callcount = callcounts[dim];
				}
			} else {
				callcount++;
				if (callcount > initial) {
					double prevMean = estimatedMean;
					double n = callcount - initial;
					estimatedMean = value / n + prevMean * (n - 1.0) / n;
					
				    double ssq = estimatedSD * estimatedSD;
				    estimatedSD = Math.sqrt(ssq + ((value - prevMean) * (value - estimatedMean) - ssq) / n);
				}
			}
			if (Double.isNaN(value) || callcount < initial + burnin) {
				return super.getRandomDelta(dim, value, windowSize);
			}
			
			double delta = windowSize * Randomizer.nextGaussian() * estimatedSD;
			
			double mean = 2 * estimatedMean - value;
			double newValue = mean + delta;
			//double mean2 = 2 * estimatedMean - newValue;
			//logHR = + logDensity(mean, windowSize * estimatedSD, newValue) // these terms cancel each other
			//		- logDensity(mean2, windowSize * estimatedSD, value);
			//logHR = 0;
			
			return newValue - value;
		} 
		
	}
	
}
