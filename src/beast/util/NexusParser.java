package beast.util;

import beast.core.util.Log;
import beast.evolution.alignment.*;
import beast.evolution.datatype.DataType;
import beast.evolution.datatype.StandardData;
import beast.evolution.datatype.UserDataType;
import beast.evolution.tree.TraitSet;
import beast.evolution.tree.Tree;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * parses nexus file and grabs alignment and calibration from the file *
 */
public class NexusParser {
    /**
     * keep track of nexus file line number, to report when the file does not parse *
     */
    int lineNr;

    /**
     * Beast II objects reconstructed from the file*
     */
    public Alignment m_alignment;
    public List<Alignment> filteredAlignments = new ArrayList<Alignment>();
    public TraitSet traitSet;

    public List<String> taxa;
    public List<Tree> trees;

    static Set<String> g_sequenceIDs;

    public Map<String, String> translationMap = null;

    static {
        g_sequenceIDs = new HashSet<String>();
    }

    public List<TaxonSet> taxonsets = new ArrayList<TaxonSet>();

//    private List<NexusParserListener> listeners = new ArrayList<NexusParserListener>();

//    public void addListener(final NexusParserListener listener) {
//        listeners.add(listener);
//    }

    /**
     * Try to parse BEAST 2 objects from the given file
     *
     * @param file the file to parse.
     */
    public void parseFile(final File file) throws Exception {
        final String fileName = file.getName().replaceAll(".*[\\/\\\\]", "").replaceAll("\\..*", "");

        parseFile(fileName, new FileReader(file));
    }

    /**
     * try to reconstruct Beast II objects from the given reader
     *
     * @param id     a name to give to the parsed results
     * @param reader a reader to parse from
     */
    public void parseFile(final String id, final Reader reader) throws Exception {
        lineNr = 0;
        final BufferedReader fin;
        if (reader instanceof BufferedReader) {
            fin = (BufferedReader) reader;
        } else {
            fin = new BufferedReader(reader);
        }
        try {
            while (fin.ready()) {
                final String sStr = nextLine(fin);
                if (sStr == null) {
                    return;
                }
                final String sLower = sStr.toLowerCase();
                if (sLower.matches("^\\s*begin\\s+data;\\s*$") || sLower.matches("^\\s*begin\\s+characters;\\s*$")) {
                    m_alignment = parseDataBlock(fin);
                    m_alignment.setID(id);
                } else if (sLower.matches("^\\s*begin\\s+calibration;\\s*$")) {
                    traitSet = parseCalibrationsBlock(fin);
                } else if (sLower.matches("^\\s*begin\\s+assumptions;\\s*$") ||
                        sLower.matches("^\\s*begin\\s+sets;\\s*$") ||
                        sLower.matches("^\\s*begin\\s+mrbayes;\\s*$")) {
                    parseAssumptionsBlock(fin);
                } else if (sLower.matches("^\\s*begin\\s+taxa;\\s*$")) {
                    parseTaxaBlock(fin);
                } else if (sLower.matches("^\\s*begin\\s+trees;\\s*$")) {
                    parseTreesBlock(fin);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Around line " + lineNr + "\n" + e.getMessage());
        }
    } // parseFile

    private void parseTreesBlock(final BufferedReader fin) throws Exception {
        trees = new ArrayList<Tree>();
        // read to first non-empty line within trees block
        String sStr = fin.readLine().trim();
        while (sStr.equals("")) {
            sStr = fin.readLine().trim();
        }

        int origin = -1;

        // if first non-empty line is "translate" then parse translate block
        if (sStr.toLowerCase().contains("translate")) {
            translationMap = parseTranslateBlock(fin);
            origin = getIndexedTranslationMapOrigin(translationMap);
            if (origin != -1) {
                taxa = getIndexedTranslationMap(translationMap, origin);
            }
        }

        // read trees
        while (sStr != null) {
            if (sStr.toLowerCase().startsWith("tree ")) {
                final int i = sStr.indexOf('(');
                if (i > 0) {
                    sStr = sStr.substring(i);
                }
                TreeParser treeParser;

                if (origin != -1) {
                    treeParser = new TreeParser(taxa, sStr, origin, false);
                } else {
                    try {
                        treeParser = new TreeParser(taxa, sStr, 0, false);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        treeParser = new TreeParser(taxa, sStr, 1, false);
                    }
                }
//                catch (NullPointerException e) {
//                    treeParser = new TreeParser(m_taxa, sStr, 1);
//                }

//                for (final NexusParserListener listener : listeners) {
//                    listener.treeParsed(trees.size(), treeParser);
//                }

                if (translationMap != null) treeParser.translateLeafIds(translationMap);

                trees.add(treeParser);

//				Node tree = treeParser.getRoot();
//				tree.sort();
//				tree.labelInternalNodes(nNrOfLabels);
            }
            sStr = fin.readLine();
            if (sStr != null) sStr = sStr.trim();
        }
    }

    private List<String> getIndexedTranslationMap(final Map<String, String> translationMap, final int origin) {

        System.out.println("translation map size = " + translationMap.size());

        final String[] taxa = new String[translationMap.size()];

        for (final String key : translationMap.keySet()) {
            taxa[Integer.parseInt(key) - origin] = translationMap.get(key);
        }
        return Arrays.asList(taxa);
    }

    /**
     * @param translationMap
     * @return minimum key value if keys are a contiguous set of integers starting from zero or one, -1 otherwise
     */
    private int getIndexedTranslationMapOrigin(final Map<String, String> translationMap) {

        final SortedSet<Integer> indices = new TreeSet<Integer>();

        int count = 0;
        for (final String key : translationMap.keySet()) {
            final int index = Integer.parseInt(key);
            indices.add(index);
            count += 1;
        }
        if ((indices.last() - indices.first() == count - 1) && (indices.first() == 0 || indices.first() == 1)) {
            return indices.first();
        }
        return -1;
    }

    /**
     * @param reader a reader
     * @return a map of taxa translations, keys are generally integer node number starting from 1
     *         whereas values are generally descriptive strings.
     * @throws IOException
     */
    private Map<String, String> parseTranslateBlock(final BufferedReader reader) throws IOException {

        final Map<String, String> translationMap = new HashMap<String, String>();

        String line = reader.readLine();
        final StringBuilder translateBlock = new StringBuilder();
        while (line != null && !line.trim().toLowerCase().equals(";")) {
            translateBlock.append(line.trim());
            line = reader.readLine();
        }
        final String[] taxaTranslations = translateBlock.toString().split(",");
        for (final String taxaTranslation : taxaTranslations) {
            final String[] translation = taxaTranslation.split("[\t ]+");
            if (translation.length == 2) {
                translationMap.put(translation[0], translation[1]);
//                System.out.println(translation[0] + " -> " + translation[1]);
            } else {
                System.err.println("Ignoring translation:" + Arrays.toString(translation));
            }
        }
        return translationMap;
    }

    private void parseTaxaBlock(final BufferedReader fin) throws Exception {
        taxa = new ArrayList<String>();
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
                    	String [] sStrs = sStr.split("\\s+");
                    	for (int i = 0; i < sStrs.length; i++) {
                        	String taxon = sStrs[i];
                            if (taxon.charAt(0) == '\'' || taxon.charAt(0) == '\"') {
                            	while (i < sStrs.length && taxon.charAt(0) != taxon.charAt(taxon.length() - 1)) {
                            		i++;
                            		if (i == sStrs.length) {
                            			throw new Exception("Unclosed quote starting with " + taxon);
                            		}
                            		taxon += " " + sStrs[i];
                            	}
                            	taxon = taxon.substring(1, taxon.length() - 1);
                            }
                            taxa.add(taxon);
                    	}
                    }
                } while (!sStr.toLowerCase().equals("end"));
            }
        } while (!sStr.toLowerCase().equals("end"));
        if (nTaxaExpected >= 0 && taxa.size() != nTaxaExpected) {
            throw new Exception("Number of taxa (" + taxa.size() + ") is not equal to 'dimension' " +
            		"field (" + nTaxaExpected + ") specified in 'taxa' block");
        }
    }

    /**
     * parse calibrations block and create TraitSet *
     */
    TraitSet parseCalibrationsBlock(final BufferedReader fin) throws Exception {
        final TraitSet traitSet = new TraitSet();
        traitSet.traitNameInput.setValue("date", traitSet);
        String sStr;
        do {
            sStr = nextLine(fin);
            if (sStr.toLowerCase().contains("options")) {
                String sScale = getAttValue("scale", sStr);
                if (sScale.endsWith("s")) {
                    sScale = sScale.substring(0, sScale.length() - 1);
                }
                traitSet.unitsInput.setValue(sScale, traitSet);
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
        final String[] sStrs = sText.split(",");
        sText = "";
        for (final String sStr2 : sStrs) {
            final String[] sParts = sStr2.split(":");
            final String sDate = sParts[0].replaceAll(".*=\\s*", "");
            final String[] sTaxa = sParts[1].split("\\s+");
            for (final String sTaxon : sTaxa) {
                if (!sTaxon.matches("^\\s*$")) {
                    sText += sTaxon + "=" + sDate + ",\n";
                }
            }
        }
        sText = sText.substring(0, sText.length() - 2);
        traitSet.traitsInput.setValue(sText, traitSet);
        final TaxonSet taxa = new TaxonSet();
        taxa.initByName("alignment", m_alignment);
        traitSet.taxaInput.setValue(taxa, traitSet);

        traitSet.initAndValidate();
        return traitSet;
    } // parseCalibrations


    /**
     * parse data block and create Alignment *
     */
    public Alignment parseDataBlock(final BufferedReader fin) throws Exception {

        final Alignment alignment = new Alignment();

        String sStr;
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

                final String sChar = getAttValue("nchar", sStr);
                if (sChar == null) {
                    throw new Exception("nchar attribute expected (e.g. 'dimensions char=123') expected, not " + sStr);
                }
                nChar = Integer.parseInt(sChar);
                final String sTaxa = getAttValue("ntax", sStr);
                if (sTaxa != null) {
                    nTaxa = Integer.parseInt(sTaxa);
                }
            } else if (sStr.toLowerCase().contains("format")) {
                while (sStr.indexOf(';') < 0) {
                    sStr += nextLine(fin);
                }
                sStr = sStr.replace(";", " ");

                //format datatype=dna interleave=no gap=-;
                final String sDataType = getAttValue("datatype", sStr);
                final String sSymbols = getAttValue("symbols", sStr);
                if (sDataType == null) {
                    System.out.println("Warning: expected datatype (e.g. something like 'format datatype=dna;') not '" + sStr + "' Assuming integer dataType");
                    alignment.dataTypeInput.setValue("integer", alignment);
                    if (sSymbols != null && (sSymbols.equals("01") || sSymbols.equals("012"))) {
                        nTotalCount = sSymbols.length();
                    }
                } else if (sDataType.toLowerCase().equals("rna") || sDataType.toLowerCase().equals("dna") || sDataType.toLowerCase().equals("nucleotide")) {
                    alignment.dataTypeInput.setValue("nucleotide", alignment);
                    nTotalCount = 4;
                } else if (sDataType.toLowerCase().equals("aminoacid") || sDataType.toLowerCase().equals("protein")) {
                    alignment.dataTypeInput.setValue("aminoacid", alignment);
                    nTotalCount = 20;
                } else if (sDataType.toLowerCase().equals("standard")) {
                    if (sSymbols == null || sSymbols.equals("01")) {
                        alignment.dataTypeInput.setValue("binary", alignment);
                        nTotalCount = 2;
                    }  else {
                        alignment.dataTypeInput.setValue("standard", alignment);
                        nTotalCount = sSymbols.length();
                    }
                } else {
                    alignment.dataTypeInput.setValue("integer", alignment);
                    if (sSymbols != null && (sSymbols.equals("01") || sSymbols.equals("012"))) {
                        nTotalCount = sSymbols.length();
                    }
                }
                final String sMissingChar = getAttValue("missing", sStr);
                if (sMissingChar != null) {
                    sMissing = sMissingChar;
                }
                final String sGapChar = getAttValue("gap", sStr);
                if (sGapChar != null) {
                    sGap = sGapChar;
                }
                sMatchChar = getAttValue("matchchar", sStr);
            }
        } while (!sStr.trim().toLowerCase().startsWith("matrix") && !sStr.toLowerCase().contains("charstatelabels"));

        if (alignment.dataTypeInput.get().equals("standard")) {
        	StandardData type = new StandardData();
        	type.initAndValidate();
            alignment.setInputValue("userDataType", type);
        }

        //reading CHATSTATELABELS block
        if (sStr.toLowerCase().contains("charstatelabels")) {
            if (!alignment.dataTypeInput.get().equals("standard")) {
                new Exception("If CHATSTATELABELS block is specified then DATATYPE has to be Standard");
            }
            StandardData standardDataType = (StandardData)alignment.userDataTypeInput.get();
            ArrayList<UserDataType> charDescriptions = new ArrayList<>();
            int maxNumberOfStates =0;
            while (true) {
                sStr = nextLine(fin);
                if (sStr.contains(";")) {
                    break;
                }
                String[] sStrSplit = sStr.split("/");
                ArrayList<String> states = new ArrayList<>();

                if (sStrSplit.length < 2) {
                    charDescriptions.add(new UserDataType(sStrSplit[0], states));
                    continue;
                }

                String stateStr = sStrSplit[1];

                final int WAITING=0, WORD=1, PHRASE_IN_QUOTES=2;
                int mode =WAITING; //0 waiting for non-space letter, 1 reading a word; 2 reading a phrase in quotes
                int begin =0, end;

                for (int i=0; i< stateStr.length(); i++) {
                    switch (mode) {
                        case WAITING:
                            while (stateStr.charAt(i) == ' ') {
                                i++;
                            }
                            mode = stateStr.charAt(i) == '\'' ? PHRASE_IN_QUOTES : WORD;
                            begin = i;
                            break;
                        case WORD:
                            end = stateStr.indexOf(" ", begin) != -1 ? stateStr.indexOf(" ", begin) : stateStr.indexOf(",", begin);
                            states.add(stateStr.substring(begin, end));
                            i=end;
                            mode = WAITING;
                            break;
                        case PHRASE_IN_QUOTES:
                            end = begin;
                            do {
                                end = stateStr.indexOf("'", end+2);
                            } while (stateStr.charAt(end+1) == '\'' || end == -1);
                            if (end == -1) {
                                System.out.println("Incorrect description in charstatelabels. Single quote found in line ");
                            }
                            end++;
                            states.add(stateStr.substring(begin, end));
                            i=end;
                            mode=WAITING;
                            break;
                        default:
                            break;
                    }
                }
                //TODO make sStrSplit[0] look nicer (remove whitespaces and may be numbers at the beginning)
                charDescriptions.add(new UserDataType(sStrSplit[0], states));
                maxNumberOfStates = Math.max(maxNumberOfStates, states.size());
            }
            standardDataType.setInputValue("charstatelabels", charDescriptions);
            standardDataType.setInputValue("nrOfStates", maxNumberOfStates);
            standardDataType.initAndValidate();
            for (UserDataType dataType : standardDataType.charStateLabelsInput.get()) {
            	dataType.initAndValidate();
            }
            //TODO figure out what should be the maxNrOfStates:
            // It coulb be the largest number occurred in the sequences
            // or the largest number of states in the charstatelabels.
            //The former can be less than the latter if some taxa were removed.
        }

        //skipping before MATRIX block
        while (!sStr.toLowerCase().contains(("matrix"))) {
            sStr = nextLine(fin);
        }

        // read character data
        // Use string builder for efficiency
        final Map<String, StringBuilder> seqMap = new HashMap<String, StringBuilder>();
        final List<String> sTaxa = new ArrayList<String>();
        String sPrevTaxon = null;
        int seqLen = 0;
        while (true) {
            sStr = nextLine(fin);
            if (sStr.contains(";")) {
                break;
            }

            int iStart = 0, iEnd;
            final String sTaxon;
            while (Character.isWhitespace(sStr.charAt(iStart))) {
                iStart++;
            }
            if (sStr.charAt(iStart) == '\'' || sStr.charAt(iStart) == '\"') {
                final char c = sStr.charAt(iStart);
                iStart++;
                iEnd = iStart;
                while (sStr.charAt(iEnd) != c) {
                    iEnd++;
                }
                sTaxon = sStr.substring(iStart, iEnd);
                seqLen = 0;
                iEnd++;
            } else {
                iEnd = iStart;
                while (iEnd < sStr.length() && !Character.isWhitespace(sStr.charAt(iEnd))) {
                    iEnd++;
                }
                if (iEnd < sStr.length()) {
                    sTaxon = sStr.substring(iStart, iEnd);
                    seqLen = 0;
                } else if ((sPrevTaxon == null || seqLen == nChar) && iEnd == sStr.length()) {
                    sTaxon = sStr.substring(iStart, iEnd);
                    seqLen = 0;
                } else {
                    sTaxon = sPrevTaxon;
                    if (sTaxon == null) {
                        throw new Exception("Could not recognise taxon");
                    }
                    iEnd = iStart;
                }
            }
            sPrevTaxon = sTaxon;
            final String sData = sStr.substring(iEnd);
            for (int k = 0; k < sData.length(); k++) {
            	if (!Character.isWhitespace(sData.charAt(k))) {
            		seqLen++;
            	}
            }
            // Do this once outside loop- save on multiple regex compilations
            //sData = sData.replaceAll("\\s", "");

//			String [] sStrs = sStr.split("\\s+");
//			String sTaxon = sStrs[0];
//			for (int k = 1; k < sStrs.length - 1; k++) {
//				sTaxon += sStrs[k];
//			}
//			sTaxon = sTaxon.replaceAll("'", "");
//			System.err.println(sTaxon);
//			String sData = sStrs[sStrs.length - 1];

            if (seqMap.containsKey(sTaxon)) {
                seqMap.put(sTaxon, seqMap.get(sTaxon).append(sData));
            } else {
                seqMap.put(sTaxon, new StringBuilder(sData));
                sTaxa.add(sTaxon);
            }
        }
        if (nTaxa > 0 && sTaxa.size() > nTaxa) {
            throw new Exception("Wrong number of taxa. Perhaps a typo in one of the taxa: " + sTaxa);
        }

        HashSet<String> sortedAmbiguities = new HashSet<>();
        for (final String sTaxon : sTaxa) {
            final StringBuilder bsData = seqMap.get(sTaxon);
            String sData = bsData.toString().replaceAll("\\s", "");
            seqMap.put(sTaxon, new StringBuilder(sData));

            //collect all ambiguities in the sequence
            List<String> ambiguities = new ArrayList<String>();
            Matcher m = Pattern.compile("\\{(.*?)\\}").matcher(sData);
            while (m.find()) {
                int mLength = m.group().length();
                ambiguities.add(m.group().substring(1, mLength-1));
            }

            //sort elements of ambiguity sets
            String sData_without_ambiguities = sData.replaceAll("\\{(.*?)\\}", "?");
            for (String amb : ambiguities) {
                List<Integer> ambInt = new ArrayList<>();
                for (int i=0; i<amb.length(); i++) {
                	char c = amb.charAt(i);
                	if (c >= '0' && c <= '9') {
                		ambInt.add(Integer.parseInt(amb.charAt(i) + ""));
                	} else {
                		// ignore
                		if (sData != sData_without_ambiguities) {
                			Log.warning.println("Ambiguity found in " + sTaxon + " that is treated as missing value");
                		}
                		sData = sData_without_ambiguities; 
                	}
                }
                Collections.sort(ambInt);
                String ambStr = "";
                for (int i=0; i<ambInt.size(); i++) {
                    ambStr += Integer.toString(ambInt.get(i));
                }
                sortedAmbiguities.add(ambStr);
            }

            //check the length of the sequence (treat ambiguity sets as single characters)
            if (sData_without_ambiguities.length() != nChar) {
                throw new Exception(sStr + "\nExpected sequence of length " + nChar + " instead of " + sData.length() + " for taxon " + sTaxon);
            }

            // map to standard missing and gap chars
            sData = sData.replace(sMissing.charAt(0), DataType.MISSING_CHAR);
            sData = sData.replace(sGap.charAt(0), DataType.GAP_CHAR);

            // resolve matching char, if any
            if (sMatchChar != null && sData.contains(sMatchChar)) {
                final char cMatchChar = sMatchChar.charAt(0);
                final String sBaseData = seqMap.get(sTaxa.get(0)).toString();
                for (int i = 0; i < sData.length(); i++) {
                    if (sData.charAt(i) == cMatchChar) {
                        final char cReplaceChar = sBaseData.charAt(i);
                        sData = sData.substring(0, i) + cReplaceChar + (i + 1 < sData.length() ? sData.substring(i + 1) : "");
                    }
                }
            }

//            alignment.setInputValue(sTaxon, sData);
            final Sequence sequence = new Sequence();
            sequence.init(nTotalCount, sTaxon, sData);
            sequence.setID(generateSequenceID(sTaxon));
            alignment.sequenceInput.setValue(sequence, alignment);
        }


        if (alignment.dataTypeInput.get().equals("standard")) {
            //convert sortedAmbiguities to a whitespace separated string of ambiguities
            String ambiguitiesStr = "";
            for (String sAmb: sortedAmbiguities) {
                ambiguitiesStr += sAmb + " ";
            }
            if (ambiguitiesStr.length() > 0) {
            	ambiguitiesStr = ambiguitiesStr.substring(0, ambiguitiesStr.length()-1);
            }
            alignment.userDataTypeInput.get().initByName("ambiguities", ambiguitiesStr);
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
     * 
     * begin assumptions;
     * wtset MySoapWeights (VECTOR) = 13 13 13 50 50 88 8
     * end;
     * 
     */
    void parseAssumptionsBlock(final BufferedReader fin) throws Exception {
        String sStr;
        do {
            sStr = nextLine(fin);
            if (sStr.toLowerCase().matches("\\s*charset\\s.*")) {
                sStr = sStr.replaceAll("^\\s+", "");
                sStr = sStr.replaceAll("\\s*-\\s*", "-");
                sStr = sStr.replaceAll("\\s*\\\\\\s*", "\\\\");
                sStr = sStr.replaceAll("\\s*;", "");
                final String[] sStrs = sStr.trim().split("\\s+");
                final String sID = sStrs[1];
                String sRange = "";
                for (int i = 3; i < sStrs.length; i++) {
                    sRange += sStrs[i] + " ";
                }
                sRange = sRange.trim().replace(' ', ',');
                final FilteredAlignment alignment = new FilteredAlignment();
                alignment.setID(sID);
                alignment.alignmentInput.setValue(m_alignment, alignment);
                alignment.filterInput.setValue(sRange, alignment);
                alignment.initAndValidate();
                filteredAlignments.add(alignment);
            } else if (sStr.toLowerCase().matches("\\s*wtset\\s.*")) {
            	String [] strs = sStr.split("=");
            	if (strs.length > 1) {
            		sStr = strs[strs.length - 1].trim();
            		strs = sStr.split("\\s+");
            		int [] weights = new int[strs.length];
            		for (int i = 0; i< strs.length; i++) {
            			weights[i] = Integer.parseInt(strs[i]);
            		}
            		if (m_alignment != null) {
            			if (weights.length != m_alignment.getSiteCount()) {
            				throw new RuntimeException("Number of weights (" + weights.length+ ") " +
            						"does not match number of sites in alignment(" + m_alignment.getSiteCount()+ ")");
            			}
            			StringBuilder weightStr = new StringBuilder();
            			for (String str : strs) {
            				weightStr.append(str);
            				weightStr.append(',');
            			}
            			weightStr.delete(weightStr.length() - 1, weightStr.length());
            			m_alignment.siteWeightsInput.setValue(weightStr.toString(), m_alignment);
            			m_alignment.initAndValidate();
            		} else {
            			Log.warning.println("WTSET was specified before alignment. WTSET is ignored.");
            		}
            	}
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
    void parseSetsBlock(final BufferedReader fin) throws Exception {
        String sStr;
        do {
            sStr = nextLine(fin);
            if (sStr.toLowerCase().matches("\\s*taxset\\s.*")) {
                sStr = sStr.replaceAll("^\\s+", "");
                sStr = sStr.replaceAll(";", "");
                final String[] sStrs = sStr.split("\\s+");
                String sID = sStrs[1];
                sID = sID.replaceAll("'\"", "");
                final TaxonSet set = new TaxonSet();
                set.setID(sID);
                for (int i = 3; i < sStrs.length; i++) {
                    sID = sStrs[i];
                    sID = sID.replaceAll("'\"", "");
                    final Taxon taxon = new Taxon();
                    taxon.setID(sID);
                    set.taxonsetInput.setValue(taxon, set);
                }
                taxonsets.add(set);
            }
        } while (!sStr.toLowerCase().contains("end;"));
    }

    public static String generateSequenceID(final String sTaxon) {
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
    String readLine(final BufferedReader fin) throws Exception {
        if (!fin.ready()) {
            return null;
        }
        lineNr++;
        return fin.readLine();
    }

    /**
     * read next line from nexus file that is not a comment and not empty *
     */
    String nextLine(final BufferedReader fin) throws Exception {
        String sStr = readLine(fin);
        if (sStr == null) {
            return null;
        }
        if (sStr.contains("[")) {
            final int iStart = sStr.indexOf('[');
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
    String getAttValue(final String sAttribute, final String sStr) {
        final Pattern pattern = Pattern.compile(".*" + sAttribute + "\\s*=\\s*([^\\s;]+).*");
        final Matcher matcher = pattern.matcher(sStr.toLowerCase());
        if (!matcher.find()) {
            return null;
        }
        String sAtt = matcher.group(1);
        if (sAtt.startsWith("\"") && sAtt.endsWith("\"")) {
            final int iStart = matcher.start(1);
            sAtt = sStr.substring(iStart + 1, sStr.indexOf('"', iStart + 1));
        }
        return sAtt;
    }

    public static void main(final String[] args) {
        try {
            final NexusParser parser = new NexusParser();
            parser.parseFile(new File(args[0]));
            if (parser.taxa != null) {
                System.out.println(parser.taxa.size() + " taxa");
                System.out.println(Arrays.toString(parser.taxa.toArray(new String[parser.taxa.size()])));
            }
            if (parser.trees != null) {
                System.out.println(parser.trees.size() + " trees");
            }
            if (parser.m_alignment != null) {
                final String sXML = new XMLProducer().toXML(parser.m_alignment);
                System.out.println(sXML);
            }
            if (parser.traitSet != null) {
                final String sXML = new XMLProducer().toXML(parser.traitSet);
                System.out.println(sXML);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    } // main

} // class NexusParser
