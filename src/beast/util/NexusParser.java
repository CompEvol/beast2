package beast.util;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import beast.evolution.alignment.*;
import beast.evolution.datatype.DataType;
import beast.evolution.tree.TraitSet;
import beast.evolution.tree.Tree;


//TODO: handle taxon sets
//begin sets;
//taxset junk1 = P._articulata P._gracilis P._fimbriata;
//taxset junk2 = P._robusta;
//end;


/**
 * parses nexus file and grabs alignment and calibration from the file *
 */
public class NexusParser {
    /**
     * keep track of nexus file line number, to report when the file does not parse *
     */
    int m_nLineNr;

    /**
     * Beast II objects reconstructed from the file*
     */
    public Alignment m_alignment;
    public List<Alignment> m_filteredAlignments = new ArrayList<Alignment>();
    public TraitSet m_traitSet;
    
    public List<String> m_taxa;
    public List<Tree> m_trees;
    
    static Set<String> g_sequenceIDs;

    static {
        g_sequenceIDs = new HashSet<String>();
    }

    public List<TaxonSet> m_taxonsets = new ArrayList<TaxonSet>();

    /**
     * try to reconstruct Beast II objects from the nexus file with given file name
     * *
     */
    public void parseFile(File file) throws Exception {
        m_nLineNr = 0;
        BufferedReader fin = new BufferedReader(new FileReader(file));
        try {
            while (fin.ready()) {
                String sStr = nextLine(fin);
                if (sStr == null) {
                    return;
                }
                if (sStr.toLowerCase().matches("^\\s*begin\\s+data;\\s*$") || sStr.toLowerCase().matches("^\\s*begin\\s+characters;\\s*$")) {
                    m_alignment = parseDataBlock(fin);
                    String fileName = file.getName().replaceAll(".*[\\/\\\\]", "").replaceAll("\\..*", "");
                    m_alignment.setID(fileName);
                } else if (sStr.toLowerCase().matches("^\\s*begin\\s+calibration;\\s*$")) {
                    m_traitSet = parseCalibrationsBlock(fin);
                } else if (sStr.toLowerCase().matches("^\\s*begin\\s+assumptions;\\s*$")) {
                    parseAssumptionsBlock(fin);
                } else if (sStr.toLowerCase().matches("^\\s*begin\\s+taxa;\\s*$")) {
                    parseTaxaBlock(fin);
                } else if (sStr.toLowerCase().matches("^\\s*begin\\s+trees;\\s*$")) {
                    parseTreesBlock(fin);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Around line " + m_nLineNr + "\n" + e.getMessage());
        }
    } // parseFile

    private void parseTreesBlock(BufferedReader fin) throws Exception {
    	m_trees = new ArrayList<Tree>();
		// parse Newick tree file
		String sStr = fin.readLine();
		// skip translate block
		while (fin.ready() && sStr.toLowerCase().indexOf("translate") < 0) {
			sStr = fin.readLine();
		}
		while (fin.ready() && !sStr.toLowerCase().trim().equals(";")) {
			sStr = fin.readLine();
		}
		
		// read trees
		while (fin.ready()) {
			sStr = fin.readLine();
			sStr = sStr.trim();
			if (sStr.toLowerCase().startsWith("tree ")) {
				int i = sStr.indexOf('(');
				if (i > 0) {
					sStr = sStr.substring(i);
				}
				try {
					TreeParser treeParser = new TreeParser(m_taxa, sStr, 0);
					m_trees.add(treeParser);
				} catch (ArrayIndexOutOfBoundsException e) {
					TreeParser treeParser = new TreeParser(m_taxa, sStr, 1);
					m_trees.add(treeParser);
				}

//				Node tree = treeParser.getRoot();
//				tree.sort();
//				tree.labelInternalNodes(nNrOfLabels);
			}
		}
	}

	private void parseTaxaBlock(BufferedReader fin) throws Exception {
		m_taxa = new ArrayList<String>();
		int nTaxaExpected = -1;
        String sStr;
        do {
            sStr = nextLine(fin);
            if (sStr.toLowerCase().matches("\\s*dimensions\\s.*")) {
                sStr = sStr.substring(sStr.toLowerCase().indexOf("ntax=") + 5);
                sStr = sStr.replaceAll(";", "");
                nTaxaExpected = Integer.parseInt(sStr.trim());
            } else if (sStr.toLowerCase().trim().equals("taxlabels")) {
                do {
                    sStr = nextLine(fin);
                    sStr = sStr.replaceAll(";", "");
                    sStr = sStr.trim();
                    if (sStr.length() > 0 && !sStr.toLowerCase().equals("end")) {
                    	for (String taxon : sStr.split("\\s+")) {
                    		m_taxa.add(taxon);
                    	}
                    }
                } while (!sStr.toLowerCase().equals("end"));
            }
        } while (!sStr.toLowerCase().equals("end"));
        if (nTaxaExpected >= 0 && m_taxa.size() != nTaxaExpected) {
        	throw new Exception("Taxa block: # taxa is not equal to dimension");
        }
	}

	/**
     * parse calibrations block and create TraitSet *
     */
    TraitSet parseCalibrationsBlock(BufferedReader fin) throws Exception {
        TraitSet traitSet = new TraitSet();
        traitSet.m_sTraitName.setValue("date", traitSet);
        String sStr = null;
        do {
            sStr = nextLine(fin);
            if (sStr.toLowerCase().contains("options")) {
                String sScale = getAttValue("scale", sStr);
                if (sScale.endsWith("s")) {
                    sScale = sScale.substring(0, sScale.length() - 1);
                }
                traitSet.m_sUnits.setValue(sScale, traitSet);
            }
        } while (sStr.toLowerCase().contains("tipcalibration"));

        String sText = "";
        while (true) {
            sStr = nextLine(fin);
            if (sStr.contains(";")) {
                break;
            }
            sText += sStr;
        }
        ;
        String[] sStrs = sText.split(",");
        sText = "";
        for (String sStr2 : sStrs) {
            String[] sParts = sStr2.split(":");
            String sDate = sParts[0].replaceAll(".*=\\s*", "");
            String[] sTaxa = sParts[1].split("\\s+");
            for (String sTaxon : sTaxa) {
                if (!sTaxon.matches("^\\s*$")) {
                    sText += sTaxon + "=" + sDate + ",\n";
                }
            }
        }
        sText = sText.substring(0, sText.length() - 2);
        traitSet.m_traits.setValue(sText, traitSet);
        TaxonSet taxa = new TaxonSet();
        taxa.initByName("alignment", m_alignment);
        traitSet.m_taxa.setValue(taxa, traitSet);

        traitSet.initAndValidate();
        return traitSet;
    } // parseCalibrations


    /**
     * parse data block and create Alignment *
     */
    public Alignment parseDataBlock(BufferedReader fin) throws Exception {

        Alignment alignment = new Alignment();

        String sStr = null;
        int nTaxa = -1;
        int nChar = -1;
        int nTotalCount = 4;
        String sMissing = "?";
        String sGap = "-";
        // indicates character matches the one in the first sequence
        String sMatchChar = null;
        do {
            sStr = nextLine(fin);

            //dimensions ntax=12 nchar=898;
            if (sStr.toLowerCase().contains("dimensions")) {
                while (sStr.indexOf(';') < 0) {
                    sStr += nextLine(fin);
                }
                sStr = sStr.replace(";", " ");

                String sChar = getAttValue("nchar", sStr);
                if (sChar == null) {
                    throw new Exception("nchar attribute expected (e.g. 'dimensions char=123') expected, not " + sStr);
                }
                nChar = Integer.parseInt(sChar);
                String sTaxa = getAttValue("ntax", sStr);
                if (sTaxa != null) {
                    nTaxa = Integer.parseInt(sTaxa);
                }
            } else if (sStr.toLowerCase().contains("format")) {
                while (sStr.indexOf(';') < 0) {
                    sStr += nextLine(fin);
                }
                sStr = sStr.replace(";", " ");

                //format datatype=dna interleave=no gap=-;
                String sDataType = getAttValue("datatype", sStr);
                String sSymbols = getAttValue("symbols", sStr);
                if (sDataType == null) {
                    System.out.println("Warning: expected datatype (e.g. something like 'format datatype=dna;') not '" + sStr + "' Assuming integer dataType");
                    alignment.m_sDataType.setValue("integer", alignment);
                } else if (sDataType.toLowerCase().equals("dna") || sDataType.toLowerCase().equals("nucleotide")) {
                    alignment.m_sDataType.setValue("nucleotide", alignment);
                    nTotalCount = 4;
                } else if (sDataType.toLowerCase().equals("aminoacid") || sDataType.toLowerCase().equals("protein")) {
                    alignment.m_sDataType.setValue("aminoacid", alignment);
                    nTotalCount = 20;
                } else if (sDataType.toLowerCase().equals("standard") && sSymbols.equals("01")) {
                    alignment.m_sDataType.setValue("binary", alignment);
                    nTotalCount = 2;
                } else {
                    alignment.m_sDataType.setValue("integer", alignment);
                    if (sSymbols.equals("01") || sSymbols.equals("012")) {
                        nTotalCount = sSymbols.length();
                    }
                }
                String sMissingChar = getAttValue("missing", sStr);
                if (sMissingChar != null) {
                    sMissing = sMissingChar;
                }
                String sGapChar = getAttValue("gap", sStr);
                if (sGapChar != null) {
                    sGap = sGapChar;
                }
                sMatchChar = getAttValue("matchchar", sStr);
            }
        } while (!sStr.toLowerCase().contains("matrix"));

        // read character data
        Map<String, String> seqMap = new HashMap<String, String>();
        List<String> sTaxa = new ArrayList<String>();
        while (true) {
            sStr = nextLine(fin);
            if (sStr.contains(";")) {
                break;
            }

            int iStart = 0, iEnd = 0;
            String sTaxon;
            while (Character.isWhitespace(sStr.charAt(iStart))) {
                iStart++;
            }
            if (sStr.charAt(iStart) == '\'' || sStr.charAt(iStart) == '\"') {
                char c = sStr.charAt(iStart);
                iStart++;
                iEnd = iStart;
                while (sStr.charAt(iEnd) != c) {
                    iEnd++;
                }
                sTaxon = sStr.substring(iStart, iEnd);
                iEnd++;
            } else {
                iEnd = iStart;
                while (!Character.isWhitespace(sStr.charAt(iEnd))) {
                    iEnd++;
                }
                sTaxon = sStr.substring(iStart, iEnd);
            }
            String sData = sStr.substring(iEnd);
            sData = sData.replaceAll("\\s", "");

//			String [] sStrs = sStr.split("\\s+");
//			String sTaxon = sStrs[0];
//			for (int k = 1; k < sStrs.length - 1; k++) {
//				sTaxon += sStrs[k];
//			}
//			sTaxon = sTaxon.replaceAll("'", "");
//			System.err.println(sTaxon);
//			String sData = sStrs[sStrs.length - 1];

            if (seqMap.containsKey(sTaxon)) {
                seqMap.put(sTaxon, seqMap.get(sTaxon) + sData);
            } else {
                seqMap.put(sTaxon, sData);
                sTaxa.add(sTaxon);
            }
        }
        if (nTaxa > 0 && sTaxa.size() > nTaxa) {
            throw new Exception("Wrong number of taxa. Perhaps a typo in one of the taxa: " + sTaxa);
        }
        for (String sTaxon : sTaxa) {
            String sData = seqMap.get(sTaxon);

            if (sData.length() != nChar) {
                throw new Exception(sStr + "\nExpected sequence of length " + nChar + " instead of " + sData.length() + " for taxon " + sTaxon);
            }
            // map to standard missing and gap chars
            sData = sData.replace(sMissing.charAt(0), DataType.MISSING_CHAR);
            sData = sData.replace(sGap.charAt(0), DataType.GAP_CHAR);

            // resolve matching char, if any
            if (sMatchChar != null && sData.contains(sMatchChar)) {
                char cMatchChar = sMatchChar.charAt(0);
                String sBaseData = seqMap.get(sTaxa.get(0));
                for (int i = 0; i < sData.length(); i++) {
                    if (sData.charAt(i) == cMatchChar) {
                        char cReplaceChar = sBaseData.charAt(i);
                        sData = sData.substring(0, i) + cReplaceChar + (i + 1 < sData.length() ? sData.substring(i + 1) : "");
                    }
                }
            }

            Sequence sequence = new Sequence();
            sequence.init(nTotalCount, sTaxon, sData);
            sequence.setID(generateSequenceID(sTaxon));
            alignment.m_pSequences.setValue(sequence, alignment);
        }
        alignment.initAndValidate();
        if (nTaxa > 0 && nTaxa != alignment.getNrTaxa()) {
            throw new Exception("dimensions block says there are " + nTaxa + " taxa, but there were " + alignment.getNrTaxa() + " taxa found");
        }
        return alignment;
    } // parseDataBlock


    /**
     * parse assumptions block
     * begin assumptions;
     * charset firsthalf = 1-449;
     * charset secondhalf = 450-898;
     * charset third = 1-457\3 662-896\3;
     * end;
     */
    void parseAssumptionsBlock(BufferedReader fin) throws Exception {
        String sStr;
        do {
            sStr = nextLine(fin);
            if (sStr.toLowerCase().matches("\\s*charset\\s.*")) {
                sStr = sStr.replaceAll("^\\s+", "");
                sStr = sStr.replaceAll(";", "");
                String[] sStrs = sStr.split("\\s+");
                String sID = sStrs[1];
                String sRange = "";
                for (int i =  3; i < sStrs.length; i++) {
                	sRange += sStrs[i] + " ";
                }
                sRange = sRange.trim().replace(' ', ',');
                FilteredAlignment alignment = new FilteredAlignment();
                alignment.setID(sID);
                alignment.m_alignmentInput.setValue(m_alignment, alignment);
                alignment.m_sFilterInput.setValue(sRange, alignment);
                alignment.initAndValidate();
                m_filteredAlignments.add(alignment);
            }
        } while (!sStr.toLowerCase().contains("end;"));
    }

    /**
     * parse sets block
     * BEGIN Sets;
     * TAXSET 'con' = 'con_SL_Gert2' 'con_SL_Tran6' 'con_SL_Tran7' 'con_SL_Gert6';
     * TAXSET 'spa' = 'spa_138a_Cerb' 'spa_JB_Eyre1' 'spa_JB_Eyre2';
     * END; [Sets]
     */
    void parseSetsBlock(BufferedReader fin) throws Exception {
        String sStr;
        do {
            sStr = nextLine(fin);
            if (sStr.toLowerCase().matches("\\s*taxset\\s.*")) {
                sStr = sStr.replaceAll("^\\s+", "");
                sStr = sStr.replaceAll(";", "");
                String[] sStrs = sStr.split("\\s+");
                String sID = sStrs[1];
                sID = sID.replaceAll("'\"", "");
                TaxonSet set = new TaxonSet();
                set.setID(sID);
                for (int i = 3; i < sStrs.length; i++) {
                    sID = sStrs[i];
                    sID = sID.replaceAll("'\"", "");
                    Taxon taxon = new Taxon();
                    taxon.setID(sID);
                    set.m_taxonset.setValue(taxon, set);
                }
                m_taxonsets.add(set);
            }
        } while (!sStr.toLowerCase().contains("end;"));
    }

    private String generateSequenceID(String sTaxon) {
        String sID = "seq_" + sTaxon;
        int i = 0;
        while (g_sequenceIDs.contains(sID + (i > 0 ? i : ""))) {
            i++;
        }
        sID = sID + (i > 0 ? i : "");
        g_sequenceIDs.add(sID);
        return sID;
    }

    /**
     * read line from nexus file *
     */
    String readLine(BufferedReader fin) throws Exception {
        if (!fin.ready()) {
            return null;
        }
        m_nLineNr++;
        return fin.readLine();
    }

    /**
     * read next line from nexus file that is not a comment and not empty *
     */
    String nextLine(BufferedReader fin) throws Exception {
        String sStr = readLine(fin);
        if (sStr == null) {
            return null;
        }
        if (sStr.contains("[")) {
            int iStart = sStr.indexOf('[');
            int iEnd = sStr.indexOf(']', iStart);
            while (iEnd < 0) {
                sStr += readLine(fin);
                iEnd = sStr.indexOf(']', iStart);
            }
            sStr = sStr.substring(0, iStart) + sStr.substring(iEnd + 1);
            if (sStr.matches("^\\s*$")) {
                return nextLine(fin);
            }
        }
        if (sStr.matches("^\\s*$")) {
            return nextLine(fin);
        }
        return sStr;
    }

    /**
     * return attribute value as a string *
     */
    String getAttValue(String sAttribute, String sStr) {
        Pattern pattern = Pattern.compile(".*" + sAttribute + "\\s*=\\s*([^\\s;]+).*");
        Matcher matcher = pattern.matcher(sStr.toLowerCase());
        if (!matcher.find()) {
            return null;
        }
        String sAtt = matcher.group(1);
        if (sAtt.startsWith("\"") && sAtt.endsWith("\"")) {
            int iStart = matcher.start(1);
            sAtt = sStr.substring(iStart + 1, sStr.indexOf('"', iStart + 1));
        }
        return sAtt;
    }

    public static void main(String[] args) {
        try {
            NexusParser parser = new NexusParser();
            parser.parseFile(new File(args[0]));
            if (parser.m_taxa != null) {
            	System.out.println(parser.m_taxa.size() + " taxa");
            	System.out.println(Arrays.toString(parser.m_taxa.toArray(new String[0])));
            }
            if (parser.m_trees != null) {
            	System.out.println(parser.m_trees.size() + " trees");
            }
            if (parser.m_alignment!= null) {
            	String sXML = new XMLProducer().toXML(parser.m_alignment);
            	System.out.println(sXML);
            }
            if (parser.m_traitSet != null) {
            	String sXML = new XMLProducer().toXML(parser.m_traitSet);
                System.out.println(sXML);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    } // main

} // class NexusParser
