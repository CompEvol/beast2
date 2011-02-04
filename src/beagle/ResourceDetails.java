package beagle;

/**
 * An interface for reporting information about the available resources
 * as reported by the BEAGLE API.
 * @author Andrew Rambaut
 * @version $Id$
 */
public class ResourceDetails {

    public ResourceDetails(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getFlags() {
        return flags;
    }

    public void setFlags(long flags) {
        this.flags = flags;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("").append(getNumber()).append(" : ").append(getName()).append("\n");
        if (getDescription() != null) {
            String[] description = getDescription().split("\\|");
            for (String desc : description) {
                if (desc.trim().length() > 0) {
                    sb.append("    ").append(desc.trim()).append("\n");
                }
            }
        }
        sb.append("    Flags:");
        sb.append(BeagleFlag.toString(getFlags()));
        sb.append("\n");
        return sb.toString();
    }

    private final int number;
    private String name;
    private String description;
    private long flags;
}
