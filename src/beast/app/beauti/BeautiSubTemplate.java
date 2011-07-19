package beast.app.beauti;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.InputSource;

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
	public Input<XML> m_sXMLInput = new Input<XML>("value","collection of objects to be created in Beast2 xml format", Validate.REQUIRED);
	public Input<List<BeautiConnector>> m_connections = new Input<List<BeautiConnector>>("connect","Specifies which part of the template get connected to the main network", new ArrayList<BeautiConnector>());

	public BeautiSubTemplate() {
		int h = 3;
		h++;
	}
	String m_sClass;
	String m_sXML;
	String [] m_sSrcIDs;
	String [] m_sTargetIDs;
	String [] m_sTargetInputs;
	
	@Override
	public void initAndValidate() throws Exception {
		m_sClass = m_sClassInput.get();
		m_sXML = m_sXMLInput.get().m_sValue.get();
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



	void createSubNet(Alignment data, BeautiDoc doc) {
		String sDataID = data.getID();
		// wrap in a beast element with appropriate name spaces
		String sXML = "<beast version='2.0' \n" +
       "namespace='beast.app.beauti:beast.core:beast.evolution.branchratemodel:beast.evolution.speciation:beast.evolution.tree.coalescent:beast.core.util:beast.evolution.nuc:beast.evolution.operators:beast.evolution.sitemodel:beast.evolution.substitutionmodel:beast.evolution.likelihood:beast.evolution:beast.math.distributions'>\n" +
	        m_sXML +
	   "</beast>\n"; 
	   
		// resolve alignment references
		sXML = sXML.replaceAll("idref=[\"']data['\"]", "idref='" + sDataID + "'");
		// ensure uniqueness of IDs
		sXML = sXML.replaceAll("\\$\\(n\\)", sDataID);

		XMLParser parser = new XMLParser();
		try {
			HashMap<String, Plugin> sIDMap = new HashMap<String, Plugin>();
			sIDMap.put(sDataID, data);
			List<Plugin> plugins = parser.parseTemplate(sXML, sIDMap);
			for (Plugin plugin : plugins) {
				doc.addPlugin(plugin);
			}
			for (int i = 0; i < m_sSrcIDs.length; i++) {
				Plugin src = sIDMap.get(m_sSrcIDs[i].replaceAll("\\$\\(n\\)", sDataID));
				doc.connect(src, m_sTargetIDs[i].replaceAll("\\$\\(n\\)", sDataID), m_sTargetInputs[i]);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}
