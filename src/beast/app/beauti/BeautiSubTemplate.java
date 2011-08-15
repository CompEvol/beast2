package beast.app.beauti;


import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.InputSource;

//import beast.app.beauti.BeautiConnector.ConnectCondition;
import beast.app.draw.PluginPanel;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Plugin;
import beast.evolution.alignment.Alignment;
import beast.evolution.datatype.DataType;
import beast.evolution.likelihood.TreeLikelihood;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.substitutionmodel.SubstitutionModel;
import beast.util.XMLParser;

@Description("Template that specifies which sub-net needs to be created when " +
		"a plugin of a paricular class is created.")
public class BeautiSubTemplate extends Plugin {
	public Input<String> sClassInput = new Input<String>("class","name of the class (with full class path) to be created", Validate.REQUIRED);
	public Input<String> sMainInput = new Input<String>("mainid","specifies id of the main plugin to be created by the template", Validate.REQUIRED);
	//public Input<XML> sXMLInput = new Input<XML>("value","collection of objects to be created in Beast2 xml format", Validate.REQUIRED);
	public Input<String> sXMLInput = new Input<String>("value","collection of objects to be created in Beast2 xml format", Validate.REQUIRED);
	public Input<List<BeautiConnector>> connections = new Input<List<BeautiConnector>>("connect","Specifies which part of the template get connected to the main network", new ArrayList<BeautiConnector>());

	Class<?> _class = null;
	Object instance;
	String sXML = null;
	String [] sSrcIDs;
	String [] sTargetIDs;
	String [] sTargetInputs;
//	ConnectCondition [] conditions;
	String sMainID = "";
	String sShortClassName;
	
	@Override
	public void initAndValidate() throws Exception {
		_class = Class.forName(sClassInput.get());
		sShortClassName = sClassInput.get().substring(sClassInput.get().lastIndexOf('.')+1);
		instance = _class.newInstance();
		sXML = sXMLInput.get();//.m_sValue.get();
		sMainID = sMainInput.get();
		// sanity check: make sure the XML is parseable
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.newDocumentBuilder().parse(new InputSource(new StringReader("<beast>" + sXML + "</beast>")));
        // make sure there are no comments in the XML: this screws up any XML when saved to file
        if (sXML.contains("<!--")){
        	while (sXML.contains("<!--")) {
        		int iStart = sXML.indexOf("<!--");
        		// next line is guaranteed to find something, things we already checked this is valid XML
        		int iEnd = sXML.indexOf("-->", iStart);
        		sXML = sXML.substring(0, iStart) + sXML.substring(iEnd + 3);
        	}
        }
        //m_sXMLInput.get().m_sValue.setValue("<![CDATA[" + m_sXML + "]]>", m_sXMLInput.get());
        sXMLInput.setValue("<![CDATA[" + sXML + "]]>", this);
        
		int nConnectors = connections.get().size();
		sSrcIDs = new String[nConnectors];
		sTargetIDs = new String[nConnectors];
		sTargetInputs = new String[nConnectors];
//		conditions = new ConnectCondition[nConnectors];

		for (int i = 0; i < nConnectors; i++) {
			BeautiConnector connector = connections.get().get(i);
			sSrcIDs[i] = connector.sSourceID.get();
			sTargetIDs[i] = connector.sTargetID.get();
			sTargetInputs[i] = connector.sInputName.get();
//			conditions[i] = connector.connectCondition.get(); 
		}
	}


	void removeSubNet(Object o) throws Exception {
		if (o== null) {
			// nothing to do
			return;
		}
		Plugin plugin = null;
		if (o instanceof Plugin) {
			plugin = (Plugin) o;
		}
		BeautiDoc doc = BeautiDoc.g_doc;
		
		// find template that created this plugin
		String sID = plugin.getID();
		String sPartition = sID.substring(sID.indexOf(".") + 1);
		sID = sID.substring(0, sID.indexOf("."));
		BeautiSubTemplate template = null;
		for (BeautiSubTemplate template2 : doc.m_beautiConfig.subTemplates.get()) {
			if (template2.matchesName(sID)) {
				template = template2;
				break;
			}
		}

		if (template == null) {
			throw new Exception("Cannot find template for removing " + plugin.getID());
		}
		
		// disconnect all connection points in the template
		for (int i = 0; i < template.sSrcIDs.length; i++) {
			Plugin src = PluginPanel.g_plugins.get(template.sSrcIDs[i].replaceAll("\\$\\(n\\)", sPartition));
			String sTargetID = template.sTargetIDs[i].replaceAll("\\$\\(n\\)", sPartition);
//			switch (template.conditions[i]) {
//			case always:
				doc.disconnect(src, sTargetID, template.sTargetInputs[i]);
//				break;
//			case ifunlinked:
//				Plugin plugin2 = PluginPanel.g_plugins.get(sTargetID);
//				Input<?> input2 = plugin.getInput(template.sTargetInputs[i]);
//				if (input2.get() == null) {
//					// no value set yet, so connect
//					doc.connect(src, sTargetID, template.sTargetInputs[i]);
//				} else if (input2.get() instanceof List) {
//					// check if the src is already in the list of plugins in the target input
//					List<?> list = (List) input2.get();
//					if (!list.contains(src)) {
//						doc.connect(src, sTargetID, template.sTargetInputs[i]);
//					}
//				} else if (input2.get() != src) {
//					// it is not a list-input, so just check equality of value and src
//					doc.connect(src, sTargetID, template.sTargetInputs[i]);
//				}
//				break;
//			}
		}
	}

	public Plugin createSubNet(String sPartition, Plugin plugin, Input<?> input) throws Exception {
		removeSubNet(input.get());
		if (sXML == null) {
			// this is the NULL_TEMPLATE
			input.setValue(null, plugin);
			return null;
		}
		Plugin o = createSubNet(sPartition, BeautiDoc.g_doc, PluginPanel.g_plugins);
		input.setValue(o, plugin);
		return o;
	}
	
	public Plugin createSubNet(String sPartition, List<Plugin> list, int iItem) throws Exception {
		removeSubNet(list.get(iItem));
		if (sXML == null) {
			// this is the NULL_TEMPLATE
			list.set(iItem, null);
			return null;
		}
		Plugin o = createSubNet(sPartition, BeautiDoc.g_doc, PluginPanel.g_plugins);
		list.set(iItem, o);
		return o;
	}
	

	
	Plugin createSubNet(Alignment data, BeautiDoc doc) {
		String sPartition = data.getID();
		HashMap<String, Plugin> sIDMap = PluginPanel.g_plugins;//new HashMap<String, Plugin>();
		sIDMap.put(sPartition, data);
		return createSubNet(sPartition, doc, sIDMap);
	}	
	
	private Plugin createSubNet(String sPartition, BeautiDoc doc, HashMap<String, Plugin> sIDMap) {
		// wrap in a beast element with appropriate name spaces
		String _sXML = "<beast version='2.0' \n" +
       "namespace='beast.app.beauti:beast.core:beast.evolution.branchratemodel:beast.evolution.speciation:beast.evolution.tree.coalescent:beast.core.util:beast.evolution.nuc:beast.evolution.operators:beast.evolution.sitemodel:beast.evolution.substitutionmodel:beast.evolution.likelihood:beast.evolution:beast.math.distributions'>\n" +
	        sXML +
	   "</beast>\n"; 
	   
		// resolve alignment references
		_sXML = _sXML.replaceAll("idref=[\"']data['\"]", "idref='" + sPartition + "'");
		// ensure uniqueness of IDs
		_sXML = _sXML.replaceAll("\\$\\(n\\)", sPartition);

		XMLParser parser = new XMLParser();
		parser.setRequiredInputProvider(doc);
		List<Plugin> plugins = null;
		try {
			plugins = parser.parseTemplate(_sXML, sIDMap, true);
			for (Plugin plugin : plugins) {
				doc.addPlugin(plugin);
			}
			for (int i = 0; i < sSrcIDs.length; i++) {
				Plugin src = sIDMap.get(sSrcIDs[i].replaceAll("\\$\\(n\\)", sPartition));
				String sTargetID = sTargetIDs[i].replaceAll("\\$\\(n\\)", sPartition);
//				switch (conditions[i]) {
//				case always:
					doc.connect(src, sTargetID, sTargetInputs[i]);
//					break;
//				case ifunlinked:
//					Plugin plugin = PluginPanel.g_plugins.get(sTargetID);
//					Input<?> input = plugin.getInput(sTargetInputs[i]);
//					if (input.get() == null) {
//						// no value set yet, so connect
//						doc.connect(src, sTargetID, sTargetInputs[i]);
//					} else if (input.get() instanceof List) {
//						// check if the src is already in the list of plugins in the target input
//						List<?> list = (List) input.get();
//						if (!list.contains(src)) {
//							doc.connect(src, sTargetID, sTargetInputs[i]);
//						}
//					} else if (input.get() != src) {
//						// it is not a list-input, so just check equality of value and src
//						doc.connect(src, sTargetID, sTargetInputs[i]);
//					}
//					break;
//				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (sMainID.equals("[top]")) {
			return plugins.get(0);
		}
		
		String sID = sMainID;
		sID = sID.replaceAll("\\$\\(n\\)", sPartition);
		Plugin plugin = PluginPanel.g_plugins.get(sID);

		if (this == doc.m_beautiConfig.partitionTemplate.get()) {
			// HACK: need to make sure the subst model is of the correct type
			Plugin treeLikelihood = PluginPanel.g_plugins.get("treeLikelihood." + sPartition);
			DataType dataType = ((TreeLikelihood) treeLikelihood).m_data.get().getDataType();
			SiteModel.Base siteModel = ((TreeLikelihood) treeLikelihood).m_pSiteModel.get();
			SubstitutionModel substModel = siteModel.m_pSubstModel.get();
			try {
				siteModel.canSetSubstModel(substModel);
			} catch (Exception e) {
				Object o = doc.createInput(siteModel, siteModel.m_pSubstModel);
				try {
					siteModel.m_pSubstModel.setValue(o, siteModel);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			
		}
		
		//System.err.println(new XMLProducer().toXML(plugin));
		return plugin;
	}

	public String getMainID() {
		return sMainID;
	}


	@Override
	public String toString() {
		String sID = getID();
		sID = sID.replaceAll("([a-z])([A-Z])", "$1 $2");
		return sID;
	}



	public boolean matchesName(String sID) {
		if (getMainID().replaceAll(".\\$\\(n\\)", "").equals(sID)) {
			return true;
		}
		if (sShortClassName != null && sShortClassName.equals(sID)) {
			return true;
		}
		return false;
	}
}
