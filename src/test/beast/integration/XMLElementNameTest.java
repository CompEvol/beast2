package test.beast.integration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import beast.core.BEASTObject;
import beast.core.Input;
import beast.util.BEASTClassLoader;
import beast.util.PackageManager;
import beast.util.XMLParser;
import junit.framework.TestCase;


public class XMLElementNameTest extends TestCase {
    /**
     * test that Inputs have a unique name
     * It can happen that a derived class uses the same Input name as one of its ancestors
     */
    @Test
    public void test_NameUniqueness() {
        List<String> pluginNames = PackageManager.find(beast.core.BEASTObject.class, PackageManager.IMPLEMENTATION_DIR);
        List<String> improperInputs = new ArrayList<String>();
        for (String beastObjectName : pluginNames) {
            try {
                BEASTObject beastObject = (BEASTObject) BEASTClassLoader.forName(beastObjectName).newInstance();
                List<Input<?>> inputs = beastObject.listInputs();
                Set<String> names = new HashSet<String>();
                for (Input<?> input : inputs) {
                    String name = input.getName();
                    if (names.contains(name)) {
                        improperInputs.add(beastObjectName + "." + name);
                        break;
                    }
                    names.add(name);

                }
            } catch (InstantiationException e) {
                // ignore
            } catch (Exception e) {
                // ignore
            }
        }
        if (improperInputs.size() > 0) {
            String str = improperInputs.toString();
            str = str.replaceAll(",", "\n");
            System.err.println("Input names are not unique:\n" + str);
        }
        // not activated till problem with naming is solved
        assertTrue("Input names are not unique: " + improperInputs.toString(), improperInputs.size() == 0);
    }

    /**
     * test that Inputs that use reserved names have the correct type *
     */
    @Test
    public void test_ReservedElementNames() {
        // retrieve list of reserved names and their classes
        XMLParser parser = new XMLParser();
        HashMap<String, String> element2ClassMap = parser.getElement2ClassMap();

        // allow 'parameter' for any of the various parameter derivatives, not just RealParameter
        element2ClassMap.put("parameter", "beast.core.parameter.Parameter");

        // check each beastObject
        List<String> pluginNames = PackageManager.find(beast.core.BEASTObject.class, PackageManager.IMPLEMENTATION_DIR);
        List<String> improperInputs = new ArrayList<String>();
        for (String beastObjectName : pluginNames) {
            try {
                BEASTObject beastObject = (BEASTObject) BEASTClassLoader.forName(beastObjectName).newInstance();
                // check each input
                List<Input<?>> inputs = beastObject.listInputs();
                for (Input<?> input : inputs) {
                    if (element2ClassMap.containsKey(input.getName())) {
                        if (beastObject.getClass() == null) {
                            input.determineClass(beastObject);
                        }
                        Class<?> type = input.getType();
                        String baseType = element2ClassMap.get(input.getName());
                        if (!isDerivedType(type, baseType)) {
                            improperInputs.add(beastObjectName + "." + input.getName());
                        }
                    }
                }
            } catch (InstantiationException e) {
                // ignore
            } catch (Exception e) {
                // ignore
            }
        }
        if (improperInputs.size() > 0) {
            String str = improperInputs.toString();
            str = str.replaceAll(",", "\n");
            System.err.println("Reserved element names used for wrong types in:\n" + str);
        }
        // not activated till problem with naming is solved
        assertTrue("Reserved element names used for wrong types in: " + improperInputs.toString(), improperInputs.size() == 0);
    }

    /**
     * true if type is a class equal to or derived from baseType *
     */
    boolean isDerivedType(Class<?> type, String baseType) {
        if (baseType.equals(type.getName())) {
            return true;
        }
        Class<?> superType = type.getSuperclass();
        if (!superType.equals(Object.class)) {
            return isDerivedType(superType, baseType);
        }

        return false;
    }

} // class XMLElementNameTest

