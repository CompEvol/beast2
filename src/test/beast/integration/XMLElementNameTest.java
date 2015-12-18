package test.beast.integration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import beast.core.BEASTObject;
import beast.core.Input;
import beast.util.AddOnManager;
import beast.util.XMLParser;
import junit.framework.TestCase;


public class XMLElementNameTest extends TestCase {
    /**
     * test that Inputs have a unique name
     * It can happen that a derived class uses the same Input name as one of its ancestors
     */
    @Test
    public void test_NameUniqueness() {
        List<String> sPluginNames = AddOnManager.find(beast.core.BEASTObject.class, AddOnManager.IMPLEMENTATION_DIR);
        List<String> sImproperInputs = new ArrayList<String>();
        for (String sPlugin : sPluginNames) {
            try {
                BEASTObject plugin = (BEASTObject) Class.forName(sPlugin).newInstance();
                List<Input<?>> inputs = plugin.listInputs();
                Set<String> sNames = new HashSet<String>();
                for (Input<?> input : inputs) {
                    String sName = input.getName();
                    if (sNames.contains(sName)) {
                        sImproperInputs.add(sPlugin + "." + sName);
                        break;
                    }
                    sNames.add(sName);

                }
            } catch (InstantiationException e) {
                // ignore
            } catch (Exception e) {
                // ignore
            }
        }
        if (sImproperInputs.size() > 0) {
            String sStr = sImproperInputs.toString();
            sStr = sStr.replaceAll(",", "\n");
            System.err.println("Input names are not unique:\n" + sStr);
        }
        // not activated till problem with naming is solved
        assertTrue("Input names are not unique: " + sImproperInputs.toString(), sImproperInputs.size() == 0);
    }

    /**
     * test that Inputs that use reserved names have the correct type *
     */
    @Test
    public void test_ReservedElementNames() {
        // retrieve list of reserved names and their classes
        XMLParser parser = new XMLParser();
        HashMap<String, String> sElement2ClassMap = parser.getElement2ClassMap();

        // allow 'parameter' for any of the various parameter derivatives, not just RealParameter
        sElement2ClassMap.put("parameter", "beast.core.parameter.Parameter");

        // check each plugin
        List<String> sPluginNames = AddOnManager.find(beast.core.BEASTObject.class, AddOnManager.IMPLEMENTATION_DIR);
        List<String> sImproperInputs = new ArrayList<String>();
        for (String sPlugin : sPluginNames) {
            try {
                BEASTObject plugin = (BEASTObject) Class.forName(sPlugin).newInstance();
                // check each input
                List<Input<?>> inputs = plugin.listInputs();
                for (Input<?> input : inputs) {
                    if (sElement2ClassMap.containsKey(input.getName())) {
                        if (plugin.getClass() == null) {
                            input.determineClass(plugin);
                        }
                        Class<?> type = input.getType();
                        String sBaseType = sElement2ClassMap.get(input.getName());
                        if (!isDerivedType(type, sBaseType)) {
                            sImproperInputs.add(sPlugin + "." + input.getName());
                        }
                    }
                }
            } catch (InstantiationException e) {
                // ignore
            } catch (Exception e) {
                // ignore
            }
        }
        if (sImproperInputs.size() > 0) {
            String sStr = sImproperInputs.toString();
            sStr = sStr.replaceAll(",", "\n");
            System.err.println("Reserved element names used for wrong types in:\n" + sStr);
        }
        // not activated till problem with naming is solved
        assertTrue("Reserved element names used for wrong types in: " + sImproperInputs.toString(), sImproperInputs.size() == 0);
    }

    /**
     * true if type is a class equal to or derived from sBaseType *
     */
    boolean isDerivedType(Class<?> type, String sBaseType) {
        if (sBaseType.equals(type.getName())) {
            return true;
        }
        Class<?> superType = type.getSuperclass();
        if (!superType.equals(Object.class)) {
            return isDerivedType(superType, sBaseType);
        }

        return false;
    }

} // class XMLElementNameTest

