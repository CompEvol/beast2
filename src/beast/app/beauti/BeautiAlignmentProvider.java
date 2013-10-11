package beast.app.beauti;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import beast.app.draw.ExtensionFileFilter;
import beast.core.Description;
import beast.core.Input;
import beast.core.BEASTObject;
import beast.core.Input.Validate;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.FilteredAlignment;
import beast.evolution.alignment.Sequence;
import beast.util.NexusParser;
import beast.util.XMLParser;



@Description("Class for creating new alignments to be edited by AlignmentListInputEditor")
public class BeautiAlignmentProvider extends BEASTObject {
	
	public Input<BeautiSubTemplate> template = new Input<BeautiSubTemplate>("template", "template to be used after creating a new alignment. ", Validate.REQUIRED); 
	
	@Override
	public void initAndValidate() throws Exception {}
	
	/** 
	 * return amount to which the provided matches an alignment 
	 * The provider with the highest match will be used to edit the alignment 
	 * */
	int matches(Alignment alignment) {
		return 1;
	}
	
	/** 
	 * return new alignment, return null if not successfull 
	 * **/
	List<BEASTObject> getAlignments(BeautiDoc doc) {
		List<BEASTObject> selectedPlugins = new ArrayList<BEASTObject>();
		JFileChooser fileChooser = new JFileChooser(Beauti.g_sDir);

		fileChooser.addChoosableFileFilter(new ExtensionFileFilter(".xml", "Beast xml file (*.xml)"));
		String[] exts = { ".nex", ".nxs", ".nexus" };
		fileChooser.addChoosableFileFilter(new ExtensionFileFilter(exts, "Nexus file (*.nex)"));

		fileChooser.setDialogTitle("Load Sequence");
		fileChooser.setMultiSelectionEnabled(true);
		int rval = fileChooser.showOpenDialog(null);

		if (rval == JFileChooser.APPROVE_OPTION) {

			File[] files = fileChooser.getSelectedFiles();
			for (File file : files) {
				String fileName = file.getName();
				// if (sFileName.lastIndexOf('/') > 0) {
				// Beauti.g_sDir = sFileName.substring(0,
				// sFileName.lastIndexOf('/'));
				// }
				if (fileName.toLowerCase().endsWith(".nex") || fileName.toLowerCase().endsWith(".nxs")
						|| fileName.toLowerCase().endsWith(".nexus")) {
					NexusParser parser = new NexusParser();
					try {
						parser.parseFile(file);
						if (parser.filteredAlignments.size() > 0) {
							/**
							 * sanity check: make sure the filters do not
							 * overlap
							 **/
							int[] used = new int[parser.m_alignment.getSiteCount()];
							Set<Integer> overlap = new HashSet<Integer>();
							int partitionNr = 1;
							for (Alignment data : parser.filteredAlignments) {
								int[] indices = ((FilteredAlignment) data).indices();
								for (int i : indices) {
									if (used[i] > 0) {
										overlap.add(used[i] * 10000 + partitionNr);
									} else {
										used[i] = partitionNr;
									}
								}
								partitionNr++;
							}
							if (overlap.size() > 0) {
								String overlaps = "<html>Warning: The following partitions overlap:<br/>";
								for (int i : overlap) {
									overlaps += parser.filteredAlignments.get(i / 10000 - 1).getID()
											+ " overlaps with "
											+ parser.filteredAlignments.get(i % 10000 - 1).getID() + "<br/>";
								}
								overlaps += "The first thing you might want to do is delete some of these partitions.</html>";
								JOptionPane.showMessageDialog(null, overlaps);
							}
							/** add alignments **/
							for (Alignment data : parser.filteredAlignments) {
								selectedPlugins.add(data);
							}
						} else {
							selectedPlugins.add(parser.m_alignment);
						}
					} catch (Exception ex) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog(null, "Loading of " + fileName + " failed: " + ex.getMessage());
						return null;
					}
				}
				if (file.getName().toLowerCase().endsWith(".xml")) {
					BEASTObject alignment = getXMLData(file);
					selectedPlugins.add(alignment);
				}
			}
			for (BEASTObject plugin : selectedPlugins) {
				doc.addAlignmentWithSubnet((Alignment) plugin, getStartTemplate());
			}
			return selectedPlugins;
		}
		return null;
	}

	/** provide GUI for manipulating the alignment **/
	void editAlignment(Alignment alignment, BeautiDoc doc) {
		try {
			AlignmentViewer viewer = new AlignmentViewer(alignment);
			viewer.showInDialog();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Something went wrong viewing the alignment: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/** check validity of the alignment, 
	 * return null if there are no problens, 
	 * return message string if something is fishy **/
	String validateAlignment() {
		return null;
	}
	
	/** return template to apply to this new alignment.
	 * By default, the partition template of the current beauti template is returned **/
	BeautiSubTemplate getStartTemplate() {
		return template.get();
	}

	static public BEASTObject getXMLData(File file) {
		String sXML = "";
		try {
			// parse as BEAST 2 xml fragment
			XMLParser parser = new XMLParser();
			BufferedReader fin = new BufferedReader(new FileReader(file));
			while (fin.ready()) {
				sXML += fin.readLine() + "\n";
			}
			fin.close();
			BEASTObject runnable = parser.parseFragment(sXML, false);
			return getAlignment(runnable);
		} catch (Exception ex) {
			// attempt to parse as BEAST 1 xml
			try {
				BEASTObject alignment = parseBeast1XML(sXML);
				if (alignment != null) {
					alignment.setID(file.getName().substring(0, file.getName().length() - 4));
				}
				return alignment;
			} catch (Exception ex2) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(null, "Loading of " + file.getName() + " failed: " + ex.getMessage()
						+ "\n" + ex2.getMessage());
			}
			return null;
		}
	}

	private static BEASTObject parseBeast1XML(String sXML) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(sXML)));
		doc.normalize();

		NodeList alignments = doc.getElementsByTagName("alignment");
		Alignment alignment = new Alignment();
		alignment.dataTypeInput.setValue("nucleotide", alignment);

		// parse first alignment
		org.w3c.dom.Node node = alignments.item(0);

		String sDataType = node.getAttributes().getNamedItem("dataType").getNodeValue();
		int nTotalCount = 4;
		if (sDataType == null) {
			alignment.dataTypeInput.setValue("integer", alignment);
		} else if (sDataType.toLowerCase().equals("dna") || sDataType.toLowerCase().equals("nucleotide")) {
			alignment.dataTypeInput.setValue("nucleotide", alignment);
			nTotalCount = 4;
		} else if (sDataType.toLowerCase().equals("aminoacid") || sDataType.toLowerCase().equals("protein")) {
			alignment.dataTypeInput.setValue("aminoacid", alignment);
			nTotalCount = 20;
		} else {
			alignment.dataTypeInput.setValue("integer", alignment);
		}

		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			org.w3c.dom.Node child = children.item(i);
			if (child.getNodeName().equals("sequence")) {
				Sequence sequence = new Sequence();
				// find the taxon
				String taxon = "";
				NodeList sequenceChildren = child.getChildNodes();
				for (int j = 0; j < sequenceChildren.getLength(); j++) {
					org.w3c.dom.Node child2 = sequenceChildren.item(j);
					if (child2.getNodeName().equals("taxon")) {
						taxon = child2.getAttributes().getNamedItem("idref").getNodeValue();
					}
				}
				String data = child.getTextContent();
				sequence.initByName("totalcount", nTotalCount, "taxon", taxon, "value", data);
				sequence.setID("seq_" + taxon);
				alignment.sequenceInput.setValue(sequence, alignment);

			}
		}
		// alignment.initAndValidate();
		alignment.setID("beast1");
		return alignment;
	} // parseBeast1XML


	static BEASTObject getAlignment(BEASTObject plugin) throws IllegalArgumentException, IllegalAccessException {
		if (plugin instanceof Alignment) {
			return plugin;
		}
		for (BEASTObject plugin2 : plugin.listActivePlugins()) {
			plugin2 = getAlignment(plugin2);
			if (plugin2 != null) {
				return plugin2;
			}
		}
		return null;
	}

}
