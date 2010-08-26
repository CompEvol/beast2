package beast.evolution.tree.coalescent;

import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.RealParameter;
import beast.core.parameter.BooleanParameter;

import java.util.List;
import java.util.ArrayList;

/**
 * @author joseph
 *         Date: 26/08/2010
 */
@Description("An effective population size function based on coalecent times from a set of trees.")
public class CompoundPopulationFunction extends PopulationFunction.Abstract {

    public Input<RealParameter> popSizeParameterInput = new Input<RealParameter>("populationSizes",
            "population value at each point.", Input.Validate.REQUIRED);

    public Input<BooleanParameter> indicatorsParameterInput = new Input<BooleanParameter>("populationIndicators",
            "Include/exclude population value from the population function.", Input.Validate.REQUIRED);

    public Input<List<TreeIntervals>> treesInput = new Input<List<TreeIntervals>>("itree", "Coalecent intervals of this tree are " +
            "used in the compound population function.", Input.Validate.REQUIRED);

    public Input<String> demographicType = new Input<String>("type", "Flavour of demographic: either linear or stepwise for " +
            " piecewise-linear or piecewise-constant.",
            "linear");

    public Input<Boolean> useMiddle = new Input<Boolean>("useIntervalsMiddle", "When true, the demographic X axis points are " +
            "in the middle of the coalecent intervals. By default they are at the beggining.",
                false);

    private RealParameter popSizeParameter;
    private BooleanParameter indicatorsParameter;
    private List<TreeIntervals> trees;

    private Type type;
    private boolean useMid;

    public enum Type {
        LINEAR("linear"),
        //EXPONENTIAL("exponential"),
        STEPWISE("stepwise");

        Type(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }

        String name;
    }

    // why do we need this additional level on top of initAndValidate - does not seem to do anything?
    @Override
    public void prepare() {
       popSizeParameter = popSizeParameterInput.get();
       indicatorsParameter = indicatorsParameterInput.get();
        // is that safe???
       trees = treesInput.get();

       useMid = useMiddle.get();

       type = Type.valueOf(demographicType.get());  // errors?
        // set lengths

        int events = 0;
        for (TreeIntervals ti : trees) {
            // number of coalescent events
            events += ti.getIntervalCount(); //getExternalNodeCount() - 1;
        }
        // all trees share time 0, need fixing for serial data

        events += type == Type.STEPWISE ? 0 : 1;
        try {
            if (popSizeParameter.getDimension() != events) {
                final RealParameter p =
                        new RealParameter(popSizeParameter.getValue(),
                                popSizeParameter.getLower(), popSizeParameter.getUpper(), events);
                popSizeParameter.assignFrom(p);
            }

            if (indicatorsParameter.getDimension() != events - 1) {
                final BooleanParameter p = new BooleanParameter(indicatorsParameter.getValue(), events-1) ;
                indicatorsParameter.assignFrom(p);
            }
        } catch( Exception e ) {
            // what to do?
            e.printStackTrace();
        }

        initInternals();
    }

    @Override
    public List<String> getParameterIds() {
        List<String> paramIDs = new ArrayList<String>();
        paramIDs.add(popSizeParameter.getID());
        paramIDs.add(indicatorsParameter.getID());

        for( TreeIntervals t : trees ) {
            // I think this may be wrong, and we need the trees themselves
            paramIDs.add(t.getID());
        }
        return paramIDs;
    }

    @Override
    public double getPopSize(double t) {
        double p;
        switch (type) {
            case STEPWISE: {
                final int j = getIntervalIndexStep(t);
                p = values[j];
                break;
            }
            case LINEAR: {
                p = linPop(t);
                break;
            }
            default:
                throw new IllegalArgumentException("");
        }
        return p;
    }

    @Override
    public double getIntensity(double t) {
        return getIntegral(0, t);
    }

    @Override
    public double getInverseIntensity(double x) {
        throw new UnsupportedOperationException();
    }

    // values participating in the demographic
    private double[] values;
    // times participating in the demographic
    private double[] times;
    // convenience: intervals[n] = times[n+1] - times[n] 
    private double[] intervals;

    // sorted times from each tree
    private double[][] ttimes;

    // sorted times from all trees (merge of ttimes above)
    private double[] alltimes;

    private void initInternals() {
             ttimes = new double[trees.size()][];
        int tot = 0;
        for (int k = 0; k < ttimes.length; ++k) {
            ttimes[k] = new double[trees.get(k).getIntervalCount()];
            tot += ttimes[k].length;
        }
        alltimes = new double[tot];
    }

    private int getIntervalIndexStep(final double t) {
        int j = 0;
        // ugly hack,
        // when doubles are added in a different order and compared later, they can be a tiny bit off. With a
        // stepwise model this creates a "one off" situation here, which is unpleasant.
        // use float comparison here to avoid it

        final float tf = (float) t;
        while (tf > (float) times[j + 1]) ++j;
        return j;
    }

    private int getIntervalIndexLin(final double t) {
        int j = 0;
        while (t > times[j + 1]) ++j;
        return j;
    }

    private double linPop(double t) {
        final int j = getIntervalIndexLin(t);
        if (j == values.length - 1) {
            return values[j];
        }

        final double a = (t - times[j]) / (intervals[j]);
        return a * values[j + 1] + (1 - a) * values[j];
    }

    private double intensityLinInterval(double start, double end, int index) {
        final double dx = end - start;
        if( dx == 0 ) {
            return 0;
        }

        final double popStart = values[index];
        final double popDiff = (index < values.length - 1) ? values[index + 1] - popStart : 0.0;
        if (popDiff == 0.0) {
            return dx / popStart;
        }
        final double time0 = times[index];
        final double interval = intervals[index];

        assert (float) start <= (float) (time0 + interval) && start >= time0
                && (float) end <= (float) (time0 + interval) && end >= time0;

        //better numerical stability but not perfect
        final double p1minusp0 = ((end - start) / interval) * popDiff;

        final double v = interval * (popStart / popDiff);
        final double p1overp0 = (v + (end - time0)) / (v + (start - time0));
        if (p1minusp0 == 0.0 || p1overp0 <= 0) {
            // either dx == 0 or is very small (numerical inaccuracy)
            final double pop0 = popStart + ((start - time0) / interval) * popDiff;
            return dx / pop0;
        }

        return dx * Math.log(p1overp0) / p1minusp0;
        // return dx * Math.log(pop1/pop0) / (pop1 - pop0);*/
    }

    private double intensityLinInterval(int index) {
        final double interval = intervals[index];
        final double pop0 = values[index];
        final double pop1 = values[index + 1];
        if (pop0 == pop1) {
            return interval / pop0;
        }
        return interval * Math.log(pop1 / pop0) / (pop1 - pop0);
    }

    public double getIntegral(double start, double finish) {

        double intensity = 0.0;

        switch (type) {
            case STEPWISE: {
                final int first = getIntervalIndexStep(start);
                final int last = getIntervalIndexStep(finish);

                final double popStart = values[first];
                if (first == last) {
                    intensity = (finish - start) / popStart;
                } else {
                    intensity = (times[first + 1] - start) / popStart;

                    for (int k = first + 1; k < last; ++k) {
                        intensity += intervals[k] / values[k];
                    }
                    intensity += (finish - times[last]) / values[last];
                }
                break;
            }
            case LINEAR: {
                final int first = getIntervalIndexLin(start);
                final int last = getIntervalIndexLin(finish);

                if (first == last) {
                    intensity += intensityLinInterval(start, finish, first);
                } else {
                    // from first to end of interval
                    intensity += intensityLinInterval(start, times[first + 1], first);
                    // intervals until (not including) last
                    for (int k = first + 1; k < last; ++k) {
                        intensity += intensityLinInterval(k);
                    }
                    // last interval
                    intensity += intensityLinInterval(times[last], finish, last);
                }
                break;
            }
        }
        return intensity;
    }

    /**
     * Get times of the (presumably changed) nt'th tree intor the local array.
     * @param nt
     */
    private void setTreeTimes(int nt) {

        TreeIntervals nti = trees.get(nt);
        nti.setMultifurcationLimit(0);

        // code probably incorrect for serial samples
        final int nLineages = nti.getIntervalCount();
        assert nLineages >= ttimes[nt].length : nLineages + " " + ttimes[nt].length;

        int iCount = 0;
        for (int k = 0; k < ttimes[nt].length; ++k) {
            double timeToCoal = nti.getInterval(iCount);
            while (nti.getIntervalType(iCount) != IntervalType.COALESCENT) {
                ++iCount;
                timeToCoal += nti.getInterval(iCount);
            }

            int linAtStart = nti.getLineageCount(iCount);
            ++iCount;

            assert !(iCount == nLineages && linAtStart != 2);

            int linAtEnd = (iCount == nLineages) ? 1 : nti.getLineageCount(iCount);

            while (linAtStart <= linAtEnd) {
                ++iCount;
                timeToCoal += nti.getInterval(iCount);

                linAtStart = linAtEnd;
                ++iCount;
                linAtEnd = nti.getLineageCount(iCount);
            }
            ttimes[nt][k] = timeToCoal + (k == 0 ? 0 : ttimes[nt][k - 1]);
        }
    }

    /** Merge sorted times in each ttimes[] array into one sorted array (alltimes) **/
    private void mergeTreeTimes() {
        // now we want to merge times together
        int[] inds = new int[ttimes.length];

        for (int k = 0; k < alltimes.length; ++k) {
            int j = 0;
            while (inds[j] == ttimes[j].length) {
                ++j;
            }
            for (int l = j + 1; l < inds.length; ++l) {
                if (inds[l] < ttimes[l].length) {
                    if (ttimes[l][inds[l]] < ttimes[j][inds[j]]) {
                        j = l;
                    }
                }
            }
            alltimes[k] = ttimes[j][inds[j]];
            inds[j]++;
        }
    }

    /** Setup the internal times,values,intervals from the rest **/
    private void setDemographicArrays() {
        // assumes lowest node has time 0. this is probably problematic when we come
        // to deal with multiple trees

        int tot = 1;
        final int nd = indicatorsParameter.getDimension();

        assert nd == alltimes.length + (type == Type.STEPWISE ? -1 : 0) :
                " nd=" + nd + " alltimes.length=" + alltimes.length + " type=" + type;
        for (int k = 0; k < nd; ++k) {
            if ( indicatorsParameter.getValue(k) ) {
                ++tot;
            }
        }

        times = new double[tot + 1];
        values = new double[tot];
        intervals = new double[tot - 1];

        times[0] = 0.0;
        times[tot] = Double.POSITIVE_INFINITY;

        values[0] = popSizeParameter.getValue(0);

        int n = 0;
        for(int k = 0; k < nd && n+1 < tot; ++k) {

            if( indicatorsParameter.getValue(k) ) {
                times[n+1] = useMid ? ((alltimes[k] + (k > 0 ? alltimes[k-1] : 0))/2) : alltimes[k];

                values[n+1] =  popSizeParameter.getValue(k+1);
                intervals[n] = times[n+1] - times[n];
                ++n;
            }
        }
    }
}
