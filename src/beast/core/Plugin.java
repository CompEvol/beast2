/*
* File Plugin.java
*
* Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
*
* This file is part of BEAST2.
* See the NOTICE file distributed with this work for additional
* information regarding copyright ownership and licensing.
*
* BEAST is free software; you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
*  BEAST is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with BEAST; if not, write to the
* Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
* Boston, MA  02110-1301  USA
*/
package beast.core;


import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Description(
        value = "Base class for all plug-ins, which is pretty much every class " +
                "you want to incorporate in a model.",
        isInheritable = false
)
public class Plugin {
    // identifiable
    protected String m_sID;

    public String getID() {
        return m_sID;
    }

    public void setID(String sID) {
        m_sID = sID;
    }


//    public Plugin() {
//        if (this instanceof Cacheable) {
//            cacheables.add((Cacheable) this);
//        }
//    }

    /**
     * Extract description from @Description annotation *
     */
    public String getDescription() {
        Annotation[] classAnnotations = this.getClass().getAnnotations();
        for (Annotation annotation : classAnnotations) {
            if (annotation instanceof Description) {
                Description description = (Description) annotation;
                return description.value();
            }
        }
        return "Not documented!!!";
    }

    /**
     * Extract citation from @Citation annotation *
     */
    public final Citation getCitation() {
        Annotation[] classAnnotations = this.getClass().getAnnotations();
        for (Annotation annotation : classAnnotations) {
            if (annotation instanceof Citation) {
                return (Citation) annotation;
            }
        }
        return null;
    }

    /**
     * produce references for this plug in and all its inputs *
     */
    public final String getCitations() {
        return getCitations(new HashSet<String>());
    }

    private final String getCitations(HashSet<String> bDone) {
        StringBuffer buf = new StringBuffer();
        if (!bDone.contains(getID())) {
            // only add citation if it is not already processed
            if (getCitation() != null) {
                // and there is actually a citation to add
                buf.append(getCitation().value());
                buf.append("\n\n");
            }
            bDone.add(getID());
        }
        try {
            for (Plugin plugin : listActivePlugins()) {
                buf.append(plugin.getCitations(bDone));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buf.toString();
    } // getCitations

    public Input<?>[] listInputs() throws IllegalArgumentException, IllegalAccessException {
        List<Input<?>> sInputs = new ArrayList<Input<?>>();
        Field[] fields = getClass().getFields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getType().isAssignableFrom(Input.class)) {
                Input<?> input = (Input<?>) fields[i].get(this);
                sInputs.add(input);
            }
        }
        return sInputs.toArray(new Input[0]);
    } // listInputs

    /**
     * create array of all plug-ins in the inputs that are instantiated.
     * If the input is a List of plug-ins, these individual plug-ins are
     * added to the list.
     */
    @SuppressWarnings("unchecked")
    public Plugin[] listActivePlugins() throws IllegalArgumentException, IllegalAccessException {
        List<Plugin> sPlugins = new ArrayList<Plugin>();
        Field[] fields = getClass().getFields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getType().isAssignableFrom(Input.class)) {
                Input<?> input = (Input<?>) fields[i].get(this);
                if (input.get() != null) {
                    if (input.get() instanceof List<?>) {
                        List vector = (List<?>) input.get();
                        for (Object o : vector) {
                            if (o instanceof Plugin) {
                                sPlugins.add((Plugin) o);
                            }
                        }
                    } else if (input.get() != null && input.get() instanceof Plugin) {
                        sPlugins.add((Plugin) input.get());
                    }
                }
            }
        }
        return sPlugins.toArray(new Plugin[0]);
    } // listActivePlugins

    public String getTipText(String sName) throws IllegalArgumentException, IllegalAccessException {
        Field[] fields = getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getType().isAssignableFrom(Input.class)) {
                Input<?> input = (Input<?>) fields[i].get(this);
                if (input.getName().equals(sName)) {
                    return input.getTipText();
                }
            }
        }
        return null;
    } // getTipText


    public boolean isPrimitive(String sName) throws Exception {
        Input<?> input = getInput(sName);
        if (input.type() == null) {
            input.determineClass(this);
        }
        if (input.type().isAssignableFrom(Integer.class)) {
            return true;
        }
        if (input.type().isAssignableFrom(Double.class)) {
            return true;
        }
        if (input.type().isAssignableFrom(Boolean.class)) {
            return true;
        }
        if (input.type().isAssignableFrom(String.class)) {
            return true;
        }
        return false;
    } // isPrimitive

    public Object getInputValue(String sName) throws Exception {
        Input<?> input = getInput(sName);
        return input.get();
    } // getInputValue

    public void setInputValue(String sName, Object value) throws Exception {
        Input<?> input = getInput(sName);
        input.setValue(value, this);
    } // setInputValue

    public Input<?> getInput(String sName) throws Exception {
        Field[] fields = getClass().getFields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getType().isAssignableFrom(Input.class)) {
                Input<?> input = (Input<?>) fields[i].get(this);
                if (input.getName().equals(sName)) {
                    return input;
                }
            }
        }
        throw new Exception("This plugin (" + this.getID() + ") has no input with name " + sName);
    } // getInput

    /**
     * @param state
     * @throws Exception
     */
    public void initAndValidate(State state) throws Exception {
        // todo: AR - Why is this not an abstract method? Does Plugin need to be concrete?
        throw new Exception("Plugin.initAndValidate(): Every plugin should implement this method to assure the class behaves, even when inputs are not specified");
    }

    /**
     * check validation rules for all its inputs *
     */
    public void validateInputs() throws Exception {
        for (Input<?> input : listInputs()) {
            input.validate();
        }
    }

    public void addCondition(Input<? extends StateNode> stateNode) {
        if (this instanceof StateNode) throw new RuntimeException();
        if (stateNode.get() == null) return;

        if (conditions == null) conditions = new ArrayList<String>();

        conditions.add(stateNode.get().getID());
    }

    protected List<String> conditions = null;


//    public static final Set<Cacheable> cacheables = new HashSet<Cacheable>();

} // class Plugin
