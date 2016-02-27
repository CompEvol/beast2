package beast.evolution.speciation;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.MathException;

import beast.core.*;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.alignment.distance.Distance;
import beast.evolution.alignment.distance.JukesCantorDistance;
import beast.evolution.tree.Node;
import beast.evolution.tree.RandomTree;
import beast.evolution.tree.Tree;
import beast.evolution.tree.coalescent.ConstantPopulation;
import beast.math.distributions.MRCAPrior;
import beast.util.ClusterTree;

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

        @Override
		public String toString() {
            return ename;
        }

        private final String ename;
    }
    final public Input<Method> initMethod = new Input<>("method", "Initialise either with a totally random " +
            "state or a point estimate based on alignments data (default point-estimate)",
            Method.POINT, Method.values());

    final public Input<Tree> speciesTreeInput = new Input<>("speciesTree", "The species tree to initialize");

    final public Input<List<Tree>> genes = new Input<>("gene", "Gene trees to initialize", new ArrayList<>());
    //,
    //        Validate.REQUIRED);

    final public Input<CalibratedYuleModel> calibratedYule = new Input<>("calibratedYule",
            "The species tree (with calibrations) to initialize", Validate.XOR, speciesTreeInput);

    final public Input<RealParameter> popMean = new Input<>("popMean",
            "Population mean hyper prior to initialse");

    final public Input<RealParameter> birthRate = new Input<>("birthRate",
            "Tree prior birth rate to initialize");

    final public Input<SpeciesTreePrior> speciesTreePriorInput =
            new Input<>("speciesTreePrior", "Population size parameters to initialise");

    final public Input<Function> muInput = new Input<>("baseRate",
            "Main clock rate used to scale trees (default 1).");


    private boolean hasCalibrations;

    @Override
    public void initAndValidate() {
        // what does this do and is it dangerous to call it or not to call it at the start or at the end??????
        super.initAndValidate();
        hasCalibrations = calibratedYule.get() != null;
    }

    @Override
    public void initStateNodes() {

        final Set<BEASTInterface> treeOutputs = speciesTreeInput.get().getOutputs();
        List<MRCAPrior> calibrations = new ArrayList<>();
        for (final Object plugin : treeOutputs ) {
            if( plugin instanceof MRCAPrior ) {
                calibrations.add((MRCAPrior) plugin);
            }
        }

        if( hasCalibrations ) {
            if( calibrations.size() > 0 ) {
                throw new IllegalArgumentException("Not implemented: mix of calibrated yule and MRCA priors: " +
                        "place all priors in the calibrated Yule");
            }
            try {
				initWithCalibrations();
			} catch (MathException e) {
				throw new IllegalArgumentException(e);
			}
        } else {
            if( calibrations.size() > 0 )  {
                initWithMRCACalibrations(calibrations);
                return;
            }

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

    private double[] firstMeetings(final Tree gtree, final Map<String, Integer> tipName2Species, final int speciesCount) {
        final Node[] nodes = gtree.listNodesPostOrder(null, null);
        @SuppressWarnings("unchecked")
		final Set<Integer>[] tipsSpecies = new Set[nodes.length];
        for(int k = 0; k < tipsSpecies.length; ++k) {
            tipsSpecies[k] = new HashSet<>();
        }
        // d[i,j] = minimum height of node which has tips belonging to species i and j
        // d is is upper triangular
        final double[] dmin = new double[(speciesCount*(speciesCount-1))/2];
        Arrays.fill(dmin, Double.MAX_VALUE);

        for (final Node n : nodes) {
            if (n.isLeaf()) {
                tipsSpecies[n.getNr()].add(tipName2Species.get(n.getID()));
            } else {
                assert n.getChildCount() == 2;
                @SuppressWarnings("unchecked")
				final Set<Integer>[] sps = new Set[2];
                sps[0] = tipsSpecies[n.getChild(0).getNr()];
                sps[1] = tipsSpecies[n.getChild(1).getNr()];
                final Set<Integer> u = new HashSet<>(sps[0]);
                u.retainAll(sps[1]);
                sps[0].removeAll(u);
                sps[1].removeAll(u);

                for (final Integer s1 : sps[0]) {
                    for (final Integer s2 : sps[1]) {
                        final int i = getDMindex(speciesCount, s1, s2);
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

    private int getDMindex(final int speciesCount, final int s1, final int s2) {
        final int mij = min(s1,s2);
        return (mij*(2*speciesCount-1 - mij))/2 + (abs(s1-s2)-1);
    }


    private void fullInit() {
        // Build gene trees from  alignments

        final Function muInput = this.muInput.get();
        final double mu =  (muInput != null )  ? muInput.getArrayValue() : 1;

        final Tree stree = speciesTreeInput.get();
        final TaxonSet species = stree.m_taxonset.get();
        final List<String> speciesNames = species.asStringList();
        final int speciesCount = speciesNames.size();

        final List<Tree> geneTrees = genes.get();

        //final List<Alignment> alignments = genes.get();
        //final List<Tree> geneTrees = new ArrayList<>(alignments.size());
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
        final Map<String, Integer> geneTips2Species = new HashMap<>();
        final List<Taxon> taxonSets = species.taxonsetInput.get();

        for(int k = 0; k < speciesNames.size(); ++k) {
            final Taxon nx = taxonSets.get(k);
            final List<Taxon> taxa = ((TaxonSet) nx).taxonsetInput.get();
            for( final Taxon n : taxa ) {
              geneTips2Species.put(n.getID(), k);
            }
        }
        final double[] dg = new double[(speciesCount*(speciesCount-1))/2];

        final double[][] genesDmins = new double[geneTrees.size()][];

        for( int ng = 0; ng < geneTrees.size(); ++ng ) {
            final Tree g = geneTrees.get(ng);
            final double[] dmin = firstMeetings(g, geneTips2Species, speciesCount);
            genesDmins[ng] = dmin;

            for(int i = 0; i < dmin.length; ++i) {
                dg[i] += dmin[i];
                if (dmin[i] == Double.MAX_VALUE) {
                	// this happens when a gene tree has no taxa for some species-tree taxon.
                	// TODO: ensure that if this happens, there will always be an "infinite"
                	// distance between species-taxon 0 and the species-taxon with missing lineages,
                	// so i < speciesCount - 1.
                	// What if lineages for species-taxon 0 are missing? Then all entries will be 'infinite'.
                	String id = (i < speciesCount - 1? stree.getExternalNodes().get(i+1).getID() : "unknown taxon");
                	if (i == 0) {
                		// test that all entries are 'infinite', which implies taxon 0 has lineages missing 
                		boolean b = true;
                		for (int k = 1; b && k < speciesCount - 1; k++) {
                			b = (dmin[k] == Double.MAX_VALUE);
                		}
                		if (b) {
                			// if all entries have 'infinite' distances, it is probably the first taxon that is at fault
                			id = stree.getExternalNodes().get(0).getID();
                		}
                	}
                	throw new RuntimeException("Gene tree " + g.getID() + " has no lineages for species taxon " + id + " ");
                }
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
                final int i = getDMindex(speciesCount, s1,s2);
                return dg[i];
            }
        };
        ctree.initByName("initial", stree, "taxonset", species,"clusterType", "upgma", "distance", distance);

        final Map<String, Integer> sptips2SpeciesIndex = new HashMap<>();
        for(int i = 0; i < speciesNames.size(); ++i) {
            sptips2SpeciesIndex.put(speciesNames.get(i), i);
        }
        final double[] spmin = firstMeetings(stree, sptips2SpeciesIndex, speciesCount);

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
                final int taxonCount =  taxaNames.size();
                // speedup
                final Map<Integer,Integer> g2s = new HashMap<>();
                for(int i = 0; i < taxonCount; ++i) {
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
                            final int i = getDMindex(speciesCount, s1,s2);
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
                for(int i = 2; i < speciesCount+1; ++i) {
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

    private void randomInit() {
        double lam = 1;
        final RealParameter lambda = birthRate.get();
        if( lambda != null ) {
            lam = lambda.getArrayValue();
        }
        final Tree stree = speciesTreeInput.get();
        final TaxonSet species = stree.m_taxonset.get();
        final int speciesCount = species.asStringList().size();
        double s = 0;
        for(int k = 2; k <= speciesCount; ++k) {
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

    private void initWithCalibrations() throws MathException {
        final CalibratedYuleModel cYule = calibratedYule.get();
        final Tree spTree = (Tree) cYule.treeInput.get();

        final List<CalibrationPoint> cals = cYule.calibrationsInput.get();

        final CalibratedYuleModel cym = new CalibratedYuleModel();
        
        cym.getOutputs().addAll(cYule.getOutputs());

        for( final CalibrationPoint cal : cals ) {
          cym.setInputValue("calibrations", cal);
        }
        cym.setInputValue("tree", spTree);
        cym.setInputValue("type", CalibratedYuleModel.Type.NONE);
        cym.initAndValidate();

        final Tree t = cym.compatibleInitialTree();
        assert spTree.getLeafNodeCount() == t.getLeafNodeCount();

        spTree.assignFromWithoutID(t);

//        final CalibratedYuleInitialTree ct = new CalibratedYuleInitialTree();
//        ct.initByName("initial", spTree, "calibrations", cYule.calibrationsInput.get());
//        ct.initStateNodes();
        final double rootHeight = spTree.getRoot().getHeight();
        randomInitGeneTrees(rootHeight);

        cYule.initAndValidate();
    }

    private void initWithMRCACalibrations(List<MRCAPrior> calibrations) {
        final Tree spTree = speciesTreeInput.get();
        final RandomTree rnd = new RandomTree();
        rnd.setInputValue("taxonset", spTree.getTaxonset());

        for( final MRCAPrior cal : calibrations ) {
          rnd.setInputValue("constraint", cal);
        }
        ConstantPopulation pf = new ConstantPopulation();
        pf.setInputValue("popSize", new RealParameter("1.0"));

        rnd.setInputValue("populationModel", pf);
        rnd.initAndValidate();
        spTree.assignFromWithoutID((Tree)rnd);

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
