package beast.util;

import beast.app.BEASTVersion;
import beast.app.util.Utils;
import beast.core.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Walter Xie
 */
public class TreeLogAnalyser extends LogReader{

    public final static String TREE_STATE_MATCH = "^tree STATE.*";
    public final static String TREE_STATE_REPLACE = "^tree STATE_[^\\s=]*";

    // trees in string but no annotation, such as "^tree STATE_[^\s=]*"
    List<String> treesInString;

    /**
     *
     * @param fileName
     * @param burninPercentage  burninPercentage typical = 10; percentage of data that can be ignored
     * @throws Exception
     */
    public TreeLogAnalyser(String fileName, int burninPercentage) throws Exception {
        readTreeLogFile(fileName, burninPercentage);
        analyse();
    }

    public TreeLogAnalyser(String fileName) throws Exception {
        this(fileName, BURN_IN_PERCENTAGE);
    }

    public TreeLogAnalyser() { }


    public void readTreeLogFile(String fileName, int burninPercentage) throws Exception {
        lowMemory = false;
        init(fileName, burninPercentage);

        long nLines = Math.max(1, total / 80);
        // reserve memory
        long nBurnIn = getNBurnIn(burninPercentage);
        long nData = initNData(nBurnIn);

        BufferedReader fin = new BufferedReader(new FileReader(fileName));

        while (fin.ready()) {
            String tree = next(fin, ++nData >= 0);

            if (tree != null)
                treesInString.add(tree);

            if (nData % nLines == 0)
                log("*");
        }
        logln("");
    } // readTreeLogFile

    public void analyse() {
        //TODO
    }

    @Override
    public void init(String fileName, int burninPercentage) throws Exception {
        this.fileName=fileName;

        log("\nLoading " + fileName);
        BufferedReader fin = new BufferedReader(new FileReader(fileName));
        String sStr = null;
        m_sPreAmble = "";
        total = 0;
        // grab data from the log, ignoring burn in samples
        long nSample0 = -1;

        // first, sweep through the log file to determine size of the log
        while (fin.ready()) {
            sStr = fin.readLine();
            if (sStr.matches(TREE_STATE_MATCH)) {
                // calculate sampleInterval
                if (total < 2) {
                    String sStr2 = sStr.substring(11, sStr.indexOf("=")).trim();
                    String[] sStrArray = sStr2.split("\\s");
                    if (nSample0 < 0) {
                        nSample0 = Long.parseLong(sStrArray[0]);
                    } else {
                        sampleInterval = (int) (Long.parseLong(sStrArray[0]) - nSample0);
                    }
                }
                total++;
            } else {
                if (total == 0) {
                    m_sPreAmble += sStr + "\n";
                }
            }
        }

        long nBurnIn = getNBurnIn(burninPercentage);

        logln(" skipping " + nBurnIn + " trees\n\n" + BAR);

        if (treesInString == null) {
            treesInString = new ArrayList<String>();
        }
    }

    @Override
    public String next(BufferedReader fin, boolean process) throws Exception {
        String sStr = fin.readLine();
        if (sStr.matches(TREE_STATE_MATCH)) {
            if (process) { //++nData >= 0
                sStr = sStr.replaceAll(TREE_STATE_REPLACE, "");
                return sStr;
            }
        }
        return null;
    }

    @Override
    public boolean hasNext() {
        return current < total;
    }

    @Override
    public void reset() throws Exception {
        current = 0;
    }

    //+++++++++ public methods ++++++
    public List<String> getTreesInString() {
        return treesInString;
    }

    public void print(PrintStream out) {
        for (int i = 0; i < treesInString.size(); i++) {
            String tree = treesInString.get(i);
            out.println("tree STATE_" + (sampleInterval * i) + (Character.isSpaceChar(tree.charAt(0)) ? "" : " ") + tree);
        }
    }

    public static void main(String[] args) {
        final String program = "TreeLogAnalyser";
        try {
            TreeLogAnalyser analyser;
            // process args
            int burninPercentage = BURN_IN_PERCENTAGE;
            List<String> files = new ArrayList<>();
            int i = 0;
            while (i < args.length) {
                String arg = args[i];
                switch (arg) {
                    case "-b":
                    case "-burnin":
                    case "--burnin":
                        if (i+1 >= args.length) {
                            Log.warning.println("-b argument requires another argument");
                            printUsageAndExit(program);
                        }
                        burninPercentage = Integer.parseInt(args[i+1]);
                        i += 2;
                        break;
                    case "-h":
                    case "-help":
                    case "--help":
                        printUsageAndExit(program);
                        break;
                    default:
                        if (arg.startsWith("-")) {
                            Log.warning.println("unrecognised command " + arg);
                            printUsageAndExit(program);
                        }
                        files.add(arg);
                        i++;
                }
            }
            if (files.size() == 0) {
                // no file specified, open file dialog to select one
                BEASTVersion version = new BEASTVersion();
                File file = Utils.getLoadFile(program + " " + version.getVersionString() + " - Select tree log file to analyse",
                        null, "BEAST tree log (*.trees) Files", "trees", "txt");
                if (file == null) {
                    return;
                }
                analyser = new TreeLogAnalyser(file.getAbsolutePath(), burninPercentage);
                analyser.print(System.out);
            } else {
                // process files
                for (String file : files) {
                    analyser = new TreeLogAnalyser(file, burninPercentage);
                    analyser.print(System.out);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
