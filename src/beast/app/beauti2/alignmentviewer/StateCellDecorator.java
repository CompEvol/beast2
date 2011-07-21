package beast.app.beauti2.alignmentviewer;

import java.awt.*;

/**
 * @author Andrew Rambaut
 * @version $Id: StateCellDecorator.java,v 1.2 2005/12/11 22:41:25 rambaut Exp $
 */
public class StateCellDecorator implements CellDecorator {

    public StateCellDecorator(StateDecorator stateDecorator, boolean inverted) {
        if (inverted) {
            foregroundDecorator = new StateDecorator() {
                public Paint getStatePaint(int stateIndex) { return Color.WHITE; }
            };
            backgroundDecorator = stateDecorator;
        } else {
            foregroundDecorator = stateDecorator;
            backgroundDecorator = new StateDecorator() {
                public Paint getStatePaint(int stateIndex) { return Color.WHITE; }
            };

        }
    }

    public Paint getCellForeground(int row, int column, int state) {
        return foregroundDecorator.getStatePaint(state);
    }

    public Paint getCellBackground(int row, int column, int state) {
        return backgroundDecorator.getStatePaint(state);
    }

    private final StateDecorator foregroundDecorator;
    private final StateDecorator backgroundDecorator;

}