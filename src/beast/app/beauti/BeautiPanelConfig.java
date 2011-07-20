package beast.app.beauti;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import beast.app.draw.InputEditor.BUTTONSTATUS;
import beast.app.draw.InputEditor.EXPAND;
import beast.app.draw.PluginPanel;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Plugin;


@Description("Defines properties for custom panels in Beauti")
public class BeautiPanelConfig extends Plugin {
	
	public Input<String> m_sNameInput = new Input<String>("panelname", "name of the panel, used to label the panel and in the visibility menu", Validate.REQUIRED);
	public Input<String> m_sTipTextInput = new Input<String>("tiptext", "tiptext shown when hovering over the tab", Validate.REQUIRED);
	
	public Input<String> m_sPathInput = new Input<String>("path", "path of the Plugin to be shown in this panel, in xpath-like format. " +
			"For example operator to edit the operator input of the top level run element. " +
			"distribution/distribution[id='prior'] for prior distributions." +
			"distribution/distribution[id='posterior']/traitset all posterior inputs with name traitset", Validate.REQUIRED);
	
	public Input<Boolean> m_bHasPartitionsInput = new Input<Boolean>("hasPartitions", "flag to indicate the panel has" +
			"a partition context (and hence a partition list), deafult false", false);
	
	public Input<Boolean> m_bAddButtonsInput = new Input<Boolean>("addButtons", "flag to indicate buttons should be added, deafult true", true);
	public Input<Boolean> m_bIsVisibleInput = new Input<Boolean>("isVisible", "flag to indicate panel is visible on startup, deafult true", true);
	
	
	public Input<String> m_sIconInput = new Input<String>("icon", "icon shown in the panel relative to /beast/app/beauti, default 0.png", "0.png");
	
	public Input<EXPAND> m_forceExpansionInput = new Input<EXPAND>("forceExpansion", "whether to expand the input(s)" +
			"This can be " + Arrays.toString(EXPAND.values()) + " (default 'FALSE')", EXPAND.FALSE, EXPAND.values());
	public Input<String> m_sTypeInput = new Input<String>("type", "type used for finding the appropriate plugin editor. By default, type is determined " +
			"by the input type of the last component of the path");
	
	public Input<Integer> m_nLabelWidthInput = new Input<Integer>("labelWidth", "width of labels used to show name of inputs in input editors", 150);

	public Input<BUTTONSTATUS> m_buttonStatusInput = new Input<BUTTONSTATUS>("buttonStatus", "whether to show add and delete buttons. " +
			"This can be " + Arrays.toString(BUTTONSTATUS.values()) + " (default 'ALL')", BUTTONSTATUS.ALL, BUTTONSTATUS.values());
	
	
	String [] m_sPathComponents;
	String [] m_sConditionalAttribute;
	String [] m_sConditionalValue;
	Class<?> m_type;
	
	/** plugins associated with inputs **/
	List<Plugin> m_inputs;
	/** plugins associated with inputs before editing starts **/
	List<Plugin> m_startInputs;
	/** plugins that are parents, i.e. contain inputs of m_inputs **/
	List<Plugin> m_parentPlugins;
	List<Input<?>> m_parentInputs = new ArrayList();
	/** flag to indicate we are dealing with a list input **/
	boolean m_bIsList;

	
	FlexibleInput<?> m_input;
	
	class FlexibleInput<T> extends Input<T> {
		FlexibleInput() {
			// sets name to something non-trivial This is used by canSetValue()
			super("xx","");
		}
		public FlexibleInput(T arrayList) {
			super("xx", "", arrayList);
		}
		public void setType(Class<?> type) {
			theClass = type;
		}
	}
	
	@Override
	public void initAndValidate() throws Exception {
		m_sPathComponents = m_sPathInput.get().split("/");
		if (m_sPathComponents[0].equals("")) {
			m_sPathComponents = new String[0];
		}
		m_sConditionalAttribute = new String[m_sPathComponents.length];
		m_sConditionalValue = new String[m_sPathComponents.length];
		for (int i = 0; i < m_sPathComponents.length; i++) {
			int j = m_sPathComponents[i].indexOf('[');
			if (j >=0 ) {
				String sConditionalComponents = m_sPathComponents[i].substring(j+1, m_sPathComponents[i].lastIndexOf(']'));
				String [] sStrs = sConditionalComponents.split("=");
				m_sConditionalAttribute[i] = sStrs[0];
				m_sConditionalValue[i] = sStrs[1].substring(1,sStrs[1].length()-1);
				m_sPathComponents[i] = m_sPathComponents[i].substring(0, j);
			}
		}
		m_inputs = new ArrayList<Plugin>();
		m_startInputs = new ArrayList<Plugin>();
	    PluginPanel.getID(this);
	}
	
	/** more elegant getters for resolving Input values**/
	public String getName() {
		return m_sNameInput.get();
	}
	
	public boolean hasPartition() {
		return m_bHasPartitionsInput.get();
	}
	
	public boolean addButtons() {
		return false;
	}
	
	public String getIcon() {
		return m_sIconInput.get();
	}
	
	public String getTipText() {
		return m_sTipTextInput.get();
	}

	public EXPAND forceExpansion() {
		return m_forceExpansionInput.get();
	}
	
	/** Find the input associated with this panel
	 * based on the path Input.
	 */
	public Input<?> resolveInput(BeautiDoc doc, int iPartition) {
		try {
			if (m_parentPlugins != null && m_parentPlugins.size() > 0 && m_input != null) System.err.println("sync " + m_parentPlugins.get(iPartition) + "[?] = " + m_input.get());
			
			List<Plugin> plugins = new ArrayList<Plugin>();
			m_parentPlugins = new ArrayList<Plugin>();
			m_parentInputs =  new ArrayList<Input<?>>();
			plugins.add(doc.m_mcmc.get());
			m_parentPlugins.add(doc);
			m_parentInputs.add(doc.m_mcmc);
			m_type = doc.m_mcmc.getType();
			m_bIsList = false;
			for (int i = 0; i < m_sPathComponents.length; i++) {
				List<Plugin> oldPlugins = plugins;
				plugins = new ArrayList<Plugin>();
				m_parentPlugins = new ArrayList<Plugin>();
				m_parentInputs =  new ArrayList<Input<?>>();
				for (Plugin plugin: oldPlugins) {
					Input<?> namedInput = plugin.getInput(m_sPathComponents[i]);
					m_type = namedInput.getType();
					if (namedInput.get() instanceof List<?>) {
						m_bIsList = true;
						List<?> list = (List<?>) namedInput.get();
						if (m_sConditionalAttribute[i] == null) {
							for (Object o : list) {
								Plugin plugin2 = (Plugin) o;
								plugins.add(plugin2);
								m_parentPlugins.add(plugin);
								m_parentInputs.add(namedInput);
							}
							//throw new Exception ("Don't know which element to pick from the list. List component should come with a condition. " + m_sPathComponents[i]);
						} else {
							int nMatches = 0;
							for (int j = 0; j < list.size(); j++) {
								Plugin plugin2 = (Plugin) list.get(j);
								if (matches(plugin2, m_sConditionalAttribute[i], m_sConditionalValue[i])) {
									plugins.add(plugin2);
									m_parentPlugins.add(plugin);
									m_parentInputs.add(namedInput);
									nMatches++;
									break;
								}
							}
							if (nMatches == 0) {
								m_parentInputs.add(namedInput);
								m_parentPlugins.add(plugin);
							}
						}
					} else if (namedInput.get() instanceof Plugin) {
						m_bIsList = false;
						if (m_sConditionalAttribute[i] == null) {
							plugins.add((Plugin)namedInput.get());
							m_parentPlugins.add(plugin);
							m_parentInputs.add(namedInput);
						} else {
							if (matches(plugin, m_sConditionalAttribute[i], m_sConditionalValue[i])) {
//							if ((m_sConditionalAttribute[i].equals("id") && plugin.getID().equals(m_sConditionalValue[i])) ||
//							    (m_sConditionalAttribute[i].equals("type") && plugin.getClass().getName().equals(m_sConditionalValue[i]))) {
									plugins.add(plugin);
									m_parentPlugins.add(plugin);
									m_parentInputs.add(namedInput);
							}
						}
					} else {
						throw new Exception("input " + m_sPathComponents[i] + "  is not a plugin or list");
					}
				}
			}
			if (m_sTypeInput.get() != null) {
				m_type = Class.forName(m_sTypeInput.get());
			}
			// sanity check
			if (!m_bIsList && !m_bHasPartitionsInput.get() && plugins.size() > 1) {
				System.err.println("WARNING: multiple plugins match, but hasPartitions=false");
				// this makes sure that all mathing plugins are available in one go
				m_bIsList = true;
				// this suppresses syncing
				m_parentInputs.clear();
			}
			m_inputs.clear();
			m_startInputs.clear();
			for (Plugin plugin: plugins) {
				m_inputs.add(plugin);
				m_startInputs.add(plugin);
			}
			
			if (!m_bIsList) {
				m_input = new FlexibleInput<Plugin>();
			} else {
				m_input = new FlexibleInput<ArrayList<Plugin>>(new ArrayList<Plugin>());
			}
			m_input.setRule(Validate.REQUIRED);
			syncTo(iPartition);
			if (m_parentPlugins != null && m_parentPlugins.size() > 0) System.err.println("sync " + m_parentPlugins.get(iPartition) + "[?] = " + m_input.get());
			return m_input;
		} catch (Exception e) {
			System.err.println("Warning: could not find objects in path " + Arrays.toString(m_sPathComponents));
		}
		return null;
	} // resolveInputs
	
	private boolean matches(Plugin plugin2, String sConditionalAttribute, String sConditionalValue) {
		if (sConditionalAttribute.equals("id") && plugin2.getID().equals(sConditionalValue)) return true;
	    if (sConditionalAttribute.equals("type") && plugin2.getClass().getName().equals(sConditionalValue)) return true;
	    if (sConditionalAttribute.equals("type!") && !plugin2.getClass().getName().equals(sConditionalValue)) return true;
		return false;
	}

	@SuppressWarnings("unchecked")
	public void sync(int iPartition) {
		if (m_parentInputs.size() > 0) { 
			Input<?> input = m_parentInputs.get(iPartition);
			if (m_bIsList) {			
				List<Object> list = (List<Object>) m_input.get();
				List<Object> targetList = ((List<Object>)input.get());
				//targetList.clear(); 
				// only clear former members
				for (Plugin plugin : m_startInputs) {
					targetList.remove(plugin);
				}
				targetList.addAll(list);
			} else {
				try {
					System.err.println("sync " + m_parentPlugins.get(iPartition) + "[" + input.getName() + "] = " + m_input.get());
					input.setValue(m_input.get(), m_parentPlugins.get(iPartition));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	} 

	/** initialise m_input, and either m_plugin or m_pluginList **/
	public void syncTo(int iPartition) {
		m_input.setType(m_type);
		try {
			if (m_bIsList) { 
				for (Plugin plugin : m_inputs) {
					m_input.setValue(plugin, this);
				}
			} else {
				Plugin plugin = m_inputs.get(iPartition); 
				m_input.setValue(plugin, this);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	Input<?> getInput() {
		return m_input;
	}

	/** returns name of the plugin **/
	public String getType() {
		if (m_bIsList) {
			return m_inputs.get(0).getClass().getName();
		}
		return m_input.get().getClass().getName();
	}

//	public void addItem(Plugin plugin) {
//		m_parentInputs.add(m_parentInputs.get(m_parentInputs.size()-1));
//		m_parentPlugins.add(m_parentPlugins.get(m_parentPlugins.size()-1));
//	}

}
