package beast.core;

import beast.util.Randomizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Runnable for generating a fixed number of samples from a prior distribution using
 * direct simulation.
 *
 * Created by Tim Vaughan <tgvaughan@gmail.com> on 16/06/17.
 */
@Description("Runnable for generating a fixed number of samples from a prior distribution" +
             "using direct simulation.")
public class DirectSimulator extends Runnable {

    public Input<Distribution> distributionInput = new Input<>("distribution",
            "Distribution to sample from via direct simulation.",
            Input.Validate.REQUIRED);

    public Input<List<Logger>> loggersInput = new Input<>("logger",
            "Log that simulated parameters and trees can be written to.",
            new ArrayList<>());

    public Input<Integer> nSamplesInput = new Input<>("nSamples",
            "Number of independent samples to generate.",
            Input.Validate.REQUIRED);

    State state;
    Distribution distribution;
    List<Logger> loggers;
    int nSamples;

    Random random;

    @Override
    public void initAndValidate() {
        distribution = distributionInput.get();
        loggers = loggersInput.get();
        nSamples = nSamplesInput.get();

        // Create new Random instance initialized with seed from Randomizer
        // (Necessary because sample() currently needs a Random instance and we
        // want to use the same seed specified on the command line.)
        random = new Random(Randomizer.getSeed());
    }

    public void clearSampledFlags(BEASTInterface obj) {
        if (obj instanceof Distribution)
            ((Distribution) obj).sampledFlag = false;

        for (String inputName : obj.getInputs().keySet()) {
            Input input = obj.getInput(inputName);

            if (input.get() == null)
                continue;

            if (input.get() instanceof List) {
                for (Object el : ((List)input.get())) {
                    if (el instanceof BEASTInterface)
                        clearSampledFlags((BEASTInterface)el);
                }
            } else if (input.get() instanceof BEASTInterface) {
                clearSampledFlags((BEASTInterface)(input.get()));
            }
        }
    }

    @Override
    public void run() throws Exception {

        // Initialize loggers
        for (Logger logger : loggers)
            logger.init();

        // Perform simulations
        for (int i=0; i<nSamples; i++) {
            clearSampledFlags(distribution);
            distribution.sample(state, random);

            for (Logger logger : loggers) {
                logger.log(i);
            }
        }

        // Finalize loggers
        for (Logger logger: loggers)
            logger.close();

        System.out.println("Direct simulation of " + nSamples + " samples completed.");
    }
}

