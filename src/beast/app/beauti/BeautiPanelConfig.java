package beast.app.beauti;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import beast.app.draw.BEASTObjectPanel;
import beast.app.draw.InputEditor;
import beast.core.BEASTInterface;
import beast.core.BEASTObject;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.util.Log;
import beast.util.BEASTClassLoader;




@Description("Defines properties for custom panels in Beauti")
public class BeautiPanelConfig extends BEASTObject {
    public enum Partition {
        none, Partition, SiteModel, ClockModel, Tree
    }

    final public Input<String> nameInput = new Input<>("panelname", "name of the panel, used to label the panel and in the visibility menu", Validate.REQUIRED);
    final public Input<String> tipTextInput = new Input<>("tiptext", "tiptext shown when hovering over the tab", Validate.REQUIRED);

    final public Input<String> pathInput = new Input<>("path", "path of the BEASTObject to be shown in this panel, in xpath-like format. " +
            "For example operator to edit the operator input of the top level run element. " +
            "distribution/distribution[id='prior'] for prior distributions." +
            "distribution/distribution[id='posterior']/traitset all posterior inputs with name traitset", Validate.REQUIRED);

    final public Input<Partition> hasPartitionsInput = new Input<>("hasPartitions", "flag to indicate the panel has" +
            "a partition context (and hence a partition list), deafult none.  Possible values: " + Arrays.toString(Partition.values()), Partition.none, Partition.values());

    final public Input<Boolean> addButtonsInput = new Input<>("addButtons", "flag to indicate buttons should be added, default true", true);
    final public Input<Boolean> isVisibleInput = new Input<>("isVisible", "flag to indicate panel is visible on startup, default true", true);


    final public Input<String> iconInput = new Input<>("icon", "icon shown in the panel relative to /beast/app/beauti, default 0.png", "0.png");

    final public Input<InputEditor.ExpandOption> forceExpansionInput = new Input<>("forceExpansion", "whether to expand the input(s)" +
            "This can be " + Arrays.toString(InputEditor.ExpandOption.values()) + " (default 'FALSE')", InputEditor.ExpandOption.FALSE, InputEditor.ExpandOption.values());
    final public Input<String> typeInput = new Input<>("type", "type used for finding the appropriate beastObject editor. By default, type is determined " +
            "by the input type of the last component of the path");

    final public Input<Integer> labelWidthInput = new Input<>("labelWidth", "width of labels used to show name of inputs in input editors", 150);

    final public Input<InputEditor.ButtonStatus> buttonStatusInput = new Input<>("buttonStatus", "whether to show add and delete buttons. " +
            "This can be " + Arrays.toString(InputEditor.ButtonStatus.values()) + " (default 'ALL')", InputEditor.ButtonStatus.ALL, InputEditor.ButtonStatus.values());

    String[] pathComponents;
    String[] conditionalAttribute;
    String[] conditionalValue;
    Class<?> type;

    /**
     * beastObjects associated with inputs *
     */
    List<BEASTInterface> inputs;
    /**
     * beastObjects associated with inputs before editing starts *
     */
    List<BEASTInterface> startInputs;
    /**
     * beastObjects that are parents, i.e. contain inputs of m_inputs *
     */
    List<BEASTInterface> parentBEASTObjects;
    List<Input<?>> parentInputs = new ArrayList<>();
    /**
     * flag to indicate we are dealing with a list input *
     */
    boolean isList;


    FlexibleInput<?> _input;

    class FlexibleInput<T> extends Input<T> {
        FlexibleInput() {
            // sets name to something non-trivial This is used by canSetValue()
            super("xx", "");
        }

        public FlexibleInput(T arrayList) {
            super("xx", "", arrayList);
        }

        @Override
		public void setType(Class<?> type) {
            theClass = type;
        }
    }

    @Override
    public void initAndValidate() {
        pathComponents = pathInput.get().split("/");
        if (pathComponents[0].equals("")) {
            pathComponents = new String[0];
        }
        conditionalAttribute = new String[pathComponents.length];
        conditionalValue = new String[pathComponents.length];
        for (int i = 0; i < pathComponents.length; i++) {
            int j = pathComponents[i].indexOf('[');
            if (j >= 0) {
                String conditionalComponents = pathComponents[i].substring(j + 1, pathComponents[i].lastIndexOf(']'));
                String[] strs = conditionalComponents.split("=");
                conditionalAttribute[i] = strs[0];
                conditionalValue[i] = strs[1].substring(1, strs[1].length() - 1);
                pathComponents[i] = pathComponents[i].substring(0, j);
            }
        }
        inputs = new ArrayList<>();
        startInputs = new ArrayList<>();
        BEASTObjectPanel.getID(this);
    }

    /**
     * more elegant getters for resolving Input values*
     */
    public String getName() {
        return nameInput.get();
    }

    public Partition hasPartition() {
        return hasPartitionsInput.get();
    }

    public boolean addButtons() {
        return false;
    }

    public String getIcon() {
        return iconInput.get();
    }

    public String getTipText() {
        return tipTextInput.get();
    }

    public InputEditor.ExpandOption forceExpansion() {
        return forceExpansionInput.get();
    }

    /**
     * Find the input associated with this panel
     * based on the path Input.
     */
    @SuppressWarnings("unchecked")
    final public Input<?> resolveInput(BeautiDoc doc, int partitionIndex) {
        try {
//            if (parentBEASTObjects != null && parentBEASTObjects.size() > 0 && _input != null)
//                System.err.println("sync " + parentBEASTObjects.get(partitionIndex) + "[?] = " + _input.get());

            List<BEASTInterface> beastObjects;
            if (hasPartitionsInput.get() == Partition.none) {
                beastObjects = new ArrayList<>();
                beastObjects.add(doc.mcmc.get());
            } else {
                beastObjects = doc.getPartitions(hasPartitionsInput.get().toString());
            }
            parentBEASTObjects = new ArrayList<>();
            parentInputs = new ArrayList<>();

            parentBEASTObjects.add(doc);
            parentInputs.add(doc.mcmc);
            type = doc.mcmc.getType();
            isList = false;
            for (int i = 0; i < pathComponents.length; i++) {
                List<BEASTInterface> oldPlugins = beastObjects;
                beastObjects = new ArrayList<>();
                parentBEASTObjects = new ArrayList<>();
                parentInputs = new ArrayList<>();
                for (BEASTInterface beastObject : oldPlugins) {
                    final Input<?> namedInput = beastObject.getInput(pathComponents[i]);
                    type = namedInput.getType();
                    if (namedInput.get() instanceof List<?>) {
                        isList = true;
                        List<?> list = (List<?>) namedInput.get();
                        if (conditionalAttribute[i] == null) {
                            for (Object o : list) {
                                BEASTInterface beastObject2 = (BEASTInterface) o;
                                beastObjects.add(beastObject2);
                                parentBEASTObjects.add(beastObject);
                                parentInputs.add(namedInput);
                            }
                            //throw new Exception ("Don't know which element to pick from the list. List component should come with a condition. " + m_sPathComponents[i]);
                        } else {
                            int matches = 0;
                            for (int j = 0; j < list.size(); j++) {
                                BEASTInterface beastObject2 = (BEASTInterface) list.get(j);
                                if (matches(beastObject2, conditionalAttribute[i], conditionalValue[i])) {
                                    beastObjects.add(beastObject2);
                                    parentBEASTObjects.add(beastObject);
                                    parentInputs.add(namedInput);
                                    matches++;
                                    break;
                                }
                            }
                            if (matches == 0) {
                                parentInputs.add(namedInput);
                                parentBEASTObjects.add(beastObject);
                            }
                        }
                    } else if (namedInput.get() instanceof BEASTInterface) {
                        isList = false;
                        if (conditionalAttribute[i] == null) {
                            beastObjects.add((BEASTInterface) namedInput.get());
                            parentBEASTObjects.add(beastObject);
                            parentInputs.add(namedInput);
                        } else {
                            if (matches(beastObject, conditionalAttribute[i], conditionalValue[i])) {
//							if ((m_sConditionalAttribute[i].equals("id") && beastObject.getID().equals(m_sConditionalValue[i])) ||
//							    (m_sConditionalAttribute[i].equals("type") && beastObject.getClass().getName().equals(m_sConditionalValue[i]))) {
                                beastObjects.add(beastObject);
                                parentBEASTObjects.add(beastObject);
                                parentInputs.add(namedInput);
                            }
                        }
                    } else {
                        throw new IllegalArgumentException("input " + pathComponents[i] + "  is not a beastObject or list");
                    }
                }
            }
            if (typeInput.get() != null) {
                type = BEASTClassLoader.forName(typeInput.get());
            }
            // sanity check
            if (!isList && (hasPartitionsInput.get() == Partition.none) && beastObjects.size() > 1) {
            	Log.warning.println("WARNING: multiple beastObjects match, but hasPartitions=none");
                // this makes sure that all mathing beastObjects are available in one go
                isList = true;
                // this suppresses syncing
                parentInputs.clear();
            }
            inputs.clear();
            startInputs.clear();
            for (BEASTInterface beastObject : beastObjects) {
                inputs.add(beastObject);
                startInputs.add(beastObject);
            }

            if (!isList) {
                _input = new FlexibleInput<>();
            } else {
                _input = new FlexibleInput<>(new ArrayList<>());
            }
            _input.setRule(Validate.REQUIRED);
            syncTo(partitionIndex);
//            if (parentBEASTObjects != null && parentBEASTObjects.size() > 0)
//                System.err.println("sync " + parentBEASTObjects.get(partitionIndex) + "[?] = " + _input.get());


            if (isList) {
                checkForDups((List<Object>) _input.get());
            }

            return _input;
        } catch (Exception e) {
            Log.err.println("Warning: could not find objects in path " + Arrays.toString(pathComponents));
        }
        return null;
    } // resolveInputs

    private void checkForDups(List<Object> list) {
        for (int i = 0; i < list.size(); i++) {
            if (list.lastIndexOf(list.get(i)) != i) {
            	Log.warning.println("We have a dup: " + list.get(i));
                list.remove(i);
                i--;
            }
        }

    }

    private boolean matches(BEASTInterface beastObject2, String conditionalAttribute, String conditionalValue) {
        if (conditionalAttribute.equals("id") && beastObject2.getID().equals(conditionalValue)) return true;
        if (conditionalAttribute.equals("type") && beastObject2.getClass().getName().equals(conditionalValue)) return true;
        if (conditionalAttribute.equals("type!") && !beastObject2.getClass().getName().equals(conditionalValue))
            return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    public void sync(int partitionIndex) {
        if (parentInputs.size() > 0 && _input.get() != null) {
            final Input<?> input = parentInputs.get(partitionIndex);
            if (isList) {
                List<Object> list = (List<Object>) _input.get();
                List<Object> targetList = ((List<Object>) input.get());
                //targetList.clear();
                // only clear former members
                for (BEASTInterface beastObject : startInputs) {
                    targetList.remove(beastObject);
                }
                targetList.addAll(list);
                // sync outputs of items in list
                for (Object o : list) {
                	if (o instanceof BEASTInterface) {
                		((BEASTInterface)o).getOutputs().add(parentBEASTObjects.get(partitionIndex));
                	}
                }
            } else {
                try {
                    //System.err.println("sync " + parentBEASTObjects.get(partitionIndex) + "[" + input.getName() + "] = " + _input.get());
                    input.setValue(_input.get(), parentBEASTObjects.get(partitionIndex));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * initialise m_input, and either m_beastObject or m_pluginList *
     */
    public void syncTo(int partitionIndex) {
        _input.setType(type);
        try {
            if (isList) {
                for (BEASTInterface beastObject : inputs) {
                    _input.setValue(beastObject, this);
                }
            } else {
            	if (inputs.size() > 0) {
	                BEASTInterface beastObject = inputs.get(partitionIndex);
	                _input.setValue(beastObject, this);
            	}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    final Input<?> getInput() {
        return _input;
    }

    /**
     * returns name of the beastObject *
     */
    public String getType() {
        if (isList) {
            return inputs.get(0).getClass().getName();
        }
        if (_input == null) {
            return null;
        }
        return _input.get().getClass().getName();
    }

//	public void addItem(BEASTObject beastObject) {
//		m_parentInputs.add(m_parentInputs.get(m_parentInputs.size()-1));
//		m_parentPlugins.add(m_parentPlugins.get(m_parentPlugins.size()-1));
//	}

}
