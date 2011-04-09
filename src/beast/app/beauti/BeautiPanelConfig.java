package beast.app.beauti;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
	
	
	String [] m_sPathComponents;
	String [] m_sConditionalAttribute;
	String [] m_sConditionalValue;
	
	/** plugins associated with inputs **/
	List<Plugin> m_inputs;
	FlexibleInput<?> m_input;
	
	class FlexibleInput<T> extends Input<T> {
		public void setType(Class<?> type) {
			theClass = type;
		}
	}
	
	Plugin m_plugin;
	List<Plugin> m_pluginList;
	
	@Override
	public void initAndValidate() throws Exception {
		m_sPathComponents = m_sPathInput.get().split("/");
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
		m_input = new FlexibleInput<Plugin>();
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
			List<Plugin> plugins = new ArrayList<Plugin>();
			plugins.add(doc.m_mcmc.get());
			for (int i = 0; i < m_sPathComponents.length; i++) {
				List<Plugin> oldPlugins = plugins;
				plugins = new ArrayList<Plugin>();
				for (Plugin plugin: oldPlugins) {
					Input<?> namedInput = plugin.getInput(m_sPathComponents[i]);
					if (namedInput.get() instanceof List<?>) {
						List<?> list = (List<?>) namedInput.get();
						if (m_sConditionalAttribute[i] == null) {
							for (Object o : list) {
								Plugin plugin2 = (Plugin) o;
								plugins.add(plugin2);
							}
							//throw new Exception ("Don't know which element to pick from the list. List component should come with a condition. " + m_sPathComponents[i]);
						} else {
							if (m_sConditionalAttribute[i].equals("id")) {
								for (int j = 0; j < list.size(); j++) {
									Plugin plugin2 = (Plugin) list.get(j);
									if (plugin2.getID().equals(m_sConditionalValue[i])) {
										plugins.add(plugin2);
										break;
									}
								}
							}
						}
					} else if (namedInput.get() instanceof Plugin) {
						if (m_sConditionalAttribute[i] == null) {
							plugins.add((Plugin)namedInput.get());
						} else {
							if (m_sConditionalAttribute[i].equals("id")) {
								if (plugin.getID().equals(m_sConditionalValue[i])) {
									plugins.add(plugin);
								}
							}
						}
					} else {
						throw new Exception("input " + m_sPathComponents[i] + "  is not a plugin or list");
					}
				}
			}
			// sanity check
			if (!m_bHasPartitionsInput.get() && plugins.size() > 1) {
				throw new Exception("multiple plugins match, but hasPartitions=false");
			}
			m_inputs.clear();
			for (Plugin plugin: plugins) {
				m_inputs.add(plugin);
			}
			syncTo(iPartition);
			return m_input;
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Invalid Beauti configuration for " + Arrays.toString(m_sPathComponents));
			System.exit(0);
		}
		return null;
	} // resolveInputs
	
	public void sync() {
		if (m_input.get() instanceof List) {
			List<Object> list = (List<Object>) m_input.get();
			list.clear();
			for (Object o: m_pluginList) {
				try {
					m_input.setValue(o, null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			try {
				m_input.setValue(m_plugin, null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	} 

	/** initialise m_input, and either m_plugin or m_pluginList **/
	public void syncTo(int iPartition) {
		Plugin plugin = m_inputs.get(iPartition); 
		try {
			m_input.setType(plugin.getClass());
			m_input.setValue(plugin, this);
		} catch (Exception e) {
			e.printStackTrace();
		} 
		if (m_input.get() instanceof List) {
			List<Plugin> list = (List<Plugin>) m_input.get();
			m_pluginList = new ArrayList<Plugin>();
			for (Plugin o: list) {
				m_pluginList.add(o);
			}
		} else {
			m_plugin = (Plugin) m_input.get();
		}
	} 
}
