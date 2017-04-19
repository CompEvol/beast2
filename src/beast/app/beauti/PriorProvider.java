package beast.app.beauti;

import java.util.List;

import beast.core.Distribution;

/** packages can implement a PriorProvider. The PrioListInputEditor will
 * pick up these PriorProviders by introspection. When a user selects the +
 * button, the user can check whichever PriorProvider to add a new Distribution
 * to the list of priors.
 */
public interface PriorProvider {
	
	/** create a distribution, but do not add to the prior -- this is handled
	 * by the PrioListInputEditor. If null is returned, the operator is canceled.
	 * @param doc useful to get information about the model being edited
	 * @return Distribution to be added to prior, or null if nothing should 
	 * be done.
	 */
	public List<Distribution> createDistribution(BeautiDoc doc);
	
	/** return description to be used in drop-down box for selecting among PriorProviders **/
	public String getDescription();
	
	/** 
	 * A provider can check that the document contains components it can provide
	 * a prior for, e.g. a prior over a geographical region can only be provided
	 * if there is a partition with geographical info in it. 
	 * @return true if a prior can be provided for this document, false otherwise.
	 **/
	default public boolean canProvidePrior(BeautiDoc doc) {
		return true;
	}
}
