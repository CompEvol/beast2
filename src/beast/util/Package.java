package beast.util;

import beast.core.Description;

import java.net.URL;
import java.util.*;

/**
 * BEAUti Package managed by AddOnManager.
 *
 * Add a new rule in issue 754:
 * Package manager should make project links compulsory.
 */
@Description("BEAUti package managed by package manager, also named as add-on previously. " +
        "Project URL is compulsory.")
public class Package {
    protected String packageName, description;
    protected PackageVersion installedVersion;
    protected Set<PackageDependency> installedVersionDeps;
    protected TreeMap<PackageVersion, URL> availableVersionURLs;
    protected TreeMap<PackageVersion, Set<PackageDependency>> availableVersionDeps;

    protected URL projectURL;

    public Package(String name) {
        this.packageName = name;
        this.description = "";

        // this class needs to be java6 compatible, so no <> notation
        availableVersionURLs = new TreeMap<PackageVersion, URL>();
        availableVersionDeps = new TreeMap<PackageVersion, Set<PackageDependency>>();
    }

    public String getName() {
        return packageName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public URL getProjectURL() {
        return projectURL;
    }

    public void setProjectURL(URL url) {
        this.projectURL = url;
    }


    /**
     * @return true if projectURL is valid
     */
    public boolean isValidFormat() {
        //https://www.geeksforgeeks.org/check-if-url-is-valid-or-not-in-java/
        try {
            getProjectURL().toURI();
            return true;
        }
        // If there was an Exception while creating URL object
        catch (Exception e) {
            return false;
        }
    }

    /**
     * @return true iff package is available online.
     */
    public boolean isAvailable() {
        return !availableVersionURLs.isEmpty();
    }

    /**
     * @param version version of package
     * @return true iff version of package is available
     */
    public boolean isAvailable(PackageVersion version) {
        return availableVersionURLs.containsKey(version);
    }

    public void addAvailableVersion(PackageVersion version, URL url, Set<PackageDependency> dependencies) {
        availableVersionURLs.put(version, url);
        availableVersionDeps.put(version, dependencies);
    }

    public void setInstalled(PackageVersion version, Set<PackageDependency> dependencies) {
        installedVersion = version;
        installedVersionDeps = dependencies;
    }

    public PackageVersion getInstalledVersion() {
        return installedVersion;
    }

    public boolean isInstalled() {
        return installedVersion != null;
    }

    /**
     * @return true if a newer version is available.
     */
    public boolean newVersionAvailable() {
        return isAvailable() &&
                (!isInstalled() || getLatestVersion().compareTo(getInstalledVersion()) > 0);
    }

    public Set<PackageDependency> getInstalledVersionDependencies() {
        return installedVersionDeps;
    }

    /**
     * @return latest available version of package.
     */
    public PackageVersion getLatestVersion() {
        return availableVersionURLs.isEmpty()
                ? null
                : availableVersionURLs.lastKey();
    }

    /**
     * @return URL corresponding to latest available version of package.
     */
    public URL getLatestVersionURL() {
        return isAvailable()
                ? availableVersionURLs.lastEntry().getValue()
                : null;
    }

    /**
     * Retrieve URL corresponding to particular available version of package.
     *
     * @param version version of package
     * @return URL
     */
    public URL getVersionURL(PackageVersion version) {
        return isAvailable(version)
                ? availableVersionURLs.get(version)
                : null;
    }

    public Set<PackageDependency> getLatestVersionDependencies() {
        return isAvailable()
                ? availableVersionDeps.lastEntry().getValue()
                : null;
    }

    public Set<PackageDependency> getDependencies(PackageVersion version) {
        return availableVersionDeps.get(version);
    }

    /**
     * @return list of available package versions, sorted in order of decreasing version.
     */
    public List<PackageVersion> getAvailableVersions() {
        // this class needs to be java6 compatible, so no <> notation
        List<PackageVersion> versionList = new ArrayList<PackageVersion>(availableVersionURLs.keySet());
        Collections.sort(versionList);
        Collections.reverse(versionList);
        return versionList;
    }

    public boolean latestVersionDependsOn(Package pkg) {

        if (!isAvailable())
            throw new IllegalStateException("Requested latest available version dependencies " +
                    "when there is no available version.");

        for (PackageDependency packageDependency : availableVersionDeps.lastEntry().getValue()) {
            if (packageDependency.dependencyName.equals(pkg.packageName))
                return true;
        }
        return false;
    }

    public boolean installedVersionDependsOn(Package pkg) {

        if (!isInstalled())
            throw new IllegalStateException("Requested installed version dependencies " +
                    "when there is no installed version.");

        for (PackageDependency packageDependency : installedVersionDeps) {
            if (packageDependency.dependencyName.equals(pkg.packageName))
                return true;
        }
        return false;
    }

    public String getDependenciesString() {

        if (availableVersionDeps.isEmpty())
            return "";

        String depString = "";
        for (PackageDependency packageDependency : availableVersionDeps.lastEntry().getValue()) {
            String s = packageDependency.dependencyName;
            if (!s.equalsIgnoreCase(AddOnManager.BEAST_PACKAGE_NAME)) {
                depString +=  s + ", ";
            }
        }
        if (depString.length() > 2) {
            depString = depString.substring(0, depString.length() - 2);
        }
        return depString;
    }

    @Override
	public String toString() {
        return packageName;
    }

    public String toHTML() {
        String html = "<html>";
        html += "<h1>" + packageName + "</h1>";
        html += "<p>Installed version: " + (isInstalled() ? getInstalledVersion() : "NA") + "</p>";
        html += "<p>Latest version: " + (isAvailable() ? getLatestVersion() : "NA") + "</p>";
        html += "<p>" + description +"</p>";
        html += "</html>";
        return html;
    }
}
