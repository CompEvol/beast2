package beast.app.treeannotator;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * @author Marc A. Suchard
 */
public class ContourWithSynder extends KernelDensityEstimator2D  {

    public ContourWithSynder(final double[] x, final double[] y, final double[] h, final int n, final double[] lims) {
        super(x, y, h, n, lims);
    }

    public ContourWithSynder(final double[] x, final double[] y, boolean bandwidthLimit) {
        super(x, y, bandwidthLimit);
    }

    public ContourWithSynder(final double[] x, final double[] y) {
        super(x, y);
    }

    public ContourWithSynder(final double[] x, final double[] y, int n) {
        super(x, y, n);
    }

    @Override
	public ContourPath[] getContourPaths(double hpdValue) {

        if (contourPaths == null) {

            double thresholdDensity = findLevelCorrespondingToMass(hpdValue);

            SnyderContour contourPlot = new SnyderContour(getXGrid().length,getYGrid().length);
            contourPlot.setDeltas(getXGrid()[1]-getXGrid()[0],getYGrid()[1]-getYGrid()[0] );
            contourPlot.setOffsets(getXGrid()[0],getYGrid()[0]);

            List<LinkedList<Point2D>> allPaths = new ArrayList<>();
            contourPlot.ContourKernel(getKDE(),allPaths,thresholdDensity);

            contourPaths = new ContourPath[allPaths.size()];
            for(int i=0; i<allPaths.size(); i++) {
                LinkedList<Point2D> path = allPaths.get(i);
                int len = path.size();
                double[] x = new double[len];
                double[] y = new double[len];
                for(int j=0; j<len; j++) {
                    Point2D pt = path.get(j);
                    x[j] = pt.getX();
                    y[j] = pt.getY();
                }
                contourPaths[i] = new ContourPath(new ContourAttrib(thresholdDensity),1,x,y);
            }
        }

        return contourPaths;
    }

    private ContourPath[] contourPaths = null;

}
