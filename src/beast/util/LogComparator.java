package beast.util;


import beast.app.BEASTVersion;
import beast.app.util.Utils;

import java.io.File;
import java.util.List;

/**
 * Compare log files to find the set of parameters same between logs but having significantly different value.
 * It is limited to 2 logs at a time at moment.
 *
 * @author Walter Xie
 */
public class LogComparator {
    /**
     * matched column labels in log files
     */
    protected List<String> matchedLabels;

    Double[] significance;

    /**
     * at least 2 logs
     * @param analyser1
     * @param analyser2
     */
    public LogComparator (LogAnalyser analyser1, LogAnalyser analyser2, boolean verbose) {
        assert analyser1 != null;
        assert analyser2 != null;

        compareLogs(analyser1, analyser2);
    }

    public double getSignificance(String sLabel) {
        int index = matchedLabels.indexOf(sLabel);
        if (index < 0)
            throw new IllegalArgumentException("Cannot find " + sLabel + " from matched parameter list !");

        return significance[index];
    }


    protected void compareLogs(LogAnalyser analyser1, LogAnalyser analyser2) {
        matchedLabels = CollectionUtils.insect(analyser1.m_sLabels, analyser2.m_sLabels);

        if (matchedLabels.size() < 1)
            throw new IllegalArgumentException("There is no parameter name matched between log files !");

        significance = new Double[matchedLabels.size()];

        for (String mLabel : matchedLabels) {
            int index1 = analyser1.indexof(mLabel);

            int index2 = analyser2.indexof(mLabel);


        }

    }

    /**
     * @param args
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

        LogComparator logComparator = new LogComparator(analyser1, analyser2, true);

    } // main


}
