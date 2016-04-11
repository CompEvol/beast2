package beast.app.beauti;





import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import beast.core.BEASTInterface;
import beast.core.BEASTObject;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.FilteredAlignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.datatype.DataType;
import beast.math.distributions.MRCAPrior;
import beast.util.NexusParser;
import beast.util.XMLParser;



@Description("Class for creating new alignments to be edited by AlignmentListInputEditor")
public class BeautiAlignmentProvider extends BEASTObject {
	
	final public Input<BeautiSubTemplate> template = new Input<>("template", "template to be used after creating a new alignment. ", Validate.REQUIRED);
	
	@Override
	public void initAndValidate() {
	}
	
	/** 
	 * return amount to which the provided matches an alignment 
	 * The provider with the highest match will be used to edit the alignment 
	 * */
	protected int matches(Alignment alignment) {
		return 1;
	}
	
	/** 
	 * return new alignment, return null if not successful 
	 * **/
	protected List<BEASTInterface> getAlignments(BeautiDoc doc) {
        File [] files = beast.app.util.Utils.getLoadFiles("Load Alignment File",
                new File(Beauti.g_sDir), "Alignment files", "xml", 
                "fa","fas","fst","fasta","fna","ffn","faa","frn",
                "nex","nxs","nexus");
        if (files != null && files.length > 0) {
            return getAlignments(doc, files);
        }
		return null;
	}

    /**
     * return new alignment given files
     * @param doc
     * @param files
     * @return
     */
    public List<BEASTInterface> getAlignments(BeautiDoc doc, File[] files) {
        List<BEASTInterface> selectedBEASTObjects = new ArrayList<>();
        List<MRCAPrior> calibrations = new ArrayList<>();
        for (File file : files) {
            String fileName = file.getName();
			String fileExtension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
			Alignment alignment;

			switch (fileExtension) {
				case ".nex":
				case ".nxs":
				case ".nexus":
					NexusParser parser = new NexusParser();
					try {
						parser.parseFile(file);
						if (parser.filteredAlignments.size() > 0) {
							/**
							 * sanity check: make sure the filters do not
							 * overlap
							 **/
							int[] used = new int[parser.m_alignment.getSiteCount()];
							Set<Integer> overlap = new HashSet<>();
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
								sortByTaxonName(data.sequenceInput.get());
								selectedBEASTObjects.add(data);
							}
							if (parser.calibrations != null) {
								if (calibrations == null) {
									calibrations = new ArrayList<>();
								}
								calibrations.addAll(parser.calibrations);
							}
						} else {
							selectedBEASTObjects.add(parser.m_alignment);
							if (parser.calibrations != null) {
								if (calibrations == null) {
									calibrations = new ArrayList<>();
								}
								calibrations.addAll(parser.calibrations);
							}
						}
					} catch (Exception ex) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog(null, "Loading of " + fileName + " failed: " + ex.getMessage());
						return null;
					}
					break;

				case ".xml":
					alignment = (Alignment)getXMLData(file);
					selectedBEASTObjects.add(alignment);
					break;

				case ".fa":
				case ".fas":
				case ".fasta":
				case ".fst":
				case ".fna":
				case ".ffn":
				case ".faa":
				case ".frn":
					alignment = getFASTAData(file);
					sortByTaxonName(alignment.sequenceInput.get());
					selectedBEASTObjects.add(alignment);
					break;

                default:
                    JOptionPane.showMessageDialog(null,
                            "Unsupported sequence file extension.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    break;
			}
        }
        addAlignments(doc, selectedBEASTObjects);
        if (calibrations != null) {
        	selectedBEASTObjects.addAll(calibrations);
        }
        // doc.addMRCAPriors(calibrations);
        return selectedBEASTObjects;
    }
    
    
    protected void addAlignments(BeautiDoc doc, List<BEASTInterface> selectedBEASTObjects) {
        for (BEASTInterface beastObject : selectedBEASTObjects) {
        	// ensure ID of alignment is unique
        	int k = 0;
        	String id = beastObject.getID();
        	while (doc.pluginmap.containsKey(id)) {
        		k++;
        		id = beastObject.getID() + k;
        	}
        	beastObject.setID(id);
        	sortByTaxonName(((Alignment) beastObject).sequenceInput.get());
            doc.addAlignmentWithSubnet((Alignment) beastObject, getStartTemplate());
        }
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
	protected BeautiSubTemplate getStartTemplate() {
		return template.get();
	}

    protected void sortByTaxonName(List<Sequence> seqs) {
        Collections.sort(seqs, (Sequence o1, Sequence o2) -> {
                return o1.taxonInput.get().compareTo(o2.taxonInput.get());
            }
        );
    }

	static public BEASTInterface getXMLData(File file) {
		String xml = "";
		try {
			// parse as BEAST 2 xml fragment
			XMLParser parser = new XMLParser();
			BufferedReader fin = new BufferedReader(new FileReader(file));
			while (fin.ready()) {
				xml += fin.readLine() + "\n";
			}
			fin.close();
			BEASTInterface runnable = parser.parseBareFragment(xml, false);
			BEASTInterface alignment = getAlignment(runnable);
            alignment.initAndValidate();
            return alignment;
		} catch (Exception ex) {
			// attempt to parse as BEAST 1 xml
			try {
				String ID = file.getName();
				ID = ID.substring(0, ID.lastIndexOf('.')).replaceAll("\\..*", "");
				BEASTInterface alignment = parseBeast1XML(ID, xml);
				if (alignment != null) {
					alignment.setID(file.getName().substring(0, file.getName().length() - 4).replaceAll("\\..*", ""));
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
	
    private Alignment getFASTAData(File file) {
    	try {
    		// grab alignment data
        	Map<String, StringBuilder> seqMap = new HashMap<>();
        	List<String> taxa = new ArrayList<>();
        	String currentTaxon = null;
			BufferedReader fin = new BufferedReader(new FileReader(file));
	        String missing = "?";
	        String gap = "-";
	        int totalCount = 4;
	        String datatype = "nucleotide";
	        // According to http://en.wikipedia.org/wiki/FASTA_format lists file formats and their data content
			// .fna = nucleic acid
			// .ffn = nucleotide coding regions
			// .frn = non-coding RNA
			// .ffa = amino acid
    		boolean mayBeAminoacid = !(file.getName().toLowerCase().endsWith(".fna") || file.getName().toLowerCase().endsWith(".ffn") || file.getName().toLowerCase().endsWith(".frn"));
    		
			while (fin.ready()) {
				String line = fin.readLine();
				if (line.startsWith(";")) {
					// it is a comment, ignore
				} else 	if (line.startsWith(">")) {
					// it is a taxon
					currentTaxon = line.substring(1).trim();
					// only up to first space
					currentTaxon = currentTaxon.replaceAll("\\s.*$", "");
				} else {
					// it is a data line
					if (currentTaxon == null) {
						fin.close();
						throw new RuntimeException("Expected taxon defined on first line");
					}
					if (seqMap.containsKey(currentTaxon)) {
						StringBuilder sb = seqMap.get(currentTaxon);
						sb.append(line);
					} else {
						StringBuilder sb = new StringBuilder();
						seqMap.put(currentTaxon, sb);
						sb.append(line);
						taxa.add(currentTaxon);
					}
				}
			}
			fin.close();
			
			int charCount = -1;
			Alignment alignment = new Alignment();
	        for (final String taxon : taxa) {
	            final StringBuilder bsData = seqMap.get(taxon);
	            String data = bsData.toString();
	            data = data.replaceAll("\\s", "");
	            seqMap.put(taxon, new StringBuilder(data));

	            if (charCount < 0) {charCount = data.length();}
	            if (data.length() != charCount) {
	                throw new IllegalArgumentException("Expected sequence of length " + charCount + " instead of " + data.length() + " for taxon " + taxon);
	            }
	            // map to standard missing and gap chars
	            data = data.replace(missing.charAt(0), DataType.MISSING_CHAR);
	            data = data.replace(gap.charAt(0), DataType.GAP_CHAR);

	            if (mayBeAminoacid && datatype.equals("nucleotide") && !data.matches("[ACGTUXNacgtuxn?_-]+")) {
	            	datatype = "aminoacid";
	            	totalCount = 20;
	            	for (Sequence seq : alignment.sequenceInput.get()) {
	            		seq.totalCountInput.setValue(totalCount, seq);
	            	}
	            }
	            
	            final Sequence sequence = new Sequence();
	            data = data.replaceAll("[Xx]", "?");
	            sequence.init(totalCount, taxon, data);
	            sequence.setID(NexusParser.generateSequenceID(taxon));
	            alignment.sequenceInput.setValue(sequence, alignment);
	        }
	        String ID = file.getName();
	        ID = ID.substring(0, ID.lastIndexOf('.')).replaceAll("\\..*", "");
	        alignment.setID(ID);
			alignment.dataTypeInput.setValue(datatype, alignment);
	        alignment.initAndValidate();
	        return alignment;
    	} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Loading of " + file.getName() + " failed: " + e.getMessage());
    	}
		return null;
	}

	private static BEASTInterface parseBeast1XML(String ID, String xml) throws SAXException, IOException, ParserConfigurationException  {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
		doc.normalize();

		NodeList alignments = doc.getElementsByTagName("alignment");
		Alignment alignment = new Alignment();
		alignment.dataTypeInput.setValue("nucleotide", alignment);

		// parse first alignment
		org.w3c.dom.Node node = alignments.item(0);

		String dataTypeName = node.getAttributes().getNamedItem("dataType").getNodeValue();
		int totalCount = 4;
		if (dataTypeName == null) {
			alignment.dataTypeInput.setValue("integer", alignment);
		} else if (dataTypeName.toLowerCase().equals("dna") || dataTypeName.toLowerCase().equals("nucleotide")) {
			alignment.dataTypeInput.setValue("nucleotide", alignment);
			totalCount = 4;
		} else if (dataTypeName.toLowerCase().equals("aminoacid") || dataTypeName.toLowerCase().equals("protein")) {
			alignment.dataTypeInput.setValue("aminoacid", alignment);
			totalCount = 20;
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
				sequence.initByName("totalcount", totalCount, "taxon", taxon, "value", data);
				sequence.setID("seq_" + taxon);
				alignment.sequenceInput.setValue(sequence, alignment);

			}
		}
		alignment.setID(ID);
		alignment.initAndValidate();
		return alignment;
	} // parseBeast1XML


	static BEASTInterface getAlignment(BEASTInterface beastObject) throws IllegalArgumentException, IllegalAccessException {
		if (beastObject instanceof Alignment) {
			return beastObject;
		}
		for (BEASTInterface beastObject2 : beastObject.listActiveBEASTObjects()) {
			beastObject2 = getAlignment(beastObject2);
			if (beastObject2 != null) {
				return beastObject2;
			}
		}
		return null;
	}

}
