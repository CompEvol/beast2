package beast.evolution.speciation;

import beast.core.*;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.alignment.distance.Distance;
import beast.evolution.alignment.distance.JukesCantorDistance;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeInterface;
import beast.util.ClusterTree;

import java.util.*;

import static java.lang.Math.*;

/**
* @author Joseph Heled
 */

@Description("Set a starting point for a *BEAST analysis from gene alignment data.")
public class StarBeastStartState extends Tree implements StateNodeInitialiser {

    static enum Method {
        POINT("point-estimate"),
        ALL_RANDOM("random");

        Method(final String name) {
            this.ename = name;
        }

        public String toString() {
            return ename;
        }

        private final String ename;
    }
    public Input<Method> initMethod = new Input<Method>("method", "Initialise either with a totally random " +
            "state or a point estimate based on alignments data (default point-estimate)",
            Method.POINT, Method.values());

    public Input<Tree> speciesTreeInput = new Input<Tree>("speciesTree", "The species tree to initialize");

    public Input<List<Tree>> genes = new Input<List<Tree>>("gene", "Gene trees to initialize", new ArrayList<Tree>(),
            Validate.REQUIRED);

    public Input<CalibratedYuleModel> calibratedYule = new Input<CalibratedYuleModel>("calibratedYule",
            "The species tree (with calibrations) to initialize", Validate.XOR, speciesTreeInput);

    public Input<RealParameter> popMean = new Input<RealParameter>("popMean",
            "Population mean hyper prior to initialse");

    public Input<RealParameter> birthRate = new Input<RealParameter>("birthRate",
            "Tree prior birth rate to initialize");

    public Input<SpeciesTreePrior> speciesTreePriorInput =
            new Input<SpeciesTreePrior>("speciesTreePrior", "Population size parameters to initialise");

    public Input<Function> muInput = new Input<Function>("baseRate",
            "Main clock rate used to scale trees (default 1).");


    private boolean hasCalibrations;

    @Override
    public void initAndValidate() throws Exception {
        // what does this do and is it dangerous to call it or not to call it at the start or at the end??????
        super.initAndValidate();
        hasCalibrations = calibratedYule.get() != null;
    }

    @Override
    public void initStateNodes() throws Exception {

        if( hasCalibrations ) {
            initWithCalibrations();
        } else {
            final Method method = initMethod.get();

            switch( method ) {
                case POINT:
                    fullInit();
                    break;
                case ALL_RANDOM:
                    randomInit();
                    break;
            }
        }
    }

    private double[] firstMeetings(final Tree gtree, final Map<String, Integer> tipName2Species, final int nSpecies) {
        final Node[] nodes = gtree.listNodesPostOrder(null, null);
        final Set<Integer>[] tipsSpecies = new Set[nodes.length];
        for(int k = 0; k < tipsSpecies.length; ++k) {
            tipsSpecies[k] = new HashSet<Integer>();
        }
        // d[i,j] = minimum height of node which has tips belonging to species i and j
        // d is is upper triangular
        final double[] dmin = new double[(nSpecies*(nSpecies-1))/2];
        Arrays.fill(dmin, Double.MAX_VALUE);

        for (final Node n : nodes) {
            if (n.isLeaf()) {
                tipsSpecies[n.getNr()].add(tipName2Species.get(n.getID()));
            } else {
                assert n.getChildCount() == 2;
                final Set<Integer>[] sps = new Set[2];
                sps[0] = tipsSpecies[n.getChild(0).getNr()];
                sps[1] = tipsSpecies[n.getChild(1).getNr()];
                final Set<Integer> u = new HashSet<Integer>(sps[0]);
                u.retainAll(sps[1]);
                sps[0].removeAll(u);
                sps[1].removeAll(u);

                for (final Integer s1 : sps[0]) {
                    for (final Integer s2 : sps[1]) {
                        final int i = getDMindex(nSpecies, s1, s2);
                        dmin[i] = min(dmin[i], n.getHeight());
                    }
                }
                u.addAll(sps[0]);
                u.addAll(sps[1]);
                tipsSpecies[n.getNr()] = u;
            }
        }
        return dmin;
    }

    private int getDMindex(final int nSpecies, final int s1, final int s2) {
        final int mij = min(s1,s2);
        return (mij*(2*nSpecies-1 - mij))/2 + (abs(s1-s2)-1);
    }


    private void fullInit() throws Exception {
        // Build gene trees from  alignments

        final Function muInput = this.muInput.get();
        final double mu =  (muInput != null )  ? muInput.getArrayValue() : 1;

        final Tree stree = speciesTreeInput.get();
        final TaxonSet species = stree.m_taxonset.get();
        final List<String> speciesNames = species.asStringList();
        final int nSpecies = speciesNames.size();

        final List<Tree> geneTrees = genes.get();

        //final List<Alignment> alignments = genes.get();
        //final List<Tree> geneTrees = new ArrayList<Tree>(alignments.size());
        double maxNsites = 0;
        //for( final Alignment alignment : alignments)  {
        for (final Tree gtree : geneTrees) {
            //final Tree gtree = new Tree();
            final Alignment alignment = gtree.m_taxonset.get().alignmentInput.get();

            final ClusterTree ctree = new ClusterTree();
            ctree.initByName("initial", gtree, "clusterType", "upgma", "taxa", alignment);
            gtree.scale(1 / mu);

            maxNsites = max(maxNsites, alignment.getSiteCount());
        }
        final Map<String, Integer> geneTips2Species = new HashMap<String, Integer>();
        final List<Taxon> taxonSets = species.taxonsetInput.get();

        for(int k = 0; k < speciesNames.size(); ++k) {
            final Taxon nx = taxonSets.get(k);
            final List<Taxon> taxa = ((TaxonSet) nx).taxonsetInput.get();
            for( final Taxon n : taxa ) {
              geneTips2Species.put(n.getID(), k);
            }
        }
        final double[] dg = new double[(nSpecies*(nSpecies-1))/2];

        final double[][] genesDmins = new double[geneTrees.size()][];

        for( int ng = 0; ng < geneTrees.size(); ++ng ) {
            final Tree g = geneTrees.get(ng);
            final double[] dmin = firstMeetings(g, geneTips2Species, nSpecies);
            genesDmins[ng] = dmin;

            for(int i = 0; i < dmin.length; ++i) {
                dg[i] += dmin[i];
            }
        }

        for(int i = 0; i < dg.length; ++i) {
            double d = dg[i] / geneTrees.size();
            if( d == 0 ) {
               d = (0.5/maxNsites) * (1/mu);
            } else {
                // heights to distances
                d *= 2;
            }
            dg[i] = d;
        }

        final ClusterTree ctree = new ClusterTree();
        final Distance distance = new Distance() {
            @Override
            public double pairwiseDistance(final int s1, final int s2) {
                final int i = getDMindex(nSpecies, s1,s2);
                return dg[i];
            }
        };
        ctree.initByName("initial", stree, "taxonset", species,"clusterType", "upgma", "distance", distance);

        final Map<String, Integer> sptips2SpeciesIndex = new HashMap<String, Integer>();
        for(int i = 0; i < speciesNames.size(); ++i) {
            sptips2SpeciesIndex.put(speciesNames.get(i), i);
        }
        final double[] spmin = firstMeetings(stree, sptips2SpeciesIndex, nSpecies);

        for( int ng = 0; ng < geneTrees.size(); ++ng ) {
            final double[] dmin = genesDmins[ng];
            boolean compatible = true;
            for(int i = 0; i < spmin.length; ++i) {
                if( dmin[i] <= spmin[i] ) {
                    compatible = false;
                    break;
                }
            }
            if( ! compatible ) {
                final Tree gtree = geneTrees.get(ng);
                final TaxonSet gtreeTaxa = gtree.m_taxonset.get();
                final Alignment alignment = gtreeTaxa.alignmentInput.get();
                final List<String> taxaNames = alignment.getTaxaNames();
                final int nTaxa =  taxaNames.size();
                // speedup
                final Map<Integer,Integer> g2s = new HashMap<Integer, Integer>();
                for(int i = 0; i < nTaxa; ++i) {
                    g2s.put(i, geneTips2Species.get(taxaNames.get(i)));
                }

                final JukesCantorDistance jc = new JukesCantorDistance();
                jc.setPatterns(alignment);
                final Distance gdistance = new Distance() {
                    @Override
                    public double pairwiseDistance(final int t1, final int t2) {
                        final int s1 = g2s.get(t1);
                        final int s2 = g2s.get(t2);
                        double d = jc.pairwiseDistance(t1,t2)/mu;
                        if( s1 != s2 ) {
                            final int i = getDMindex(nSpecies, s1,s2);
                            final double minDist = 2 * spmin[i];
                            if( d <= minDist ) {
                                d = minDist * 1.001;
                            }
                        }
                        return d;
                    }
                };
                final ClusterTree gtreec = new ClusterTree();
                gtreec.initByName("initial", gtree, "taxonset", gtreeTaxa,
                        "clusterType", "upgma", "distance", gdistance);
            }
        }

        {
            final RealParameter lambda = birthRate.get();
            if( lambda != null ) {
                final double rh = stree.getRoot().getHeight();
                double l = 0;
                for(int i = 2; i < nSpecies+1; ++i) {
                    l += 1./i;
                }
                lambda.setValue((1 / rh) * l);
            }

            double totBranches = 0;
            final Node[] streeNodeas = stree.getNodesAsArray();
            for( final Node n : streeNodeas ) {
                if( ! n.isRoot() ) {
                    totBranches += n.getLength();
                }
            }
            totBranches /= 2* (streeNodeas.length - 1);
            final RealParameter popm = popMean.get();
            if( popm != null ) {
                popm.setValue(totBranches);
            }
            final SpeciesTreePrior speciesTreePrior = speciesTreePriorInput.get();
            if( speciesTreePrior != null ) {
                final RealParameter popb = speciesTreePrior.popSizesBottomInput.get();
                if( popb != null ) {
                    for(int i = 0; i < popb.getDimension(); ++i) {
                      popb.setValue(i, 2*totBranches);
                    }
                }
                final RealParameter popt = speciesTreePrior.popSizesTopInput.get();
                if( popt != null ) {
                    for(int i = 0; i < popt.getDimension(); ++i) {
                        popt.setValue(i, totBranches);
                    }
                }
            }
        }
    }

    private void randomInitGeneTrees(double speciesTreeHeight) {
      final List<Tree> geneTrees = genes.get();
        for (final Tree gtree : geneTrees) {
            gtree.makeCaterpillar(speciesTreeHeight, speciesTreeHeight/gtree.getInternalNodeCount(), true);
        }
    }

    private void randomInit() throws Exception {
        double lam = 1;
        final RealParameter lambda = birthRate.get();
        if( lambda != null ) {
            lam = lambda.getArrayValue();
        }
        final Tree stree = speciesTreeInput.get();
        final TaxonSet species = stree.m_taxonset.get();
        final int nSpecies = species.asStringList().size();
        double s = 0;
        for(int k = 2; k <= nSpecies; ++k) {
            s += 1.0/k;
        }
        final double rootHeight = (1/lam) * s;
        stree.scale(rootHeight/stree.getRoot().getHeight());
        randomInitGeneTrees(rootHeight);
//        final List<Tree> geneTrees = genes.get();
//        for (final Tree gtree : geneTrees) {
//            gtree.makeCaterpillar(rootHeight, rootHeight/gtree.getInternalNodeCount(), true);
//        }
    }

    private void initWithCalibrations() throws Exception {
        final CalibratedYuleModel cYule = calibratedYule.get();
        final Tree spTree = (Tree) cYule.treeInput.get();

        final List<CalibrationPoint> cals = cYule.calibrationsInput.get();

        final CalibratedYuleModel cym = new CalibratedYuleModel();
        for( final CalibrationPoint cal : cals ) {
          cym.setInputValue("calibrations", cal);
        }
        cym.setInputValue("tree", spTree);
        cym.setInputValue("type", CalibratedYuleModel.Type.NONE);
        cym.initAndValidate();

        final Tree t = cym.compatibleInitialTree();

        spTree.assignFromWithoutID(t);

//        final CalibratedYuleInitialTree ct = new CalibratedYuleInitialTree();
//        ct.initByName("initial", spTree, "calibrations", cYule.calibrationsInput.get());
//        ct.initStateNodes();
        final double rootHeight = spTree.getRoot().getHeight();
        randomInitGeneTrees(rootHeight);
    }

    @Override
    public void getInitialisedStateNodes(final List<StateNode> stateNodes) {
        if( hasCalibrations ) {
            stateNodes.add((Tree) calibratedYule.get().treeInput.get());
        } else {
          stateNodes.add(speciesTreeInput.get());
        }

        for( final Tree g : genes.get() ) {
            stateNodes.add(g);
        }

        final RealParameter popm = popMean.get();
        if( popm != null ) {
            stateNodes.add(popm);
        }
        final RealParameter brate = birthRate.get();
        if( brate != null ) {
            stateNodes.add(brate) ;
        }

        final SpeciesTreePrior speciesTreePrior = speciesTreePriorInput.get();
        if( speciesTreePrior != null ) {
            final RealParameter popb = speciesTreePrior.popSizesBottomInput.get();
            if( popb != null ) {
                stateNodes.add(popb) ;
            }
            final RealParameter popt = speciesTreePrior.popSizesTopInput.get();
            if( popt != null ) {
                stateNodes.add(popt);
            }
        }
    }
}
