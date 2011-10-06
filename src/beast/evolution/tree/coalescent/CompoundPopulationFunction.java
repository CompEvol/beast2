package beast.evolution.tree.coalescent;

import beast.core.Description;
import beast.core.Input;
import beast.core.Loggable;
import beast.core.parameter.BooleanParameter;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.coalescent.IntervalType;
import beast.evolution.tree.coalescent.PopulationFunction;
import beast.evolution.tree.coalescent.TreeIntervals;
import beast.math.statistic.DiscreteStatistics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author joseph
 *         Date: 26/08/2010
 */
@Description("An effective population size function based on coalecent times from a set of trees.")
public class CompoundPopulationFunction extends PopulationFunction.Abstract implements Loggable {

    public Input<RealParameter> popSizeParameterInput = new Input<RealParameter>("populationSizes",
            "population value at each point.", Input.Validate.REQUIRED);

    public Input<BooleanParameter> indicatorsParameterInput = new Input<BooleanParameter>("populationIndicators",
            "Include/exclude population value from the population function.", Input.Validate.REQUIRED);

    public Input<List<TreeIntervals>> treesInput = new Input<List<TreeIntervals>>("itree", "Coalecent intervals of this tree are " +
            "used in the compound population function.", new ArrayList<TreeIntervals>(), Input.Validate.REQUIRED);

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

    private void getParams() {
        popSizeParameter = popSizeParameterInput.get();
        indicatorsParameter = indicatorsParameterInput.get();
        assert popSizeParameter != null && popSizeParameter.getArrayValue(0) > 0 &&  indicatorsParameter != null; 
    }

    // why do we need this additional level on top of initAndValidate - does not seem to do anything?
    @Override
    public void prepare() {
       getParams();
        // is that safe???
       trees = treesInput.get();

       useMid = useMiddle.get();

       // used to work without upper case ???
       type = Type.valueOf(demographicType.get().toUpperCase());  // errors?
        // set lengths

        int events = 0;
        for (TreeIntervals ti : trees) {
            // number of coalescent events
            events += ti.m_tree.get().getLeafNodeCount() - 1;
        }
        // all trees share time 0, need fixing for serial data

        events += type == Type.STEPWISE ? 0 : 1;
        try {
            if (popSizeParameter.getDimension() != events) {
                final RealParameter p = new RealParameter();
        		p.initByName("value", popSizeParameter.getValue() + "", "upper", popSizeParameter.getUpper(), "lower", popSizeParameter.getLower(), "dimension", events);
                p.setID(popSizeParameter.getID());
                popSizeParameter.assignFrom(p);
            }

            if (indicatorsParameter.getDimension() != events - 1) {
                final BooleanParameter p = new BooleanParameter();
                p.initByName("value", "" + indicatorsParameter.getValue(), "dimension", events-1) ;
                p.setID(indicatorsParameter.getID()); 
                indicatorsParameter.assignFrom(p);
            }
        } catch( Exception e ) {
            // what to do?
            e.printStackTrace();
        }

        initInternals();
        for (int nt = 0; nt < trees.size(); ++nt) {
            setTreeTimes(nt);
        }
        mergeTreeTimes();
        setDemographicArrays();

        shadow = new Shadow();
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

    // no allocations, minimal copying
    class Shadow {
        double[] values;
        double[] times;
        double[] intervals;
        double[][] ttimes;
        double[] alltimes;

        boolean c_demo, c_alltimes;
        boolean[] c_ttimes;

        Shadow() {
            values = CompoundPopulationFunction.this.values.clone();
            times = CompoundPopulationFunction.this.times.clone();
            intervals = CompoundPopulationFunction.this.intervals.clone();

            alltimes = CompoundPopulationFunction.this.alltimes.clone();

            ttimes = CompoundPopulationFunction.this.ttimes.clone();
            for (int nt = 0; nt < ttimes.length; ++nt) {
                ttimes[nt] = CompoundPopulationFunction.this.ttimes[nt].clone();
            }
            c_ttimes = new boolean[ttimes.length];

            reset();
        }

        void reset() {
            c_alltimes = false;
            c_demo = false;
            Arrays.fill(c_ttimes, false);
        }

        void protect_demo() {

                values = CompoundPopulationFunction.this.values;
                times = CompoundPopulationFunction.this.times;
                intervals = CompoundPopulationFunction.this.intervals;

                CompoundPopulationFunction.this.values = null;
                CompoundPopulationFunction.this.times = null;
                CompoundPopulationFunction.this.intervals = null;
//            {
//                final double[] src = CompoundPopulationFunction.this.values;
//                final double[] target = values;
//                if( src.length == target.length ) {
//                    System.arraycopy(src, 0,  target, 0, src.length);
//                } else {
//                    values = src.clone();
//                }
//            }
//            {
//                final double[] src = CompoundPopulationFunction.this.times;
//                final double[] target = times;
//                if( src.length == target.length ) {
//                    System.arraycopy(src, 0,  target, 0, src.length);
//                }  else {
//                    times = src.clone();
//                }
//            }
//            {
//                final double[] src = CompoundPopulationFunction.this.intervals;
//                final double[] target = intervals;
//                if( src.length == target.length ) {
//                    System.arraycopy(src, 0,  target, 0, src.length);
//                } else {
//                    intervals = src.clone();
//                }
//            }
            c_demo = true;
        }

        void protect_alltimes() {
            final double[] src = CompoundPopulationFunction.this.alltimes;
            System.arraycopy(src, 0,  alltimes, 0, src.length);
            c_alltimes = true;
        }

        void protect_ttimes(int nt) {
            final double[] src = CompoundPopulationFunction.this.ttimes[nt];
            System.arraycopy(src, 0,  ttimes[nt], 0, src.length);
            c_ttimes[nt] = true;
        }

        void accept() {
          values = times = intervals = null;
        }

        void reject() {
            if( c_alltimes ) {
                final double[] v = CompoundPopulationFunction.this.alltimes;
                CompoundPopulationFunction.this.alltimes = alltimes;
                alltimes = v;
            }

            if( c_demo ) {
                CompoundPopulationFunction.this.values = values;
                CompoundPopulationFunction.this.times = times;
                CompoundPopulationFunction.this.intervals = intervals;

                values = times = intervals = null;
//                double[] v = CompoundPopulationFunction.this.values;
//                CompoundPopulationFunction.this.values = values;
//                values = v;
//
//                v = CompoundPopulationFunction.this.times;
//                CompoundPopulationFunction.this.times = times;
//                times = v;
//
//                v = CompoundPopulationFunction.this.intervals;
//                CompoundPopulationFunction.this.intervals = intervals;
//                intervals = v;
            }

            for (int nt = 0; nt < c_ttimes.length; ++nt) {
                if( c_ttimes[nt] ) {
                   double[] v = CompoundPopulationFunction.this.ttimes[nt];
                   CompoundPopulationFunction.this.ttimes[nt] = ttimes[nt];
                   ttimes[nt] = v;
                }
            }
        }
    }

    private Shadow shadow;

    private void initInternals() {
        ttimes = new double[trees.size()][];
        int tot = 0;
        for (int k = 0; k < ttimes.length; ++k) {
            ttimes[k] = new double[trees.get(k).m_tree.get().getLeafNodeCount() -1];
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
     * Get times of the (presumably changed) nt'th tree into the local array.
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

    @Override
    protected void store() {
        super.store();
    }

    @Override
    protected boolean requiresRecalculation() {
        boolean anyTreesChanged = false;
        for (int nt = 0; nt < trees.size(); ++nt) {
            TreeIntervals ti = trees.get(nt);
            if( ti.isDirtyCalculation() ) {
                shadow.protect_ttimes(nt);

                setTreeTimes(nt);
                anyTreesChanged = true;
            }
        }

        // we access parameters in any case
        getParams();

        if( anyTreesChanged ) {
            shadow.protect_alltimes();
            shadow.protect_demo();

            mergeTreeTimes();
            setDemographicArrays();
        } else {
            if( popSizeParameter.somethingIsDirty() &&  !indicatorsParameter.somethingIsDirty() ) {

            }
            shadow.protect_demo();
            setDemographicArrays();
        }
        return true;
    }

    @Override
    protected void restore() {
        shadow.reject();
        shadow.reset();
        super.restore();
    }

    @Override
    protected void accept() {
        shadow.accept();
        shadow.reset();
        super.accept();
    }

	@Override
	public void init(PrintStream out) throws Exception {
		// interval sizes
		out.print("popsSize0\t");
		for (int i = 0; i < alltimes.length; i++) {
			out.print(getID() +".times." + i + "\t");
		}
	}

	@Override
	public void log(int nSample, PrintStream out) {
		// interval sizes
		out.print("0:" + popSizeParameter.getArrayValue(0) + "\t");
		for (int i = 0; i < alltimes.length; i++) {
			out.print(alltimes[i]);
			if (indicatorsParameter.getArrayValue(i) > 0) {
				out.print(":" + popSizeParameter.getArrayValue(i + 1));
			}
			out.print("\t");
		}
	}


	
	@Override
	public void close(PrintStream out) {
	}
}