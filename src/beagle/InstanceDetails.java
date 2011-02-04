package beagle;

/**
 * An interface for reporting information about a particular instance
 * as reported by the BEAGLE API.
 * @author Andrew Rambaut
 * @author Marc Suchard
 * @version $Id$
 */
public class InstanceDetails {

    public InstanceDetails() {
    }

    public int getResourceNumber() {
        return resourceNumber;
    }

    public void setResourceNumber(int resourceNumber) {
        this.resourceNumber = resourceNumber;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getImplementationName() {
        return implementationName;
    }

    public void setImplementationName(String implementationName) {
        this.implementationName = implementationName;
    }

    public long getFlags() {
        return flags;
    }

    public void setFlags(long flags) {
        this.flags = flags;
    }

    @Override
    public String toString() {
        return BeagleFlag.toString(getFlags());
    }

    private int resourceNumber;
    private long flags;
    private String resourceName;
    private String implementationName;
}
