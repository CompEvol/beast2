package beast.util;

import beast.core.BEASTInterface;
import beast.core.parameter.RealParameter;
import beast.core.util.Log;
import beast.evolution.alignment.*;
import beast.evolution.datatype.DataType;
import beast.evolution.datatype.StandardData;
import beast.evolution.datatype.UserDataType;
import beast.evolution.tree.TraitSet;
import beast.evolution.tree.Tree;
import beast.math.distributions.*;

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
    protected int lineNr;

    /**
     * Beast II objects reconstructed from the file*
     */
    public Alignment m_alignment;
    public List<Alignment> filteredAlignments = new ArrayList<>();
    public TraitSet traitSet;
    public List<MRCAPrior> calibrations;

    public List<String> taxa;
    List<Taxon> taxonList = new ArrayList<>();
    public List<Tree> trees;

    static Set<String> g_sequenceIDs;

    public Map<String, String> translationMap = null;

    static {
        g_sequenceIDs = new HashSet<>();
    }

    public List<TaxonSet> taxonsets = new ArrayList<>();

    protected List<NexusParserListener> listeners = new ArrayList<>();

    /**
     * Adds a listener for client classes that want to monitor progress of the parsing.
     * @param listener
     */
    public void addListener(final NexusParserListener listener) {
        listeners.add(listener);
    }

    /**
     * Try to parse BEAST 2 objects from the given file
     *
     * @param file the file to parse.
     */
    public void parseFile(final File file) throws IOException {
        final String fileName = file.getName().replaceAll(".*[\\/\\\\]", "").replaceAll("\\..*", "");

        parseFile(fileName, new FileReader(file));
    }

    /**
     * try to reconstruct Beast II objects from the given reader
     *
     * @param id     a name to give to the parsed results
     * @param reader a reader to parse from
     * TODO: RRB: throws IOException now instead of just Exception. 
     * java.text.ParseException seems more appropriate, but requires keeping track of the position in the file, which is non-trivial 
     */
    public void parseFile(final String id, final Reader reader) throws IOException {
        lineNr = 0;
        final BufferedReader fin;
        if (reader instanceof BufferedReader) {
            fin = (BufferedReader) reader;
        } else {
            fin = new BufferedReader(reader);
        }
        try {
            while (fin.ready()) {
                final String str = nextLine(fin);
                if (str == null) {
                    processSets();
                    return;
                }
                final String lower = str.toLowerCase();
                if (lower.matches("^\\s*begin\\s+data;\\s*$") || lower.matches("^\\s*begin\\s+characters;\\s*$")) {
                    m_alignment = parseDataBlock(fin);
                    m_alignment.setID(id);
                } else if (lower.matches("^\\s*begin\\s+calibration;\\s*$")) {
                    traitSet = parseCalibrationsBlock(fin);
                } else if (lower.matches("^\\s*begin\\s+assumptions;\\s*$") ||
                        lower.matches("^\\s*begin\\s+sets;\\s*$") ||
                        lower.matches("^\\s*begin\\s+mrbayes;\\s*$")) {
                    parseAssumptionsBlock(fin);
                } else if (lower.matches("^\\s*begin\\s+taxa;\\s*$")) {
                    parseTaxaBlock(fin);
                } else if (lower.matches("^\\s*begin\\s+trees;\\s*$")) {
                    parseTreesBlock(fin);
                }
            }
            processSets();

        } catch (TreeParser.TreeParsingException e) {
        	e.printStackTrace();
            int errorLine = lineNr + 1;

            if (e.getLineNum() != null)
                errorLine += e.getLineNum()-1;

            String errorMsg = "Encountered error interpreting the Newick string found around line " +
                    errorLine + " of the input file.";

            if (e.getCharacterNum() != null)
                errorMsg += "\nThe parser reports that the error occurred at character " + (e.getCharacterNum()+1)
                        + " of the Newick string on this line.";

            errorMsg += "\nThe parser gives the following clue:\n" + e.getMessage();

            throw new IOException(errorMsg);

        } catch (Exception e) {
        	e.printStackTrace();
            throw new IOException("Around line " + (lineNr+1) + "\n" + e.getMessage());
        }
    } // parseFile

    /**
     * Class representing a single command in a nexus file.
     *
     * Command may be terminated by either a ";" or an EOF. All whitespace
     * is converted to single spaces.
     *
     * Currently only used by parseTreesBlock and parseTaxaBlock.
     */
    class NexusCommand {
        String command;
        String arguments;

        Map<String,String> kvArgs;
        List<String> argList;

        boolean isCommand(String commandName) {
            return command.equals(commandName.toLowerCase());
        }

        boolean isEndOfBlock() {
            return command.equals("end");
        }

        NexusCommand(String commandString) {
            commandString = commandString.trim().replaceAll("\\s+", " ");

            command = commandString.split(" ")[0].toLowerCase();

            try {
                arguments = commandString.substring(command.length() + 1);
            } catch (IndexOutOfBoundsException ex) {
                arguments = "";
            }
        }

        /**
         * Used by argument processing methods to identify the end of a
         * nexus comment, begining at index idx.  Allows for nested comments.
         *
         * @param idx index at start of comment
         * @return first index past end of comment.
         * @throws IOException if the comment or nested string is not terminated.
         */
        private int findCommentEnd(int idx) throws IOException {

            idx += 1;

            while (idx < arguments.length()) {

                switch (arguments.charAt(idx)) {
                    case ']':
                        return idx+1;

                    case '"':
                    case '\'':
                        idx = findStringEnd(idx, arguments.charAt(idx));
                        break;

                    default:
                        idx += 1;
                        break;
                }
            }

            throw new IOException("Unterminated comment.");
        }

        /**
         * Used by argument processing methods to identify the end of a
         * string, begining at index idx with delmiter delim.
         *
         * @param idx index at start of string
         * @param delim terminating index
         * @return first index past end of string.
         * @throws IOException if the string is not terminated.
         */
        private int findStringEnd(int idx, char delim) throws IOException {

            idx += 1;

            while (idx < arguments.length()) {

                if (arguments.charAt(idx) == delim)
                    return idx+1;

                idx += 1;
            }

            throw new IOException("Unterminated string.");

        }

        /**
         * Used by argument processing methods to identify the end of a
         * chunk of whitespace.
         *
         * @param idx start of (potential) whitespace block.
         * @return first non-whitespace character found.
         */
        private int findWhitespaceEnd(int idx) {
            while (idx < arguments.length() && Character.isWhitespace(arguments.charAt(idx)))
                idx += 1;

            return idx;
        }

        /**
         * Used by argument processing methods to identify the end of a token
         * in a manner that allows for comments and strings.
         *
         * @param idx start index
         * @return index of first character past the end of the identified token
         * @throws IOException if unterminated comments or strings are found.
         */
        private int findTokenEnd(int idx) throws IOException {

            boolean done = false;
            while (!done && idx < arguments.length()) {

                switch(arguments.charAt(idx)) {
                    case '[':
                        idx = findCommentEnd(idx);
                        break;

                    case '"':
                    case '\'':
                        idx = findStringEnd(idx, arguments.charAt(idx));
                        break;

                    default:
                        if (Character.isWhitespace(arguments.charAt(idx))
                                || arguments.charAt(idx) == '=')
                            done = true;
                        else
                            idx += 1;
                        break;
                }
            }

            return idx;
        }

        /**
         * Attempt to interpret arguments as key value pairs.
         * Arguments matching this pattern are added to a map, which
         * is then returned.
         *
         * @return map of key strings to value strings.
         * @throws IOException if unterminated comments/strings are found.
         */
        Map<String,String> getKeyValueArgs() throws IOException {
            if (kvArgs != null)
                return kvArgs;

            kvArgs = new HashMap<>();

            int idx=0;
            while (idx < arguments.length()) {

                idx = findWhitespaceEnd(idx);

                int keyStart = idx;
                idx = findTokenEnd(idx);
                int keyEnd = idx;

                idx = findWhitespaceEnd(idx);

                if (idx>= arguments.length() || arguments.charAt(idx) != '=')
                    continue;

                idx += 1;

                idx = findWhitespaceEnd(idx);

                int valStart = idx;
                idx = findTokenEnd(idx);
                int valEnd = idx;

                kvArgs.put(arguments.substring(keyStart, keyEnd).trim(),
                        arguments.substring(valStart, valEnd).trim());
            }

            return kvArgs;
        }

        /**
         * Attempt to interpret arguments string as a whitespace-delimited set of
         * individual arguments. Arguments matching this pattern are added to a
         * list, which is then returned.
         *
         * @return list of argument strings in this command.
         * @throws IOException if unterminated comments/strings are found.
         */
        List<String> getArgList() throws IOException {
            if (argList != null)
                return argList;

            argList = new ArrayList<>();

            int idx=0;
            while (idx < arguments.length()) {
                idx = findWhitespaceEnd(idx);

                int keyStart = idx;
                idx = findTokenEnd(idx);
                int keyEnd = idx;

                idx = findWhitespaceEnd(idx);

                if (idx >= arguments.length() || arguments.charAt(idx) != '=') {
                    argList.add(arguments.substring(keyStart, keyEnd).trim());
                    continue;
                }

                idx += 1;

                idx = findWhitespaceEnd(idx);

                int valStart = idx;
                idx = findTokenEnd(idx);
                int valEnd = idx;

                argList.add(arguments.substring(keyStart, valEnd).trim());
            }

            return argList;
        }

        @Override
        public String toString() {
            return "Command: " + command + ", Args: " + arguments;
        }
    }

    /**
     * Used to advance reader past nexus strings.
     *
     * @param fin intput file reader
     * @param builder string builder where characters read are to be appended
     * @param stringDelim string delimiter
     *
     * @throws IOException on unterminated string
     */
    private void readNexusString(BufferedReader fin, StringBuilder builder, char stringDelim) throws IOException {
        boolean stringTerminated = false;
        while(true) {
            int nextVal = fin.read();
            if (nextVal<0)
                break;

            char nextChar = (char)nextVal;

            builder.append(nextChar);

            if (nextChar == stringDelim) {
                stringTerminated = true;
                break;
            }

            if (nextChar == '\n')
                lineNr += 1;
        }

        if (!stringTerminated)
            throw new IOException("Unterminated string.");
    }

    /**
     * Used to advance reader past nexus comments. Comments may themselves
     * contain strings.
     *
     * @param fin intput file reader
     * @param builder string builder where characters read are to be appended
     *
     * @throws IOException on unterminated comment.
     */
    private void readNexusComment(BufferedReader fin, StringBuilder builder) throws IOException {
        boolean commentTerminated = false;
        while(true) {
            int nextVal = fin.read();
            if (nextVal<0)
                break;


            char nextChar = (char)nextVal;
            builder.append(nextChar);

            if (nextChar == ']') {
                commentTerminated = true;
                break;
            }

            if (nextChar == '"' || nextChar == '\'')
                readNexusString(fin, builder, nextChar);

            if (nextChar == '\n')
                lineNr += 1;
        }

        if (!commentTerminated)
            throw new IOException("Unterminated comment.");
    }

    /**
     * Get next nexus command, if available.
     *
     * @param fin nexus file reader
     * @return nexus command, or null if none available.
     * @throws IOException if error reading from file
     */
    NexusCommand readNextCommand(BufferedReader fin) throws IOException {
        StringBuilder commandBuilder = new StringBuilder();

        while(true) {
            int nextVal = fin.read();
            if (nextVal<0)
                break;

            char nextChar = (char)nextVal;
            if (nextChar == ';')
                break;

            commandBuilder.append(nextChar);

            switch (nextChar) {
                case '[':
                    readNexusComment(fin, commandBuilder);
                    break;

                case '"':
                case '\'':
                    readNexusString(fin, commandBuilder, nextChar);
                    break;

                case '\n':
                    lineNr += 1;
                    break;

                default:
                    break;
            }
        }

        if (commandBuilder.toString().isEmpty())
            return null;
        else
            return new NexusCommand(commandBuilder.toString());
    }

    /**
     * Remove nexus comments from a given string.
     *
     * @param string input string
     * @return string with nexus comments removed.
     */
    String stripNexusComments(String string) {
        return string.replaceAll("\\[[^]]*]","");
    }

    protected void parseTreesBlock(final BufferedReader fin) throws IOException {
        trees = new ArrayList<>();
        // read to first command within trees block
        NexusCommand nextCommand = readNextCommand(fin);

        int origin = -1;

        // if first non-empty line is "translate" then parse translate block
        if (nextCommand.isCommand("translate")) {
            translationMap = parseTranslateCommand(nextCommand.arguments);
            origin = getIndexedTranslationMapOrigin(translationMap);
            if (origin != -1) {
                taxa = getIndexedTranslationMap(translationMap, origin);
            }
        }

        // read trees
        while (nextCommand != null && !nextCommand.isEndOfBlock()) {
            if (nextCommand.isCommand("tree")) {
                String treeString = nextCommand.arguments;
                final int i = treeString.indexOf('(');
                if (i > 0) {
                    treeString = treeString.substring(i);
                }
                TreeParser treeParser;

                if (origin != -1) {
                    treeParser = new TreeParser(taxa, treeString, origin, false);
                } else {
                    try {
                        treeParser = new TreeParser(taxa, treeString, 0, false);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        treeParser = new TreeParser(taxa, treeString, 1, false);
                    }
                }

                // this needs to go after translation map or listeners have an incomplete tree!
                for (final NexusParserListener listener : listeners) {
                    listener.treeParsed(trees.size(), treeParser);
                }

                // this must come after listener or trees.size() gives the wrong index to treeParsed
                trees.add(treeParser);

            }
            nextCommand = readNextCommand(fin);
        }
    }

    protected List<String> getIndexedTranslationMap(final Map<String, String> translationMap, final int origin) {

        Log.warning.println("translation map size = " + translationMap.size());

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
    protected int getIndexedTranslationMapOrigin(final Map<String, String> translationMap) {

        final SortedSet<Integer> indices = new TreeSet<>();

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
     * @param translateArgs string containing arguments of the translate command
     * @return a map of taxa translations, keys are generally integer node number starting from 1
     *         whereas values are generally descriptive strings.
     * @throws IOException
     */
    protected Map<String, String> parseTranslateCommand(String translateArgs) throws IOException {

        final Map<String, String> translationMap = new HashMap<>();

        final String[] taxaTranslations = translateArgs.toString().split(",");
        for (final String taxaTranslation : taxaTranslations) {
            final String[] translation = taxaTranslation.trim().split("[\t ]+");
            if (translation.length == 2) {
                translationMap.put(translation[0], translation[1]);
//                Log.info.println(translation[0] + " -> " + translation[1]);
            } else {
                Log.warning.println("Ignoring translation:" + Arrays.toString(translation));
            }
        }
        return translationMap;
    }

    /**
     * Parse taxa block and add to taxa and taxonList.
     */
    protected void parseTaxaBlock(final BufferedReader fin) throws IOException {
        taxa = new ArrayList<>();
        int expectedTaxonCount = -1;
        NexusCommand nextCommand;
        do {
            nextCommand = readNextCommand(fin);
            if (nextCommand.isCommand("dimensions")) {
                if (nextCommand.getKeyValueArgs().get("ntax") != null)
                    expectedTaxonCount = Integer.parseInt(nextCommand.getKeyValueArgs().get("ntax"));
            } else if (nextCommand.isCommand("taxlabels")) {

                List<String> labels = nextCommand.getArgList();

                for (String taxonString : labels) {
                    taxonString = stripNexusComments(taxonString).trim();

                    if (taxonString.isEmpty())
                        continue;

                    if (taxonString.charAt(0) == '\'' || taxonString.charAt(0) == '\"')
                        taxonString = taxonString.substring(1, taxonString.length() - 1).trim();

                    if (taxonString.isEmpty())
                        continue;

                    if (!taxa.contains(taxonString))
                        taxa.add(taxonString);

                    if (!taxonListContains(taxonString))
                        taxonList.add(new Taxon(taxonString));

                }
            }
        } while (!nextCommand.isEndOfBlock());
        if (expectedTaxonCount >= 0 && taxa.size() != expectedTaxonCount) {
            throw new IOException("Number of taxa (" + taxa.size() + ") is not equal to 'dimension' " +
            		"field (" + expectedTaxonCount + ") specified in 'taxa' block");
        }
    }

    /**
     * parse calibrations block and create TraitSet *
     */
    protected TraitSet parseCalibrationsBlock(final BufferedReader fin) throws IOException {
        final TraitSet traitSet = new TraitSet();
        traitSet.setID("traitsetDate");
        traitSet.traitNameInput.setValue("date", traitSet);
        String str;
        do {
            str = nextLine(fin);
            if (str.toLowerCase().contains("options")) {
                String scale = getAttValue("scale", str);
                if (scale.endsWith("s")) {
                    scale = scale.substring(0, scale.length() - 1);
                }
                traitSet.unitsInput.setValue(scale, traitSet);
            }
        } while (str.toLowerCase().contains("tipcalibration"));

        String text = "";
        while (true) {
            str = nextLine(fin);
            if (str.contains(";")) {
                break;
            }
            text += str;
        }
        final String[] strs = text.split(",");
        text = "";
        for (final String str2 : strs) {
            final String[] parts = str2.split(":");
            final String date = parts[0].replaceAll(".*=\\s*", "");
            final String[] taxa = parts[1].split("\\s+");
            for (final String taxon : taxa) {
                if (!taxon.matches("^\\s*$")) {
                    text += taxon + "=" + date + ",\n";
                }
            }
        }
        text = text.substring(0, text.length() - 2);
        traitSet.traitsInput.setValue(text, traitSet);
        final TaxonSet taxa = new TaxonSet();
        taxa.initByName("alignment", m_alignment);
        traitSet.taxaInput.setValue(taxa, traitSet);

        traitSet.initAndValidate();
        return traitSet;
    } // parseCalibrations


    /**
     * parse data block and create Alignment *
     */
    public Alignment parseDataBlock(final BufferedReader fin) throws IOException {

        final Alignment alignment = new Alignment();

        String str;
        int taxonCount = -1;
        int charCount = -1;
        int totalCount = 4;
        String missing = "?";
        String gap = "-";
        // indicates character matches the one in the first sequence
        String matchChar = null;
        do {
            str = nextLine(fin);

            //dimensions ntax=12 nchar=898;
            if (str.toLowerCase().contains("dimensions")) {
            	str = getNextDataBlock(str, fin);

                final String character = getAttValue("nchar", str);
                if (character == null) {
                    throw new IOException("nchar attribute expected (e.g. 'dimensions char=123') expected, not " + str);
                }
                charCount = Integer.parseInt(character);
                final String taxa = getAttValue("ntax", str);
                if (taxa != null) {
                    taxonCount = Integer.parseInt(taxa);
                }
            } else if (str.toLowerCase().contains("format")) {
            	str = getNextDataBlock(str, fin);

                //format datatype=dna interleave=no gap=-;
                final String dataTypeName = getAttValue("datatype", str);
                final String symbols;
                if (getAttValue("symbols", str) == null) {
                    symbols = getAttValue("symbols", str);
                } else {
                    symbols = getAttValue("symbols", str).replaceAll("\\s", "");
                }
                if (dataTypeName == null) {
                    Log.warning.println("Warning: expected datatype (e.g. something like 'format datatype=dna;') not '" + str + "' Assuming integer dataType");
                    alignment.dataTypeInput.setValue("integer", alignment);
                    if (symbols != null && (symbols.equals("01") || symbols.equals("012"))) {
                        totalCount = symbols.length();
                    }
                } else if (dataTypeName.toLowerCase().equals("rna") || dataTypeName.toLowerCase().equals("dna") || dataTypeName.toLowerCase().equals("nucleotide")) {
                    alignment.dataTypeInput.setValue("nucleotide", alignment);
                    totalCount = 4;
                } else if (dataTypeName.toLowerCase().equals("aminoacid") || dataTypeName.toLowerCase().equals("protein")) {
                    alignment.dataTypeInput.setValue("aminoacid", alignment);
                    totalCount = 20;
                } else if (dataTypeName.toLowerCase().equals("standard")) {
                    alignment.dataTypeInput.setValue("standard", alignment);
                    totalCount = symbols.length();
//                    if (symbols == null || symbols.equals("01")) {
//                        alignment.dataTypeInput.setValue("binary", alignment);
//                        totalCount = 2;
//                    }  else {
//                        alignment.dataTypeInput.setValue("standard", alignment);
//                        totalCount = symbols.length();
//                    }
                } else if (dataTypeName.toLowerCase().equals("binary")) {
                    alignment.dataTypeInput.setValue("binary", alignment);
                    totalCount = 2;
                } else {
                    alignment.dataTypeInput.setValue("integer", alignment);
                    if (symbols != null && (symbols.equals("01") || symbols.equals("012"))) {
                        totalCount = symbols.length();
                    }
                }
                final String missingChar = getAttValue("missing", str);
                if (missingChar != null) {
                    missing = missingChar;
                }
                final String gapChar = getAttValue("gap", str);
                if (gapChar != null) {
                    gap = gapChar;
                }
                matchChar = getAttValue("matchchar", str);
            }
        } while (!str.trim().toLowerCase().startsWith("matrix") && !str.toLowerCase().contains("charstatelabels"));

        if (alignment.dataTypeInput.get().equals("standard")) {
        	StandardData type = new StandardData();
            type.setInputValue("nrOfStates", totalCount);
            //type.setInputValue("symbols", symbols);
        	type.initAndValidate();
            alignment.setInputValue("userDataType", type);
        }

        //reading CHARSTATELABELS block
        if (str.toLowerCase().contains("charstatelabels")) {
            if (!alignment.dataTypeInput.get().equals("standard")) {
                throw new IllegalArgumentException("If CHARSTATELABELS block is specified then DATATYPE has to be Standard");
            }
            StandardData standardDataType = (StandardData)alignment.userDataTypeInput.get();
            int[] maxNumberOfStates = new int[] {0};
            ArrayList<String> tokens = readInCharstatelablesTokens(fin);
            ArrayList<UserDataType> charDescriptions = processCharstatelabelsTokens(tokens, maxNumberOfStates);

//            while (true) {
//                str = nextLine(fin);
//                if (str.contains(";")) {
//                    break;
//                }
//                String[] strSplit = str.split("/");
//                ArrayList<String> states = new ArrayList<>();
//
//                if (strSplit.length < 2) {
//                    charDescriptions.add(new UserDataType(strSplit[0], states));
//                    continue;
//                }
//
//                String stateStr = strSplit[1];
//
//                //add a comma at the end of the string if the last non-whitespace character is not a comma or all the
//                // characters are whitespaces in the string. Also remove whitespaces at the end of the string.
//                for (int i=stateStr.length()-1; i>=0; i--) {
//                    if (!Character.isWhitespace(stateStr.charAt(i))) {
//                        if (stateStr.charAt(i-1) != ',') {
//                            stateStr = stateStr.substring(0, i)+",";
//                            break;
//                        }
//                    }
//                    if (i==0) {
//                        stateStr = stateStr.substring(0, i)+",";
//                    }
//                }
//                if (stateStr.isEmpty()) {
//                    stateStr = stateStr+",";
//                }
//
//                final int WAITING=0, WORD=1, PHRASE_IN_QUOTES=2;
//                int mode =WAITING; //0 waiting for non-space letter, 1 reading a word; 2 reading a phrase in quotes
//                int begin =0, end;
//
//                for (int i=0; i< stateStr.length(); i++) {
//                    switch (mode) {
//                        case WAITING:
//                            while (stateStr.charAt(i) == ' ') {
//                                i++;
//                            }
//                            mode = stateStr.charAt(i) == '\'' ? PHRASE_IN_QUOTES : WORD;
//                            begin = i;
//                            break;
//                        case WORD:
//                            end = stateStr.indexOf(" ", begin) != -1 ? stateStr.indexOf(" ", begin) : stateStr.indexOf(",", begin);
//                            states.add(stateStr.substring(begin, end));
//                            i=end;
//                            mode = WAITING;
//                            break;
//                        case PHRASE_IN_QUOTES:
//                            end = begin;
//                            do {
//                                end = stateStr.indexOf("'", end+2);
//                            } while (stateStr.charAt(end+1) == '\'' || end == -1);
//                            if (end == -1) {
//                                Log.info.println("Incorrect description in charstatelabels. Single quote found in line ");
//                            }
//                            end++;
//                            states.add(stateStr.substring(begin, end));
//                            i=end;
//                            mode=WAITING;
//                            break;
//                        default:
//                            break;
//                    }
//                }
//                // oldTODO make strSplit[0] look nicer (remove whitespaces and may be numbers at the beginning)
//                charDescriptions.add(new UserDataType(strSplit[0], states));
//                maxNumberOfStates = Math.max(maxNumberOfStates, states.size());
//            }
            standardDataType.setInputValue("charstatelabels", charDescriptions);
            standardDataType.setInputValue("nrOfStates", Math.max(maxNumberOfStates[0], totalCount));
            standardDataType.initAndValidate();
            for (UserDataType dataType : standardDataType.charStateLabelsInput.get()) {
            	dataType.initAndValidate();
            }
        }

        //skipping before MATRIX block
        while (!str.toLowerCase().contains(("matrix"))) {
            str = nextLine(fin);
        }

        // read character data
        // Use string builder for efficiency
        final Map<String, StringBuilder> seqMap = new HashMap<>();
        final List<String> taxa = new ArrayList<>();
        String prevTaxon = null;
        int seqLen = 0;
        while (true) {
            str = nextLine(fin);

            int start = 0, end;
            final String taxon;
            while (Character.isWhitespace(str.charAt(start))) {
                start++;
            }
            if (str.charAt(start) == '\'' || str.charAt(start) == '\"') {
                final char c = str.charAt(start);
                start++;
                end = start;
                while (str.charAt(end) != c) {
                    end++;
                }
                taxon = str.substring(start, end).trim();
                seqLen = 0;
                end++;
            } else {
                end = start;
                while (end < str.length() && !Character.isWhitespace(str.charAt(end))) {
                    end++;
                }
                if (end < str.length()) {
                    taxon = str.substring(start, end).trim();
                    seqLen = 0;
                } else if ((prevTaxon == null || seqLen == charCount) && end == str.length()) {
                    taxon = str.substring(start, end).trim();
                    seqLen = 0;
                } else {
                    taxon = prevTaxon;
                    if (taxon == null) {
                        throw new IOException("Could not recognise taxon");
                    }
                    end = start;
                }
            }
            prevTaxon = taxon;
            String data = str.substring(end);
            for (int k = 0; k < data.length(); k++) {
            	if (!Character.isWhitespace(data.charAt(k))) {
            		seqLen++;
            	}
            }
            // Do this once outside loop- save on multiple regex compilations
            //data = data.replaceAll("\\s", "");

//			String [] strs = str.split("\\s+");
//			String taxon = strs[0];
//			for (int k = 1; k < strs.length - 1; k++) {
//				taxon += strs[k];
//			}
//			taxon = taxon.replaceAll("'", "");
//			Log.warning.println(taxon);
//			String data = strs[strs.length - 1];

            data = data.replaceAll(";", "");
            if (data.trim().length() > 0) {
	            if (seqMap.containsKey(taxon)) {
	                seqMap.put(taxon, seqMap.get(taxon).append(data));
	            } else {
	                seqMap.put(taxon, new StringBuilder(data));
	                taxa.add(taxon);
	            }
            }
            if (str.contains(";")) {
                break;
            }

        }
        if (taxonCount > 0 && taxa.size() > taxonCount) {
            throw new IOException("Wrong number of taxa. Perhaps a typo in one of the taxa: " + taxa);
        }

        HashSet<String> sortedAmbiguities = new HashSet<>();
        for (final String taxon : taxa) {
        	if (!taxonListContains(taxon)) {
        		taxonList.add(new Taxon(taxon));
        	}
            final StringBuilder bsData = seqMap.get(taxon);
            String data = bsData.toString().replaceAll("\\s", "");
            seqMap.put(taxon, new StringBuilder(data));

            //collect all ambiguities in the sequence
            List<String> ambiguities = new ArrayList<>();
            Matcher m = Pattern.compile("\\{(.*?)\\}").matcher(data);
            while (m.find()) {
                int mLength = m.group().length();
                ambiguities.add(m.group().substring(1, mLength-1));
            }

            //sort elements of ambiguity sets
            String data_without_ambiguities = data.replaceAll("\\{(.*?)\\}", "?");
            for (String amb : ambiguities) {
                List<Integer> ambInt = new ArrayList<>();
                for (int i=0; i<amb.length(); i++) {
                	char c = amb.charAt(i);
                	if (c >= '0' && c <= '9') {
                		ambInt.add(Integer.parseInt(amb.charAt(i) + ""));
                	} else {
                		// ignore
                		if (data != data_without_ambiguities) {
                			Log.warning.println("Ambiguity found in " + taxon + " that is treated as missing value");
                		}
                		data = data_without_ambiguities; 
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
            if (data_without_ambiguities.length() != charCount) {
                throw new IOException(str + "\nExpected sequence of length " + charCount + " instead of " + data.length() + " for taxon " + taxon);
            }

            // map to standard missing and gap chars
            data = data.replace(missing.charAt(0), DataType.MISSING_CHAR);
            data = data.replace(gap.charAt(0), DataType.GAP_CHAR);

            // resolve matching char, if any
            if (matchChar != null && data.contains(matchChar)) {
                final char cMatchChar = matchChar.charAt(0);
                final String baseData = seqMap.get(taxa.get(0)).toString();
                for (int i = 0; i < data.length(); i++) {
                    if (data.charAt(i) == cMatchChar) {
                        final char cReplaceChar = baseData.charAt(i);
                        data = data.substring(0, i) + cReplaceChar + (i + 1 < data.length() ? data.substring(i + 1) : "");
                    }
                }
            }

            // Using Alignment as Map gives problems when producing XML: 
            // Sequence names are used as attribute names, producing very readable XML
            // However, since attribute names cannot start with a number or contain
            // special characters (like ":" or "]") but sequence names do contain them
            // on occasion, it is more robust to create a Sequence object for each 
            // sequence where the taxon name is stored as an XML attribute values
            // that do not have the attribute name restrictions.
//            if (alignment.dataTypeInput.get().equals("nucleotide") || 
//            	alignment.dataTypeInput.get().equals("binary")  ||
//            	alignment.dataTypeInput.get().equals("aminoacid") ) {
//            	alignment.setInputValue(taxon, data);
//            } else {
	            final Sequence sequence = new Sequence();
	            sequence.init(totalCount, taxon, data);
	            sequence.setID(generateSequenceID(taxon));
	            alignment.sequenceInput.setValue(sequence, alignment);
//            }
        }


        if (alignment.dataTypeInput.get().equals("standard")) {
            //convert sortedAmbiguities to a whitespace separated string of ambiguities
            String ambiguitiesStr = "";
            for (String amb: sortedAmbiguities) {
                ambiguitiesStr += amb + " ";
            }
            if (ambiguitiesStr.length() > 0) {
            	ambiguitiesStr = ambiguitiesStr.substring(0, ambiguitiesStr.length()-1);
            }
            alignment.userDataTypeInput.get().initByName("ambiguities", ambiguitiesStr);
        }

        alignment.initAndValidate();
        if (taxonCount > 0 && taxonCount != alignment.getTaxonCount()) {
            throw new IOException("dimensions block says there are " + taxonCount + " taxa, but there were " + alignment.getTaxonCount() + " taxa found");
        }
        return alignment;
    } // parseDataBlock

    private boolean taxonListContains(String taxon) {
    	for (Taxon t : taxonList) {
    		if (t.getID().equals(taxon)) {
    			return true;
    		}
    	}
		return false;
	}

	private String getNextDataBlock(String str, BufferedReader fin) throws IOException {
        while (str.indexOf(';') < 0) {
            str += nextLine(fin);
        }
        str = str.replace(";", " ");

		if (str.toLowerCase().matches(".*matrix.*")) {
			// will only get here when there
			throw new IllegalArgumentException("Malformed nexus file: perhaps a semi-colon is missing before 'matrix'");
		}
		return str;
    }

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
    protected void parseAssumptionsBlock(final BufferedReader fin) throws IOException {
        String str;
        do {
            str = nextLine(fin);
            if (str.toLowerCase().matches("\\s*charset\\s.*")) {
            	// remove text in brackets (as TreeBase files are wont to contain)
                str = str.replaceAll("\\(.*\\)", "");
                // clean up spaces
                str = str.replaceAll("=", " = ");
                str = str.replaceAll("^\\s+", "");
                str = str.replaceAll("\\s*-\\s*", "-");
                str = str.replaceAll("\\s*\\\\\\s*", "\\\\");
                str = str.replaceAll("\\s*;", "");
                // replace "," to " " as BEAST 1 uses ,
                str = str.replaceAll(",\\s+", " ");
                // use white space as delimiter
                final String[] strs = str.trim().split("\\s+");
                final String id = strs[1];
                String rangeString = "";
                for (int i = 3; i < strs.length; i++) {
                    rangeString += strs[i] + " ";
                }
                rangeString = rangeString.trim().replace(' ', ',');
                final FilteredAlignment alignment = new FilteredAlignment();
                alignment.setID(id);
                alignment.alignmentInput.setValue(m_alignment, alignment);
                alignment.filterInput.setValue(rangeString, alignment);
                alignment.initAndValidate();
                filteredAlignments.add(alignment);
            } else if (str.toLowerCase().matches("\\s*wtset\\s.*")) {
            	String [] strs = str.split("=");
            	if (strs.length > 1) {
            		str = strs[strs.length - 1].trim();
            		strs = str.split("\\s+");
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
            			for (String str2 : strs) {
            				weightStr.append(str2);
            				weightStr.append(',');
            			}
            			weightStr.delete(weightStr.length() - 1, weightStr.length());
            			m_alignment.siteWeightsInput.setValue(weightStr.toString(), m_alignment);
            			m_alignment.initAndValidate();
            		} else {
            			Log.warning.println("WTSET was specified before alignment. WTSET is ignored.");
            		}
            	}
            } else if (str.toLowerCase().matches("\\s*taxset\\s.*")) {
            	String [] strs = str.split("=");
            	if (strs.length > 1) {
            		String str0 = strs[0].trim();
            		String [] strs2 = str0.split("\\s+");
            		if (strs2.length != 2) {
            			throw new RuntimeException("expected 'taxset <name> = ...;' but did not get two words before the = sign: " + str);
            		}
            		String taxonSetName = strs2[1];
            		str0 = strs[strs.length - 1].trim();
            		if (!str0.endsWith(";")) {
            			Log.warning.println("expected 'taxset <name> = ...;' semi-colon is missing: " + str + "\n"
            					+ "Taxa from following lines may be missing.");
            		}
            		str0 = str0.replaceAll(";", "");
            		String [] taxonNames = str0.split("\\s+");
            		TaxonSet taxonset = new TaxonSet();
            		for (String taxon : taxonNames) {
            			taxonset.taxonsetInput.get().add(new Taxon(taxon.replaceAll("'\"", "")));
            		}
            		taxonset.setID(taxonSetName.replaceAll("'\"", ""));
            		taxonsets.add(taxonset);
            	}
            } else if (str.toLowerCase().matches("^\\s*calibrate\\s.*")) {
            	// define calibration represented by an MRCAPRior, 
            	// taxon sets need to be specified earlier, but can also be a single taxon
            	// e.g.
            	// begin mrbayes;
            	// calibrate germanic = normal(1000,50)
            	// calibrate hittite = normal(3450,100)
            	// calibrate english = fixed(0)
            	// end;
            	String [] strs = str.split("=");
            	if (strs.length > 1) {
            		String str0 = strs[0].trim();
            		String [] strs2 = str0.split("\\s+");
            		if (strs2.length != 2) {
            			throw new RuntimeException("expected 'calibrate <name> = ...' but did not get two words before the = sign: " + str);
            		}
            		// first, get the taxon
            		String taxonSetName = strs2[1].replaceAll("'\"", "");
            		TaxonSet taxonset = null;
            		for (Taxon t : taxonsets) {
            			if (t.getID().equals(taxonSetName) && t instanceof TaxonSet) {
            				taxonset = (TaxonSet) t;
            			}
            		}
            		if (taxonset == null) {
            			// perhaps it is a singleton
            			for (Taxon t : taxonList) {
            				if (t.getID().equals(taxonSetName)) {
            					taxonset = new TaxonSet();
            					taxonset.setID(t.getID() + ".leaf");
            					taxonset.taxonsetInput.setValue(t, taxonset);
            				}
            			}
            		}
            		if (taxonset == null) {
            			throw new RuntimeException("Could not find taxon/taxonset " + taxonSetName + " in calibration: " + str);
            		}
            		
            		// next get the calibration
            		str0 = strs[strs.length - 1].trim();
            		String [] strs3 = str0.split("[\\(,\\)]");

            		try {
                        MRCAPrior prior = getMRCAPrior(taxonset, strs3);

                        // should set Tree before initialising, but we do not know the tree yet...
                        if (calibrations == null) {
                            calibrations = new ArrayList<>();
                        }
                        calibrations.add(prior);
                    } catch (RuntimeException ex) {
                        throw new RuntimeException(ex.getMessage() + "in calibration: " + str);
                    }
            	}
            }


        } while (!str.toLowerCase().contains("end;"));
    }

    //TODO mv to somewhere easy to access ?
    /**
     * get a MRCAPrior object for given taxon set,
     * from a string array which determines the distribution
     * @param taxonset
     * @param strs3 [0] is distribution name,
     *              [1]-[3] for values to determine the distribution
     * @return a MRCAPrior object
     * @throws RuntimeException
     */
    public MRCAPrior getMRCAPrior(TaxonSet taxonset, String[] strs3) throws RuntimeException {
        RealParameter[] param = new RealParameter[strs3.length];
        for (int i = 1; i < strs3.length; i++) {
            try {
                param[i] = new RealParameter(strs3[i]);
                param[i].setID("param." + i);
            } catch (Exception  e) {
                // ignore parsing errors
            }
        }
        ParametricDistribution distr  = null;
        switch (strs3[0]) {
        case "normal":
            distr = new Normal();
            distr.initByName("mean", param[1], "sigma", param[2]);
            distr.setID("Normal.0");
            break;
        case "uniform":
            distr = new Uniform();
            distr.initByName("lower", strs3[1], "upper", strs3[2]);
            distr.setID("Uniform.0");
            break;
        case "fixed":
            // uniform with lower == upper
            distr = new Normal();
            distr.initByName("mean", param[1], "sigma", "+Infinity");
            distr.setID("Normal.0");
            break;
        case "offsetlognormal":
            distr = new LogNormalDistributionModel();
            distr.initByName("offset", strs3[1], "M", param[2], "S", param[3], "meanInRealSpace", true);
            distr.setID("LogNormalDistributionModel.0");
            break;
        case "lognormal":
            distr = new LogNormalDistributionModel();
            distr.initByName("M", param[1], "S", param[2], "meanInRealSpace", true);
            distr.setID("LogNormalDistributionModel.0");
            break;
        case "offsetexponential":
            distr = new Exponential();
            distr.initByName("offset", strs3[1], "mean", param[2]);
            distr.setID("Exponential.0");
            break;
        case "gamma":
            distr = new Gamma();
            distr.initByName("alpha", param[1], "beta", param[2]);
            distr.setID("Gamma.0");
            break;
        case "offsetgamma":
            distr = new Gamma();
            distr.initByName("offset", strs3[1], "alpha", param[2], "beta", param[3]);
            distr.setID("Gamma.0");
            break;
        default:
            throw new RuntimeException("Unknwon distribution "+ strs3[0]);
        }
        MRCAPrior prior = new MRCAPrior();
        prior.isMonophyleticInput.setValue(true, prior);
        prior.distInput.setValue(distr, prior);
        prior.taxonsetInput.setValue(taxonset, prior);
        prior.setID(taxonset.getID() + ".prior");
        return prior;
    }

    
    protected void processSets() {
    	// create monophyletic MRCAPrior for each taxon set that 
    	// does not already have a calibration associated with it
    	for (TaxonSet taxonset : taxonsets) {
    		boolean found = false;
    		for (BEASTInterface o : taxonset.getOutputs()) {
    			if (o instanceof MRCAPrior) {
    				found = true;
    				break;
    			}
    		}
    		if (!found) {
        		MRCAPrior prior = new MRCAPrior();
        		prior.isMonophyleticInput.setValue(true, prior);
        		prior.taxonsetInput.setValue(taxonset, prior);
        		prior.setID(taxonset.getID() + ".prior");
        		// should set Tree before initialising, but we do not know the tree yet...
        		if (calibrations == null) {
        			calibrations = new ArrayList<>();
        		}
        		calibrations.add(prior);
    		}
    	}
	}


    /**
     * parse sets block
     * BEGIN Sets;
     * TAXSET 'con' = 'con_SL_Gert2' 'con_SL_Tran6' 'con_SL_Tran7' 'con_SL_Gert6';
     * TAXSET 'spa' = 'spa_138a_Cerb' 'spa_JB_Eyre1' 'spa_JB_Eyre2';
     * END; [Sets]
     */
    void parseSetsBlock(final BufferedReader fin) throws IOException {
        String str;
        do {
            str = nextLine(fin);
            if (str.toLowerCase().matches("\\s*taxset\\s.*")) {
            	String [] strs = str.split("=");
            	if (strs.length > 1) {
            		String str0 = strs[0].trim();
            		String [] strs2 = str0.split("\\s+");
            		if (strs2.length != 2) {
            			throw new RuntimeException("expected 'taxset <name> = ...;' but did not get two words before the = sign: " + str);
            		}
            		String taxonSetName = strs2[1];
            		str0 = strs[strs.length - 1].trim();
            		if (!str0.endsWith(";")) {
            			Log.warning.println("expected 'taxset <name> = ...;' semi-colin is missing: " + str + "\n"
            					+ "Taxa from following lines may be missing.");
            		}
            		str0 = str0.replaceAll(";", "");
            		String [] taxonNames = str0.split("\\s+");
            		TaxonSet taxonset = new TaxonSet();
            		for (String taxon : taxonNames) {
            			taxonset.taxonsetInput.get().add(new Taxon(taxon.replaceAll("'\"", "")));
            		}
            		taxonset.setID(taxonSetName.replaceAll("'\"", ""));
            		taxonsets.add(taxonset);
            	}
            }
        } while (!str.toLowerCase().contains("end;"));
    }

    public static String generateSequenceID(final String taxon) {
        String id = "seq_" + taxon;
        int i = 0;
        while (g_sequenceIDs.contains(id + (i > 0 ? i : ""))) {
            i++;
        }
        id = id + (i > 0 ? i : "");
        g_sequenceIDs.add(id);
        return id;
    }

    /**
     * read line from nexus file *
     */
    protected String readLine(final BufferedReader fin) throws IOException {
        if (!fin.ready()) {
            return null;
        }
        lineNr++;
        return fin.readLine();
    }

    /**
     * read next line from nexus file that is not a comment and not empty *
     */
    protected String nextLine(final BufferedReader fin) throws IOException {
        String str = readLine(fin);
        if (str == null) {
            return null;
        }
        if (str.contains("[")) {
            final int start = str.indexOf('[');
            int end = str.indexOf(']', start);
            while (end < 0) {
                str += readLine(fin);
                end = str.indexOf(']', start);
            }
            str = str.substring(0, start) + str.substring(end + 1);
            if (str.matches("^\\s*$")) {
                return nextLine(fin);
            }
        }
        if (str.matches("^\\s*$")) {
            return nextLine(fin);
        }
        return str;
    }

    /**
     * return attribute value as a string *
     */
    protected String getAttValue(final String attribute, final String str) {
        final Pattern pattern = Pattern.compile(".*" + attribute + "\\s*=\\s*([^\\s;]+).*");
        final Matcher matcher = pattern.matcher(str.toLowerCase());
        if (!matcher.find()) {
            return null;
        }
        String att = matcher.group(1);
        if (att.startsWith("\"") && att.endsWith("\"")) {
            final int start = matcher.start(1);
            att = str.substring(start + 1, str.indexOf('"', start + 1));
        }
        return att;
    }

    private ArrayList<String> readInCharstatelablesTokens(final BufferedReader fin) throws IOException {

        ArrayList<String> tokens = new ArrayList<>();
        String token="";
        final int READING=0, OPENQUOTE=1, WAITING=2;
        int mode = WAITING;
        int numberOfQuotes=0;
        boolean endOfBlock=false;
        String str;

        while (!endOfBlock) {
            str = nextLine(fin);
            Character nextChar;
            for (int i=0; i< str.length(); i++) {
                nextChar=str.charAt(i);
                switch (mode) {
                    case WAITING:
                        if (!Character.isWhitespace(nextChar)) {
                            if (nextChar == '\'') {
                                mode=OPENQUOTE;
                            } else if (nextChar == '/' || nextChar == ',') {
                                tokens.add(nextChar.toString());
                                token="";
                            } else if (nextChar == ';') {
                                endOfBlock = true;
                            } else {
                                token=token+nextChar;
                                mode=READING;
                            }
                        }
                        break;
                    case READING:
                        if (nextChar == '\'') {
                            tokens.add(token);
                            token="";
                            mode=OPENQUOTE;
                        } else if (nextChar == '/' || nextChar == ',') {
                            tokens.add(token);
                            tokens.add(nextChar.toString());
                            token="";
                            mode=WAITING;
                        } else if (nextChar == ';') {
                            tokens.add(token);
                            endOfBlock = true;
                        } else if (Character.isWhitespace(nextChar)) {
                            tokens.add(token);
                            token="";
                            mode=WAITING;
                        } else {
                            token=token+nextChar;
                        }
                        break;
                    case OPENQUOTE:
                        if (nextChar == '\'') {
                            numberOfQuotes++;
                        } else {
                            if (numberOfQuotes % 2 == 0) {
                                for (int ind=0; ind< numberOfQuotes/2; ind++) {
                                    token=token+"'";
                                }
                                token=token+nextChar;
                            } else {
                                for (int ind=0; ind< numberOfQuotes/2; ind++) {
                                    token=token+"'";
                                }
                                tokens.add(token);
                                token="";
                                if (nextChar == '/' || nextChar == ',') {
                                    tokens.add(nextChar.toString());
                                    mode=WAITING;
                                } else if (nextChar == ';') {
                                    endOfBlock = true;
                                } else if (Character.isWhitespace(nextChar)) {
                                    mode=WAITING;
                                } else {
                                    token=token+nextChar;
                                    mode=READING;
                                }
                            }
                            numberOfQuotes=0;
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        if (!tokens.get(tokens.size()-1).equals(",")) {
            tokens.add(",");
        }

        return tokens;
    }

    private ArrayList<UserDataType> processCharstatelabelsTokens(ArrayList<String> tokens, int[] maxNumberOfStates) throws IOException {

        ArrayList<UserDataType> charDescriptions = new ArrayList<>();

        final int CHAR_NR=0, CHAR_NAME=1, STATES=2;
        int mode = CHAR_NR;
        int charNumber = -1;
        String charName = "";
        ArrayList<String> states = new ArrayList<>();

        for (String token:tokens) {
            switch (mode) {
                case CHAR_NR:
                    charNumber = Integer.parseInt(token);
                    mode = CHAR_NAME;
                    break;
                case CHAR_NAME:
                    if (token.equals("/")) {
                        mode = STATES;
                    } else if (token.equals(",")) {
                        if (charNumber > charDescriptions.size()+1) {
                            throw new IOException("Character descriptions should go in the ascending order and there " +
                                    "should not be any description missing.");
                        }
                        charDescriptions.add(new UserDataType(charName, states));
                        maxNumberOfStates[0] = Math.max(maxNumberOfStates[0], states.size());
                        charNumber = -1;
                        charName = "";
                        states = new ArrayList<>();
                        mode = CHAR_NR;
                    } else {
                        charName = token;
                    }
                    break;
                case STATES:
                    if (token.equals(",")) {
                        if (charNumber > charDescriptions.size()+1) {
                            throw new IOException("Character descriptions should go in the ascending order and there " +
                                    "should not be any description missing.");
                        }
                        charDescriptions.add(new UserDataType(charName, states));
                        maxNumberOfStates[0] = Math.max(maxNumberOfStates[0], states.size());
                        charNumber = -1;
                        charName = "";
                        states = new ArrayList<>();
                        mode = CHAR_NR;
                    } else {
                        states.add(token);
                    }
                default:
                    break;
            }
        }

        return charDescriptions;

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
                final String xml = new XMLProducer().toXML(parser.m_alignment);
                System.out.println(xml);
            }
            if (parser.traitSet != null) {
                final String xml = new XMLProducer().toXML(parser.traitSet);
                System.out.println(xml);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    } // main

} // class NexusParser
