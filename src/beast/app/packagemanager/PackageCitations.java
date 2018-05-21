package beast.app.packagemanager;

import beast.core.BEASTInterface;
import beast.core.BEASTObject;
import beast.core.Citation;
import beast.core.Description;
import beast.core.util.Log;
import beast.util.Package;
import beast.util.PackageDependency;
import beast.util.PackageManager;
import beast.util.PackageVersion;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Print the citation annotated in a class inherited from BEASTObject.
 * @author Walter Xie
 */
public class PackageCitations {

//    protected final Package pkg;
    protected File[] libJarFile;
    protected Map<String, CitedClass> citedClassMap = new TreeMap<>();

    //TODO unique citations ?
    public PackageCitations(Package pkg) {
//        this.pkg = pkg;
        try {
            libJarFile = guessLibJarFile(pkg);
            assert libJarFile != null;

            addJarFilesToClassPath();

            setCitedClassMap(libJarFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addJarFilesToClassPath() throws IOException {
        // add all jars to class path
        for (File f: libJarFile)
            PackageManager.addURL(f.toURL());
    }

    public void removeJarFilesFromClassPath() {
        String separator = System.getProperty("path.separator");
        String classpath = System.getProperty("java.class.path");
        for (File f: libJarFile) {
            String fPath = f.toString();
            if (classpath.contains(fPath)) {
                classpath = classpath.replace(separator + fPath, "");
            }
        }
        System.setProperty("java.class.path", classpath);
    }

    /**
     * Find all cited classes from a {@link Package beast package}.
     * @return the total number of cited classes
     * @throws IOException
     */
    public int findAllCitedClasses() {
//        setCitedClassMap(libJarFile);
        printCitedClasses(getCitedClassMap());
        return getCitedClassMap().size();
    }

    public Map<String, CitedClass> getCitedClassMap() {
        return citedClassMap;
    }

    /**
     * Add all cited classes from jar files in a {@link Package beast package} lib dir.
     * @param libJarFile jar files
     * @throws IOException
     */
    private void setCitedClassMap(File[] libJarFile) throws IOException {
        for (File f: libJarFile) {
            Log.info.println("Load classes from : " + f + "");

            Map<String, CitedClass> tmp = getAllCitedClasses(f);
            // add all to the final map
            tmp.keySet().removeAll(citedClassMap.keySet());
            citedClassMap.putAll(tmp);
        }
        Log.info.println();
    }

    /**
     * get a {@link Citation Citation} list from a beast class.
     * @see BEASTInterface#getCitationList()
     * @param beastClass
     * @return
     */
    public List<Citation> getCitationList(Class<?> beastClass) {
        final Annotation[] classAnnotations = beastClass.getAnnotations();
        List<Citation> citations = new ArrayList<>();
        for (final Annotation annotation : classAnnotations) {
            if (annotation instanceof Citation) {
                citations.add((Citation) annotation);
            }
            if (annotation instanceof Citation.Citations) {
                for (Citation citation : ((Citation.Citations) annotation).value()) {
                    citations.add(citation);
                }
            }
        }
        return citations;
    }

    /**
     * get a description from a beast class.
     * @see BEASTInterface#getDescription()
     * @param beastClass
     * @return
     */
    public String getDescription(Class<?> beastClass) {
        final Annotation[] classAnnotations = beastClass.getAnnotations();
        for (final Annotation annotation : classAnnotations) {
            if (annotation instanceof Description) {
                final Description description = (Description) annotation;
                return description.value();
            }
        }
        return "Not documented!!!";
    }

    // find all *.jar in lib, but exclude *.src.jar
    private File[] guessLibJarFile(Package pkg) throws IOException {
        // get dir where pkg is installed
        String dirName = PackageManager.getPackageDir(pkg, pkg.getLatestVersion(), false, null);

        // beast installed package path
        File libDir = new File(dirName + File.separator + "lib");
        if (!libDir.exists())
            throw new IOException("Cannot find package " + pkg.getName() + " in path " + dirName);

        // first guess: *.jar but exclude *.src.jar
        File[] libFiles = libDir.listFiles((dir, name) -> name.endsWith(".jar") && !name.endsWith("src.jar"));
        if (libFiles == null || libFiles.length < 1)
            throw new IOException("Cannot find jar file in package " +  pkg.getName() + " in path " + dirName);

        return libFiles;
    }

    // find all cited classes from a jar file
    private Map<String, CitedClass> getAllCitedClasses(File libFile) throws IOException {
        // find all *.class in the jar
        JarFile jarFile = new JarFile(libFile);
        Enumeration allEntries = jarFile.entries();

        Map<String, CitedClass> citedClassMap = new TreeMap<>();
        while (allEntries.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) allEntries.nextElement();
            String name = jarEntry.getName();
            // exclude tests, cern (colt.jar) and com (google) have troubles
            if ( name.endsWith(".class") && !(name.startsWith("test") || name.startsWith("cern") || name.startsWith("com")) ) {
                String className = name.replaceAll("/", "\\.");
                className = className.substring(0, className.lastIndexOf('.'));

//                if (!className.startsWith("beast"))
//                    System.out.println(className);
//                System.out.println(System.getProperty("java.class.path"));

                // making own child classloader
                // https://stackoverflow.com/questions/60764/how-should-i-load-jars-dynamically-at-runtime/60775#60775
                URLClassLoader child = new URLClassLoader(new URL[]{libFile.toURL()},
                        PackageCitations.class.getClassLoader());
                Class<?> beastClass = null;
                try {
                    beastClass = Class.forName(className, false, child);
                } catch (Throwable t) {
                    t.printStackTrace();
                    throw new IOException(className + " cannot be loaded by ClassLoader !");
                }

                // no abstract classes
                if (!Modifier.isAbstract(beastClass.getModifiers()) &&
                        // must implement interface
                        (beastClass.isInterface() && PackageManager.hasInterface(BEASTObject.class, beastClass)) ||
                        // must be derived from class
                        (!beastClass.isInterface() && PackageManager.isSubclass(BEASTObject.class, beastClass))) {

                    List<Citation> citations = getCitationList(beastClass);
                    // add citations (if any)
                    if (citations.size() > 0) {
//                        System.out.println(className);
                        CitedClass citedClass = new CitedClass(className, citations);
                        String description = getDescription(beastClass);
                        // add description when having a citation
                        citedClass.setDescription(description);

                        citedClassMap.put(className, citedClass);
                    }
                }
            }
        }
        return citedClassMap;
    }

    // print citedClassMap
    private void printCitedClasses(Map<String, CitedClass> citedClassMap) {
        for (Map.Entry<String, CitedClass> entry : citedClassMap.entrySet()) {
            Log.info.println(entry.getKey());
            CitedClass citedClass = entry.getValue();
            Log.info.println(citedClass.getCitations());
            Log.info.println("Description : " + citedClass.getDescription() + "\n");
        }
        Log.info.println("Find total " + citedClassMap.size() + " cited BEAST classes.");
        Log.info.println();
    }

    private void toXML(){
        //TODO
    }

    // update/install all packages
    private static void installOrUpdateAllPackages(Map<String, Package> packageMap) throws IOException {
        Log.info.println("Start update/install all packages ...");
        for (Package aPackage : packageMap.values()) {
            Map<Package, PackageVersion> packagesToInstall = new HashMap<>();
            // always latest version
            packagesToInstall.put(aPackage, aPackage.getLatestVersion());
            try {
                // Populate given map with versions of packages to install which satisfy dependencies
                PackageManager.populatePackagesToInstall(packageMap, packagesToInstall);
            } catch (PackageManager.DependencyResolutionException ex) {
                Log.err("Installation aborted: " + ex.getMessage());
            }
            // Look through packages to be installed,
            // and uninstall any that are already installed but not match the version that is to be installed.
            PackageManager.prepareForInstall(packagesToInstall, false, null);
            // Download and install specified versions of packages
            Map<String, String> dirs = PackageManager.installPackages(packagesToInstall, false, null);
            if (dirs.size() == 0) {
                Log.info.println("Skip installed latest version package " + aPackage + " " + aPackage.getLatestVersion() + ".");
            } else {
                for (String pkgName : dirs.keySet())
                    Log.info.println("Package " + pkgName + " is installed in " + dirs.get(pkgName) + ".");
            }
        }
    }

    //find all installed and available packages
    private static Map<String, Package> getInstalledAvailablePackages() {
        // String::compareToIgnoreCase
        Map<String, Package> packageMap = new TreeMap<>(Comparator.comparing(String::toLowerCase));
        try {
            PackageManager.addInstalledPackages(packageMap);
            PackageManager.addAvailablePackages(packageMap);
        } catch (PackageManager.PackageListRetrievalException e) {
            Log.warning.println(e.getMessage());
            if (e.getCause() instanceof IOException)
                Log.warning.println(PackageManager.NO_CONNECTION_MESSAGE);
            return null;
        }
        Log.info.println("Find installed and available " + packageMap.size() + " packages.");
        return packageMap;
    }


    // process citations for pkg and add name to processedPkgMap
    private static int processCitations(Package pkg, Map<String, PackageCitations> processedPkgMap) throws IOException {
        if (processedPkgMap.containsKey(pkg.getName())) {
            PackageCitations packageCitations = processedPkgMap.get(pkg.getName());
            packageCitations.addJarFilesToClassPath();
            return 0;
        } else {
            Log.info.println("====== Package " + (processedPkgMap.size() + 1) + " : " + pkg.getName() + " ======\n");

            PackageCitations packageCitations = new PackageCitations(pkg);
            processedPkgMap.put(pkg.getName(), packageCitations);
            return packageCitations.findAllCitedClasses();
        }
    }

    private static void cleanClassPath(Map<String, PackageCitations> processedPkgMap) throws MalformedURLException {
        for (Map.Entry<String, PackageCitations> entry : processedPkgMap.entrySet()) {
            if (! (entry.getKey().equalsIgnoreCase("beast2") ||
                    entry.getKey().equalsIgnoreCase("beast")) ) {
                PackageCitations packageCitations = entry.getValue();
                packageCitations.removeJarFilesFromClassPath();
            }
        }
    }

    // only work for BEASTObject
    public static void main(String[] args) throws IOException {
        //****** find all installed and available packages ******//
        Map<String, Package> packageMap = getInstalledAvailablePackages();
        if (packageMap == null) return;

        //****** update/install all packages ******//
        if (false)
            installOrUpdateAllPackages(packageMap);

        //****** list all citations ******//
        int cc = 0;
        Map<String, PackageCitations> processedPkgMap = new TreeMap<>(Comparator.comparing(String::toLowerCase));
        for (Map.Entry<String, Package> entry : packageMap.entrySet()) {
            Package pkg = entry.getValue();
            // process depended packages first
            Set<PackageDependency> dependencies = pkg.getDependencies(pkg.getLatestVersion());
            for (PackageDependency dependency : dependencies) {
                Package depPkg = packageMap.get(dependency.dependencyName);
                cc += processCitations(depPkg, processedPkgMap);
            }
            cc += processCitations(pkg, processedPkgMap);

            cleanClassPath(processedPkgMap);
//System.out.println(System.getProperty("java.class.path"));
        }

        Log.info.println("====== Summary ======\n");
        Log.info.println("Count " + packageMap.size() + " BEAST packages.");
        Log.info.println("Find total " + cc + " cited BEAST classes. \n");
    }

}
