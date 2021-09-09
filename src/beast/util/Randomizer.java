/*
* File Randomizer.java
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
/*
 * MathUtils.java
 *
 * Copyright (C) 2002-2006 Alexei Drummond and Andrew Rambaut
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

package beast.util;


/**
 * Handy utility functions which have some Mathematical relevance.
 *
 * @author Matthew Goode
 * @author Alexei Drummond
 * @author Gerton Lunter
 * @version $Id: MathUtils.java,v 1.13 2006/08/31 14:57:24 rambaut Exp $
 */
public class Randomizer {
    private Randomizer() {
    }

    /**
     * A random number generator that is initialized with the clock when this
     * class is loaded into the JVM. Use this for all random numbers.
     * Note: This method or getting random numbers in not thread-safe. Since
     * MersenneTwisterFast is currently (as of 9/01) not synchronized using
     * this function may cause concurrency issues. Use the static get methods of the
     * MersenneTwisterFast class for access to a single instance of the class, that
     * has synchronization.
     */
    //private static final MersenneTwisterFast random = MersenneTwisterFast.DEFAULT_INSTANCE;
    final private static MersenneTwisterFast random = MersenneTwisterFast.DEFAULT_INSTANCE;

    /**
     * Chooses one category if a cumulative probability distribution is given
     * 
     * @param cf
     * @return 
     */
    public static int randomChoice(double[] cf) {

        double U = random.nextDouble();

        int s;
        if (U <= cf[0]) {
            s = 0;
        } else {
            for (s = 1; s < cf.length; s++) {
                if (U <= cf[s] && U > cf[s - 1]) {
                    break;
                }
            }
        }

        return s;
    }

    /**
     * Binary search to sample an integer given a cumulative probability distribution.
     * Modified from {@link java.util.Arrays#binarySearch(double[], double)}.
     * @param cpd  normalized cumulative probability distribution.
     * @return     a sample (index of <code>cpd[]</code>) according to CPD.
     *             Negative integer if something is wrong.
     */
    public static int binarySearchSampling(double[] cpd) {
        double U = random.nextDouble();

        if (U <= cpd[0])
            return 0;

        int mid,low = 0;
        int high = cpd.length - 1;
        double midVal;
        while (low <= high) {
            mid = (low + high) >>> 1;
            midVal = cpd[mid];

//            if (U <= cpd[mid] && U > cpd[mid - 1])
//                return mid; // take i where cpd[i - 1] < U <= cpd[i]

            if (midVal < U)
                low = mid + 1;
            else if (midVal > U) {
                if (cpd[mid - 1] < U)
                    return mid;
                high = mid - 1;
            } else
                return mid; // cpd == random

        }
        return -(low + 1);  // cpd not found.
    }

    /**
     * @param pdf array of unnormalized probabilities
     * @return a sample according to an unnormalized probability distribution
     */
    public static int randomChoicePDF(double[] pdf) {

        double U = random.nextDouble() * getTotal(pdf);
        for (int i = 0; i < pdf.length; i++) {

            U -= pdf[i];
            if (U < 0.0) {
                return i;
            }

        }
        for (int i = 0; i < pdf.length; i++) {
            System.err.println(i + "\t" + pdf[i]);
        }
        throw new Error("randomChoiceUnnormalized falls through -- negative components in input distribution?");
    }


    /**
     * @param array to normalize
     * @return a new double array where all the values sum to 1.
     *         Relative ratios are preserved.
     */
    public static double[] getNormalized(double[] array) {
        double[] newArray = new double[array.length];
        double total = getTotal(array);
        for (int i = 0; i < array.length; i++) {
            newArray[i] = array[i] / total;
        }
        return newArray;
    }


    /**
     * @param array entries to be summed
     * @param start start position
     * @param end   the index of the element after the last one to be included
     * @return the total of a the values in a range of an array
     */
    public static double getTotal(double[] array, int start, int end) {
        double total = 0.0;
        for (int i = start; i < end; i++) {
            total += array[i];
        }
        return total;
    }

    /**
     * @param array to sum over
     * @return the total of the values in an array
     */
    public static double getTotal(double[] array) {
        return getTotal(array, 0, array.length);

    }

    // ===================== (Synchronized) Static access methods to the private random instance ===========

    /**
     * Access a default instance of this class, access is synchronized
     * @return 
     */
    public static long getSeed() {
        synchronized (random) {
            return random.getSeed();
        }
    }

    /**
     * Access a default instance of this class, access is synchronized
     */
    public static void setSeed(long seed) {
        synchronized (random) {
            random.setSeed(seed);
        }
    }

    /**
     * Access a default instance of this class, access is synchronized
     */
    public static byte nextByte() {
        synchronized (random) {
            return random.nextByte();
        }
    }

    /**
     * Access a default instance of this class, access is synchronized
     */
    public static boolean nextBoolean() {
        synchronized (random) {
            return random.nextBoolean();
        }
    }

    /**
     * Access a default instance of this class, access is synchronized
     */
    public static void nextBytes(byte[] bs) {
        synchronized (random) {
            random.nextBytes(bs);
        }
    }

    /**
     * Access a default instance of this class, access is synchronized
     */
    public static char nextChar() {
        synchronized (random) {
            return random.nextChar();
        }
    }

    /**
     * Sample a double from a Gaussian distribution with zero mean
     * and unit variance.
     * 
     * @return sample
     */
    public static double nextGaussian() {
        synchronized (random) {
            return random.nextGaussian();
        }
    }

    /**
     * Sample a double from a Gamma distribution with a mean of
     * alpha/lambda and a variance of alpha/lambda^2.
     * Access a default instance of this class, access is synchronized.
     *
     * @param alpha
     * @param lambda
     * @return sample
     */
    public static double nextGamma(double alpha, double lambda) {
        synchronized (random) {
            return random.nextGamma(alpha, lambda);
        }
    }
    
    /**
     * Draw sample from a Poissonian distribution of mean lambda. Accesses
     * a default instance of this class, access is synchronized.
     * 
     * @param lambda mean of Poissonian distribution
     * @return sample (as double for historical reasons)
     */
    public static long nextPoisson(double lambda) {
        synchronized (random) {
            return random.nextPoisson(lambda);
        }
    }

    /**
     * Access a default instance of this class, access is synchronized
     *
     * @return a pseudo random double precision floating point number in [01)
     */
    public static double nextDouble() {
        synchronized (random) {
            return random.nextDouble();
        }
    }

    /**
     * @return log of random variable in [0,1]
     */
    public static double randomLogDouble() {
        return Math.log(nextDouble());
    }

    /**
     * Draw from an exponential distribution.  Accesses a default instance of
     * this class, access is synchronized.
     * 
     * @param lambda rate parameter (not mean) for the exponential
     * @return number drawn from distribution
     */
    public static double nextExponential(double lambda) {
        synchronized (random) {
            return -1.0 * Math.log(1 - random.nextDouble()) / lambda;
        }
    }

    /**
     * Draw from a geometric distribution with trial success probability p.
     * This method uses the form of the geometric distribution in which
     * the random variable represents the number of failures before success,
     * i.e. P(n) = (1-p)^n * p
     * Access a default instance of this class, access is synchronized.
     * 
     * @param p success probability of each Bernoulli trial
     * @return number drawn from distribution
     */
    public static long nextGeometric(double p) {
        synchronized (random) {
            double lambda = -Math.log(1.0-p);
            return Math.round(Math.floor(nextExponential(lambda)));
        }
    }

    /**
     * Samples a float uniformly from [0,1). Access a default
     * instance of this class, access is synchronized
     * 
     * @return sample
     */
    public static float nextFloat() {
        synchronized (random) {
            return random.nextFloat();
        }
    }

    /**
     * Samples a long int uniformly from between Long.MIN_VALUE
     * and Long.MAX_VALUE.
     * Access a default instance of this class, access is synchronized
     * 
     * @return sample
     */
    public static long nextLong() {
        synchronized (random) {
            return random.nextLong();
        }
    }

    
    /**
     * Samples a short int uniformly from between Short.MIN_VALUE
     * and Short.MAX_VALUE.
     * Access a default instance of this class, access is synchronized
     * 
     * @return sample
     */
    public static short nextShort() {
        synchronized (random) {
            return random.nextShort();
        }
    }

    /**
     * Samples an int uniformly from between Integer.MIN_VALUE
     * and Integer.MAX_VALUE.
     * Access a default instance of this class, access is synchronized
     * 
     * @return sample
     */
    public static int nextInt() {
        synchronized (random) {
            return random.nextInt();
        }
    }

    /**
     * Samples an int uniformly from between 0 and n-1.
     * Access a default instance of this class, access is synchronized
     * 
     * @param n
     * @return sample
     */
    public static int nextInt(int n) {
        synchronized (random) {
            return random.nextInt(n);
        }
    }

    /**
     * Samples from a log-normal distribution.
     *
     * @param M mu parameter of lognormal distribution
     * @param S sigma parameter of lognormal distribution
     * @param meanInRealspace if true, M is the real (not log) space mean of the distribution.
     * @return sample
     */
    public static double nextLogNormal(double M, double S, boolean meanInRealspace) {
        if(meanInRealspace)
            M = Math.log(M) - 0.5 * S * S;

        return Math.exp(S * nextGaussian() + M);
    }

    /**
     * Samples a double uniformly from between low and high.
     * 
     * @param low
     * @param high
     * @return sample
     */
    public static double uniform(double low, double high) {
        return low + nextDouble() * (high - low);
    }

    /**
     * Shuffles an array in place.
     * @param array
     */
    public static void shuffle(int[] array) {
        synchronized (random) {
            random.shuffle(array);
        }
    }

    /**
     * Shuffles an array in place. Shuffles numberOfShuffles times
     * @param array
     * @param numberOfShuffles
     */
    public static void shuffle(int[] array, int numberOfShuffles) {
        synchronized (random) {
            random.shuffle(array, numberOfShuffles);
        }
    }

    /**
     * Returns an array of shuffled indices of length l.
     *
     * @param l length of the array required.
     * @return array
     */
    public static int[] shuffled(int l) {
        synchronized (random) {
            return random.shuffled(l);
        }
    }


    /**
     * Returns an array of l ints sampled uniformly (with replacement)
     * from between 0 and l-1.
     * 
     * @param l length of array required
     * @return array
     */
    public static int[] sampleIndicesWithReplacement(int l) {
        synchronized (random) {
            int[] result = new int[l];
            for (int i = 0; i < l; i++)
                result[i] = random.nextInt(l);
            return result;
        }
    }

    /**
     * Permutes the elements of array in place.
     * 
     * @param array
     */
    public static void permute(int[] array) {
        synchronized (random) {
            random.permute(array);
        }
    }

    /**
     * Returns a uniform random permutation of 0,...,l-1
     *
     * @param l length of the array required.
     * @return array containing permuted indices
     */
    public static int[] permuted(int l) {
        synchronized (random) {
            return random.permuted(l);
        }
    }

    static int m_nIDNr = 0;

    static public int nextIDNr() {
        return m_nIDNr++;
    }

//
//    static public void storeToFile(String stateFile) {
//        // serialize random
//        try {
//            FileOutputStream fos = new FileOutputStream(stateFile + ".rstate");
//            ObjectOutputStream out = new ObjectOutputStream(fos);
//            out.writeObject(random);
//            out.close();
//        }
//        catch(IOException ex)
//        {
//            ex.printStackTrace();
//        }
//    }
//
//    static public void restoreFromFile(String stateFile) {
//        // unserialize random
//        try {
//            FileInputStream fis = new FileInputStream(stateFile + ".rstate");
//            ObjectInputStream in = new ObjectInputStream(fis);
//            random = (MersenneTwisterFast)in.readObject();
//            in.close();
//        }
//        catch(IOException ex)
//        {
//            ex.printStackTrace();
//        }
//        catch(ClassNotFoundException ex)
//        {
//            ex.printStackTrace();
//        }
//    }

}
