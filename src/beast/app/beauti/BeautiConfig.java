package beast.app.beauti;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

import beast.core.BEASTInterface;
import beast.core.BEASTObject;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.evolution.alignment.Alignment;
import beast.util.XMLParser;

@Description("Beauti configuration object, used to find Beauti configuration " +
        "information from Beauti template files.")
public class BeautiConfig extends BEASTObject {
    final public Input<String> inlineInput = new Input<>("inlinePlugins", "comma separated list of inputs that should " +
            "go inline, e.g. beast.evolution.sitemodel.SiteModel.substModel");
    final public Input<String> collapsedInput = new Input<>("collapsedPlugins", "comma separated list of inputs that should " +
            "go inline, but are initially collapsed, e.g. beast.core.MCMC.logger");
    final public Input<String> suppressInputs = new Input<>("suppressPlugins", "comma separated list of inputs that should " +
            "be suppressed. e.g. beast.core.MCMC.operator");
    final public Input<String> inputLabelMapInput = new Input<>("inputLabelMap", "comma separated list of inputs and their " +
            "display labels separated by a '=', e.g. beast.core.MCMC.logger=Loggers ");
    //	public Input<String> m_hidePanels = new Input<>("hidePanels","comma separated list of panes that should not" +
//			"be displayed when starting beauti, e.g. TAXON_SETS_PANEL,TIP_DATES_PANEL");
    final public Input<String> buttonLabelMapInput = new Input<>("buttonLabelMap", "comma separated list of buttons in dialogs and their " +
            "display labels separated by a '=', e.g. beast.app.beauti.BeautiInitDlg.&gt;&gt; details=Edit parameters");
    final public Input<String> disableMenus = new Input<>("disableMenus", "comma separated list of menus that should " +
            "not be visible, e.g., View.Show Data Panel,Mode");
    final public Input<String> disableButtons = new Input<>("disableButtons", "comma separated list of buttons that should " +
            "not be visible, e.g., beast.app.beauti.BeautiInitDlg.Analysis template:");
//	public Input<String> m_editButtonStatus = new Input<>("editButtonStatus","comma separated list of list-inputs with custom " +
//	"button status. One of 'none', 'addonly' 'delonly' +, e.g., beast.core.MCMC.operator=addonly");

    final public Input<List<BeautiPanelConfig>> panelsInput = new Input<>("panel", "define custom panels and their properties",
            new ArrayList<>());
    final public Input<Boolean> isExpertInput = new Input<>("isExpert", "flag to indicate Beauti should start in expert mode", false);


    final public Input<BeautiSubTemplate> partitionTemplate = new Input<>("partitiontemplate", "defines template used when creating a partition", Validate.REQUIRED);
    final public Input<List<BeautiSubTemplate>> subTemplatesInput = new Input<>("subtemplate", "defines subtemplates for creating selected classes",
            new ArrayList<>());

    final public Input<List<BeautiAlignmentProvider>> alignmentProviderInput = new Input<>("alignmentProvider", "defines providers for adding new alignments",
            new ArrayList<>());

    /**
     * list of inputs for which the input editor should be expanded inline in a dialog
     * in the format <className>.<inputName>, e.g. beast.evolution.sitemodel.SiteModel.substModel
     */
    public Set<String> inlineBEASTObject = new HashSet<>();
    /**
     * list of inputs for which the input editor should be expanded inline in a dialog but initially collapsed.
     * e.g. beast.evolution.sitemodel.SiteModel.substModel
     */
    public Set<String> collapsedBEASTObjects = new HashSet<>();
    /**
     * list of inputs that should not be shown in a dialog. Same format as for inlineBEASTObjects*
     */
    public Set<String> suppressBEASTObjects = new HashSet<>();
    /**
     * map that identifies the label to be used for a particular input *
     */
    public HashMap<String, String> inputLabelMap = new HashMap<>();
    public HashMap<String, String> buttonLabelMap = new HashMap<>();
//	public static HashMap<String, String> g_sEditButtonStatus = new HashMap<>();

    //	public static Set<String> g_sHidePanels = new HashSet<>();
    public Set<String> disabledMenus = new HashSet<>();
    public Set<String> disabledButtons = new HashSet<>();

    public List<BeautiPanelConfig> panels = new ArrayList<>();

    public List<BeautiSubTemplate> subTemplates;
    public List<BeautiAlignmentProvider> alignmentProvider;

    public BeautiSubTemplate hyperPriorTemplate = null;
    
    @Override
    public void initAndValidate() {
        parseSet(inlineInput.get(), null, inlineBEASTObject);
        parseSet(collapsedInput.get(), null, collapsedBEASTObjects);
        inlineBEASTObject.addAll(collapsedBEASTObjects);
//		parseSet(m_hidePanels.get(), "TAXON_SETS_PANEL,TIP_DATES_PANEL,PRIORS_PANEL,OPERATORS_PANEL", g_sHidePanels);
        parseSet(suppressInputs.get(), null, suppressBEASTObjects);
        parseSet(disableMenus.get(), null, disabledMenus);
        parseSet(disableButtons.get(), null, disabledButtons);

        parseMap(inputLabelMapInput.get(), inputLabelMap);
        parseMap(buttonLabelMapInput.get(), buttonLabelMap);
//		parseMap(m_editButtonStatus.get(), g_sEditButtonStatus);
        for (BeautiPanelConfig panel : panelsInput.get()) {
            panels.add(panel);
            // check for duplicates
            for (BeautiPanelConfig panel2 : panels) {
                if (panel2.nameInput.get().equals(panel.nameInput.get()) && panel2 != panel) {
                    panels.remove(panels.size() - 1);
                    break;
                }
            }
        }
        //InputEditor.setExpertMode(isExpertInput.get());
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
    		"    	        <beastObject id='HyperPrior.$(n)' spec='Prior' x='@parameter.$(n)'>\n" +
    		"    	            <distr spec='OneOnX'/>\n" +
    		"    			</beastObject>\n" +
    		"\n" +
    		"    	        <beastObject id='hyperScaler.$(n)' spec='ScaleOperator' scaleFactor='0.5' weight='0.1' parameter='@parameter.$(n)'/>\n" +
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
        doc.setExpertMode(isExpertInput.get());
        hyperPriorTemplate.doc = doc;
    }

    public void clear() {
        inlineBEASTObject = new HashSet<>();
        collapsedBEASTObjects = new HashSet<>();
        suppressBEASTObjects = new HashSet<>();
        inputLabelMap = new HashMap<>();
        buttonLabelMap = new HashMap<>();
        disabledMenus = new HashSet<>();
        disabledButtons = new HashSet<>();
        panels = new ArrayList<>();
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
        List<BEASTInterface> beastObjects = selectedProvider.getAlignments(doc);
        // create taxon sets, if any
        if (beastObjects != null) {
	        for (BEASTInterface o : beastObjects) {
	        	if (o instanceof Alignment) {
	        		try {
	        			BeautiDoc.createTaxonSet((Alignment) o, doc);
	        		} catch(Exception e) {
	        			e.printStackTrace();
	        		}
	        	}
	        }
        }
        return beastObjects;
    } // selectAlignments
    
    public List<BeautiSubTemplate> getInputCandidates(BEASTInterface beastObject, Input<?> input, Class<?> type) {
        List<BeautiSubTemplate> candidates = new ArrayList<>();
        for (BeautiSubTemplate template : subTemplates) {
            if (type.isAssignableFrom(template._class)) {
                try {
                    if (input.canSetValue(template.instance, beastObject)) {
                        candidates.add(template);
                    }
                } catch (Exception e) {
                    // ignore: cannot set value
                }
            }
        }
        return candidates;
    }

    private void parseMap(String str, HashMap<String, String> stringMap) {
        if (str != null) {
            for (String str2 : str.split(",")) {
                String[] strs = str2.split("=");
                stringMap.put(normalize(strs[0]), normalize(strs.length == 1 ? "" : strs[1]));
            }
        }
    }

    private void parseSet(String str, String defaultValue, Set<String> stringSet) {
        if (str == null) {
            str = defaultValue;
        }
        if (str != null) {
            for (String str2 : str.split(",")) {
                stringSet.add(normalize(str2));
            }
        }
    }

    // remove leading and tailing spaces
    String normalize(String str) {
        int i = 0;
        int n = str.length();
        while (i < n && Character.isWhitespace(str.charAt(i))) {
            i++;
        }
        while (n > 0 && Character.isWhitespace(str.charAt(n - 1))) {
            n--;
        }
        return str.substring(i, n);
    }

    public String getButtonLabel(String className, String str) {
        if (buttonLabelMap.containsKey(className + "." + str)) {
            return buttonLabelMap.get(className + "." + str);
        }
        return str;
    }

    public String getButtonLabel(Object o, String str) {
        if (buttonLabelMap.containsKey(o.getClass().getName() + "." + str)) {
            return buttonLabelMap.get(o.getClass().getName() + "." + str);
        }
        return str;
    }

    public String getInputLabel(BEASTInterface beastObject, String name) {
        if (inputLabelMap.containsKey(beastObject.getClass().getName() + "." + name)) {
            name = inputLabelMap.get(beastObject.getClass().getName() + "." + name);
        }
        return name;
    }

    public boolean menuIsInvisible(String menuName) {
        return disabledMenus.contains(menuName);
    }

    static BeautiSubTemplate NULL_TEMPLATE = new BeautiSubTemplate();

    public static BeautiSubTemplate getNullTemplate(BeautiDoc doc) {
        NULL_TEMPLATE.setID("[none]");
        NULL_TEMPLATE._class = Object.class;
        NULL_TEMPLATE.doc = doc;
        return NULL_TEMPLATE;
    }

//	public static boolean hasDeleteButton(String fullInputName) {
//		if (!g_sEditButtonStatus.containsKey(fullInputName)) {
//			return true;
//		}
//		String status = g_sEditButtonStatus.get(fullInputName);
//		if (status.equals("none") || status.equals("onlyadd")) {
//			return false;
//		}
//		return true;
//	}
//	public static boolean hasAddButton(String fullInputName) {
//		if (!g_sEditButtonStatus.containsKey(fullInputName)) {
//			return true;
//		}
//		String status = g_sEditButtonStatus.get(fullInputName);
//		if (status.equals("none") || status.equals("onlydel")) {
//			return false;
//		}
//		return true;
//	}
} // class BeautiConfig
