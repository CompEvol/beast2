package beast.app.tools;

import jam.console.ConsoleApplication;
import jam.panels.OptionsPanel;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import beast.app.BEASTVersion;
import beast.app.util.Utils;
import beast.app.util.WholeNumberField;
import beast.evolution.tree.coalescent.CompoundPopulationFunction;
import beast.evolution.tree.coalescent.CompoundPopulationFunction.Type;
import beast.math.statistic.DiscreteStatistics;
import beast.util.HeapSort;



public class EBSPAnalyser {
    String m_sFileOut;
    PrintStream m_out = System.out;
    CompoundPopulationFunction.Type m_type = Type.LINEAR;
    String m_sInputFile;
    int m_nBurninPercentage = 10;

    private void run() throws Exception {
        parse(m_sInputFile, m_nBurninPercentage, m_type, m_out);
    }

    void parse(String sFile, int nBurnInPercentage, CompoundPopulationFunction.Type type, PrintStream out) throws Exception {
        logln("Processing " + sFile);
        BufferedReader fin = new BufferedReader(new FileReader(sFile));
        String sStr;
        int nData = 0;
        // first, sweep through the log file to determine size of the log
        while (fin.ready()) {
            sStr = fin.readLine();
            // terrible hackish code, must improve later
            if( sStr.charAt(0) == '#' ) {
                int i = sStr.indexOf("spec=");
                if( i > 0 ) {
                   if( sStr.indexOf("type=\"stepwise\"") > 0 ) {
                      m_type = Type.STEPWISE;
                   }  else if( sStr.indexOf("type=\"linear\"") > 0 ) {
                      m_type = Type.LINEAR;
                   }
                }
            }
            if (sStr.indexOf('#') < 0 && sStr.matches(".*[0-9a-zA-Z].*")) {
                nData++;
            }
        }
        final int nBurnIn = nData * nBurnInPercentage / 100;
        logln(" skipping " + nBurnIn + " line\n\n");
        nData = -nBurnIn - 1;
        fin = new BufferedReader(new FileReader(sFile));

        // process log
        final List<List<Double>> times = new ArrayList<List<Double>>();
        final List<List<Double>> popSizes = new ArrayList<List<Double>>();
        double[] alltimes = null;
        while (fin.ready()) {
            sStr = fin.readLine();
            if (sStr.indexOf('#') < 0 && sStr.matches(".*[0-9a-zA-Z].*")) {
                if (++nData > 0) {
                    final String[] sStrs = sStr.split("\t");
                    final List<Double> times2 = new ArrayList<Double>();
                    final List<Double> popSizes2 = new ArrayList<Double>();
                    if (alltimes == null) {
                        alltimes = new double[sStrs.length - 1];
                    }
                    for (int i = 1; i < sStrs.length; i++) {
                        final String[] sStrs2 = sStrs[i].split(":");
                        final Double time = Double.parseDouble(sStrs2[0]);
                        alltimes[i - 1] += time;
                        if (sStrs2.length > 1) {
                            times2.add(time);
                            popSizes2.add(Double.parseDouble(sStrs2[1]));
                        }
                    }
                    times.add(times2);
                    popSizes.add(popSizes2);

                }
            }
        }

        if (alltimes == null) {
            //burn-in too large?
            return;
        }

        // take average of coalescent times
        for (int i = 0; i < alltimes.length; i++) {
            alltimes[i] /= times.size();
        }

        // generate output
        out.println("time\tmean\tmedian\t95HPD lower\t95HPD upper");
        final double[] popSizeAtTimeT = new double[times.size()];
        int[] indices = new int[times.size()];

        for (final double time : alltimes) {
            for (int j = 0; j < popSizeAtTimeT.length; j++) {
                popSizeAtTimeT[j] = calcPopSize(type, times.get(j), popSizes.get(j), time);
            }

            HeapSort.sort(popSizeAtTimeT, indices);

            out.print(time + "\t");

            out.print(DiscreteStatistics.mean(popSizeAtTimeT) + "\t");
            out.print(DiscreteStatistics.median(popSizeAtTimeT) + "\t");

            double[] hpdInterval = DiscreteStatistics.HPDInterval(0.95, popSizeAtTimeT, indices);
            out.println(hpdInterval[0] + "\t" + hpdInterval[1]);
        }
    }

    private double calcPopSize(CompoundPopulationFunction.Type type, List<Double> xs, List<Double> ys, double d) {
        // TODO completely untested
        // assume linear
        //assert sType.equals("Linear");

        final int n = xs.size();
        final double xn = xs.get(n - 1);
        if (d >= xn) {
            return ys.get(n - 1);
        }
        assert d >= xs.get(0);

        int i = 1;
        while (d >= xs.get(i)) {
            ++i;
        }
        // d < xs.get(i)

        double x0 = xs.get(i-1);
        double x1 = xs.get(i);
        double y0 = ys.get(i-1);
        double y1 = ys.get(i);
        assert x0 <= d && d <= x1 : "" + x0 + "," + x1 + "," + d;
        switch (type) {
            case LINEAR:
                final double p = (d * (y1 - y0) + (y0 * x1 - y1 * x0)) / (x1 - x0);
                assert p > 0;
                return p;
            case STEPWISE:
                assert y1 > 0;
                return y1;
        }
        return 0;
    }

    private void parseArgs(String[] args) throws Exception {
        int i = 0;
        try {
            while (i < args.length) {
                int iOld = i;
                if (i < args.length) {
                    if (args[i].equals("")) {
                        i += 1;
                    } else if (args[i].equals("-help") || args[i].equals("-h") || args[i].equals("--help")) {
                        System.out.println(getUsage());
                        System.exit(0);
                    } else if (args[i].equals("-i")) {
                        m_sInputFile = args[i + 1];
                        i += 2;
                    } else if (args[i].equals("-o")) {
                        m_sFileOut = args[i + 1];
                        m_out = new PrintStream(m_sFileOut);
                        i += 2;
                    } else if (args[i].equals("-type")) {
                        if (args[i + 1].equals("linear")) {
                            m_type = Type.LINEAR;
                        } else if (args[i + 1].equals("stepwise")) {
                            m_type = Type.STEPWISE;
                        } else {
                            throw new Exception("Expected linear of stepwise, not " + args[i + 1]);
                        }
                        i += 2;
                    } else if (args[i].equals("-burnin")) {
                        m_nBurninPercentage = Integer.parseInt(args[i + 1]);
                        i += 2;
                    }
                    if (i == iOld) {
                        throw new Exception("Unrecognised argument (argument " + i + ": " + args[i] + ")");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Error parsing command line arguments: " + Arrays.toString(args) + "\nArguments ignored\n\n" + getUsage());
        }
        if (m_sFileOut == null) {
            System.err.println("No output file specified");
        }

    }

    static String getUsage() {
        return "EBSPAnalyse -i <inputfile> [options]\n" +
                "analyses trace file generated by EBSP analysis\n" +
                "Options are:\n" +
                "-i <inputfile> name of input file (required)\n" +
                "-burnin <percentage> percent of log to consider burn in, default 10\n" +
                "-type [linear|step] type of population function\n" +
                "-o <outputfile> name of output file, default to output on stdout\n" +
                "";
    }

    protected void log(String s) {
        System.err.print(s);
    }

    protected void logln(String s) {
        System.err.println(s);
    }

    private void printTitle(String aboutString) {
        aboutString = "LogCombiner" + aboutString.replaceAll("</p>", "\n\n");
        aboutString = aboutString.replaceAll("<br>", "\n");
        aboutString = aboutString.replaceAll("<[^>]*>", " ");
        String[] sStrs = aboutString.split("\n");
        for (String sStr : sStrs) {
            int n = 80 - sStr.length();
            int n1 = n / 2;
            for (int i = 0; i < n1; i++) {
                log(" ");
            }
            logln(sStr);
        }
    }

    public class EBSPAnalyserDialog {
        private final JFrame frame;

        private final OptionsPanel optionPanel;

        private final JTextField inputFileNameText = new JTextField("not selected", 16);
        private final JComboBox typeCombo = new JComboBox(new String[]{"linear", "stepwise"});

        final WholeNumberField burninText = new WholeNumberField((long) 0, Long.MAX_VALUE);
        private final JTextField outputFileNameText = new JTextField("not selected", 16);

        private File outputFile = null;
        private File inputFile = null;

        public EBSPAnalyserDialog(final JFrame frame, String titleString, Icon icon) {
            this.frame = frame;

            optionPanel = new OptionsPanel(12, 12);

            final JLabel titleText = new JLabel(titleString);
            titleText.setIcon(icon);
            optionPanel.addSpanningComponent(titleText);
            titleText.setFont(new Font("sans-serif", 0, 12));

            JPanel panel = new JPanel(new BorderLayout());
            panel.setOpaque(false);

            JButton button = new JButton("Choose Input File...");
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    File file = Utils.getLoadFile("Select input file...");
                    if (file == null) {
                        // the dialog was cancelled...
                        return;
                    }

                    inputFile = file;
                    inputFileNameText.setText(inputFile.getName());

                }
            });
            inputFileNameText.setEditable(false);

            JButton button2 = new JButton("Choose Output File...");
            button2.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    File file = Utils.getSaveFile("Select output file...");
                    if (file == null) {
                        // the dialog was cancelled...
                        return;
                    }

                    outputFile = file;
                    outputFileNameText.setText(outputFile.getName());

                }
            });
            outputFileNameText.setEditable(false);

            JPanel panel1 = new JPanel(new BorderLayout(0, 0));
            panel1.add(inputFileNameText, BorderLayout.CENTER);
            panel1.add(button, BorderLayout.EAST);
            optionPanel.addComponentWithLabel("Input File: ", panel1);

            optionPanel.addComponentWithLabel("File type: ", typeCombo);

            burninText.setColumns(12);
            burninText.setValue(10);
            optionPanel.addComponentWithLabel("Burn in percentage: ", burninText);

            optionPanel.addSpanningComponent(panel);

            JPanel panel3 = new JPanel(new BorderLayout(0, 0));
            panel3.add(outputFileNameText, BorderLayout.CENTER);
            panel3.add(button2, BorderLayout.EAST);
            optionPanel.addComponentWithLabel("Output File: ", panel3);
        }

        public boolean showDialog(String title) {

            JOptionPane optionPane = new JOptionPane(optionPanel,
                    JOptionPane.PLAIN_MESSAGE,
                    JOptionPane.OK_CANCEL_OPTION,
                    null,
                    new String[]{"Run", "Quit"},
                    null);
            optionPane.setBorder(new EmptyBorder(12, 12, 12, 12));

            final JDialog dialog = optionPane.createDialog(frame, title);
            //dialog.setResizable(true);
            dialog.pack();

            dialog.setVisible(true);

            return optionPane.getValue().equals("Run");
        }

        public String getOutputFileName() {
            if (outputFile == null) return null;
            return outputFile.getPath();
        }

        public String[] getArgs() {
            java.util.List<String> args = new ArrayList<String>();
            if (inputFile != null) {
                args.add("-i");
                args.add(inputFile.getPath());
            }
            args.add("-burnin");
            args.add(burninText.getText());
            args.add("-type");
            args.add(typeCombo.getSelectedItem().toString());
            if (outputFile != null) {
                args.add("-o");
                args.add(outputFile.getPath());
            }
            return args.toArray(new String[0]);
        }

    }


    /**
     * @param args
     */
    public static void main(String[] args) {
        BEASTVersion version = new BEASTVersion();
        final String versionString = version.getVersionString();
        String nameString = "EBSP Analyser " + versionString;
        String aboutString = "<html><center><p>" + versionString + ", " + version.getDateString() + "</p>" +
                "<p>by<br>" +
                "<p>Joseph Heled and Remco Bouckaert</p>" +
                "<p>Department of Computer Science, University of Auckland<br>" +
                "<a href=\"mailto:jheled@gmail.com\">jheled@gmail.com</a></p>" +
                "<a href=\"mailto:remco@cs.auckland.ac.nz\">remco@cs.auckland.ac.nz</a></p>" +
                "<p>Part of the BEAST 2 package:<br>" +
                "<a href=\"http://beast2.cs.auckland.ac.nz/\">http://beast2.cs.auckland.ac.nz/</a></p>" +
                "</center></html>";


        try {
            EBSPAnalyser analyser = new EBSPAnalyser();
            if (args.length == 0) {
                System.setProperty("com.apple.macos.useScreenMenuBar", "true");
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("apple.awt.showGrowBox", "true");

                // TODO: set up ICON
                java.net.URL url = EBSPAnalyser.class.getResource("images/EBSPAnalyser.png");
                javax.swing.Icon icon = null;

                if (url != null) {
                    icon = new javax.swing.ImageIcon(url);
                }


                //ConsoleApplication consoleApp =
                new ConsoleApplication(nameString, aboutString, icon, true);

                analyser.printTitle(aboutString);

                String titleString = "<html><center><p>EBSPAnalyser<br>" +
                        "Version " + version.getVersionString() + ", " + version.getDateString() + "</p></center></html>";

                EBSPAnalyserDialog dialog = analyser.new EBSPAnalyserDialog(new JFrame(), titleString, icon);

                if (!dialog.showDialog(nameString)) {
                    return;
                }
                String[] args2 = dialog.getArgs();

                try {
                    analyser.parseArgs(args2);
                    analyser.run();

                } catch (Exception ex) {
                    System.err.println("Exception: " + ex.getMessage());
                    ex.printStackTrace();
                }
                System.out.println("Finished - Quit program to exit.");
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                analyser.printTitle(aboutString);
                analyser.parseArgs(args);
                analyser.run();
            }
        } catch (Exception e) {
            System.out.println(getUsage());
            e.printStackTrace();
        }

    }
}
