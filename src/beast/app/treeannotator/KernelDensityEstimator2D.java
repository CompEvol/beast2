package beast.app.treeannotator;

import beast.math.statistic.DiscreteStatistics;
import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;

import java.util.Arrays;


/**
 * KernelDensityEstimator2D creates a bi-variate kernel density smoother for data
 * @author Marc A. Suchard
 * @author Philippe Lemey
 */

public class KernelDensityEstimator2D implements ContourMaker {

//    kde2d =
//    function (x, y, h, n = 25, lims = c(range(x), range(y)))
//    {
//        nx <- length(x)
//        if (length(y) != nx)
//            stop("data vectors must be the same length")
//        if (any(!is.finite(x)) || any(!is.finite(y)))
//            stop("missing or infinite values in the data are not allowed")
//        if (any(!is.finite(lims)))
//            stop("only finite values are allowed in 'lims'")
//        gx <- seq.int(lims[1], lims[2], length.out = n)
//        gy <- seq.int(lims[3], lims[4], length.out = n)
//        if (missing(h))
//            h <- c(bandwidth.nrd(x), bandwidth.nrd(y))
//        h <- h/4
//        ax <- outer(gx, x, "-")/h[1]
//        ay <- outer(gy, y, "-")/h[2]
//        z <- matrix(dnorm(ax), n, nx) %*% t(matrix(dnorm(ay), n,
//            nx))/(nx * h[1] * h[2])
//        return(list(x = gx, y = gy, z = z))
//    }

    /*
     * @param x x-coordinates of observations
     * @param y y-coordinates of observations
     * @param h bi-variate smoothing bandwidths
     * @param n smoothed grid size
     * @param lims bi-variate min/max for grid
     */
    public KernelDensityEstimator2D(final double[] x, final double[] y, final double[] h, final int n, final double[] lims) {
        this(x, y, h, n, lims, false);
    }

    public KernelDensityEstimator2D(final double[] x, final double[] y, final double[] h, final int n, final double[] lims, boolean bandwdithLimited) {
        this.x = x;
        this.y = y;
        if (x.length != y.length)
            throw new RuntimeException("data vectors must be the same length");

        this.nx = x.length;

        if (n <= 0)
            throw new RuntimeException("must have a positive number of grid points");
        this.n = n;

        if (lims != null)
            this.lims = lims;
        else
            setupLims();

        this.limitBandwidth = bandwdithLimited;
        if (h != null)
            this.h = h;
        else
            setupH();

        doKDE2D();
    }
    
    public KernelDensityEstimator2D(final double[] x, final double[] y, boolean limitBandwidth) {
        this(x,y,null,50,null,limitBandwidth);
    }

    public KernelDensityEstimator2D(final double[] x, final double[] y) {
        this(x,y,null,50,null);
    }

    public KernelDensityEstimator2D(final double[] x, final double[] y, final int n) {
        this(x,y,null,n,null);
    }

    public void doKDE2D() {
        gx = makeSequence(lims[0], lims[1], n);
        gy = makeSequence(lims[2], lims[3], n);
        double[][] ax = outerMinusScaled(gx, x, h[0]);
        double[][] ay = outerMinusScaled(gy, y, h[1]);
        normalize(ax);
        normalize(ay);
        z = new double[n][n];
        double scale = nx * h[0] * h[1];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double value = 0;
                for (int k = 0; k < nx; k++) {
                    value += ax[i][k] * ay[j][k];
                }
                z[i][j] = value / scale;
            }
        }
    }

    public double findLevelCorrespondingToMass(double probabilityMass) {
        double level = 0;
        double[] sz = new double[n*n];
        double[] c1 = new double[n*n];
        for(int i=0; i<n; i++)
            System.arraycopy(z[i],0,sz,i*n,n);
        Arrays.sort(sz);
        final double dx = gx[1] - gx[0];
        final double dy = gy[1] - gy[0];
        final double dxdy = dx * dy;
        c1[0] = sz[0] * dxdy;
        final double criticalValue = 1.0 - probabilityMass;
        if (criticalValue < c1[0] || criticalValue >= 1.0)
                throw new RuntimeException();
        // do linearInterpolation on density (y) as function of cumulative sum (x)
        for(int i=1; i<n*n; i++) {
            c1[i] = sz[i] * dxdy + c1[i-1];
            if (c1[i] > criticalValue) { // first largest point
                final double diffC1 = c1[i] - c1[i-1];
                final double diffSz = sz[i] - sz[i-1];
                level = sz[i] - (c1[i]-criticalValue) / diffC1 * diffSz;
                break;
            }
        }
        return level;
    }

    public ContourPath[] getContourPaths(double hpdValue) {

        double thresholdDensity = findLevelCorrespondingToMass(hpdValue);
        ContourGenerator contour = new ContourGenerator(getXGrid(), getYGrid(), getKDE(),
                new ContourAttrib[]{new ContourAttrib(thresholdDensity)});

        ContourPath[] paths = null;
        try {
            paths = contour.getContours();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return paths;
    }

    public double[][] getKDE() {
        return z;
    }

    public double[] getXGrid() {
        return gx;
    }

    public double[] getYGrid() {
        return gy;
    }

    public void normalize(double[][] X) {
        for (int i = 0; i < X.length; i++) {
            for (int j = 0; j < X[0].length; j++)
                X[i][j] = pdf(X[i][j], 0, 1);
        }
    }
    
    public static double pdf(double x, double m, double sd) {
        double a = 1.0 / (Math.sqrt(2.0 * Math.PI) * sd);
        double b = -(x - m) * (x - m) / (2.0 * sd * sd);

        return a * Math.exp(b);
    }


    public double[][] outerMinusScaled(double[] X, double[] Y, double scale) {
        double[][] A = new double[X.length][Y.length];
        for (int indexX = 0; indexX < X.length; indexX++) {
            for (int indexY = 0; indexY < Y.length; indexY++)
                A[indexX][indexY] = (X[indexX] - Y[indexY]) / scale;
        }
        return A;
    }

    public double[] makeSequence(double start, double end, int length) {
        double[] seq = new double[length];
        double by = (end - start) / (length - 1);
        double value = start;
        for (int i = 0; i < length; i++, value += by) {
            seq[i] = value;
        }
        return seq;
    }

    private double margin = 0.1;

    private void setupLims() {
        lims = new double[4];
        lims[0] = DiscreteStatistics.min(x);
        lims[1] = DiscreteStatistics.max(x);
        lims[2] = DiscreteStatistics.min(y);
        lims[3] = DiscreteStatistics.max(y);

        double xDelta = (lims[1] - lims[0]) * margin;
        double yDelta = (lims[3] - lims[2]) * margin;
        lims[0] -= xDelta;
        lims[1] += xDelta;
        lims[2] -= yDelta;
        lims[3] += yDelta;
    }

    private void setupH() {
        h = new double[2];
        h[0] = bandwidthNRD(x) / 4;
        h[1] = bandwidthNRD(y) / 4;

        if (limitBandwidth) {
            if (h[0] >  0.5) {
                h[0] = 0.5;
            }
            if (h[1] > 0.5) {
                h[1] = 0.5;
            }
        }
    }


//   bandwidth.nrd =
//   function (x)
//   {
//       r <- quantile(x, c(0.25, 0.75))
//       h <- (r[2] - r[1])/1.34
//       4 * 1.06 * min(sqrt(var(x)), h) * length(x)^(-1/5)

    //   }
    public double bandwidthNRD(double[] in) {

        DoubleArrayList inList = new DoubleArrayList(in.length);
        for (double d : in)
            inList.add(d);
        inList.sort();

        final double h = (Descriptive.quantile(inList, 0.75) - Descriptive.quantile(inList, 0.25)) / 1.34;

        return 4 * 1.06 *
                Math.min(Math.sqrt(DiscreteStatistics.variance(in)), h) *
                Math.pow(in.length, -0.2);
    }

    public static void main(String[] arg) {

        double[] x = {3.4, 1.2, 5.6, 2.2, 3.1};
        double[] y = {1.0, 2.0, 1.0, 2.0, 1.0};

        KernelDensityEstimator2D kde = new KernelDensityEstimator2D(x, y, 4);

//        System.out.println(new Vector(kde.getXGrid()));
//        System.out.println(new Vector(kde.getYGrid()));
//        System.out.println(new Matrix(kde.getKDE()));
        System.exit(-1);

    }

    public double[] getLims() { return lims; }

    private final double[] x; // x coordinates
    private final double[] y; // y coordinates
    private double[] h; // h[0] x-bandwidth, h[1] y-bandwidth
    private final int n; // grid size
    private double[] lims; // x,y limits
    private int nx; // length of vectors
    private double[] gx; // x-grid points
    private double[] gy; // y-grid points
    private double[][] z; // KDE estimate;

    private final boolean limitBandwidth;

}
