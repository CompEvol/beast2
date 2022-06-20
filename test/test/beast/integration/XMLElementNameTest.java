package test.beast.integration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import beast.base.core.BEASTObject;
import beast.base.core.Input;
import beast.base.parser.XMLParser;
import beast.pkgmgmt.BEASTClassLoader;
import beast.pkgmgmt.PackageManager;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class XMLElementNameTest  {
    /**
     * test that Inputs have a unique name
     * It can happen that a derived class uses the same Input name as one of its ancestors
     */
    @Test
    public void test_NameUniqueness() {
		final Set<String> pluginNames = PackageManager.listServices("beast.base.core.BEASTInterface");
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
        assertTrue(improperInputs.size() == 0,
        		"Input names are not unique: " + improperInputs.toString());
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
        element2ClassMap.put("parameter", "beast.base.inference.parameter.Parameter");
        

        // check each beastObject
		final Set<String> pluginNames = PackageManager.listServices("beast.base.core.BEASTInterface");
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
        
        element2ClassMap.put("parameter", "beast.base.inference.parameter.RealParameter");

        // not activated till problem with naming is solved
        assertTrue(improperInputs.size() == 0,
        		"Reserved element names used for wrong types in: " + improperInputs.toString());
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

