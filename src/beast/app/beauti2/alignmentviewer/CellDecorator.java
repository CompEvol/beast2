package beast.app.beauti2.alignmentviewer;

import java.awt.*;

/**
 * @author Andrew Rambaut
 * @version $Id: CellDecorator.java,v 1.2 2005/11/11 16:40:41 rambaut Exp $
 */
public interface CellDecorator {
    Paint getCellForeground(int row, int column, int state);
    Paint getCellBackground(int row, int column, int state);
}
