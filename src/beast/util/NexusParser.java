package beast.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.datatype.DataType;
import beast.evolution.tree.TraitSet;


//TODO: handle taxon sets
//begin sets;
//taxset junk1 = P._articulata P._gracilis P._fimbriata;
//taxset junk2 = P._robusta;
//end;




/** parses nexus file and grabs alignment and calibration from the file **/
public class NexusParser {
	/** keep track of nexus file line number, to report when the file does not parse **/
	int m_nLineNr;

	/**Beast II objects reconstructed from the file**/
	public Alignment m_alignment;
	public TraitSet m_traitSet;
	static Set<String> g_sequenceIDs;
	static { g_sequenceIDs = new HashSet<String>();}
	
	/**  
	 * try to reconstruct Beast II objects from the nexus file with given file name   
	 * **/
	public void parseFile(String sFileName) throws Exception {
		m_nLineNr = 0;
		BufferedReader fin = new BufferedReader(new FileReader(sFileName));
		try {
			while (fin.ready()) {
				String sStr = nextLine(fin);
				if (sStr == null) {
					return;
				}
				if (sStr.toLowerCase().matches("^\\s*begin\\s+data;\\s*$") || sStr.toLowerCase().matches("^\\s*begin\\s+characters;\\s*$")) {
					m_alignment = parseDataBlock(fin);
					sFileName = sFileName.replaceAll(".*[\\/]", "");
					sFileName = sFileName.replaceAll("\\..*", "");
					m_alignment.setID(sFileName);
				} else if (sStr.toLowerCase().matches("^\\s*begin\\s+calibration;\\s*$")) {
					m_traitSet = parseCalibrationsBlock(fin);
				}
			}			
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Around line " + m_nLineNr + "\n" + e.getMessage());
		}
	} // parseFile
	
	/** parse calibrations block and create TraitSet **/
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
		};
		String [] sStrs = sText.split(",");
		sText = "";
		for (String sStr2 : sStrs) {
			String [] sParts = sStr2.split(":");
			String sDate = sParts[0].replaceAll(".*=\\s*","");
			String [] sTaxa = sParts[1].split("\\s+");
			for (String sTaxon : sTaxa) {
				if (!sTaxon.matches("^\\s*$")) {
					sText += sTaxon + "=" + sDate + ",\n";
				}
			}
		}
		sText = sText.substring(0, sText.length()-2);
		traitSet.m_traits.setValue(sText, traitSet);
		traitSet.m_taxa.setValue(m_alignment, traitSet);
		
		traitSet.initAndValidate();
		return traitSet;
	} // parseCalibrations
	
	/** parse data block and create Alignment **/
	public Alignment parseDataBlock(BufferedReader fin) throws Exception {

		Alignment alignment = new Alignment();
		
		String sStr = null;
		int nTaxa = -1;
		int nChar = -1;
		int nTotalCount = 4;
		String sMissing="?";
		String sGap="-";
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
				
				String sChar=getAttValue("nchar", sStr);
				if (sChar == null) {
					throw new Exception ("nchar attribute expected (e.g. 'dimensions char=123') expected, not "+ sStr);
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
					System.out.println("Warning: expected datatype (e.g. something like 'format datatype=dna;') not '" + sStr +"' Assuming integer dataType"); 
					alignment.m_sDataType.setValue("integer", alignment);
				} else 
				if (sDataType.toLowerCase().equals("dna") || sDataType.toLowerCase().equals("nucleotide")) {
					alignment.m_sDataType.setValue("nucleotide", alignment);
					nTotalCount = 4;
				} else 
				if (sDataType.toLowerCase().equals("aminoacid") || sDataType.toLowerCase().equals("protein")) {
					alignment.m_sDataType.setValue("aminoacid", alignment);
					nTotalCount = 20;
				} else 
				if (sDataType.toLowerCase().equals("standard") && sSymbols.equals("01")) {
					alignment.m_sDataType.setValue("binary", alignment);
					nTotalCount = 2;
				} else {
					alignment.m_sDataType.setValue("integer", alignment);
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
			String [] sStrs = sStr.split("\\s+");
			String sTaxon = sStrs[sStrs.length - 2];
			sTaxon = sTaxon.replaceAll("'", "");
			System.err.println(sTaxon);
			String sData = sStrs[sStrs.length - 1];
			
			if (seqMap.containsKey(sTaxon)) {
				seqMap.put(sTaxon, seqMap.get(sTaxon) + sData);
			} else {
				seqMap.put(sTaxon, sData);
				sTaxa.add(sTaxon);
			}
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
				    	sData = sData.substring(0, i) + cReplaceChar + (i+1<sData.length() ? sData.substring(i+1) : "" );
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
	
	private String generateSequenceID(String sTaxon) {
		String sID = "seq_" + sTaxon;
		int i = 0;
		while (g_sequenceIDs.contains(sID+(i>0?i:""))) {
			i++;
		}
		sID = sID + (i>0?i:"");
		g_sequenceIDs.add(sID);
		return sID;
	}

	/** read line from nexus file **/
	String readLine(BufferedReader fin) throws Exception {
		if (!fin.ready()) {
			return null;
		}
		m_nLineNr++;
		return fin.readLine();
	}

	/** read next line from nexus file that is not a comment and not empty **/
	String nextLine(BufferedReader fin) throws Exception {
		String sStr = readLine(fin);
		if (sStr == null) {
			return null;
		}
		if (sStr.contains("[")) {
			int iStart = sStr.indexOf('[');
			int iEnd = sStr.indexOf(']', iStart);
			while (iEnd < 0){
				sStr += readLine(fin);
				iEnd = sStr.indexOf(']', iStart);
			}
			sStr = sStr.substring(0, iStart) + sStr.substring(iEnd+1);
			if (sStr.matches("^\\s*$")) {
				return nextLine(fin);
			}
		}
		if (sStr.matches("^\\s*$")) {
			return nextLine(fin);
		}
		return sStr;
	}

	/** return attribute value as a string **/
	String getAttValue(String sAttribute, String sStr) {
		Pattern pattern = Pattern.compile(".*" + sAttribute +"\\s*=\\s*([^\\s;]+).*");
		Matcher matcher = pattern.matcher(sStr.toLowerCase());
		if (!matcher.find()) {
			return null; 
		}
		String sAtt = matcher.group(1);
		if (sAtt.startsWith("\"") && sAtt.endsWith("\"")) {
			int iStart = matcher.start(1);
			sAtt = sStr.substring(iStart + 1, sStr.indexOf('"', iStart+1));
		}
		return sAtt;
	}
	
	public static void main(String [] args) {
		try {
			NexusParser parser = new NexusParser();
			parser.parseFile(args[0]);
			String sXML = new XMLProducer().toXML(parser.m_alignment);
			System.out.println(sXML);
			if (parser.m_traitSet != null) {
				sXML = new XMLProducer().toXML(parser.m_traitSet);
				System.out.println(sXML);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	} // main
	
} // class NexusParser
