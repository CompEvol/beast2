package beast.app.beauti2.util;

import javax.swing.*;
import java.awt.*;

/**
 * @author Andrew Rambaut
 * @version $Id$
 */
public class PanelUtils {
    public static JPanel createAddRemoveButtonPanel(Action addAction, Icon addIcon, String addToolTip,
                                                    Action removeAction, Icon removeIcon, String removeToolTip, int axis) {

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, axis));
        buttonPanel.setOpaque(false);
        JButton addButton = new JButton(addAction);
        if (addIcon != null) {
            addButton.setIcon(addIcon);
            addButton.setText(null);
        }
        addButton.setToolTipText(addToolTip);
        addButton.putClientProperty("JButton.buttonType", "toolbar");
        addButton.setOpaque(false);
        addAction.setEnabled(false);

        JButton removeButton = new JButton(removeAction);
        if (removeIcon != null) {
            removeButton.setIcon(removeIcon);
            removeButton.setText(null);
        }
        removeButton.setToolTipText(removeToolTip);
        removeButton.putClientProperty("JButton.buttonType", "toolbar");
        removeButton.setOpaque(false);
        removeAction.setEnabled(false);

        buttonPanel.add(addButton);
        buttonPanel.add(new JToolBar.Separator(new Dimension(6, 6)));
        buttonPanel.add(removeButton);

        return buttonPanel;
    }

    public static void setupComponent(JComponent comp) {
        comp.setOpaque(false);

        //comp.setFont(UIManager.getFont("SmallSystemFont"));
        //comp.putClientProperty("JComponent.sizeVariant", "small");
        if (comp instanceof JButton) {
            comp.putClientProperty("JButton.buttonType", "roundRect");
        }
        if (comp instanceof JComboBox) {
            comp.putClientProperty("JComboBox.isPopDown", Boolean.TRUE);
//            comp.putClientProperty("JComboBox.isSquare", Boolean.TRUE);
        }
    }


}
