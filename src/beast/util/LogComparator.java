package beast.util;


import static beast.util.OutputUtils.format;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import beast.app.BEASTVersion;
import beast.app.util.Utils;

/**
 * Compare log files to find the set of parameters same between logs but having significantly different value.
 * Z score = 2 * |mean1 - mean2| / (stdError1 + stdError2), If Z score > 2 it is significant.
 * It is limited to 2 logs at a time at moment.
 *
 * @author Walter Xie
 */
public class LogComparator {
    /**
     * matched column labels in log files
     */
    protected List<String> matchedLabels;

    protected Double[] zScore; // Z score

    protected LogAnalyser analyser1, analyser2;

    /**
     * at least 2 logs
     * @param analyser1
     * @param analyser2
     */
    public LogComparator (LogAnalyser analyser1, LogAnalyser analyser2) {
        assert analyser1 != null;
        assert analyser2 != null;

        this.analyser1 = analyser1;
        this.analyser2 = analyser2;
        compareLogs();
    }

    public double getZScore(String label) {
        int index = matchedLabels.indexOf(label);
        if (index < 0)
            throw new IllegalArgumentException("Cannot find " + label + " from matched parameter list !");

        return zScore[index];
    }

    protected void compareLogs() {
        matchedLabels = CollectionUtils.intersection(analyser1.getLabels(), analyser2.getLabels());

        if (matchedLabels.size() < 1)
            throw new IllegalArgumentException("There is no parameter name matched between log files !");

        zScore = new Double[matchedLabels.size()];

        for (String mLabel : matchedLabels) {
            int index1 = analyser1.indexof(mLabel);
            double m1 = analyser1.getMean(index1);
            double se1 = analyser1.getStdError(index1);

            int index2 = analyser2.indexof(mLabel);
            double m2 = analyser2.getMean(index2);
            double se2 = analyser2.getStdError(index2);

            int index = matchedLabels.indexOf(mLabel);
            // Z score = 2 * |m1 - m2| / (se1 + se2), If Z score > 2 it is significant
            zScore[index] = 2 * Math.abs(m1 - m2) / (se1 + se2);
        }
    }

    final String SPACE = OutputUtils.SPACE;
    final String STAR = "* ";
    final String NON_STAR = "  ";
    public void print(PrintStream out, boolean verbose) {
        // set up header for prefix, if any is specified
        String prefix = System.getProperty("prefix");
        String prefixHead = (prefix == null ? "" : "prefix ");
        if (prefix != null) {
            String [] p = prefix.trim().split("\\s+");
            if (p.length > 1) {
                prefixHead = "";
                for (int i = 0; i < p.length; i++) {
                    prefixHead += "prefix" + i + " ";
                }
            }
        }

        int nMax = 0;
        for (int i = 1; i < matchedLabels.size(); i++)
            nMax = Math.max(matchedLabels.get(i).length(), nMax);
        String space = "";
        for (int i = 0; i < nMax; i++)
            space += " ";

        out.println("Comparing log " + analyser1.getLogFile() + " and " + analyser2.getLogFile() + "\n");

        List<String> significantLabels = new ArrayList<>();
        if (verbose) {
            out.println("item" + space.substring(4) + " " + prefixHead + "   " + format("ZScore") +
                    format("mean1") + format("stderr1") + format("mean2") + format("stderr2"));

            for (int i = 1; i < matchedLabels.size(); i++) {
                String mLabel = matchedLabels.get(i);

                int index1 = analyser1.indexof(mLabel);
                double m1 = analyser1.getMean(index1);
                double se1 = analyser1.getStdError(index1);

                int index2 = analyser2.indexof(mLabel);
                double m2 = analyser2.getMean(index2);
                double se2 = analyser2.getStdError(index2);

                out.println(mLabel + space.substring(mLabel.length()) + SPACE + (prefix == null ? "" : prefix + SPACE) +
                        (zScore[i] > 2 ? STAR : NON_STAR) + SPACE + format(zScore[i]) + SPACE +
                        format(m1) + SPACE + format(se1) + SPACE + format(m2) + SPACE + format(se2));

                if (zScore[i] > 2) significantLabels.add(mLabel);
            }
        }

        out.println("\nThere are " + significantLabels.size() + " parameters having significantly different value : " +
                OutputUtils.toString(significantLabels) + "\n\n");
    }

    /**
     * main
     */
    public static void main(String[] args) {
        LogAnalyser analyser1 = null;
        LogAnalyser analyser2 = null;
        try {
            if (args.length == 0) {
                BEASTVersion version = new BEASTVersion();
                File file = Utils.getLoadFile("LogComparator " + version.getVersionString() + " - Select first log file to analyse",
                        null, "BEAST log (*.log) Files", "log", "txt");
                if (file == null) {
                    return;
                }
                analyser1 = new LogAnalyser(file.getAbsolutePath());

                file = Utils.getLoadFile("LogComparator " + version.getVersionString() + " - Select second log file to analyse",
                        null, "BEAST log (*.log) Files", "log", "txt");
                if (file == null) {
                    return;
                }
                analyser2 = new LogAnalyser(file.getAbsolutePath());

            } else {
                analyser1 = new LogAnalyser(args[0]);
                analyser2 = new LogAnalyser(args[1]);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        LogComparator logComparator = new LogComparator(analyser1, analyser2);
        logComparator.print(System.out, true);

    } // main


}
