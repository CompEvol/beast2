package beast.pkgmgmt;


/**
 * modified by Walter Xie
 */
/** BEAUti beastObject dependency class **/
public class PackageDependency {
    public final String dependencyName;
    public final PackageVersion atLeast, atMost;

    public PackageDependency(String dependencyName,
                             PackageVersion minimumVersion,
                             PackageVersion maximumVersion) {
    	if (dependencyName.equals("beast2")) {
    		dependencyName = PackageManager.BEAST_PACKAGE_NAME;
    	}
        this.dependencyName = dependencyName;
        
        atLeast = minimumVersion;
        atMost = maximumVersion;
    }

    /**
     * Test to see whether given version of package satisfies
     * version range of this package dependency.
     *
     * @param version version of package to check
     * @return true iff version meets criterion
     */
    public boolean isMetBy(PackageVersion version) {
        return (atLeast == null || version.compareTo(atLeast)>=0)
                && (atMost == null || version.compareTo(atMost)<=0);
    }

    public String getRangeString() {
        if (atLeast != null && atMost != null)
            return "versions " + atLeast + " to " + atMost;

        if (atLeast != null)
            return "version " + atLeast + " or greater";

        return "version " + atMost + " or lesser";
    }

    @Override
	public String toString() {
        return dependencyName + " " + getRangeString();
    }
}
