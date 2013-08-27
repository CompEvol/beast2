package test.beast.beast2vs1.trace;


import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A class that stores a set of traces from a single chain
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @author Walter Xie
 */

public class LogFileTraces {
    protected final File file;
    protected final String name;

    private final List<String> tracesNameList = new ArrayList<String>();
    protected List<List<Double>> valuesList = new ArrayList<List<Double>>();
    private long burnIn = -1;
    private long firstState = -1;
    private long lastState = -1;
    private long stepSize = -1;

    public LogFileTraces(String name, File file) {
        this.name = name;
        this.file = file;
    }

    /**
     * @return the name of this traceset
     */
    public String getName() {
        return name;
    }

    public File getFile() {
        return file;
    }

    /**
     * @return the last state in the chain
     */
    public long getMaxState() {
        return lastState;
    }

    public boolean isIncomplete() {
        return false;
    }

    /**
     * @return the number of states excluding the burnin
     */
    public long getStateCount() {
        // This is done as two integer divisions to ensure the same rounding for
        // the burnin...
        return ((lastState - firstState) / stepSize) - (getBurnIn() / stepSize) + 1;
    }

    /**
     * @return the number of states in the burnin
     */
    public long getBurninStateCount() {
        return (getBurnIn() / stepSize);
    }

    /**
     * @return the size of the step between states
     */
    public long getStepSize() {
        return stepSize;
    }

    public long getBurnIn() {
        return burnIn;
    }

    /**
     * @return the number of traces in this traceset
     */
    public int getTraceCount() {
        return tracesNameList.size();
    }

    public void setBurnIn(long burnIn) {
        this.burnIn = burnIn;
    }


    public void loadTraces() throws TraceException, IOException {
        FileReader reader = new FileReader(file);
        loadTraces(reader);
        reader.close();
    }

    public void loadTraces(Reader r) throws TraceException, IOException {

        TrimLineReader reader = new TrimLineReader(r);

        // lines starting with [ are ignored, assuming comments in MrBayes file
        // lines starting with # are ignored, assuming comments in Migrate or BEAST file
        StringTokenizer tokens = reader.readTokensIgnoringEmptyLinesAndComments(new String[]{"[", "#"});

        // read label tokens
        String[] labels = new String[tokens.countTokens()];

        for (int i = 0; i < labels.length; i++) {
            labels[i] = tokens.nextToken();
            tracesNameList.add(labels[i]);
            valuesList.add(new ArrayList<Double>());
        }

        int traceCount = getTraceCount();

        boolean firstState = true;

        tokens = reader.tokenizeLine();
        while (tokens != null && tokens.hasMoreTokens()) {
            String stateString = tokens.nextToken();
            long state = 0;

            try {
                try {
                    // Changed this to parseDouble because LAMARC uses scientific notation for the state number
                    state = (long) Double.parseDouble(stateString);
                } catch (NumberFormatException nfe) {
                    throw new TraceException("Unable to parse state number in column 1 (Line " + reader.getLineNumber() + ")");
                }

                if (firstState) {
                    // MrBayes puts 1 as the first state, BEAST puts 0
                    // In order to get the same gap between subsequent samples,
                    // we force this to 0.
                    if (state == 1) state = 0;
                    firstState = false;
                }

                if (!addState(state)) {
                    throw new TraceException("State " + state + " is not consistent with previous spacing (Line " + reader.getLineNumber() + ")");
                }

            } catch (NumberFormatException nfe) {
                throw new TraceException("State " + state + ":Expected real value in column " + reader.getLineNumber());
            }

            for (int i = 0; i < traceCount; i++) {
                if (tokens.hasMoreTokens()) {
                    String value = tokens.nextToken();

                    try {
                        valuesList.get(i).add(Double.parseDouble(value));
                    } catch (NumberFormatException nfe) {
                        throw new TraceException("State " + state + ": Expected correct number type (Double) in column "
                                + (i + 1) + " (Line " + reader.getLineNumber() + ")");
                    }

                } else {
                    throw new TraceException("State " + state + ": missing values at line " + reader.getLineNumber());
                }
            }

            tokens = reader.tokenizeLine();
        }

        burnIn = (int) (0.1 * lastState);
    }

    /**
     * Add a state number for these traces. This should be
     * called before adding values for each trace. The spacing
     * between stateNumbers should remain constant.
     *
     * @param stateNumber the state
     * @return false if the state number is inconsistent
     */
    private boolean addState(long stateNumber) {
        if (firstState < 0) {
            firstState = stateNumber;
        } else if (stepSize < 0) {
            stepSize = stateNumber - firstState;
        } else {
            long step = stateNumber - lastState;
            if (step != stepSize) {
                return false;
            }
        }
        lastState = stateNumber;
        return true;
    }

    public TraceStatistics analyseTrace(int index) {
        int start = (int) (getBurnIn() / getStepSize());

        List<Double> values = valuesList.get(index).subList(start, valuesList.get(index).size());
        double[] doubleValues = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            doubleValues[i] = values.get(i);
        }

        return new TraceStatistics(doubleValues, getStepSize());
    }

    public String getTraceName(int i) {
        return tracesNameList.get(i);
    }

    public int getTraceIndex(String traceName) {
        return tracesNameList.indexOf(traceName);
    }
}

