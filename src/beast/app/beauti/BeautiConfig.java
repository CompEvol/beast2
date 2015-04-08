package beast.app.beauti;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import beast.core.Description;
import beast.core.Input;
import beast.core.BEASTObject;
import beast.core.BEASTInterface;
import beast.core.Input.Validate;
import beast.evolution.alignment.Alignment;
import beast.util.XMLParser;

import javax.swing.*;

@Description("Beauti configuration object, used to find Beauti configuration " +
        "information from Beauti template files.")
public class BeautiConfig extends BEASTObject {
    public Input<String> inlineInput = new Input<String>("inlinePlugins", "comma separated list of inputs that should " +
            "go inline, e.g. beast.evolution.sitemodel.SiteModel.substModel");
    public Input<String> collapsedInput = new Input<String>("collapsedPlugins", "comma separated list of inputs that should " +
            "go inline, but are initially collapsed, e.g. beast.core.MCMC.logger");
    public Input<String> suppressInputs = new Input<String>("suppressPlugins", "comma separated list of inputs that should " +
            "be suppressed. e.g. beast.core.MCMC.operator");
    public Input<String> inputLabelMapInput = new Input<String>("inputLabelMap", "comma separated list of inputs and their " +
            "display labels separated by a '=', e.g. beast.core.MCMC.logger=Loggers ");
    //	public Input<String> m_hidePanels = new Input<String>("hidePanels","comma separated list of panes that should not" +
//			"be displayed when starting beauti, e.g. TAXON_SETS_PANEL,TIP_DATES_PANEL");
    public Input<String> buttonLabelMapInput = new Input<String>("buttonLabelMap", "comma separated list of buttons in dialogs and their " +
            "display labels separated by a '=', e.g. beast.app.beauti.BeautiInitDlg.&gt;&gt; details=Edit parameters");
    public Input<String> disableMenus = new Input<String>("disableMenus", "comma separated list of menus that should " +
            "not be visible, e.g., View.Show Data Panel,Mode");
    public Input<String> disableButtons = new Input<String>("disableButtons", "comma separated list of buttons that should " +
            "not be visible, e.g., beast.app.beauti.BeautiInitDlg.Analysis template:");
//	public Input<String> m_editButtonStatus = new Input<String>("editButtonStatus","comma separated list of list-inputs with custom " +
//	"button status. One of 'none', 'addonly' 'delonly' +, e.g., beast.core.MCMC.operator=addonly");

    public Input<List<BeautiPanelConfig>> panelsInput = new Input<List<BeautiPanelConfig>>("panel", "define custom panels and their properties",
            new ArrayList<BeautiPanelConfig>());
    public Input<Boolean> bIsExpertInput = new Input<Boolean>("isExpert", "flag to indicate Beauti should start in expert mode", false);


    public Input<BeautiSubTemplate> partitionTemplate = new Input<BeautiSubTemplate>("partitiontemplate", "defines template used when creating a partition", Validate.REQUIRED);
    public Input<List<BeautiSubTemplate>> subTemplatesInput = new Input<List<BeautiSubTemplate>>("subtemplate", "defines subtemplates for creating selected classes",
            new ArrayList<BeautiSubTemplate>());

    public Input<List<BeautiAlignmentProvider>> alignmentProviderInput = new Input<List<BeautiAlignmentProvider>>("alignmentProvider", "defines providers for adding new alignments",
            new ArrayList<BeautiAlignmentProvider>());

    /**
     * list of inputs for which the input editor should be expanded inline in a dialog
     * in the format <className>.<inputName>, e.g. beast.evolution.sitemodel.SiteModel.substModel
     */
    public Set<String> inlinePlugins = new HashSet<String>();
    /**
     * list of inputs for which the input editor should be expanded inline in a dialog but initially collapsed.
     * e.g. beast.evolution.sitemodel.SiteModel.substModel
     */
    public Set<String> collapsedPlugins = new HashSet<String>();
    /**
     * list of inputs that should not be shown in a dialog. Same format as for m_inlinePlugins*
     */
    public Set<String> suppressPlugins = new HashSet<String>();
    /**
     * map that identifies the label to be used for a particular input *
     */
    public HashMap<String, String> inputLabelMap = new HashMap<String, String>();
    public HashMap<String, String> buttonLabelMap = new HashMap<String, String>();
//	public static HashMap<String, String> g_sEditButtonStatus = new HashMap<String, String>();

    //	public static Set<String> g_sHidePanels = new HashSet<String>();
    public Set<String> sDisabledMenus = new HashSet<String>();
    public Set<String> sDisabledButtons = new HashSet<String>();

    public List<BeautiPanelConfig> panels = new ArrayList<BeautiPanelConfig>();

    public List<BeautiSubTemplate> subTemplates;
    public List<BeautiAlignmentProvider> alignmentProvider;

    public BeautiSubTemplate hyperPriorTemplate = null;
    
    @Override
    public void initAndValidate() {
        parseSet(inlineInput.get(), null, inlinePlugins);
        parseSet(collapsedInput.get(), null, collapsedPlugins);
        inlinePlugins.addAll(collapsedPlugins);
//		parseSet(m_hidePanels.get(), "TAXON_SETS_PANEL,TIP_DATES_PANEL,PRIORS_PANEL,OPERATORS_PANEL", g_sHidePanels);
        parseSet(suppressInputs.get(), null, suppressPlugins);
        parseSet(disableMenus.get(), null, sDisabledMenus);
        parseSet(disableButtons.get(), null, sDisabledButtons);

        parseMap(inputLabelMapInput.get(), inputLabelMap);
        parseMap(buttonLabelMapInput.get(), buttonLabelMap);
//		parseMap(m_editButtonStatus.get(), g_sEditButtonStatus);
        for (BeautiPanelConfig panel : panelsInput.get()) {
            panels.add(panel);
            // check for duplicates
            for (BeautiPanelConfig panel2 : panels) {
                if (panel2.sNameInput.get().equals(panel.sNameInput.get()) && panel2 != panel) {
                    panels.remove(panels.size() - 1);
                    break;
                }
            }
        }
        //InputEditor.setExpertMode(bIsExpertInput.get());
        subTemplates = subTemplatesInput.get();
        alignmentProvider = alignmentProviderInput.get();

        try {
            XMLParser parser = new XMLParser();
        	hyperPriorTemplate = (BeautiSubTemplate) parser.parseBareFragment(HYPER_PRIOR_XML, true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    final static String HYPER_PRIOR_XML = 
    		"    <beast version='2.0'\n" +
    		"    	       namespace='beast.app.beauti:beast.core:beast.evolution.branchratemodel:beast.evolution.speciation:beast.evolution.tree.coalescent:beast.core.util:beast.evolution.nuc:beast.evolution.operators:beast.evolution.sitemodel:beast.evolution.substitutionmodel:beast.evolution.likelihood:beast.evolution:beast.math.distributions'>\n" +
    		"    	<!-- Parameter Hyper Prior -->\n" +
    		"    	        <subtemplate id='HyperPrior' class='beast.math.distributions.Prior' mainid='HyperPrior.$(n)'>\n" +
    		"    	<![CDATA[\n" +
    		"    	        <plugin id='HyperPrior.$(n)' spec='Prior' x='@parameter.$(n)'>\n" +
    		"    	            <distr spec='OneOnX'/>\n" +
    		"    			</plugin>\n" +
    		"\n" +
    		"    	        <plugin id='hyperScaler.$(n)' spec='ScaleOperator' scaleFactor='0.5' weight='0.1' parameter='@parameter.$(n)'/>\n" +
    		"    	]]>\n" +
    		"    	            <connect srcID='parameter.$(n)'            targetID='state' inputName='stateNode' if='inposterior(parameter.$(n)) and parameter.$(n)/estimate=true'/>\n" +
    		"\n" +
    		"    	            <connect srcID='hyperScaler.$(n)'          targetID='mcmc' inputName='operator' if='inposterior(parameter.$(n)) and parameter.$(n)/estimate=true'>Scale hyper parameter $(n)</connect>\n" +
    		"\n" +
    		"    	            <connect srcID='parameter.$(n)'            targetID='tracelog' inputName='log'  if='inposterior(parameter.$(n)) and parameter.$(n)/estimate=true'/>\n" +
    		"    	            <connect srcID='HyperPrior.$(n)'           targetID='tracelog' inputName='log'  if='inposterior(parameter.$(n)) and parameter.$(n)/estimate=true'/>\n" +
    		"\n" +
    		"    	            <connect srcID='HyperPrior.$(n)'           targetID='prior' inputName='distribution' if='inposterior(parameter.$(n)) and parameter.$(n)/estimate=true'>Hyper prior for parameter $(n)</connect>\n" +
    		"    	        </subtemplate>\n" +
    		"    	</beast>\n";
    
    public void setDoc(BeautiDoc doc) {
        partitionTemplate.get().setDoc(doc);
        for (BeautiSubTemplate sub : subTemplates) {
            sub.setDoc(doc);
        }
        doc.setExpertMode(bIsExpertInput.get());
        hyperPriorTemplate.doc = doc;
    }

    public void clear() {
        inlinePlugins = new HashSet<String>();
        collapsedPlugins = new HashSet<String>();
        suppressPlugins = new HashSet<String>();
        inputLabelMap = new HashMap<String, String>();
        buttonLabelMap = new HashMap<String, String>();
        sDisabledMenus = new HashSet<String>();
        sDisabledButtons = new HashSet<String>();
        panels = new ArrayList<BeautiPanelConfig>();
    }

    /**
     * @param doc
     * @param parent
     * @return a list of alignments based on the user selected alignment provider
     */
    public List<BEASTInterface> selectAlignments(BeautiDoc doc, JComponent parent) {
        List<BeautiAlignmentProvider> providers = alignmentProvider;
        BeautiAlignmentProvider selectedProvider = null;
        if (providers.size() == 1) {
            selectedProvider = providers.get(0);
        } else {
            selectedProvider = (BeautiAlignmentProvider) JOptionPane.showInputDialog(parent, "Select what to add",
                    "Add partition",
                    JOptionPane.QUESTION_MESSAGE, null, providers.toArray(),
                    providers.get(0));
            if (selectedProvider == null) {
                return null;
            }
        }
        List<BEASTInterface> plugins = selectedProvider.getAlignments(doc);
        // create taxon sets, if any
        if (plugins != null) {
	        for (BEASTInterface o : plugins) {
	        	if (o instanceof Alignment) {
	        		try {
	        			BeautiDoc.createTaxonSet((Alignment) o, doc);
	        		} catch(Exception e) {
	        			e.printStackTrace();
	        		}
	        	}
	        }
        }
        return plugins;
    } // selectAlignments
    
    public List<BeautiSubTemplate> getInputCandidates(BEASTInterface plugin, Input<?> input, Class<?> type) {
        List<BeautiSubTemplate> candidates = new ArrayList<BeautiSubTemplate>();
        for (BeautiSubTemplate template : subTemplates) {
            if (type.isAssignableFrom(template._class)) {
                try {
                    if (input.canSetValue(template.instance, plugin)) {
                        candidates.add(template);
                    }
                } catch (Exception e) {
                    // ignore: cannot set value
                }
            }
        }
        return candidates;
    }

    private void parseMap(String sStr, HashMap<String, String> stringMap) {
        if (sStr != null) {
            for (String sStr2 : sStr.split(",")) {
                String[] sStrs = sStr2.split("=");
                stringMap.put(normalize(sStrs[0]), normalize(sStrs.length == 1 ? "" : sStrs[1]));
            }
        }
    }

    private void parseSet(String sStr, String sDefault, Set<String> stringSet) {
        if (sStr == null) {
            sStr = sDefault;
        }
        if (sStr != null) {
            for (String sStr2 : sStr.split(",")) {
                stringSet.add(normalize(sStr2));
            }
        }
    }

    // remove leading and tailing spaces
    String normalize(String sStr) {
        int i = 0;
        int n = sStr.length();
        while (i < n && Character.isWhitespace(sStr.charAt(i))) {
            i++;
        }
        while (n > 0 && Character.isWhitespace(sStr.charAt(n - 1))) {
            n--;
        }
        return sStr.substring(i, n);
    }

    public String getButtonLabel(String sClass, String sStr) {
        if (buttonLabelMap.containsKey(sClass + "." + sStr)) {
            return buttonLabelMap.get(sClass + "." + sStr);
        }
        return sStr;
    }

    public String getButtonLabel(Object o, String sStr) {
        if (buttonLabelMap.containsKey(o.getClass().getName() + "." + sStr)) {
            return buttonLabelMap.get(o.getClass().getName() + "." + sStr);
        }
        return sStr;
    }

    public String getInputLabel(BEASTInterface plugin, String sName) {
        if (inputLabelMap.containsKey(plugin.getClass().getName() + "." + sName)) {
            sName = inputLabelMap.get(plugin.getClass().getName() + "." + sName);
        }
        return sName;
    }

    public boolean menuIsInvisible(String sMenuName) {
        return sDisabledMenus.contains(sMenuName);
    }

    static BeautiSubTemplate NULL_TEMPLATE = new BeautiSubTemplate();

    public static BeautiSubTemplate getNullTemplate(BeautiDoc doc) {
        NULL_TEMPLATE.setID("[none]");
        NULL_TEMPLATE._class = Object.class;
        NULL_TEMPLATE.doc = doc;
        return NULL_TEMPLATE;
    }

//	public static boolean hasDeleteButton(String sFullInputName) {
//		if (!g_sEditButtonStatus.containsKey(sFullInputName)) {
//			return true;
//		}
//		String sStatus = g_sEditButtonStatus.get(sFullInputName);
//		if (sStatus.equals("none") || sStatus.equals("onlyadd")) {
//			return false;
//		}
//		return true;
//	}
//	public static boolean hasAddButton(String sFullInputName) {
//		if (!g_sEditButtonStatus.containsKey(sFullInputName)) {
//			return true;
//		}
//		String sStatus = g_sEditButtonStatus.get(sFullInputName);
//		if (sStatus.equals("none") || sStatus.equals("onlydel")) {
//			return false;
//		}
//		return true;
//	}
} // class BeautiConfig
