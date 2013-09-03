/*
* File MersenneTwisterFast.java
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
 * MersenneTwisterFast.java
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

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.Serializable;

import beast.math.GammaFunction;
import beast.math.statistic.DiscreteStatistics;



/**
 * MersenneTwisterFast:
 * <p/>
 * A simulation quality fast random number generator (MT19937)
 * with the  same public methods as java.beast.util.Random.
 * <p/>
 * <p>About the Mersenne Twister.
 * This is a Java version of the C-program for MT19937: Integer version.
 * next(32) generates one pseudorandom unsigned integer (32bit)
 * which is uniformly distributed among 0 to 2^32-1  for each
 * call.  next(int bits) >>>'s by (32-bits) to get a value ranging
 * between 0 and 2^bits-1 long inclusive; hope that's correct.
 * setSeed(seed) set initial values to the working area
 * of 624 words. For setSeed(seed), seed is any 32-bit integer
 * except for 0.
 * <p/>
 * Reference.
 * M. Matsumoto and T. Nishimura,
 * "Mersenne Twister: A 623-Dimensionally Equidistributed Uniform
 * Pseudo-Random Number Generator",
 * <i>ACM Transactions on Modeling and Computer Simulation,</i>
 * Vol. 8, No. 1, January 1998, pp 3--30.
 * <p/>
 * <p>Bug Fixes. This implementation implements the bug fixes made
 * in Java 1.2's version of Random, which means it can be used with
 * earlier versions of Java.  See
 * <a href="http://www.javasoft.com/products/jdk/1.2/docs/api/java/beast.util/Random.html">
 * the JDK 1.2 java.beast.util.Random documentation</a> for further documentation
 * on the random-number generation contracts made.  Additionally, there's
 * an undocumented bug in the JDK java.beast.util.Random.nextBytes() method,
 * which this code fixes.
 * <p/>
 * <p> Important Note.  Just like java.beast.util.Random, this
 * generator accepts a long seed but doesn't use all of it.  java.beast.util.Random
 * uses 48 bits.  The Mersenne Twister instead uses 32 bits (int size).
 * So it's best if your seed does not exceed the int range.
 * <p/>
 * <p><a href="http://www.cs.umd.edu/users/seanl/">Sean Luke's web page</a>
 * <p/>
 * <p/>
 * - added shuffling method (Alexei Drummond)
 * <p/>
 * - added gamma RV method (Marc Suchard)
 * <p/>
 * - thread safety (Remco Bouckaert)
 * <p/>
 * This is now package private - it should be accessed using the instance in Randomizer
 */
public class MersenneTwisterFast implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 6185086957226269797L;
    // Period parameters
    private static final int N = 624;
    private static final int M = 397;
    private static final int MATRIX_A = 0x9908b0df;   //  private static final * constant vector a
    private static final int UPPER_MASK = 0x80000000; // most significant w-r bits
    private static final int LOWER_MASK = 0x7fffffff; // least significant r bits


    // Tempering parameters
    private static final int TEMPERING_MASK_B = 0x9d2c5680;
    private static final int TEMPERING_MASK_C = 0xefc60000;

    // #define TEMPERING_SHIFT_U(y)  (y >>> 11)
    // #define TEMPERING_SHIFT_S(y)  (y << 7)
    // #define TEMPERING_SHIFT_T(y)  (y << 15)
    // #define TEMPERING_SHIFT_L(y)  (y >>> 18)

    private int mt[]; // the array for the state vector
    private int mti; // mti==N+1 means mt[N] is not initialized
    private int mag01[];

    // a good initial seed (of int size, though stored in a long)
    private static final long GOOD_SEED = 4357;

    private double nextNextGaussian;
    private boolean haveNextNextGaussian;

    // The following can be accessed externally by the static accessor methods which
    // inforce synchronization
    public static final MersenneTwisterFast DEFAULT_INSTANCE = new MersenneTwisterFast();

    // Added to curernt time in default constructor, and then adjust to allow for programs that construct
    // multiple MersenneTwisterFast in a short amount of time.
    private static long seedAdditive_ = 0;

    private long initializationSeed;

    /**
     * Constructor using the time of day as default seed.
     */
    private MersenneTwisterFast() {
        this(System.currentTimeMillis() + seedAdditive_);
        seedAdditive_ += nextInt();
    }

    /**
     * Constructor using a given seed.  Though you pass this seed in
     * as a long, it's best to make sure it's actually an integer.
     *
     * @param seed generator starting number, often the time of day.
     */
    private MersenneTwisterFast(long seed) {
        if (seed == 0) {
            setSeed(GOOD_SEED);
        } else {
            setSeed(seed);
        }
    }


    /**
     * Initalize the pseudo random number generator.
     * The Mersenne Twister only uses an integer for its seed;
     * It's best that you don't pass in a long that's bigger
     * than an int.
     *
     * @param seed from constructor
     */
    public final void setSeed(long seed) {
        if (seed == 0) {
            throw new IllegalArgumentException("Non zero random seed required.");
        }
        initializationSeed = seed;
        haveNextNextGaussian = false;

        mt = new int[N];

        // setting initial seeds to mt[N] using
        // the generator Line 25 of Table 1 in
        // [KNUTH 1981, The Art of Computer Programming
        //    Vol. 2 (2nd Ed.), pp102]

        // the 0xffffffff is commented out because in Java
        // ints are always 32 bits; hence i & 0xffffffff == i

        mt[0] = ((int) seed); // & 0xffffffff;

        for (mti = 1; mti < N; mti++)
            mt[mti] = (69069 * mt[mti - 1]); //& 0xffffffff;

        // mag01[x] = x * MATRIX_A  for x=0,1
        mag01 = new int[2];
        mag01[0] = 0x0;
        mag01[1] = MATRIX_A;
    }

    public final long getSeed() {
        return initializationSeed;
    }

    /**
     * grabbing the next int should be synchronized,
     * If 2 threads request, say, a double and a gaussian at the same time,
     * and mti = N - 1, the condition (mit >= N) is false for both threads,
     * but the first increase mti before the second results in an out-of-bounds
     * exception when 'return mt[mti++] is called.
     * <p/>
     * To prevent this, this part of the code should be synchronized
     */
    synchronized private int next() {
        int y;
        if (mti >= N)   // generate N words at one time
        {
            int kk;

            for (kk = 0; kk < N - M; kk++) {
                y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
                mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
            }
            for (; kk < N - 1; kk++) {
                y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
                mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
            }
            y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
            mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

            mti = 0;
        }

        return mt[mti++];
    }

    public final int nextInt() {
        int y;

        y = next();
        y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
        y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
        y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
        y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

        return y;
    }


    public final short nextShort() {
        int y;

        y = next();
        y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
        y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
        y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
        y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

        return (short) (y >>> 16);
    }


    public final char nextChar() {
        int y;

        y = next();
        y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
        y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
        y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
        y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

        return (char) (y >>> 16);
    }


    public final boolean nextBoolean() {
        int y;

        y = next();
        y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
        y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
        y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
        y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

        return ((y >>> 31) != 0);
    }


    public final byte nextByte() {
        int y;

        y = next();
        y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
        y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
        y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
        y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

        return (byte) (y >>> 24);
    }


    public final void nextBytes(byte[] bytes) {
        int y;

        for (int x = 0; x < bytes.length; x++) {
            y = next();
            y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
            y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
            y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
            y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

            bytes[x] = (byte) (y >>> 24);
        }
    }


    public final long nextLong() {
        int y;
        int z;

        y = next();
        y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
        y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
        y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
        y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

        z = next();
        z ^= z >>> 11;                          // TEMPERING_SHIFT_U(z)
        z ^= (z << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(z)
        z ^= (z << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(z)
        z ^= (z >>> 18);                        // TEMPERING_SHIFT_L(z)

        return (((long) y) << 32) + (long) z;
    }


    public final double nextDouble() {
        int y;
        int z;

        y = next();
        y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
        y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
        y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
        y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

        z = next();
        z ^= z >>> 11;                          // TEMPERING_SHIFT_U(z)
        z ^= (z << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(z)
        z ^= (z << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(z)
        z ^= (z >>> 18);                        // TEMPERING_SHIFT_L(z)

        /* derived from nextDouble documentation in jdk 1.2 docs, see top */
        return ((((long) (y >>> 6)) << 27) + (z >>> 5)) / (double) (1L << 53);
    }

    public final double nextGaussian() {
        if (haveNextNextGaussian) {
            haveNextNextGaussian = false;
            return nextNextGaussian;
        } else {
            double v1, v2, s;
            do {
                int y;
                int z;
                int a;
                int b;

                y = next();
                y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
                y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
                y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
                y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

                z = next();
                z ^= z >>> 11;                          // TEMPERING_SHIFT_U(z)
                z ^= (z << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(z)
                z ^= (z << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(z)
                z ^= (z >>> 18);                        // TEMPERING_SHIFT_L(z)

                a = next();
                a ^= a >>> 11;                          // TEMPERING_SHIFT_U(a)
                a ^= (a << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(a)
                a ^= (a << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(a)
                a ^= (a >>> 18);                        // TEMPERING_SHIFT_L(a)

                b = next();
                b ^= b >>> 11;                          // TEMPERING_SHIFT_U(b)
                b ^= (b << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(b)
                b ^= (b << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(b)
                b ^= (b >>> 18);                        // TEMPERING_SHIFT_L(b)

                /* derived from nextDouble documentation in jdk 1.2 docs, see top */
                v1 = 2 *
                        (((((long) (y >>> 6)) << 27) + (z >>> 5)) / (double) (1L << 53))
                        - 1;
                v2 = 2 * (((((long) (a >>> 6)) << 27) + (b >>> 5)) / (double) (1L << 53))
                        - 1;
                s = v1 * v1 + v2 * v2;
            } while (s >= 1);
            double multiplier = Math.sqrt(-2 * Math.log(s) / s);
            nextNextGaussian = v2 * multiplier;
            haveNextNextGaussian = true;
            return v1 * multiplier;
        }
    }

    public final float nextFloat() {
        int y;

        y = next();
        y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
        y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
        y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
        y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

        return (y >>> 8) / ((float) (1 << 24));
    }


    /**
     * Returns an integer drawn uniformly from 0 to n-1.  Suffice it to say,
     * n must be > 0, or an IllegalArgumentException is raised.
     */
    public int nextInt(int n) {
        if (n <= 0)
            throw new IllegalArgumentException("n must be positive");

        if ((n & -n) == n)  // i.e., n is a power of 2
        {
            int y;

            y = next();
            y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
            y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
            y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
            y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

            return (int) ((n * (long) (y >>> 1)) >> 31);
        }

        int bits, val;
        do {
            int y;

            y = next();
            y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
            y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
            y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
            y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

            bits = (y >>> 1);
            val = bits % n;
        } while (bits - val + (n - 1) < 0);
        return val;
    }

    /**
     * Returns a uniform random permutation of int objects in array
     */
    public final void permute(int[] array) {
        int l = array.length;
        for (int i = 0; i < l; i++) {
            int index = nextInt(l - i) + i;
            int temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }


    /**
     * Shuffles an array.
     */
    public final void shuffle(int[] array) {
        int l = array.length;
        for (int i = 0; i < l; i++) {
            int index = nextInt(l - i) + i;
            int temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }

    /**
     * Shuffles an array. Shuffles numberOfShuffles times
     */
    public final void shuffle(int[] array, int numberOfShuffles) {
        int i, j, temp, l = array.length;
        for (int shuffle = 0; shuffle < numberOfShuffles; shuffle++) {
            do {
                i = nextInt(l);
                j = nextInt(l);
            } while (i != j);
            temp = array[j];
            array[j] = array[i];
            array[i] = temp;
        }
    }

    /**
     * Returns an array of shuffled indices of length l.
     *
     * @param l length of the array required.
     */
    public int[] shuffled(int l) {

        int[] array = new int[l];

        // initialize array
        for (int i = 0; i < l; i++) {
            array[i] = i;
        }
        shuffle(array);

        return array;
    }

    /**
     * Returns a uniform random permutation of ints 0,...,l-1
     *
     * @param l length of the array required.
     */
    public int[] permuted(int l) {

        int[] array = new int[l];

        // initialize array
        for (int i = 0; i < l; i++) {
            array[i] = i;
        }
        permute(array);

        return array;
    }


    public double nextGamma(double alpha, double lambda) {
        /******************************************************************
         *                                                                *
         *    Gamma Distribution - Acceptance Rejection combined with     *
         *                         Acceptance Complement                  *
         *                                                                *
         ******************************************************************
         *                                                                *
         * FUNCTION:    - gds samples a random number from the standard   *
         *                gamma distribution with parameter  a > 0.       *
         *                Acceptance Rejection  gs  for  a < 1 ,          *
         *                Acceptance Complement gd  for  a >= 1 .         *
         * REFERENCES:  - J.H. Ahrens, U. Dieter (1974): Computer methods *
         *                for sampling from gamma, beta, Poisson and      *
         *                binomial distributions, Computing 12, 223-246.  *
         *              - J.H. Ahrens, U. Dieter (1982): Generating gamma *
         *                variates by a modified rejection technique,     *
         *                Communications of the ACM 25, 47-54.            *
         * SUBPROGRAMS: - drand(seed) ... (0,1)-Uniform generator with    *
         *                unsigned long integer *seed                     *
         *              - NORMAL(seed) ... Normal generator N(0,1).       *
         *                                                                *
         ******************************************************************/
        double a = alpha;
        double aa = -1.0, aaa = -1.0,
                b = 0.0, c = 0.0, d = 0.0, e, r, s = 0.0, si = 0.0, ss = 0.0, q0 = 0.0,
                q1 = 0.0416666664, q2 = 0.0208333723, q3 = 0.0079849875,
                q4 = 0.0015746717, q5 = -0.0003349403, q6 = 0.0003340332,
                q7 = 0.0006053049, q8 = -0.0004701849, q9 = 0.0001710320,
                a1 = 0.333333333, a2 = -0.249999949, a3 = 0.199999867,
                a4 = -0.166677482, a5 = 0.142873973, a6 = -0.124385581,
                a7 = 0.110368310, a8 = -0.112750886, a9 = 0.104089866,
                e1 = 1.000000000, e2 = 0.499999994, e3 = 0.166666848,
                e4 = 0.041664508, e5 = 0.008345522, e6 = 0.001353826,
                e7 = 0.000247453;

        double gds, p, q, t, sign_u, u, v, w, x;
        double v1, v2, v12;

        // Check for invalid input values

        if (a <= 0.0) throw new IllegalArgumentException();
        if (lambda <= 0.0) new IllegalArgumentException();

        if (a < 1.0) { // CASE A: Acceptance rejection algorithm gs
            b = 1.0 + 0.36788794412 * a;              // Step 1
            for (; ; ) {
                p = b * nextDouble();
                if (p <= 1.0) {                       // Step 2. Case gds <= 1
                    gds = Math.exp(Math.log(p) / a);
                    if (Math.log(nextDouble()) <= -gds) return (gds / lambda);
                } else {                                // Step 3. Case gds > 1
                    gds = -Math.log((b - p) / a);
                    if (Math.log(nextDouble()) <= ((a - 1.0) * Math.log(gds))) return (gds / lambda);
                }
            }
        } else {        // CASE B: Acceptance complement algorithm gd (gaussian distribution, box muller transformation)
            if (a != aa) {                        // Step 1. Preparations
                aa = a;
                ss = a - 0.5;
                s = Math.sqrt(ss);
                d = 5.656854249 - 12.0 * s;
            }
            // Step 2. Normal deviate
            do {
                v1 = 2.0 * nextDouble() - 1.0;
                v2 = 2.0 * nextDouble() - 1.0;
                v12 = v1 * v1 + v2 * v2;
            } while (v12 > 1.0);
            t = v1 * Math.sqrt(-2.0 * Math.log(v12) / v12);
            x = s + 0.5 * t;
            gds = x * x;
            if (t >= 0.0) return (gds / lambda);         // Immediate acceptance

            u = nextDouble();                // Step 3. Uniform random number
            if (d * u <= t * t * t) return (gds / lambda); // Squeeze acceptance

            if (a != aaa) {                           // Step 4. Set-up for hat case
                aaa = a;
                r = 1.0 / a;
                q0 = ((((((((q9 * r + q8) * r + q7) * r + q6) * r + q5) * r + q4) *
                        r + q3) * r + q2) * r + q1) * r;
                if (a > 3.686) {
                    if (a > 13.022) {
                        b = 1.77;
                        si = 0.75;
                        c = 0.1515 / s;
                    } else {
                        b = 1.654 + 0.0076 * ss;
                        si = 1.68 / s + 0.275;
                        c = 0.062 / s + 0.024;
                    }
                } else {
                    b = 0.463 + s - 0.178 * ss;
                    si = 1.235;
                    c = 0.195 / s - 0.079 + 0.016 * s;
                }
            }
            if (x > 0.0) {                        // Step 5. Calculation of q
                v = t / (s + s);                  // Step 6.
                if (Math.abs(v) > 0.25) {
                    q = q0 - s * t + 0.25 * t * t + (ss + ss) * Math.log(1.0 + v);
                } else {
                    q = q0 + 0.5 * t * t * ((((((((a9 * v + a8) * v + a7) * v + a6) *
                            v + a5) * v + a4) * v + a3) * v + a2) * v + a1) * v;
                }                                  // Step 7. Quotient acceptance
                if (Math.log(1.0 - u) <= q) return (gds / lambda);
            }

            for (; ; ) {                                // Step 8. Double exponential deviate t
                do {
                    e = -Math.log(nextDouble());
                    u = nextDouble();
                    u = u + u - 1.0;
                    sign_u = (u > 0) ? 1.0 : -1.0;
                    t = b + (e * si) * sign_u;
                } while (t <= -0.71874483771719); // Step 9. Rejection of t
                v = t / (s + s);                  // Step 10. New q(t)
                if (Math.abs(v) > 0.25) {
                    q = q0 - s * t + 0.25 * t * t + (ss + ss) * Math.log(1.0 + v);
                } else {
                    q = q0 + 0.5 * t * t * ((((((((a9 * v + a8) * v + a7) * v + a6) *
                            v + a5) * v + a4) * v + a3) * v + a2) * v + a1) * v;
                }
                if (q <= 0.0) continue;           // Step 11.
                if (q > 0.5) {
                    w = Math.exp(q) - 1.0;
                } else {
                    w = ((((((e7 * q + e6) * q + e5) * q + e4) * q + e3) * q + e2) *
                            q + e1) * q;
                }                                  // Step 12. Hat acceptance
                if (c * u * sign_u <= w * Math.exp(e - 0.5 * t * t)) {
                    x = s + 0.5 * t;
                    return (x * x / lambda);
                }
            }
        }
    }
    
    /**********************************************************
     *                                                        *
     *  Poissonian distribution - Rejection + direct method   *
     *                                                        *
     **********************************************************/
    
    /**
     * Rejection method from NR, apparently good for lambda>=12, although
     * systematic errors start to creep in at lambda>=1e14.  Can we improve
     * on this?  A straight Gaussian seems to stay accurate for larger
     * means.
     * 
     * @param lambda
     * @return 
     */
    private double poissonian_reject(double lambda) {
        double sq = Math.sqrt(2.0*lambda);
        double alxm = Math.log(lambda);
        double g = lambda*alxm-GammaFunction.lnGamma(lambda+1.0);
        double em, t, y;

        do {
            do {
                y = Math.tan(Math.PI*nextDouble());
                em = sq*y+lambda;
            } while (em<0.0);

            em = Math.floor(em);
            t = 0.9*(1.0+y*y)*Math.exp(em*alxm
                    -GammaFunction.lnGamma(em+1.0)-g);

        } while (nextDouble()>t);

        return em;
    }

    /**
     * Direct method: only efficient for small lambda.
     * 
     * @param lambda
     * @return 
     */
    private double poissonian_knuth(double lambda) {
        double L = Math.exp(-lambda);
        double p;
        int k;

        for (k = 0, p = 1; p>=L; k++)
            p = p*nextDouble();

        return k-1;
    }

    /**
     * Sample from a Poissonian distribution.  Note that samples are expressed
     * as doubles, allowing for sensible convergence to the appropriate
     * Gaussian when extremely large lambdas are used.  Be aware however that
     * systematic errors due to rounding start to creep in for lambda>1e14.
     * Can we improve on this?
     * 
     * @param lambda
     * @return Draw from Pois(lambda).
     */
    public double nextPoisson(double lambda) {
        if (lambda<12)
            return poissonian_knuth(lambda);

        return poissonian_reject(lambda);
    }
    
    /**
     * Main for debugging only.
     */
    public static void main (String [] args) throws FileNotFoundException {
        
        double lambda = 1e12;
        int reps=100000;
        
        double sqrtlambda = Math.sqrt(lambda);
        double [] vals = new double[reps];

        PrintStream outf = new PrintStream("vals.txt");
        
        for (int i=0; i<reps; i++) {
            double val = Randomizer.nextPoisson(lambda);
            //double val = Randomizer.nextGaussian()*sqrtlambda + lambda;
            vals[i] = val;
            outf.println(val);
        }
        outf.close();
        
        System.out.format("E[x]=%g\n",DiscreteStatistics.mean(vals));
        System.out.format("Var[x]=%g\n", DiscreteStatistics.variance(vals));
    }
}
