package beast.evolution.speciation;

import beast.core.Description;
import beast.core.Input;
import beast.core.BEASTObject;
import beast.evolution.alignment.TaxonSet;
import beast.math.distributions.ParametricDistribution;

/**
* @author Joseph Heled
 */

@Description("Specification of a single calibration point of the calibrated Yule.")
public class CalibrationPoint extends BEASTObject {
    final public Input<TaxonSet> taxonsetInput = new Input<>("taxonset",
            "Set of taxa. The prior distribution is applied to their TMRCA.", Input.Validate.REQUIRED);

    final public Input<ParametricDistribution> distInput = new Input<>("distr",
            "Prior distribution applied to time of clade MRCA", Input.Validate.REQUIRED);

//    public Input<Boolean> m_bIsMonophyleticInput = new Input<>("monophyletic",
//            "whether the taxon set is monophyletic (forms a clade without other taxa) or nor. Default is false.", false);

    final public Input<Boolean> forParentInput = new Input<>("parentOf",
            "Use time of clade parent. Default is false.", false);


    private TaxonSet t;
    private boolean forPar;
    private ParametricDistribution pd;

    public CalibrationPoint() {}

    public void initAndValidate() throws Exception {
        t = taxonsetInput.get();
        forPar = forParentInput.get();
        pd = distInput.get();
    }

    public TaxonSet taxa() {
       return t;
    }

    public boolean forParent() {
        return forPar;
    }

    public ParametricDistribution dist() {
      return pd;
    }

    public double logPdf(final double x) {
        return pd.logDensity(x);
    }
}
