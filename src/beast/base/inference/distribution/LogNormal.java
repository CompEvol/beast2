package beast.base.inference.distribution;

import beast.base.core.Description;

/**
 * A subclass of LogNormalDistributionModel with a shorter, more user-friendly name.
 * This class provides exactly the same functionality as LogNormalDistributionModel
 * but can be referenced as "LogNormal" in XML and model scripts.
 */
@Description("A log-normal distribution with mean and variance parameters. Alias for LogNormalDistributionModel.")
public class LogNormal extends LogNormalDistributionModel {
    // No need to override anything - this class simply provides a nicer name
    // while inheriting all functionality from LogNormalDistributionModel
}