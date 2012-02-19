package beast.evolution.speciation;

import beast.core.Description;
import beast.core.Input;
import beast.core.Plugin;
import beast.evolution.alignment.TaxonSet;
import beast.math.distributions.ParametricDistribution;


@Description("Specification of a single calibration point of the calibrated Yule.")
public class CalibrationPoint extends Plugin {
    public Input<TaxonSet> m_taxonset = new Input<TaxonSet>("taxonset",
            "set of taxa. A prior distribution is applied to their MRCA.", Input.Validate.REQUIRED);

    public Input<ParametricDistribution> m_distInput = new Input<ParametricDistribution>("distr",
            "prior distribution applied to time of clade TMRCA", Input.Validate.REQUIRED);

//    public Input<Boolean> m_bIsMonophyleticInput = new Input<Boolean>("monophyletic",
//            "whether the taxon set is monophyletic (forms a clade without other taxa) or nor. Default is false.", false);

    public Input<Boolean> m_forParent = new Input<Boolean>("parentOf",
            "use time of clade parent. Default is false.", false);


    private TaxonSet t;
    private boolean forPar;
    private ParametricDistribution pd;

    public CalibrationPoint() {}

    public void initAndValidate() throws Exception {
        t = m_taxonset.get();
        forPar = m_forParent.get();
        pd = m_distInput.get();
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
