package deadcode.inference.mle;

import beast.core.Description;
import beast.core.MCMC;
import beast.core.Input;
import beast.core.Runnable;

/**
 * Package: PathSamplingMLE
 * Description:
 * <p/>
 * <p/>
 * Created by
 *
 * @author Alexander V. Alekseyenko (alexander.alekseyenko@gmail.com)
 *         Date: Jul 1, 2010
 *         Time: 12:37:36 PM
 */

@Description("Perform Marginal Likelihood Estimation using Thermodynamic Integration (a.k.a. Path Sampling)."+
"Currently this does nothing... waiting for porting from Beast 1.x.y")
public class PathSamplingMLE extends Runnable {
    public Input<MCMC> m_originalMCMC = new Input<MCMC>("originalMCMC",
                                               "MCMC sampler to sample with...",
                                               Input.Validate.REQUIRED);
    public void initAndValidate(){

    }
    public void run() {
        System.err.println("Running MCMC inside PathSamplingMLE");
        try{
        m_originalMCMC.get().run();
        }
        catch (Exception e){
            System.err.println("There was some sort of error.");
        }
        System.err.println("Done running MCMC inside PathSamplingMLE");
    }

}
