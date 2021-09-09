package beast.app.inputeditor;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

import beast.app.inputeditor.InputEditor.ButtonStatus;
import beast.app.inputeditor.InputEditor.ExpandOption;
import beast.base.BEASTInterface;
import beast.base.Input;
import beast.base.Log;
import beast.base.Input.Validate;
import beast.pkgmgmt.BEASTClassLoader;
import beast.pkgmgmt.PackageManager;



/** Can create InputEditors for inputs of BEASTObjects 
 * and there are some associated utility methods **/
public class InputEditorFactory {
    /**
     * map that identifies the InputEditor to use for a particular type of Input *
     */
    HashMap<Class<?>, String> inputEditorMap;
    HashMap<Class<?>, String> listInputEditorMap;
    BeautiDoc doc;
    
	public InputEditorFactory(BeautiDoc doc) {
		this.doc = doc;
		init();
	}

    public void init() {
        // register input editors
        inputEditorMap = new HashMap<>();
        listInputEditorMap = new HashMap<>();

//        String [] knownEditors = new String [] {"beast.app.draw.DataInputEditor","beast.app.beauti.AlignmentListInputEditor", "beast.app.beauti.FrequenciesInputEditor", "beast.app.beauti.OperatorListInputEditor", "beast.app.beauti.ParametricDistributionInputEditor", "beast.app.beauti.PriorListInputEditor", "beast.app.beauti.SiteModelInputEditor", "beast.app.beauti.TaxonSetInputEditor", "beast.app.beauti.TipDatesInputEditor", "beast.app.draw.BooleanInputEditor", "beast.app.draw.DoubleInputEditor", "beast.app.draw.EnumInputEditor", "beast.app.draw.IntegerInputEditor", "beast.app.draw.ListInputEditor", 
//        		"beast.app.draw.ParameterInputEditor", "beast.app.draw.PluginInputEditor", "beast.app.draw.StringInputEditor"};
//        registerInputEditors(knownEditors);
        String[] PACKAGE_DIRS = {"beast.app",};
        for (String packageName : PACKAGE_DIRS) {
            List<String> inputEditors = PackageManager.find("beast.app.draw.InputEditor", packageName);
            registerInputEditors(inputEditors.toArray(new String[0]));
        }
    }

    private void registerInputEditors(String[] inputEditors) {
    	//BeautiDoc doc = new BeautiDoc();
        for (String inputEditor : inputEditors) {
        	// ignore inner classes, which are marked with $
        	if (!inputEditor.contains("$")) {
	            try {
	                Class<?> _class = BEASTClassLoader.forName(inputEditor);
	                
	                
	                Constructor<?> con = _class.getConstructor(BeautiDoc.class);
	                InputEditor editor = (InputEditor) con.newInstance(doc);
	                
	                //InputEditor editor = (InputEditor) _class.newInstance();
	                Class<?>[] types = editor.types();
	                for (Class<?> type : types) {
	                    inputEditorMap.put(type, inputEditor);
	                    if (editor instanceof ListInputEditor) {
	                        Class<?> baseType = ((ListInputEditor) editor).baseType();
	                        listInputEditorMap.put(baseType, inputEditor);
	                    }
	                }
	            } catch (java.lang.InstantiationException e) {
	                // ingore input editors that are inner classes
	            } catch (Throwable e) {
	                // print message
	                Log.err.println(e.getClass().getName() + ": " + e.getMessage());
	            }
        	}
        }
    }

    /**
     * add all inputs of a beastObject to a box *
     */
    public List<InputEditor> addInputs(Box box, BEASTInterface beastObject, InputEditor editor, InputEditor validateListener, BeautiDoc doc) {
        /* add individual inputs **/
        List<Input<?>> inputs = null;
        List<InputEditor> editors = new ArrayList<>();
    	
        try {
            inputs = beastObject.listInputs();
        } catch (Exception e) {
            // TODO: handle exception
        }
        for (Input<?> input : inputs) {
            try {
                String fullInputName = beastObject.getClass().getName() + "." + input.getName();
                if (!doc.beautiConfig.suppressBEASTObjects.contains(fullInputName)) {
                    InputEditor inputEditor = createInputEditor(input, beastObject, true, ExpandOption.FALSE, ButtonStatus.ALL, editor, doc);
                    box.add(inputEditor.getComponent());
                    box.add(Box.createVerticalStrut(5));
                    //box.add(Box.createVerticalGlue());
                    if (validateListener != null) {
                        inputEditor.addValidationListener(validateListener);
                    }
                    editors.add(inputEditor);
                }
            } catch (Exception e) {
                // ignore
                Log.err.println(e.getClass().getName() + ": " + e.getMessage() + "\n" +
                        "input " + input.getName() + " could not be added.");
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Could not add entry for " + input.getName());
            }
        }
        box.add(Box.createVerticalGlue());
        return editors;
    } // addInputs


    public InputEditor createInputEditor(Input<?> input, BEASTInterface beastObject, BeautiDoc doc) throws NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return createInputEditor(input, beastObject, true, InputEditor.ExpandOption.FALSE, ButtonStatus.ALL, null, doc);
    }

    public InputEditor createInputEditor(Input<?> input, BEASTInterface beastObject, boolean addButtons,
                                                ExpandOption forceExpansion, ButtonStatus buttonStatus,
                                                InputEditor editor, BeautiDoc doc) throws NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    	return createInputEditor(input, -1, beastObject, addButtons, forceExpansion, buttonStatus, editor, doc);
    }
    
    public InputEditor createInputEditor(Input<?> input, int listItemNr, BEASTInterface beastObject, boolean addButtons,
                ExpandOption forceExpansion, ButtonStatus buttonStatus,
                InputEditor editor, BeautiDoc doc) throws NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (input.getType() == null) {
            input.determineClass(beastObject);
        }
        //Class<?> inputClass = input.get() != null ? input.get().getClass(): input.getType();
        Class<?> inputClass = input.getType();
        if (inputClass == null) {
        	return null;
        }
        if (listItemNr >= 0) {
        	inputClass = ((List<?>)input.get()).get(listItemNr).getClass();
        } else {
        	if (input.get() != null && !input.get().getClass().equals(inputClass)
        			&& !(input.get() instanceof ArrayList)) {
        		Log.trace.println(input.get().getClass() + " != " + inputClass);
        		inputClass = input.get().getClass();
        	}
        }

        //Log.trace.print(inputClass.getName() + " => ");
        InputEditor inputEditor;

        // check whether the super.editor has a custom method for creating an Editor
        if (editor != null) {        	
            try {
                String name = input.getName();
                name = new String(name.charAt(0) + "").toUpperCase() + name.substring(1);
                name = "create" + name + "Editor";
                Class<?> _class = editor.getClass();
                Method method = _class.getMethod(name);
                inputEditor = (InputEditor) method.invoke(editor);
                //Log.trace.println(inputEditor.getClass().getName() + " (CUSTOM EDITOR)");
                return inputEditor;
            } catch (Exception e) {
                // ignore
            }
        }
        if (listItemNr < 0 && (List.class.isAssignableFrom(inputClass) ||
                (input.get() != null && input.get() instanceof List<?>))) {
            // handle list inputs
            if (listInputEditorMap.containsKey(inputClass)) {
                // use custom list input editor
                String inputEditorName = listInputEditorMap.get(inputClass);
                Constructor<?> con = BEASTClassLoader.forName(inputEditorName).getConstructor(BeautiDoc.class);
                inputEditor = (InputEditor) con.newInstance(doc);

                //inputEditor = (InputEditor) BEASTClassLoader.forName(inputEditor).newInstance();
            } else {
                // otherwise, use generic list editor
                inputEditor = new ListInputEditor(doc);
            }
            ((ListInputEditor) inputEditor).setButtonStatus(buttonStatus);
        } else if (input.possibleValues != null) {
            // handle enumeration inputs
            inputEditor = new EnumInputEditor(doc);
        } else {
        	Class<?> inputClass2 = inputClass;
        	while (inputClass2 != null && !inputEditorMap.containsKey(inputClass2)) {
        		inputClass2 = inputClass2.getSuperclass(); 
        	}
        	if (inputClass2 == null) {
        		inputEditor = new BEASTObjectInputEditor(doc);
        	} else {
	            // handle BEASTObject-input with custom input editors
	            String inputEditorName = inputEditorMap.get(inputClass2);
	            
	            Constructor<?> con = BEASTClassLoader.forName(inputEditorName).getConstructor(BeautiDoc.class);
	            inputEditor = (InputEditor) con.newInstance(doc);
        	}
        }        	
//    	} else if (inputEditorMap.containsKey(inputClass)) {
//            // handle BEASTObject-input with custom input editors
//            String inputEditor = inputEditorMap.get(inputClass);
//            
//            Constructor<?> con = BEASTClassLoader.forName(inputEditor).getConstructor(BeautiDoc.class);
//            inputEditor = (InputEditor) con.newInstance(doc);
//            //inputEditor = (InputEditor) BEASTClassLoader.forName(inputEditor).newInstance(doc);
//            //} else if (inputClass.isEnum()) {
//            //    inputEditor = new EnumInputEditor();
//        } else {
//            // assume it is a general BEASTObject, so create a default BEASTObject input editor
//            inputEditor = new PluginInputEditor(doc);
//        }
        String fullInputName = beastObject.getClass().getName() + "." + input.getName();
        //System.err.println(fullInputName);
        ExpandOption expandOption = forceExpansion;
        if (doc.beautiConfig.inlineBEASTObject.contains(fullInputName) || forceExpansion == ExpandOption.TRUE_START_COLLAPSED) {
            expandOption = ExpandOption.TRUE;
            // deal with initially collapsed beastObjects
            if (doc.beautiConfig.collapsedBEASTObjects.contains(fullInputName) || forceExpansion == ExpandOption.TRUE_START_COLLAPSED) {
                if (input.get() != null) {
                    Object o = input.get();
                    if (o instanceof ArrayList) {
                        for (Object o2 : (ArrayList<?>) o) {
                            if (o2 instanceof BEASTInterface) {
                                String id = ((BEASTInterface) o2).getID();
                                if (!ListInputEditor.g_initiallyCollapsedIDs.contains(id)) {
                                    ListInputEditor.g_initiallyCollapsedIDs.add(id);
                                    ListInputEditor.g_collapsedIDs.add(id);
                                }
                            }
                        }
                    } else if (o instanceof BEASTInterface) {
                        String id = ((BEASTInterface) o).getID();
                        if (!ListInputEditor.g_initiallyCollapsedIDs.contains(id)) {
                            ListInputEditor.g_initiallyCollapsedIDs.add(id);
                            ListInputEditor.g_collapsedIDs.add(id);
                        }
                    }
                }

            }
        }
        inputEditor.setDoc(doc);
        inputEditor.init(input, beastObject, listItemNr, expandOption, addButtons);
        ((JComponent) inputEditor).setBorder(BorderFactory.createEmptyBorder());
        inputEditor.getComponent().setVisible(true);
        //Log.trace.println(inputEditor.getClass().getName());
        return inputEditor;
    } // createInputEditor

    /**
     * find beastObjects that could fit the input
     * @param input
     * @param parent beastObject containing the input
     * @param tabuList list of ids that are not allowed
     * @param doc
     * @return
     */
    
    public List<String> getAvailablePlugins(Input<?> input, BEASTInterface parent, List<String> tabuList, BeautiDoc doc) {

        //List<String> beastObjectNames = BeautiConfig.getInputCandidates(parent, input);
        List<String> beastObjectNames = new ArrayList<>();
        if (beastObjectNames != null) {
            return beastObjectNames;
        }


        /* add ascendants to tabu list */
        if (tabuList == null) {
            tabuList = new ArrayList<>();
        }
        if (!doc.isExpertMode()) {
            for (BEASTInterface beastObject : BEASTObjectPanel.listAscendants(parent, doc.pluginmap.values())) {
                tabuList.add(beastObject.getID());
            }
        }
        //System.err.println(tabuList);

        /* collect all beastObjects in the system, that are not in the tabu list*/
        beastObjectNames = new ArrayList<>();
        for (BEASTInterface beastObject : doc.pluginmap.values()) {
            if (input.getType().isAssignableFrom(beastObject.getClass())) {
                boolean isTabu = false;
                if (tabuList != null) {
                    for (String tabu : tabuList) {
                        if (tabu.equals(beastObject.getID())) {
                            isTabu = true;
                        }
                    }
                }
                if (!isTabu) {
                    try {
                        if (input.canSetValue(beastObject, parent)) {
                            beastObjectNames.add(beastObject.getID());
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }
        /* add all beastObject-classes of type assignable to the input */
        if (doc.isExpertMode()) {
            List<String> classes = PackageManager.find(input.getType(), "beast");
            for (String className : classes) {
                try {
                    Object o = BEASTClassLoader.forName(className).newInstance();
                    if (input.canSetValue(o, parent)) {
                        beastObjectNames.add("new " + className);
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        return beastObjectNames;
    } // getAvailablePlugins

    /**
     * finds beauti templates that can create subnets that fit an input
     * @param input
     * @param parent
     * @param tabuList
     * @param doc
     * @return
     */

    public List<BeautiSubTemplate> getAvailableTemplates(Input<?> input, BEASTInterface parent, List<String> tabuList, BeautiDoc doc) {
        Class<?> type = input.getType();
        List<BeautiSubTemplate> candidates = doc.beautiConfig.getInputCandidates(parent, input, type);
        if (input.getRule().equals(Validate.OPTIONAL)) {
            candidates.add(BeautiConfig.getNullTemplate(doc));
        }
        return candidates;
    }

}
