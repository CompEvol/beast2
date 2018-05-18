package beast.app.packagemanager;

import beast.core.BEASTInterface;
import beast.core.BEASTObject;
import beast.core.Citation;
import beast.core.Description;
import beast.util.Package;
import beast.util.PackageManager;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
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

    private final Package pkg;

    //TODO unique citations ?
    public PackageCitations(Package pkg) {
        this.pkg = pkg;
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

    /**
     * Find all cited classes from a {@link Package beast package}.
     * @return the total number of cited classes
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public int findAllCitedClasses() throws IOException, ClassNotFoundException {
        // get dir where pkg is installed
        String dirName = PackageManager.getPackageDir(pkg, pkg.getLatestVersion(), false, null);

        // beast installed package path
        File libDir = new File(dirName + File.separator + "lib");
        if (!libDir.exists())
            throw new IOException("Cannot find package " + pkg.getName() + " in path " + dirName);

        // *.jar but exclude *.src.jar
        File[] libFiles = libDir.listFiles((dir, name) -> name.endsWith("jar") && !name.endsWith("src.jar"));
        if (libFiles == null || libFiles.length != 1)
            throw new IllegalArgumentException("Invalid number of jar file found in package " +
                    pkg.getName() + " in path " + dirName);
        System.out.println("Load classes from : " + libFiles[0] + "\n");

        Map<String, CitedClass> citedClassMap = getAllCitedClasses(libFiles[0]);

        printCitedClasses(citedClassMap);
        return citedClassMap.size();
    }

    // find all cited classes from a jar file
    private Map<String, CitedClass> getAllCitedClasses(File libFile) throws IOException, ClassNotFoundException {
        // find all *.class in the jar
        JarFile jarFile = new JarFile(libFile);
        Enumeration allEntries = jarFile.entries();

        Map<String, CitedClass> citedClassMap = new TreeMap<>();
        while (allEntries.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) allEntries.nextElement();
            String name = jarEntry.getName();
            // exclude tests and colt.jar
            if ( name.endsWith(".class") && !(name.startsWith("test") || name.startsWith("cern")) ) {
                String className = name.replaceAll("/", "\\.");
                className = className.substring(0, className.lastIndexOf('.'));

//                if (!className.startsWith("beast"))
//                    System.out.println(className);

                // making own child classloader
                // https://stackoverflow.com/questions/60764/how-should-i-load-jars-dynamically-at-runtime/60775#60775
                URLClassLoader child = new URLClassLoader(new URL[]{libFile.toURL()},
                        PackageCitations.class.getClassLoader());
                Class<?> beastClass = Class.forName(className, false, child);

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
            System.out.println(entry.getKey());
            CitedClass citedClass = entry.getValue();
            System.out.println(citedClass.getCitations());
            System.out.println("Description : " + citedClass.getDescription() + "\n");
        }
        System.out.println("Find total " + citedClassMap.size() + " cited BEAST classes.");
        System.out.println();
    }

    private void toXML(){
        //TODO
    }

    // only work for BEASTObject
    public static void main(String[] args) throws IOException, ClassNotFoundException {

        TreeMap<String, Package> packages = new TreeMap<>();

        // TODO update/install all
//        PackageManager.installPackages(all);

        // add them into TreeMap<String, Package> packages
        PackageManager.addInstalledPackages(packages);
//        String[] pkgnames = packages.keySet().toArray(new String[]{});

        int p = 0;
        int cc = 0;
        for (Map.Entry<String, Package> entry : packages.entrySet()) {
            System.out.println("====== Package " + (p+1) + " : " + entry.getKey() + " ======\n");

            Package pkg = entry.getValue();
            PackageCitations packageCitations = new PackageCitations(pkg);
            cc += packageCitations.findAllCitedClasses();

            p++;
        }

        System.out.println("====== Summary ======\n");
        System.out.println("Count " + packages.size() + " BEAST packages.");
        System.out.println("Find total " + cc + " cited BEAST classes. \n");
    }

}
