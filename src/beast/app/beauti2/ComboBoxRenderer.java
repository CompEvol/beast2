package beast.app.beauti2;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * @author Andrew Rambaut
* @version $Id$
*/
public class ComboBoxRenderer extends JComboBox implements TableCellRenderer {
    private final boolean isListAll;

    public ComboBoxRenderer() {
        super();
        setOpaque(true);
        isListAll = false;
    }

    public <T> ComboBoxRenderer(T[] allValues) {
        super(allValues);
        setOpaque(true);
        isListAll = true;
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {

        if (isSelected) {
            this.setForeground(table.getSelectionForeground());
            this.setBackground(table.getSelectionBackground());
        } else {
            this.setForeground(table.getForeground());
            this.setBackground(table.getBackground());
        }

        if (isListAll) {
            setSelectedItem(value);
        } else {
            if (value != null) {
                removeAllItems();
                addItem(value);
            }
        }
        return this;
    }

    public void revalidate() {
    }

}
