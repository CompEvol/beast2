package beast.base;

import beast.base.core.Log;
import beast.base.core.Log.Level;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.Sequence;
import beast.base.evolution.likelihood.BeagleTreeLikelihood;
import beast.base.evolution.sitemodel.SiteModel;
import beast.base.evolution.substitutionmodel.JukesCantor;
import beast.base.evolution.tree.TreeParser;

public class CudaDetector {

	 /**
     * Used to detect whether CUDA with BEAGLE is installed on OS X in {@link Utils6#testCudaStatusOnMac()},
     * which is used by {@link beast.pkgmgmt.launcher.BeastLauncher#main(String[])}.
     * @see <a href="https://github.com/CompEvol/beast2/issues/500">issues 500</a>.
     */
    public static void main(String[] args) {
		try {
			Log.setLevel(Level.none);
			Sequence a = new Sequence("A", "A");
	        Sequence b = new Sequence("B", "A");
	        Sequence c = new Sequence("C", "A");
	        Sequence d = new Sequence("D", "A");

	        Alignment data = new Alignment();
	        data.initByName("sequence", a, "sequence", b, "sequence", c, "sequence", d, "dataType", "nucleotide");

	        TreeParser tree = new TreeParser();
	        tree.initByName("taxa", data,
	                "newick", "(((A:1,B:1):1,C:2):1,D:3)",
	                "IsLabelledNewick", true);

	        JukesCantor JC = new JukesCantor();
	        JC.initAndValidate();

	        SiteModel siteModel = new SiteModel();
	        siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 1, "substModel", JC);

	    	BeagleTreeLikelihood likelihood = new BeagleTreeLikelihood();
	        likelihood.initByName("data", data, "tree", tree, "siteModel", siteModel);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


    	// System.out.println("Success");
    	// if we got this far, exit with status 0
		System.exit(0);
	}
}
