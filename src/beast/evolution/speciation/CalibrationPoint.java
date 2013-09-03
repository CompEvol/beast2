package beast.evolution.speciation;

import beast.core.Description;
import beast.core.Input;
import beast.core.BEASTObject;
import beast.evolution.alignment.TaxonSet;
import beast.math.distributions.ParametricDistribution;


@Description("Specification of a single calibration point of the calibrated Yule.")
public class CalibrationPoint extends BEASTObject {
    public Input<TaxonSet> taxonsetInput = new Input<TaxonSet>("taxonset",
            "set of taxa. A prior distribution is applied to their MRCA.", Input.Validate.REQUIRED);

    public Input<ParametricDistribution> distInput = new Input<ParametricDistribution>("distr",
            "prior distribution applied to time of clade TMRCA", Input.Validate.REQUIRED);

//    public Input<Boolean> m_bIsMonophyleticInput = new Input<Boolean>("monophyletic",
//            "whether the taxon set is monophyletic (forms a clade without other taxa) or nor. Default is false.", false);

    public Input<Boolean> forParentInput = new Input<Boolean>("parentOf",
            "use time of clade parent. Default is false.", false);


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
