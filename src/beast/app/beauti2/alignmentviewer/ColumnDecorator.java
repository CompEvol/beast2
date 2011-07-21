package beast.app.beauti2.alignmentviewer;

import java.awt.*;

/**
 * @author Andrew Rambaut
 * @version $Id: ColumnDecorator.java,v 1.1 2005/11/01 23:52:04 rambaut Exp $
 */
public interface ColumnDecorator {
    Paint getColumnBackground(int column);
}
