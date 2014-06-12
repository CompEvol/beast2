package beast.app.treeannotator;

/**
 * @author Marc Suchard
 */

public interface ContourMaker {

    ContourPath[] getContourPaths(double level);

}
