package beast.app.beauti;


import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.InputSource;

import beast.app.draw.PluginPanel;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Plugin;
import beast.evolution.alignment.Alignment;
import beast.util.XMLParser;

@Description("Template that specifies which sub-net needs to be created when " +
		"a plugin of a paricular class is created.")
public class BeautiSubTemplate extends Plugin {
	public Input<String> m_sClassInput = new Input<String>("class","name of the class (with full class path) to be created", Validate.REQUIRED);
	public Input<String> m_sMainInput = new Input<String>("mainid","specifies id of the main plugin to be created by the template", Validate.REQUIRED);
	public Input<XML> m_sXMLInput = new Input<XML>("value","collection of objects to be created in Beast2 xml format", Validate.REQUIRED);
	public Input<List<BeautiConnector>> m_connections = new Input<List<BeautiConnector>>("connect","Specifies which part of the template get connected to the main network", new ArrayList<BeautiConnector>());

	Class<?> m_class = null;
	String m_sXML = null;
	String [] m_sSrcIDs;
	String [] m_sTargetIDs;
	String [] m_sTargetInputs;
	String m_sMainID = "";
	
	@Override
	public void initAndValidate() throws Exception {
		m_class = Class.forName(m_sClassInput.get());
		m_sXML = m_sXMLInput.get().m_sValue.get();
		m_sMainID = m_sMainInput.get();
		// sanity check: make sure the XML is parseable
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.newDocumentBuilder().parse(new InputSource(new StringReader("<beast>" + m_sXML + "</beast>")));
        // make sure there are no comments in the XML: this screws up any XML when saved to file
        if (m_sXML.contains("<!--")){
        	while (m_sXML.contains("<!--")) {
        		int iStart = m_sXML.indexOf("<!--");
        		// next line is guaranteed to find something, things we already checked this is valid XML
        		int iEnd = m_sXML.indexOf("-->", iStart);
        		m_sXML = m_sXML.substring(0, iStart) + m_sXML.substring(iEnd + 3);
        	}
        }
        m_sXMLInput.get().m_sValue.setValue("<![CDATA[" + m_sXML + "]]>", m_sXMLInput.get());
        
		int nConnectors = m_connections.get().size();
		m_sSrcIDs = new String[nConnectors];
		m_sTargetIDs = new String[nConnectors];
		m_sTargetInputs = new String[nConnectors];
		for (int i = 0; i < nConnectors; i++) {
			BeautiConnector connector = m_connections.get().get(i);
			m_sSrcIDs[i] = connector.m_sSourceID.get();
			m_sTargetIDs[i] = connector.m_sTargetID.get();
			m_sTargetInputs[i] = connector.m_sInputName.get();
		}
	}



	public Plugin createSubNet(String sPartition) {
		if (m_sXML == null) {
			// this is the NULL_TEMPLATE
			return null;
		}
		return createSubNet(sPartition, BeautiDoc.g_doc, PluginPanel.g_plugins);
	}
	
	Plugin createSubNet(Alignment data, BeautiDoc doc) {
		String sPartition = data.getID();
		HashMap<String, Plugin> sIDMap = new HashMap<String, Plugin>();
		sIDMap.put(sPartition, data);
		return createSubNet(sPartition, doc, sIDMap);
	}	
	
	private Plugin createSubNet(String sPartition, BeautiDoc doc, HashMap<String, Plugin> sIDMap) {
		// wrap in a beast element with appropriate name spaces
		String sXML = "<beast version='2.0' \n" +
       "namespace='beast.app.beauti:beast.core:beast.evolution.branchratemodel:beast.evolution.speciation:beast.evolution.tree.coalescent:beast.core.util:beast.evolution.nuc:beast.evolution.operators:beast.evolution.sitemodel:beast.evolution.substitutionmodel:beast.evolution.likelihood:beast.evolution:beast.math.distributions'>\n" +
	        m_sXML +
	   "</beast>\n"; 
	   
		// resolve alignment references
		sXML = sXML.replaceAll("idref=[\"']data['\"]", "idref='" + sPartition + "'");
		// ensure uniqueness of IDs
		sXML = sXML.replaceAll("\\$\\(n\\)", sPartition);

		XMLParser parser = new XMLParser();
		List<Plugin> plugins = null;
		try {
			plugins = parser.parseTemplate(sXML, sIDMap);
			for (Plugin plugin : plugins) {
				doc.addPlugin(plugin);
			}
			for (int i = 0; i < m_sSrcIDs.length; i++) {
				Plugin src = sIDMap.get(m_sSrcIDs[i].replaceAll("\\$\\(n\\)", sPartition));
				doc.connect(src, m_sTargetIDs[i].replaceAll("\\$\\(n\\)", sPartition), m_sTargetInputs[i]);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (m_sMainID.equals("[top]")) {
			return plugins.get(0);
		}
		
		String sID = m_sMainID;
		sID = sID.replaceAll("\\$\\(n\\)", sPartition);
		Plugin plugin = PluginPanel.g_plugins.get(sID);
		//System.err.println(new XMLProducer().toXML(plugin));
		return plugin;
	}

	public String getMainID() {
		return m_sMainID;
	}


	@Override
	public String toString() {
		String sID = getID();
		sID = sID.replaceAll("([a-z])([A-Z])", "$1 $2");
		return sID;
	}
}
