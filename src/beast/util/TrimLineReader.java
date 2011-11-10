package beast.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.StringTokenizer;

/**
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @author Walter Xie
 */
public class TrimLineReader extends BufferedReader {

    public TrimLineReader(Reader reader) {
        super(reader);
    }

    public String readLine() throws IOException {
        lineNumber += 1;
        String line = super.readLine();
        if (line != null) return line.trim();
        return null;
    }

    public StringTokenizer tokenizeLine() throws IOException {
        String line = readLine();
        if (line == null) return null;
        return new StringTokenizer(line, "\t");
    }

    public StringTokenizer readTokensIgnoringEmptyLinesAndComments(String[] commentSigns) throws IOException {
        // Read through to first token
        StringTokenizer tokens = tokenizeLine();

        if (tokens == null) {
            throw new IOException("File is empty.");
        }

        // read over empty lines
        while (!tokens.hasMoreTokens()) {
            tokens = tokenizeLine();
        }

        // skip the first column which should be the state number
        String token = tokens.nextToken();

        // lines starting with [ are ignored, assuming comments in MrBayes file
        // lines starting with # are ignored, assuming comments in Migrate or BEAST file
        while (hasComment(commentSigns, token)) {
            tokens = tokenizeLine();

            // read over empty lines
            while (!tokens.hasMoreTokens()) {
                tokens = tokenizeLine();
            }
            // read state token and ignore
            token = tokens.nextToken();
        }

        return tokens;
    }

    private boolean hasComment(String[] commentSigns, String token) {
        for (String c : commentSigns) {
            if (token.startsWith(c)) return true;
        }
        return false;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    private int lineNumber = 0;
}


