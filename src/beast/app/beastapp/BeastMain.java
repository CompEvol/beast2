package beast.app.beastapp;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;

import beagle.BeagleFlag;
import beagle.BeagleInfo;
import beast.app.BEASTVersion;
import beast.app.BeastMCMC;
import beast.app.util.Arguments;
import beast.app.util.ErrorLogHandler;
import beast.app.util.MessageLogHandler;
import beast.app.util.Utils;
import beast.app.util.Version;
import beast.core.util.Log;
import beast.util.Randomizer;
import beast.util.XMLParserException;
import jam.util.IconUtils;

public class BeastMain {

    private final static Version version = new BEASTVersion();

    static class BeastConsoleApp extends jam.console.ConsoleApplication {

        public BeastConsoleApp(final String nameString, final String aboutString, final javax.swing.Icon icon) throws IOException {
            super(nameString, aboutString, icon, false);
            getDefaultFrame().setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        }

        public void doStop() {
            // thread.stop is deprecated so need to send a message to running threads...
//            Iterator iter = parser.getThreads();
//            while (iter.hasNext()) {
//                Thread thread = (Thread) iter.next();
//                thread.stop(); // http://java.sun.com/j2se/1.5.0/docs/guide/misc/threadPrimitiveDeprecation.html
//            }
        }

        public void setTitle(final String title) {
            getDefaultFrame().setTitle(title);
        }

        BeastMCMC beastMCMC;
    }

    public BeastMain(final BeastMCMC beastMCMC, final BeastConsoleApp consoleApp, final int maxErrorCount) {

        final Logger infoLogger = Logger.getLogger("beast.app");
        try {

            if (consoleApp != null) {
                consoleApp.beastMCMC = beastMCMC;
            }

            // Add a handler to handle warnings and errors. This is a ConsoleHandler
            // so the messages will go to StdOut..
            final Logger logger = Logger.getLogger("beast");

            Handler handler = new MessageLogHandler();
            handler.setFilter(record -> record.getLevel().intValue() < Level.WARNING.intValue());
            logger.addHandler(handler);

//            // Add a handler to handle warnings and errors. This is a ConsoleHandler
//            // so the messages will go to StdErr..
//            handler = new ConsoleHandler();
//            handler.setFilter(new Filter() {
//                public boolean isLoggable(LogRecord record) {
//                    if (verbose) {
//                        return record.getLevel().intValue() >= Level.WARNING.intValue();
//                    } else {
//                        return record.getLevel().intValue() >= Level.SEVERE.intValue();
//                    }
//                }
//            });
//            logger.addHandler(handler);

            logger.setUseParentHandlers(false);

//            infoLogger.info("Parsing XML file: " + fileName);
//            infoLogger.info("  File encoding: " + fileReader.getEncoding());

            // This is a special logger that is for logging numerical and statistical errors
            // during the MCMC run. It will tolerate up to maxErrorCount before throwing a
            // RuntimeException to shut down the run.
            //Logger errorLogger = Logger.getLogger("error");
            handler = new ErrorLogHandler(maxErrorCount);
            handler.setLevel(Level.WARNING);
            logger.addHandler(handler);

            beastMCMC.run();

        } catch (java.io.IOException ioe) {
            infoLogger.severe("File error: " + ioe.getMessage());
            /* Catch exceptions and report useful information

        }  catch (org.xml.sax.SAXParseException spe) {
            if (spe.getMessage() != null && spe.getMessage().equals("Content is not allowed in prolog")) {
                infoLogger.severe("Parsing error - the input file, " + fileName + ", is not a valid XML file.");
            } else {
                infoLogger.severe("Error running file: " + fileName);
                infoLogger.severe("Parsing error - poorly formed XML (possibly not an XML file):\n" +
                        spe.getMessage());
            }
        } catch (org.w3c.dom.DOMException dome) {
            infoLogger.severe("Error running file: " + fileName);
            infoLogger.severe("Parsing error - poorly formed XML:\n" +
                    dome.getMessage());
        } catch (dr.xml.XMLParseException pxe) {
            if (pxe.getMessage() != null && pxe.getMessage().equals("Unknown root document element, beauti")) {
                infoLogger.severe("Error running file: " + fileName);
                infoLogger.severe(
                        "The file you just tried to run in BEAST is actually a BEAUti document.\n" +
                                "Although this uses XML, it is not a format that BEAST understands.\n" +
                                "These files are used by BEAUti to save and load your settings so that\n" +
                                "you can go back and alter them. To generate a BEAST file you must\n" +
                                "select the 'Generate BEAST File' option, either from the File menu or\n" +
                                "the button at the bottom right of the window.");

            } else {
                infoLogger.severe("Parsing error - poorly formed BEAST file, " + fileName + ":\n" +
                        pxe.getMessage());
            }

        } catch (RuntimeException rex) {
            if (rex.getMessage() != null && rex.getMessage().startsWith("The initial posterior is zero")) {
                infoLogger.warning("Error running file: " + fileName);
                infoLogger.severe(
                        "The initial model is invalid because state has a zero probability.\n\n" +
                                "If the log likelihood of the tree is -Inf, his may be because the\n" +
                                "initial, random tree is so large that it has an extremely bad\n" +
                                "likelihood which is being rounded to zero.\n\n" +
                                "Alternatively, it may be that the product of starting mutation rate\n" +
                                "and tree height is extremely small or extremely large. \n\n" +
                                "Finally, it may be that the initial state is incompatible with\n" +
                                "one or more 'hard' constraints (on monophyly or bounds on parameter\n" +
                                "values. This will result in Priors with zero probability.\n\n" +
                                "The individual components of the posterior are as follows:\n" +
                                rex.getMessage() + "\n" +
                                "For more information go to <http://beast.bio.ed.ac.uk/>.");
            } else {
                // This call never returns as another RuntimeException exception is raised by
                // the error log handler???
                infoLogger.warning("Error running file: " + fileName);
                System.err.println("Fatal exception: " + rex.getMessage());
                rex.printStackTrace(System.err);
            }
            */
        } catch (XMLParserException e) {
            System.out.println(e.getMessage());
            //e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
//            infoLogger.warning("Error running file: " + fileName);
//            infoLogger.severe("Fatal exception: " + ex.getMessage());
//            System.err.println("Fatal exception: " + ex.getMessage());
//            ex.printStackTrace(System.err);
//        }
    }

    static String getFileNameByDialog(final String title) {
        final JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
        fc.addChoosableFileFilter(new FileFilter() {
            public boolean accept(final File f) {
                if (f.isDirectory()) {
                    return true;
                }
                final String name = f.getName().toLowerCase();
                if (name.endsWith(".xml")) {
                    return true;
                }
                return false;
            }

            // The description of this filter
            public String getDescription() {
                return "xml files";
            }
        });

        fc.setDialogTitle(title);
        final int rval = fc.showOpenDialog(null);

        if (rval == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile().toString();
        }
        return null;
    } // getFileNameByDialog


    public static void centreLine(final String line, final int pageWidth) {
        final int n = pageWidth - line.length();
        final int n1 = n / 2;
        for (int i = 0; i < n1; i++) {
            System.out.print(" ");
        }
        System.out.println(line);
    }

    public static void printTitle() {

        int pageWidth = 72;

        System.out.println();
        centreLine("BEAST " + version.getVersionString() + ", " + version.getDateString(), pageWidth);
        centreLine("Bayesian Evolutionary Analysis Sampling Trees", pageWidth);
        for (final String creditLine : version.getCredits()) {
            centreLine(creditLine, pageWidth);
        }
        System.out.println();
    }

    public static void printUsage(final Arguments arguments) {

        arguments.printUsage("beast", "[<input-file-name>]");
        System.out.println();
        System.out.println("  Example: beast test.xml");
        System.out.println("  Example: beast -window test.xml");
        System.out.println("  Example: beast -help");
        System.out.println();
    }

    //Main method
    public static void main(final String[] args) throws java.io.IOException {

        final List<String> MCMCargs = new ArrayList<>();
//    	Utils.loadUIManager();

        final Arguments arguments = new Arguments(
                new Arguments.Option[]{

//                        new Arguments.Option("verbose", "Give verbose XML parsing messages"),
//                        new Arguments.Option("warnings", "Show warning messages about BEAST XML file"),
//                        new Arguments.Option("strict", "Fail on non-conforming BEAST XML file"),
                        new Arguments.Option("window", "Provide a console window"),
                        new Arguments.Option("options", "Display an options dialog"),
                        new Arguments.Option("working", "Change working directory to input file's directory"),
                        new Arguments.LongOption("seed", "Specify a random number generator seed"),
                        new Arguments.StringOption("prefix", "PREFIX", "Specify a prefix for all output log filenames"),
                        new Arguments.StringOption("statefile", "STATEFILE", "Specify the filename for storing/restoring the state"),
                        new Arguments.Option("overwrite", "Allow overwriting of log files"),
                        new Arguments.Option("resume", "Allow appending of log files"),
                        new Arguments.Option("validate", "Parse the XML, but do not run -- useful for debugging XML"),
                        // RRB: not sure what effect this option has
                        new Arguments.IntegerOption("errors", "Specify maximum number of numerical errors before stopping"),
                        new Arguments.IntegerOption("threads", "The number of computational threads to use (default auto)"),
                        new Arguments.Option("java", "Use Java only, no native implementations"),
                        new Arguments.Option("noerr", "Suppress all output to standard error"),
                        new Arguments.StringOption("loglevel", "LEVEL", "error,warning,info,debug,trace"),
                        new Arguments.Option("beagle", "Use beagle library if available"),
                        new Arguments.Option("beagle_info", "BEAGLE: show information on available resources"),
                        new Arguments.StringOption("beagle_order", "order", "BEAGLE: set order of resource use"),
                        new Arguments.IntegerOption("beagle_instances", "BEAGLE: divide site patterns amongst instances"),
                        new Arguments.Option("beagle_CPU", "BEAGLE: use CPU instance"),
                        new Arguments.Option("beagle_GPU", "BEAGLE: use GPU instance if available"),
                        new Arguments.Option("beagle_SSE", "BEAGLE: use SSE extensions if available"),
                        new Arguments.Option("beagle_single", "BEAGLE: use single precision if available"),
                        new Arguments.Option("beagle_double", "BEAGLE: use double precision if available"),
                        new Arguments.StringOption("beagle_scaling", new String[]{"default", "none", "dynamic", "always"},
                                false, "BEAGLE: specify scaling scheme to use"),
                        new Arguments.Option("help", "Print this information and stop"),
                });

        try {
            arguments.parseArguments(args);
        } catch (Arguments.ArgumentException ae) {
            System.out.println();
            System.out.println(ae.getMessage());
            System.out.println();
            printUsage(arguments);
            System.exit(1);
        }

        if (arguments.hasOption("help")) {
            printUsage(arguments);
            System.exit(0);
        }

//        final boolean verbose = arguments.hasOption("verbose");
//        final boolean parserWarning = arguments.hasOption("warnings"); // if dev, then auto turn on, otherwise default to turn off
//        final boolean strictXML = arguments.hasOption("strict");
        final boolean window = arguments.hasOption("window");
        final boolean options = arguments.hasOption("options");
        final boolean working = arguments.hasOption("working");
        final boolean doNotRun = arguments.hasOption("validate");
        String fileNamePrefix = null;
        String stateFileName = null;
        //boolean allowOverwrite = arguments.hasOption("overwrite");

        long seed = Randomizer.getSeed();
        boolean useJava = false;

        int threadCount = 0;

        if (arguments.hasOption("java")) {
            useJava = true;
        }

        if (arguments.hasOption("prefix")) {
            fileNamePrefix = arguments.getStringOption("prefix");
        }

        if (arguments.hasOption("statefile")) {
        	stateFileName = arguments.getStringOption("statefile");
        }

        long beagleFlags = 0;

        boolean useBeagle = arguments.hasOption("beagle") ||
                arguments.hasOption("beagle_CPU") ||
                arguments.hasOption("beagle_GPU") ||
                arguments.hasOption("beagle_SSE") ||
                arguments.hasOption("beagle_double") ||
                arguments.hasOption("beagle_single") ||
                arguments.hasOption("beagle_order") ||
                arguments.hasOption("beagle_instances");

        if (arguments.hasOption("beagle_scaling")) {
            System.setProperty("beagle.scaling", arguments.getStringOption("beagle_scaling"));
        }

        boolean beagleShowInfo = arguments.hasOption("beagle_info");

        if (arguments.hasOption("beagle_CPU")) {
            beagleFlags |= BeagleFlag.PROCESSOR_CPU.getMask();
        }
        if (arguments.hasOption("beagle_GPU")) {
            beagleFlags |= BeagleFlag.PROCESSOR_GPU.getMask();
        }
        if (arguments.hasOption("beagle_SSE")) {
            beagleFlags |= BeagleFlag.PROCESSOR_CPU.getMask();
            beagleFlags |= BeagleFlag.VECTOR_SSE.getMask();
        }
        if (arguments.hasOption("beagle_double")) {
            beagleFlags |= BeagleFlag.PRECISION_DOUBLE.getMask();
        }
        if (arguments.hasOption("beagle_single")) {
            beagleFlags |= BeagleFlag.PRECISION_SINGLE.getMask();
        }

        if (arguments.hasOption("noerr")) {
		 	System.setErr(new PrintStream(new OutputStream() {
		 		public void write(int b) {
		 		}
		 	}));
        }        
        
        if (arguments.hasOption("beagle_order")) {
            System.setProperty("beagle.resource.order", arguments.getStringOption("beagle_order"));
        }

        if (arguments.hasOption("beagle_instances")) {
            System.setProperty("beagle.instance.count", Integer.toString(arguments.getIntegerOption("beagle_instances")));
        }

        if (arguments.hasOption("beagle_scaling")) {
            System.setProperty("beagle.scaling", arguments.getStringOption("beagle_scaling"));
        }

        if (arguments.hasOption("threads")) {
            threadCount = arguments.getIntegerOption("threads");
            if (threadCount < 0) {
                printTitle();
                System.err.println("The number of threads should be >= 0");
                System.exit(1);
            }
        }

        if (arguments.hasOption("seed")) {
            seed = arguments.getLongOption("seed");
            if (seed <= 0) {
                printTitle();
                System.err.println("The random number seed should be > 0");
                System.exit(1);
            }
        }

        int maxErrorCount = 0;
        if (arguments.hasOption("errors")) {
            maxErrorCount = arguments.getIntegerOption("errors");
            if (maxErrorCount < 0) {
                maxErrorCount = 0;
            }
        }

        BeastConsoleApp consoleApp = null;

        final String nameString = "BEAST " + version.getVersionString();

        if (window) {
            Utils.loadUIManager();
            System.setProperty("com.apple.macos.useScreenMenuBar", "true");
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.showGrowBox", "true");
            System.setProperty("beast.useWindow", "true");

            final javax.swing.Icon icon = IconUtils.getIcon(BeastMain.class, "images/beast.png");

            final String aboutString = "<html><div style=\"font-family:sans-serif;\"><center>" +
                    "<div style=\"font-size:12;\"><p>Bayesian Evolutionary Analysis Sampling Trees<br>" +
                    "Version " + version.getVersionString() + ", " + version.getDateString() + "</p>" +
                    version.getHTMLCredits() +
                    "</div></center></div></html>";

            consoleApp = new BeastConsoleApp(nameString, aboutString, icon);
        }

        printTitle();

        File inputFile = null;

        if (options) {

            final String titleString = "<html><center><p>Bayesian Evolutionary Analysis Sampling Trees<br>" +
                    "Version " + version.getVersionString() + ", " + version.getDateString() + "</p></center></html>";
            final javax.swing.Icon icon = IconUtils.getIcon(BeastMain.class, "images/beast.png");

            final BeastDialog dialog = new BeastDialog(new JFrame(), titleString, icon);

            if (!dialog.showDialog(nameString, seed)) {
                System.exit(0);
            }

//            if (dialog.allowOverwrite()) {
//                allowOverwrite = true;
//            }
            switch (dialog.getLogginMode()) {
                case 0:/* do not ovewrite */
                    break;
                case 1:
                    MCMCargs.add("-overwrite");
                    break;
                case 2:
                    MCMCargs.add("-resume");
                    break;
            }

            seed = dialog.getSeed();
            threadCount = dialog.getThreadPoolSize();

            useBeagle = dialog.useBeagle();
            if (useBeagle) {
                beagleShowInfo = dialog.showBeagleInfo();
                if (dialog.preferBeagleCPU()) {
                    beagleFlags |= BeagleFlag.PROCESSOR_CPU.getMask();
                }
                if (dialog.preferBeagleSSE()) {
                    beagleFlags |= BeagleFlag.VECTOR_SSE.getMask();
                }
                if (dialog.preferBeagleGPU()) {
                    beagleFlags |= BeagleFlag.PROCESSOR_GPU.getMask();
                }
                if (dialog.preferBeagleDouble()) {
                    beagleFlags |= BeagleFlag.PRECISION_DOUBLE.getMask();
                }
                if (dialog.preferBeagleSingle()) {
                    beagleFlags |= BeagleFlag.PRECISION_SINGLE.getMask();
                }
            }

            inputFile = dialog.getInputFile();
            if (!beagleShowInfo && inputFile == null) {
                System.err.println("No input file specified");
                System.exit(1);
            }

        } else {
            if (arguments.hasOption("overwrite")) {
                MCMCargs.add("-overwrite");
            }

            if (arguments.hasOption("resume")) {
                MCMCargs.add("-resume");
            }
        }

        if (beagleShowInfo) {
            BeagleInfo.printResourceList();
            System.exit(0);
        }

        if (inputFile == null) {

            final String[] args2 = arguments.getLeftoverArguments();

            if (args2.length > 1) {
                System.err.println("Unknown option: " + args2[1]);
                System.err.println();
                printUsage(arguments);
                System.exit(1);
            }

            String inputFileName = null;


            if (args2.length > 0) {
                inputFileName = args2[0];
                inputFile = new File(inputFileName);
            }

            if (inputFileName == null) {
                // No input file name was given so throw up a dialog box...
            	String fileName = getFileNameByDialog("BEAST " + version.getVersionString() + " - Select XML input file");
            	if (fileName == null) {
            		System.exit(0);
            	}
                inputFile = new File(fileName);
            }
        }

        if (inputFile != null && inputFile.getParent() != null && working) {
            System.setProperty("file.name.prefix", inputFile.getParentFile().getAbsolutePath());
        }

        if (window) {
            if (inputFile == null) {
                consoleApp.setTitle("null");
            } else {
                consoleApp.setTitle(inputFile.getName());
            }
        }

        if (useJava) {
            System.setProperty("java.only", "true");
        }

        if (arguments.hasOption("loglevel")) {
            String l = arguments.getStringOption("loglevel");
            switch (l) {
                case "error":
                    Log.setLevel(Log.Level.error);
                    break;
                case "warning":
                    Log.setLevel(Log.Level.warning);
                    break;
                case "info":
                    Log.setLevel(Log.Level.info);
                    break;
	            case "debug":
	                Log.setLevel(Log.Level.debug);
	                break;
	            case "trace":
	                Log.setLevel(Log.Level.trace);
	                break;
            }
        }

        if (fileNamePrefix != null && fileNamePrefix.trim().length() > 0) {
            System.setProperty("file.name.prefix", fileNamePrefix.trim());
        }

        if (stateFileName!= null && stateFileName.trim().length() > 0) {
            System.setProperty("state.file.name", stateFileName.trim());
            System.out.println("Writing state to file " + stateFileName);
        }

//        if (allowOverwrite) {
//            System.setProperty("log.allow.overwrite", "true");
//        }

        if (beagleFlags != 0) {
            System.setProperty("beagle.preferred.flags", Long.toString(beagleFlags));

        }

        if (threadCount > 0) {
            System.setProperty("thread.count", String.valueOf(threadCount));
            MCMCargs.add("-threads");
            MCMCargs.add(threadCount + "");
        }

        MCMCargs.add("-seed");
        MCMCargs.add(seed + "");
        Randomizer.setSeed(seed);

        System.out.println("Random number seed: " + seed);
        System.out.println();

        // Construct the beast object
        final BeastMCMC beastMCMC = new BeastMCMC();

        try {
            // set all the settings...
            MCMCargs.add(inputFile.getAbsolutePath());
            beastMCMC.parseArgs(MCMCargs.toArray(new String[0]));
            
            if (!doNotRun) {
            	new BeastMain(beastMCMC, consoleApp, maxErrorCount);
            } else {
            	Log.info.println("Done!");
            }
        } catch (RuntimeException rte) {
            if (window) {
                // This sleep for 2 seconds is to ensure that the final message
                // appears at the end of the console.
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println();
                System.out.println("BEAST has terminated with an error. Please select QUIT from the menu.");
            }
            // logger.severe will throw a RTE but we want to keep the console visible
        } catch (XMLParserException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        if (!window) {
            System.exit(0);
        }
    }
}


