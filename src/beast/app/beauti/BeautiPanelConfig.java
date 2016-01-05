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




@Description("Defines properties for custom panels in Beauti")
public class BeautiPanelConfig extends BEASTObject {
    public enum Partition {
        none, Partition, SiteModel, ClockModel, Tree
    }

    final public Input<String> sNameInput = new Input<>("panelname", "name of the panel, used to label the panel and in the visibility menu", Validate.REQUIRED);
    final public Input<String> sTipTextInput = new Input<>("tiptext", "tiptext shown when hovering over the tab", Validate.REQUIRED);

    final public Input<String> sPathInput = new Input<>("path", "path of the Plugin to be shown in this panel, in xpath-like format. " +
            "For example operator to edit the operator input of the top level run element. " +
            "distribution/distribution[id='prior'] for prior distributions." +
            "distribution/distribution[id='posterior']/traitset all posterior inputs with name traitset", Validate.REQUIRED);

    final public Input<Partition> bHasPartitionsInput = new Input<>("hasPartitions", "flag to indicate the panel has" +
            "a partition context (and hence a partition list), deafult none.  Possible values: " + Partition.values(), Partition.none, Partition.values());

    final public Input<Boolean> bAddButtonsInput = new Input<>("addButtons", "flag to indicate buttons should be added, default true", true);
    final public Input<Boolean> bIsVisibleInput = new Input<>("isVisible", "flag to indicate panel is visible on startup, default true", true);


    final public Input<String> sIconInput = new Input<>("icon", "icon shown in the panel relative to /beast/app/beauti, default 0.png", "0.png");

    final public Input<InputEditor.ExpandOption> forceExpansionInput = new Input<>("forceExpansion", "whether to expand the input(s)" +
            "This can be " + Arrays.toString(InputEditor.ExpandOption.values()) + " (default 'FALSE')", InputEditor.ExpandOption.FALSE, InputEditor.ExpandOption.values());
    final public Input<String> sTypeInput = new Input<>("type", "type used for finding the appropriate plugin editor. By default, type is determined " +
            "by the input type of the last component of the path");

    final public Input<Integer> nLabelWidthInput = new Input<>("labelWidth", "width of labels used to show name of inputs in input editors", 150);

    final public Input<InputEditor.ButtonStatus> buttonStatusInput = new Input<>("buttonStatus", "whether to show add and delete buttons. " +
            "This can be " + Arrays.toString(InputEditor.ButtonStatus.values()) + " (default 'ALL')", InputEditor.ButtonStatus.ALL, InputEditor.ButtonStatus.values());

    String[] sPathComponents;
    String[] sConditionalAttribute;
    String[] sConditionalValue;
    Class<?> type;

    /**
     * plugins associated with inputs *
     */
    List<BEASTInterface> inputs;
    /**
     * plugins associated with inputs before editing starts *
     */
    List<BEASTInterface> startInputs;
    /**
     * plugins that are parents, i.e. contain inputs of m_inputs *
     */
    List<BEASTInterface> parentPlugins;
    List<Input<?>> parentInputs = new ArrayList<>();
    /**
     * flag to indicate we are dealing with a list input *
     */
    boolean bIsList;


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
    public void initAndValidate() throws Exception {
        sPathComponents = sPathInput.get().split("/");
        if (sPathComponents[0].equals("")) {
            sPathComponents = new String[0];
        }
        sConditionalAttribute = new String[sPathComponents.length];
        sConditionalValue = new String[sPathComponents.length];
        for (int i = 0; i < sPathComponents.length; i++) {
            int j = sPathComponents[i].indexOf('[');
            if (j >= 0) {
                String sConditionalComponents = sPathComponents[i].substring(j + 1, sPathComponents[i].lastIndexOf(']'));
                String[] sStrs = sConditionalComponents.split("=");
                sConditionalAttribute[i] = sStrs[0];
                sConditionalValue[i] = sStrs[1].substring(1, sStrs[1].length() - 1);
                sPathComponents[i] = sPathComponents[i].substring(0, j);
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
        return sNameInput.get();
    }

    public Partition hasPartition() {
        return bHasPartitionsInput.get();
    }

    public boolean addButtons() {
        return false;
    }

    public String getIcon() {
        return sIconInput.get();
    }

    public String getTipText() {
        return sTipTextInput.get();
    }

    public InputEditor.ExpandOption forceExpansion() {
        return forceExpansionInput.get();
    }

    /**
     * Find the input associated with this panel
     * based on the path Input.
     */
    @SuppressWarnings("unchecked")
    final public Input<?> resolveInput(BeautiDoc doc, int iPartition) {
        try {
//            if (parentPlugins != null && parentPlugins.size() > 0 && _input != null)
//                System.err.println("sync " + parentPlugins.get(iPartition) + "[?] = " + _input.get());

            List<BEASTInterface> plugins;
            if (bHasPartitionsInput.get() == Partition.none) {
                plugins = new ArrayList<>();
                plugins.add(doc.mcmc.get());
            } else {
                plugins = doc.getPartitions(bHasPartitionsInput.get().toString());
            }
            parentPlugins = new ArrayList<>();
            parentInputs = new ArrayList<>();

            parentPlugins.add(doc);
            parentInputs.add(doc.mcmc);
            type = doc.mcmc.getType();
            bIsList = false;
            for (int i = 0; i < sPathComponents.length; i++) {
                List<BEASTInterface> oldPlugins = plugins;
                plugins = new ArrayList<>();
                parentPlugins = new ArrayList<>();
                parentInputs = new ArrayList<>();
                for (BEASTInterface plugin : oldPlugins) {
                    final Input<?> namedInput = plugin.getInput(sPathComponents[i]);
                    type = namedInput.getType();
                    if (namedInput.get() instanceof List<?>) {
                        bIsList = true;
                        List<?> list = (List<?>) namedInput.get();
                        if (sConditionalAttribute[i] == null) {
                            for (Object o : list) {
                                BEASTInterface plugin2 = (BEASTInterface) o;
                                plugins.add(plugin2);
                                parentPlugins.add(plugin);
                                parentInputs.add(namedInput);
                            }
                            //throw new Exception ("Don't know which element to pick from the list. List component should come with a condition. " + m_sPathComponents[i]);
                        } else {
                            int nMatches = 0;
                            for (int j = 0; j < list.size(); j++) {
                                BEASTInterface plugin2 = (BEASTInterface) list.get(j);
                                if (matches(plugin2, sConditionalAttribute[i], sConditionalValue[i])) {
                                    plugins.add(plugin2);
                                    parentPlugins.add(plugin);
                                    parentInputs.add(namedInput);
                                    nMatches++;
                                    break;
                                }
                            }
                            if (nMatches == 0) {
                                parentInputs.add(namedInput);
                                parentPlugins.add(plugin);
                            }
                        }
                    } else if (namedInput.get() instanceof BEASTInterface) {
                        bIsList = false;
                        if (sConditionalAttribute[i] == null) {
                            plugins.add((BEASTInterface) namedInput.get());
                            parentPlugins.add(plugin);
                            parentInputs.add(namedInput);
                        } else {
                            if (matches(plugin, sConditionalAttribute[i], sConditionalValue[i])) {
//							if ((m_sConditionalAttribute[i].equals("id") && plugin.getID().equals(m_sConditionalValue[i])) ||
//							    (m_sConditionalAttribute[i].equals("type") && plugin.getClass().getName().equals(m_sConditionalValue[i]))) {
                                plugins.add(plugin);
                                parentPlugins.add(plugin);
                                parentInputs.add(namedInput);
                            }
                        }
                    } else {
                        throw new IllegalArgumentException("input " + sPathComponents[i] + "  is not a plugin or list");
                    }
                }
            }
            if (sTypeInput.get() != null) {
                type = Class.forName(sTypeInput.get());
            }
            // sanity check
            if (!bIsList && (bHasPartitionsInput.get() == Partition.none) && plugins.size() > 1) {
            	Log.warning.println("WARNING: multiple plugins match, but hasPartitions=none");
                // this makes sure that all mathing plugins are available in one go
                bIsList = true;
                // this suppresses syncing
                parentInputs.clear();
            }
            inputs.clear();
            startInputs.clear();
            for (BEASTInterface plugin : plugins) {
                inputs.add(plugin);
                startInputs.add(plugin);
            }

            if (!bIsList) {
                _input = new FlexibleInput<>();
            } else {
                _input = new FlexibleInput<>(new ArrayList<>());
            }
            _input.setRule(Validate.REQUIRED);
            syncTo(iPartition);
//            if (parentPlugins != null && parentPlugins.size() > 0)
//                System.err.println("sync " + parentPlugins.get(iPartition) + "[?] = " + _input.get());


            if (bIsList) {
                checkForDups((List<Object>) _input.get());
            }

            return _input;
        } catch (Exception e) {
            Log.err.println("Warning: could not find objects in path " + Arrays.toString(sPathComponents));
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

    private boolean matches(BEASTInterface plugin2, String sConditionalAttribute, String sConditionalValue) {
        if (sConditionalAttribute.equals("id") && plugin2.getID().equals(sConditionalValue)) return true;
        if (sConditionalAttribute.equals("type") && plugin2.getClass().getName().equals(sConditionalValue)) return true;
        if (sConditionalAttribute.equals("type!") && !plugin2.getClass().getName().equals(sConditionalValue))
            return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    public void sync(int iPartition) {
        if (parentInputs.size() > 0 && _input.get() != null) {
            final Input<?> input = parentInputs.get(iPartition);
            if (bIsList) {
                List<Object> list = (List<Object>) _input.get();
                List<Object> targetList = ((List<Object>) input.get());
                //targetList.clear();
                // only clear former members
                for (BEASTInterface plugin : startInputs) {
                    targetList.remove(plugin);
                }
                targetList.addAll(list);
                // sync outputs of items in list
                for (Object o : list) {
                	if (o instanceof BEASTInterface) {
                		((BEASTInterface)o).getOutputs().add(parentPlugins.get(iPartition));
                	}
                }
            } else {
                try {
                    //System.err.println("sync " + parentPlugins.get(iPartition) + "[" + input.getName() + "] = " + _input.get());
                    input.setValue(_input.get(), parentPlugins.get(iPartition));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * initialise m_input, and either m_plugin or m_pluginList *
     */
    public void syncTo(int iPartition) {
        _input.setType(type);
        try {
            if (bIsList) {
                for (BEASTInterface plugin : inputs) {
                    _input.setValue(plugin, this);
                }
            } else {
            	if (inputs.size() > 0) {
	                BEASTInterface plugin = inputs.get(iPartition);
	                _input.setValue(plugin, this);
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
     * returns name of the plugin *
     */
    public String getType() {
        if (bIsList) {
            return inputs.get(0).getClass().getName();
        }
        if (_input == null) {
            return null;
        }
        return _input.get().getClass().getName();
    }

//	public void addItem(Plugin plugin) {
//		m_parentInputs.add(m_parentInputs.get(m_parentInputs.size()-1));
//		m_parentPlugins.add(m_parentPlugins.get(m_parentPlugins.size()-1));
//	}

}
